package com.smart_bdash.mobile.analytics.notification;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import android.text.TextUtils;
import android.util.Base64;

import java.io.File;

import com.smart_bdash.mobile.analytics.MobileSDKManager;
import com.smart_bdash.mobile.analytics.connect.ConnectClient;
import com.smart_bdash.mobile.analytics.connect.ConnectType;
import com.smart_bdash.mobile.analytics.connect.DownloadClient;
import com.smart_bdash.mobile.analytics.connect.IConnectAsyncResponse;
import com.smart_bdash.mobile.analytics.connect.IConnectClientController;
import com.smart_bdash.mobile.analytics.connect.factory.AbstractConnectControllerCreator;
import com.smart_bdash.mobile.analytics.connect.factory.ConnectClientControllerCreator;
import com.smart_bdash.mobile.analytics.model.Device;
import com.smart_bdash.mobile.analytics.model.JsonToken;
import com.smart_bdash.mobile.analytics.model.config.JsonKey;
import com.smart_bdash.mobile.analytics.model.config.SDKConfig;
import com.smart_bdash.mobile.analytics.util.DeviceUtil;
import com.smart_bdash.mobile.analytics.util.LogUtil;
import com.smart_bdash.mobile.analytics.util.LogicUtil;
import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <pre>
 * b->dash の通知処理をコントロールするコアクラス
 * </pre>
 */
public class BDashNotification {

    private static BDashNotification instance;
    private boolean isProcess;
    private MobileSDKManager sdkManager;
    private AbstractConnectControllerCreator connectControllerCreator = new ConnectClientControllerCreator();

    private final static String PREFERENCE_NAME        = "com.smart_bdash.mobile.analytics.notification";
    // 機密情報(FCM トークン)を暗号化保存する prefs. 平文の PREFERENCE_NAME とは分離する
    private final static String SECURE_PREFERENCE_NAME = "com.smart_bdash.mobile.analytics.notification.secure";
    private final static String PROPERTY_REG_ID        = "regId";
    private final static String PROPERTY_ENABLE        = "enable";        // ローカル視点での有効無効 / FCM サービスクラスの実行判定
    private final static String PROPERTY_SERVER_ENABLE = "serverEnable";  // サーバー視点での有効無効 / 通知をアプリに行うかの判定
    private final static String PROPERTY_APP_VERSION   = "version";
    private final static String PROPERTY_POPUP_USE_SOUND = "popupUseSound";
    private final static String PROPERTY_POPUP_USE_VIBRATION = "popupUseVib";

    private final static String TYPE_DIALOG = "dialog";

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainHandler = new Handler(Looper.getMainLooper());

    private ArrayList<NotificationStateListener> stateListener = new ArrayList<>();
    private PopupNotificationListener popupListener= null;

    // Activity を起動したときにプッシュ通知で送られてくる任意パラメーターを取得するキー名
    /** プッシュ通知からアプリを起動したときのプッシュパラメーターを取得するキー名 */
    public final static String LAUNCH_TITLE = "bdash_launch_title";
    public final static String LAUNCH_MESSAGE = "bdash_launch_message";
    public final static String LAUNCH_CUSTOM_PAYLOAD = "bdash_launch_custom_payload";
    public final static String LAUNCH_DID = "bdash_launch_did";
    public final static String LAUNCH_BDID = "bdash_launch_bdid";
    public final static String LAUNCH_JP_CO_DATAX = "bdash_launch_jp_co_datax";
    public final static String LAUNCH_IMAGE = "bdash_launch_image";
    public final static String LAUNCH_TYPE  = "bdash_launch_type";

    // FCM legacy
    public final static String LAUNCH_MESSAGE_ID = "bdash_launch_message_id";
    public final static String LAUNCH_EXTRA_PARAM  = "bdash_launch_param";
    public final static String LAUNCH_EXTRA_NOTIFICATION_PARAM = "bdash_launch_notification_param";
    public final static String LAUNCH_ID = "bdash_launch_id";
    public final static String LAUNCH_EXTRA_MAIN_PARAM = "bdash_launch_main_param";
    public final static String LAUNCH_EXTRA_SUB_PARAM = "bdash_launch_sub_param";

    /** プッシュ通知からアプリを起動したかどうかを取得するキー名 */
    public final static String LAUNCH_NOTIFICATION = "bdash_launch_notification";

    private final static String CACHE_NAME = "com.smart_bdash.mobile.analytics.cache";

    /* register/cancel の最後に呼ばれる処理 */
    private final Runnable processEnd = new Runnable() {
        @Override
        public void run() {
            synchronized (BDashNotification.this) {
                isProcess = false;
            }
        }
    };

    /* Hook 用のレスポンス. テストクラスで上書きされる事がある */
    private IConnectAsyncResponse hookResponse = new IConnectAsyncResponse() {
        @Override
        public void onConnect(ConnectClient client) throws Exception {
        }

        @Override
        public void onPostExecuteImpl(ConnectClient client, Throwable exception) throws Exception {
        }
    };

    /* バイブレーション **/
    private static Vibrator vib;
    interface BaseNotificationStateListener {
        public void onFCMStart();
        public void onFCMErrorNotInitialized();
        public void onEnable();
        public void onDisable();
        public void onError( int errType, Throwable exception );
    }

    /**
     * b->dash プッシュ通知API の ON/OFF リクエストの成否を監視するリスナー
     */
    public static abstract class NotificationStateListener implements BaseNotificationStateListener{
        /** FCM でエラーが発生. v17.0.1以降は未使用 */
        public static final int ERROR_FCM       = 0;
        /** FCM の準備ができていない */
        public static final int ERROR_FCM_READY = 1;
        /** 通信エラー */
        public static final int ERROR_CONNECT   = 2;

        public void onFCMStart(){}
        public void onFCMErrorNotInitialized(){}
    }

    private final EventListener eventListener = new EventListener() {
        @Override
        public boolean onRegistered(Context context, String registrationId) {
            LogUtil.s("[b-dash] >>onRegistered");

            // サーバーに送信
            boolean result = sendRegistrationIdToBackend(registrationId);
            if( result ){
                // 通知受付状態にする
                setServerEnableNotification(true);
            }
            return result;
        }
    };

    /**
     * 割り込み通知のイベントリスナー
     */
    public static abstract class PopupNotificationListener {
        public void onClick01(Intent intent){}
        public void onClick02(Intent intent){}
    }

    /***
     * コンストラクタ
     * @param context
     */
    protected BDashNotification(Context context){
        init(context);
    }

    /***
     * インスタンス取得
     * @param context
     * @return
     */
    public static synchronized BDashNotification getInstance( Context context ) {
        if( instance == null ) {
            instance = new BDashNotification(context);
        }
        return instance;
    }

    /***
     * 初期化処理
     * @param context
     */
    private void init(Context context){
        // 必ず、公開外部クラスのイニシャライズ処理の先頭に記載
        sdkManager = MobileSDKManager.getInstance_needCreate(context.getApplicationContext(), null);

        // SDK v2.1.0 以降では Android Oreo 以降は指定されたチャンネルを作成する
        try {
            createChannelByManifest();
        }catch( Throwable e ) {
            // 指定していない場合はここに例外が来る. 無視して良い例外のため特に処理を行わない
        }
    }

    /***
     * 通知状態を受け取るリスナーを追加します。
     * @param listener
     */
    public void addStateListener( NotificationStateListener listener ){
        if( listener!=null ) {
            synchronized (stateListener) {
                stateListener.add(listener);
            }
        }
    }

    /**
     * 割り込み通知のイベントを受け取るリスナーを追加します。
     * @param listener
     */
    public void setPopupListener( PopupNotificationListener listener ){
        synchronized (this) {
            popupListener = listener;
        }
    }

    /**
     * 割り込み通知の1つ目ボタン（デフォルトOKボタン）のコールバック
     * @param intent
     */
    public void call_onClickPopup01(Intent intent) {
        if (popupListener != null) {
            popupListener.onClick01(intent);
        }
    }

    /**
     * 割り込み通知の二つ目ボタン（デフォルト閉じるボタン）のコールバック
     * @param intent
     */
    public void call_onClickPopup02(Intent intent){
        if(popupListener != null){
            popupListener.onClick02(intent);
        }
    }

    /***
     * 通知状態を受け取るリスナーを削除します。
     * @param listener
     */
    public void removeStateListener( NotificationStateListener listener ){
        if( listener!=null ) {
            synchronized (stateListener) {
                stateListener.remove(listener);
            }
        }
    }

    /***
     * 通知状態を受け取るリスナーをすべて削除します。
     */
    public void clearStateListener(){
        synchronized (stateListener) {
            stateListener.clear();
        }
    }

    /***
     * 通知トークンの有効化の一連の処理を開始します。トークンの内部保持後に利用する必要があります。
     * @throws BDashBusyException 既に通知ON/OFFの同期が走っている最中は、この例外がスローされます
     */
    public void registerNotification() throws Exception {
        prepare(true);
    }

    /***
     * 通知トークンの無効化の一連の処理を開始します。
     * @throws BDashBusyException  既に通知ON/OFFの同期が走っている最中は、この例外がスローされます
     */
    public void cancelNotification() throws Exception {
        prepare(false);
    }

    /**
     * お客様側でプッシュ通知を表示したい場合に呼び出す処理
     * Firebaseから受け取ったBundleを引数とする
     * @param bundle
     */
    public void displayNotifyMessage(Bundle bundle){
        LogUtil.s( "[b-dash] >>displayNotifyMessage");

        // BundleからMapを生成
        Map<String, String> data;
        String fcmApi = bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_FCM_API);
        if (SDKConfig.APP_BDASH_FCM_API_V1.equals(fcmApi)) {
            // FCMのAPIがHTTPv1の場合
            LogUtil.s("FCM API:HTTPv1");
            data = makePushPayloadData(bundle);
        } else {
            // FCMのAPIがレガシーの場合
            LogUtil.s("FCM API:Legacy");
            data = makeLegacyPushPayloadData(bundle);
        }

        String property_message = data.get(SDKConfig.APP_BDASH_NOTIFICATION_BODY);

        if(isFilteringNotifyMessage()){
            return;
        }

        // 本文メッセージが指定されてるときのみプッシュ通知を行う
        if( property_message!=null ) {
            notifyMessage(NotificationMessage.decode(data), new BDashPopupCustomOption());
        }
        logNotifyMessage(data);
    }

    /**
     * お客様側でプッシュ通知を表示したい場合に呼び出す処理(割り込み通知のオーバーレイ有無を変更可能)
     * Firebaseから受け取ったBundleを引数とする
     * @param bundle
     * @param useOverlay
     */
    public void displayCustomNotifyMessage(Bundle bundle, Boolean useOverlay){
        LogUtil.s( "[b-dash] >>displayCustomNotifyMessage(overlay)");

        // BundleからMapを生成
        Map<String, String> data;
        String fcmApi = bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_FCM_API);
        if (SDKConfig.APP_BDASH_FCM_API_V1.equals(fcmApi)) {
            // FCMのAPIがHTTPv1の場合
            LogUtil.s("FCM API:HTTPv1");
            data = makePushPayloadData(bundle);
        } else {
            // FCMのAPIがレガシーの場合
            LogUtil.s("FCM API:Legacy");
            data = makeLegacyPushPayloadData(bundle);
        }

        String property_message = data.get(SDKConfig.APP_BDASH_NOTIFICATION_BODY);

        if(isFilteringNotifyMessage()){
            return;
        }

        // 本文メッセージが指定されてるときのみプッシュ通知を行う
        if( property_message!=null ) {
            BDashPopupCustomOption option = new BDashPopupCustomOption(useOverlay);
            notifyMessage(NotificationMessage.decode(data), option);
        }
        logNotifyMessage(data);
    }

    /**
     * お客様側でプッシュ通知を表示したい場合に呼び出す処理(割り込み通知のオプション付き)
     * Firebaseから受け取ったBundleを引数とする
     * @param bundle
     * @param option
     */
    public void displayCustomNotifyMessage(Bundle bundle, BDashPopupCustomOption option){
        LogUtil.s( "[b-dash] >>displayCustomNotifyMessage");

        // BundleからMapを生成
        Map<String, String> data;
        String fcmApi = bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_FCM_API);
        if (SDKConfig.APP_BDASH_FCM_API_V1.equals(fcmApi)) {
            // FCMのAPIがHTTPv1の場合
            LogUtil.s("FCM API:HTTPv1");
            data = makePushPayloadData(bundle);
        } else {
            // FCMのAPIがレガシーの場合
            LogUtil.s("FCM API:Legacy");
            data = makeLegacyPushPayloadData(bundle);
        }

        String property_message = data.get(SDKConfig.APP_BDASH_NOTIFICATION_BODY);

        if(isFilteringNotifyMessage()){
            return;
        }

        // 本文メッセージが指定されてるときのみプッシュ通知を行う
        if( property_message!=null ) {
            notifyMessage(NotificationMessage.decode(data), option);
        }
        logNotifyMessage(data);
    }

    /**
     * BundleからMapを生成(FCMのAPIがレガシーからHTTPv1に変更された影響)
     * @return Map<String, String>
     */
    private Map<String, String> makePushPayloadData(Bundle bundle) {
        Map<String, String> data = new HashMap<>();
        data.put(SDKConfig.APP_BDASH_NOTIFICATION_TITLE,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_TITLE));
        data.put(SDKConfig.APP_BDASH_NOTIFICATION_BODY,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_BODY));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_CUSTOM_PAYLOAD,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_CUSTOM_PAYLOAD));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_DID,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_DID));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_BDID,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_BDID));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_JP_CO_DATAX,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_JP_CO_DATAX));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_FCM_API,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_FCM_API));
        data.put(SDKConfig.APP_BDASH_NOTIFICATION_IMAGE,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_IMAGE));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_NOTIFICATION_TYPE_ANDROID,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_NOTIFICATION_TYPE_ANDROID));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_MESSAGE_ID,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_MESSAGE_ID));
        return data;
    }

    /**
     * BundleからMapを生成(FCMのAPIがレガシーの場合、こちらのメソッドを利用する)
     * @return Map<String, String>
     */
    private Map<String, String> makeLegacyPushPayloadData(Bundle bundle) {
        Map<String, String> data = new HashMap<>();
        data.put(SDKConfig.APP_BDASH_NOTIFICATION_TITLE,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_TITLE_LEGACY));
        data.put(SDKConfig.APP_BDASH_NOTIFICATION_BODY,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_MESSAGE));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_PARAM,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_PARAM));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_NOTIFICATION_TYPE_ANDROID,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_NOTIFICATION_TYPE_ANDROID));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_MESSAGE_ID,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_MESSAGE_ID));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_ID,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_ID));
        data.put(SDKConfig.APP_BDASH_NOTIFICATION_IMAGE,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_MEDIA_URL));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_JP_CO_DATAX,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_JP_CO_DATAX));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_NOTIFICATION_PARAM,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_NOTIFICATION_PARAM));
        data.put(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_BUTTONS,
                bundle.getString(SDKConfig.APP_BDASH_FCM_PAYLOAD_KEY_BUTTONS));
        return data;
    }

    /**
     * oreo 未満の判定 無効化時にきてしまった通知の握り潰し
     * @return
     */
    private Boolean isFilteringNotifyMessage() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O ){
            // oreo 以降は通知チャンネルで制御したいためサーバーと同期ができていなくても通知を受信する
            if( !isServerEnableNotification() ) {
                LogUtil.s( "通知を受信しましたが server では無効化しているため表示しません");
                return true;
            }
        }
        return false;
    }

    /**
     * debug 通知時出力用
     * @param data
     */
    private void logNotifyMessage(Map<String, String> data) {
        try {
            if( LogUtil.isDebuggable() ) {
                Iterator<String> ite = data.keySet().iterator();
                while (ite.hasNext()) {
                    String key = ite.next();
                    String val = data.get(key);
                    LogUtil.s(key + ":" + val);
                }
            }
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    /***
     * 通知が登録されているかを確認する( サーバーと同期を取った時点.データ削除や root 化があるため、厳密ではない )
     * @return true: 通知登録されている
     */
    public boolean isRegisterNotification(){
        String id = getRegistrationId();
        if( id==null ) {
            return false;
        }
        return isServerEnableNotification();
    }


    /***
     * 現在 register/cancel 処理中かを返す
     * @return
     */
    public boolean isProcessing (){
        return isProcess;
    }

    /***
     * サーバーへの通知ON/OFF同期をする前の下準備(ここで通知ONにするか通知OFFにするかを判断する)
     * @param enable
     */
    private void prepare( boolean enable) throws Exception{
        LogUtil.s("[b-dash] >>prepare: " + enable + "  isProcess:" + isProcess);

        synchronized (BDashNotification.this) {
            if (isProcess) {
                throw new BDashBusyException();
            }
            isProcess = true;
        }

        // store に反映
        setEnableNotification(enable);

        if( enable ) {
            // 有効化する
            String regId = getRegistrationId();
            LogUtil.s("[b-dash] >>current regId exists: " + (regId != null));

            requestTokenRefresh(sdkManager.getContext());
        } else {
            // 無効化する
            _requestTokenDisable();
        }
    }

    /***
     * 通知ON/OFFステータスを保存
     * @param enable
     */
    private void setEnableNotification( boolean enable ){
        final SharedPreferences prefs = getPreferences(sdkManager.getContext());
        prefs.edit().putBoolean(PROPERTY_ENABLE, enable).apply();
    }

    /***
     * 保持中の通知ON/OFFステータスを取得
     * @return
     */
    private boolean isEnableNotification(){
        final SharedPreferences prefs = getPreferences(sdkManager.getContext());
        return prefs.getBoolean(PROPERTY_ENABLE, false);
    }

    /***
     * 通知ON/OFFをサーバーへ通知済みかどうかのステータスを保存
     * @param enable
     */
    private void setServerEnableNotification( boolean enable ){
        final SharedPreferences prefs = getPreferences(sdkManager.getContext());
        prefs.edit().putBoolean(PROPERTY_SERVER_ENABLE, enable).apply();
    }

    /***
     * 通知ON/OFFをサーバーへ通知済みかどうかのステータスを取得
     * @return
     */
    private boolean isServerEnableNotification(){
        final SharedPreferences prefs = getPreferences(sdkManager.getContext());
        return prefs.getBoolean(PROPERTY_SERVER_ENABLE, false);
    }

    /**
     * 割り込み通知上でのサウンド設定を行う
     * @param enable
     */
    public void setEnablePopupSound(boolean enable){
        final SharedPreferences prefs = getPreferences(sdkManager.getContext());
        prefs.edit().putBoolean(PROPERTY_POPUP_USE_SOUND, enable).apply();
    }

    /**
     * 割り込み通知上でのバイブレーション設定を行う
     * @param enable
     */
    public void setEnablePopupVibration(boolean enable){
        final SharedPreferences prefs = getPreferences(sdkManager.getContext());
        prefs.edit().putBoolean(PROPERTY_POPUP_USE_VIBRATION, enable).apply();
    }

    /**
     * 割り込み通知上でサウンドをならすかどうかの取得
     * @return
     */
    public boolean isEnablePopupSound(){
        final SharedPreferences prefs = getPreferences(sdkManager.getContext());
        boolean result = prefs.getBoolean(PROPERTY_POPUP_USE_SOUND, false);
        return result;
    }

    /**
     * 割り込み通知上でバイブレーションするかどうかの取得
     * @return
     */
    public boolean isEnablePopupVibration(){
        final SharedPreferences prefs = getPreferences(sdkManager.getContext());
        boolean result = prefs.getBoolean(PROPERTY_POPUP_USE_VIBRATION, false);
        return result;
    }

    /***
     * 現在ストレージに保存されている FCM トークンを返す
     * @return Token 通知が無効のとき null が返却される
     */
    public String getRegistrationId() {
        Context context = MobileSDKManager.getInstance().getContext();
        final SharedPreferences prefs = getSecurePreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");

        // 旧バージョンの平文 prefs にトークンが残っていれば暗号化 prefs へ移行する
        if (registrationId.isEmpty()) {
            String legacyId = getPreferences(context).getString(PROPERTY_REG_ID, "");
            if (!legacyId.isEmpty()) {
                prefs.edit().putString(PROPERTY_REG_ID, legacyId).commit();
                getPreferences(context).edit().remove(PROPERTY_REG_ID).commit();
                registrationId = legacyId;
            }
        }

        if (registrationId.isEmpty()) {
            return null;
        }
        return registrationId;
    }

    /***
     * SharedPreferenceにてデータの保存先を名前(PREFERENCE_NAME)で指定
     * @param context
     * @return
     */
    private SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
    }

    /***
     * FCM トークン等の機密情報を暗号化保存する SharedPreferences を取得する.<br>
     *  ・Android Keystore で管理する鍵で暗号化されるため、Root 化端末でファイルを読み取られても値は保護される<br>
     *  ・Keystore の不整合等で生成・復号に失敗した場合は、破損ファイルを削除して再生成する
     * @param context
     * @return 暗号化された SharedPreferences
     */
    private SharedPreferences getSecurePreferences(Context context) {
        try {
            return createEncryptedPreferences(context);
        } catch (Exception e) {
            LogUtil.out(e);
            deleteSecurePreferencesFile(context);
            try {
                return createEncryptedPreferences(context);
            } catch (Exception e2) {
                LogUtil.out(e2);
                throw new RuntimeException(e2);
            }
        }
    }

    private SharedPreferences createEncryptedPreferences(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context,
                SECURE_PREFERENCE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    private void deleteSecurePreferencesFile(Context context) {
        try {
            File prefFile = new File(context.getApplicationInfo().dataDir
                    + "/shared_prefs/" + SECURE_PREFERENCE_NAME + ".xml");
            if (prefFile.exists()) {
                prefFile.delete();
            }
        } catch (Exception ignore) {
            // 削除に失敗しても致命的ではない
        }
    }

    /***
     * お客様側のFCMで発行したトークンをSDK側で内部保持するためのメソッド
     * サーバー同期前にお客様側でこのメソッドを呼び出してもらう
     * @param context
     * @param regId
     */
    public void storeFCMToken(Context context, String regId){
        // トークンは暗号化 prefs、機密でないアプリバージョンは平文 prefs に分けて保存する
        getSecurePreferences(context).edit().putString(PROPERTY_REG_ID, regId).commit();

        int appVersion = DeviceUtil.getVersionCode(context);
        getPreferences(context).edit().putInt(PROPERTY_APP_VERSION, appVersion).commit();
    }


    /**
     * 通知設定をOFFにしたことをb->dashサーバーへと同期する
     * サーバーとの通知OFF同期成功 -> NotificationStateListener.onDisable
     * 同期の失敗 -> NotificationStateListener.onError
     */
    private void _requestTokenDisable() {
        executor.execute(() -> {
            LogUtil.s("[b-dash] >>> _requestTokenDisable run");
            // サーバーに通信
            boolean result = sendDisableNotificationToBackend();

            mainHandler.post(() -> {
                synchronized (stateListener) {
                    if (result) {
                        setServerEnableNotification(false);
                        LogUtil.s("[b-dash] >>> 通知を無効化しました");
                    }
                    processEnd.run();  // 一連の処理の終端

                    LogUtil.s("[b-dash] >>> _requestTokenDisable end");
                    if(result) {
                        notifySuccessListener(false);
                    } else {
                        notifyFailedListener(NotificationStateListener.ERROR_CONNECT, null);
                    }

                }
            });
        });
    }

    /***
     * トークンをリフレッシュする
     * @param context
     */
    void requestTokenRefresh(final Context context) {
        _requestTokenRefresh(context);
    }

    /***
     * お客様側でトークン同期を行ってもらう際(registerNotification)の実際の同期処理をまとめたメソッド
     * サーバーとの通知ON成功 -> NotificationStateListener.onEnable
     * 同期の失敗 -> NotificationStateListener.onError
     * 
     * @param context
     */
    void _requestTokenRefresh(final Context context) {

        final EventListener listener = getListener();
        if (listener == null) return;

        executor.execute(() -> {
            LogUtil.s("[b-dash] >>> requestTokenRefresh run");

            String regId = getRegistrationId();

            //トークンが内部保持されていない時点でトークン同期はキャンセルする
            if (regId == null) {
                LogUtil.s("[b-dash] >>token failed.");
                mainHandler.post(() -> {
                    processEnd.run();
                    setEnableNotification(false); // 自動通知有効化を無効にする
                    // トークンを内部保持する前に同期を走らせようとした
                    notifyFailedListener(NotificationStateListener.ERROR_FCM_READY, null);
                    notifyFCMInitErrorListener();
                });
            }

            boolean isSuccess = listener.onRegistered(context, regId);

            mainHandler.post(() -> {
                LogUtil.s("[b-dash] >>requestTokenRefresh end");
                processEnd.run();

                if (isSuccess) {
                    notifySuccessListener(true);
                } else {
                    notifyFailedListener(NotificationStateListener.ERROR_CONNECT, new BDashNativeSyncException());
                }
            });
        });
    }

    /***
     * トークンの有効化 / BDash サーバーにトークンID を通知する
     */
    private boolean sendRegistrationIdToBackend( String regId ) {
        LogUtil.s("[sendServer] FCM Token を取得しました");

        return requestTokenAPI(regId);
    }

    /***
     * トークンの無効化 / BDash サーバーに無効化をリクエストする
     */
    private boolean sendDisableNotificationToBackend() {
        LogUtil.s("[sendServer] 通知を無効化します");
        return requestTokenAPI(null);
    }


    /**
     * プッシュ通知ON/OFFの設定をサーバーへと同期するAPI
     * プッシュ通知OFFの際はサーバーにトークンをnullで渡すが、内部保持はそのままにする
     * @param notificationId
     * @return
     */
    private boolean requestTokenAPI( String notificationId ) {
        JsonToken token = JsonKey.createJsonTokenFields();
        token.notificationId = notificationId;
        try {
            String request = LogicUtil.createJsonRequestByJsonToken(token);
            // FCMトークン(notificationId)等を含むため、秘匿キーをマスクして出力する
            LogUtil.s(LogUtil.maskJson(request));


            IConnectClientController con = connectControllerCreator.create();
            ConnectClient client = con.connect(new IConnectAsyncResponse() {
                @Override
                public void onConnect(ConnectClient client) throws Exception {
                }

                @Override
                public void onPostExecuteImpl(ConnectClient client, Throwable exception) throws Exception {
                    if( hookResponse!=null ){
                        hookResponse.onPostExecuteImpl(client, exception);
                    }
                    if (exception != null) {
                        LogUtil.s("exception: " + exception.toString());
                        LogUtil.s("code: " + client.getResponseCode());
                    }
                    LogUtil.s(">>response");
                    // レスポンス本文は秘匿情報を含みうるため、文字数のみ出力する
                    LogUtil.s(LogUtil.maskData(client.getResponse()));
                }
            }, ConnectType.API_TOKEN_POST, request);
            con.waitConnect();

            // 200 以外は失敗とみなす
            if( client.getResponseCode() != ConnectClient.SUCCESS ){
                LogUtil.s(">>resultCode: " + client.getResponseCode() );
                // error
                return false;
            }
        } catch( Exception e ) {
            return false;
        }

        return true;
    }


    private void notifyFailedListener(int errType, Throwable exception) {
        notifyListener(false, false, errType, exception);
    }

    private void notifySuccessListener( boolean is_enable ) {
        notifyListener(true, is_enable, -1, null);
    }
    /***
     * アプリ側(NotificationStateListener)へ結果を通知
     * @param success
     * @param is_enable
     */
    private void notifyListener( boolean success, boolean is_enable, int errorType, Throwable exception ) {
        synchronized (stateListener) {
            if (stateListener.size() == 0) return;

            try {
                for( int i=0 ; i<stateListener.size() ; i++ ) {
                    if (success) {
                        if (is_enable) {
                            stateListener.get(i).onEnable();
                        } else {
                            stateListener.get(i).onDisable();
                        }
                    } else {
                        stateListener.get(i).onError(errorType, exception);
                    }
                }
            } catch (Throwable e) {
                // 呼び出し先でのクラッシュ. SDK としては exception を出すのみ.
                e.printStackTrace();
            }

        }
    }
    private void notifyFCMStartListener() {
        notifyFCM_TrueStart_FalseError(true);
    }

    private void notifyFCMInitErrorListener() {
        notifyFCM_TrueStart_FalseError(false);
    }

    private void notifyFCM_TrueStart_FalseError( boolean isStart) {
        synchronized (stateListener) {
            if (stateListener.size() == 0) return;

            try {
                for( int i=0 ; i<stateListener.size() ; i++ ) {
                    if (isStart) {
                        stateListener.get(i).onFCMStart();
                    } else {
                        stateListener.get(i).onFCMErrorNotInitialized();
                    }
                }
            } catch (Throwable e) {
                // 呼び出し先でのクラッシュ. SDK としては exception を出すのみ.
                e.printStackTrace();
            }
        }
    }

    /**
     * リスナー(EventListener)のインスタンスを取得
     * @return
     */
    EventListener getListener() {
        return eventListener;
    }


    /***
     * Launch 対象としている Activity を取得
     * @return
     */
    private static String getMainActivityName(){
        ActivityInfo[] activities = null;
        Context context = MobileSDKManager.getInstance().getContext();
        PackageManager pm = context.getPackageManager();
        try {
            // 当アプリの全アクティビティを取得
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            activities = packageInfo.activities;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(activities.length < 1) return null;
        // 端末の全アプリのACTION_MAINで起動できる全アクティビティを取得
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfo = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);

        // 全メインアクティビティ内から当アプリのメインアクティビティを探す
        for(int i = 0; i < resolveInfo.size(); i++) {
            String activityName = resolveInfo.get(i).activityInfo.name;
            for(int j = 0; j < activities.length; j++){
                String name = activities[j].name;
                if(activityName.equals(name)) return name;
            }
        }
        return null;
    }

    /**
     * 本メソッドのテストは検証用アプリで実施している
     * @param str <pre>
     *     high: NotificationManager.IMPORTANCE_HIGH
     *     default: NotificationManager.IMPORTANCE_DEFAULT
     *     low: NotificationManager.IMPORTANCE_LOW
     *     min: NotificationManager.IMPORTANCE_MIN
     *     none: NotificationManager.IMPORTANCE_NONE
     *     unspecified: NotificationManager.IMPORTANCE_UNSPECIFIED
     * </pre>
     * @return NotificationManager.IMPORTANCE_XXX
     */
    private static int stringToChannelImportanceValue( String str ) throws Exception {
        if (str != null) {
            str = str.toLowerCase(Locale.ROOT);
            switch (str) {
                case "high":
                    return NotificationManager.IMPORTANCE_HIGH;
                case "default":
                    return NotificationManager.IMPORTANCE_DEFAULT;
                case "low":
                    return NotificationManager.IMPORTANCE_LOW;
                case "min":
                    return NotificationManager.IMPORTANCE_MIN;
                case "none":
                    return NotificationManager.IMPORTANCE_NONE;
                case "unspecified":
                    return NotificationManager.IMPORTANCE_UNSPECIFIED;
            }
        }
        // 未定義は今までと互換性のある high を返す
        return NotificationManager.IMPORTANCE_HIGH;
    }


    /**
     * デバイス情報を元に通知チャンネルを作成する
     * @throws Throwable
     */
    private void createChannelByManifest()throws Throwable{
        createChannel(
                 Device.getInstance().channel_id
                ,Device.getInstance().channel_name
                ,Device.getInstance().channel_desc
                ,Device.getInstance().channel_badge
                ,stringToChannelImportanceValue(Device.getInstance().channel_imp)
                );
    }

    /**
     * Android Oreo(API26)以降のデバイスにて通知チャンネルを作成する
     * @param channelId チャンネルID
     * @param name 名称
     * @param description 説明文
     * @param showBadge バッジ表示フラグ
     * @param importance 重要度
     * @throws Throwable
     */
    public void createChannel( String channelId, String name, String description, boolean showBadge, int importance )throws Throwable{
        // oreo 以降
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            if( channelId==null || channelId.length()==0 ) {
                throw new Exception("channel id is empty.");
            }

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            // オプション設定
            if( description!=null ) channel.setDescription(description);

            // 必須設定
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setVibrationPattern(getVibrationPattern());
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.setSound(defaultSoundUri, null);
            channel.setShowBadge(showBadge); // ランチャー上でアイコンバッジを表示するかどうか
            try {

                Context context = MobileSDKManager.getInstance().getContext();
                NotificationManager nm = context.getSystemService(NotificationManager.class);
                nm.createNotificationChannel(channel);

                LogUtil.s( ">>sdk createChannel name: " + name + " ID: " + channelId );
                LogUtil.s( ">>    badge: " + showBadge + " imp:" + importance );
            }catch( Throwable e ) {
                e.printStackTrace();
                throw e;
            }
        } else{
            throw new Exception("channel api is require version's oreo.");
        }
    }

    /**
     * 指定したIDの通知チャンネルを削除する
     * @param channelId
     */
    public void deleteChannel( String channelId ) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            Context context = MobileSDKManager.getInstance().getContext();
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            nm.deleteNotificationChannel(channelId);
            LogUtil.s( ">>sdk deleteChannel");
        }
    }


    /***
     * 通常のプッシュ通知(リッチプッシュ通知)を送るかb->dash特有の割り込み通知を送るかの判別を行う
     * @param message
     * @param option
     */
    private void notifyMessage( final NotificationMessage message, BDashPopupCustomOption option) {
        if(message.notification_type_android.equals(BDashNotification.TYPE_DIALOG) && isForeground(sdkManager.getContext())) {
            // 独自のリッチ通知
            notifyMessageSyncWithRichUI(message, MobileSDKManager.getInstance().getContext(), option);
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    notifyMessageSync(message);
                }
            }, "NotificationThread");
            thread.start();
        }
    }

    /***
     * 通知メッセージを同期して送る
     * @param message
     */
    private void notifyMessageSync( final NotificationMessage message ) {

        Context context = MobileSDKManager.getInstance().getContext();


        // 画像が指定されている場合
        ImagePack normalIcon = null;
        ImagePack bigIcon = null;

        // アイコンがあるとき
        normalIcon = resource2Bitmap(context, Device.getInstance().notification_icon, false);
        if( normalIcon!= null ){
            LogUtil.s( "icon が指定されています。 " + Device.getInstance().notification_icon );
            LogUtil.s( String.format(" >>id: %d", normalIcon.resourceId) );
        }

        bigIcon = resource2Bitmap(context, Device.getInstance().notification_lollipop_bigIcon, true);
        if (bigIcon != null) {
            LogUtil.s("big icon が指定されています。 " + Device.getInstance().notification_lollipop_bigIcon);
            LogUtil.s(String.format(" >>id: %d", bigIcon.resourceId));
        }

        // メッセージID
        int requestId = 1;
        if( message.message_id >0 ) {
            requestId = message.message_id;
        }

        if( message.body == null ) {
            LogUtil.s( "message is empty");
        }
        if( message.title == null ) {
            LogUtil.s( "title is empty");
        }

        try {
            NotificationCompat.Builder builder;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                builder = new NotificationCompat.Builder(context, Device.getInstance().channel_id);
            }else {
                //noinspection deprecation
                builder = new NotificationCompat.Builder(context);
            }

            if (normalIcon != null) {
                // アイコンが指定されていたとき
                builder.setSmallIcon(normalIcon.resourceId);
            } else {
                LogUtil.s( "small icon is app default.");
                // 組み込みのアプリアイコンを利用する
                builder.setSmallIcon(getNotificationSmallIcon(context));
            }
            String accent_color = Device.getInstance().notification_icon_accent_color;
            if(  accent_color != null ){
                try{
                    int color = Integer.parseInt(accent_color);
                    builder.setColor(color);
                    LogUtil.s(String.format(">>set accent color: 0x%x", color));
                } catch( Exception e ) {
                    e.printStackTrace();
                }
            }


            if (bigIcon != null) {
                // ビッグアイコンが指定されていたとき
                builder.setLargeIcon(bigIcon.image);
            }

            // カスタムペイロードをデコード
            if (message.custom_payload != null && !message.custom_payload.isEmpty()) {
                message.custom_payload = new String(Base64.decode(message.custom_payload, Base64.DEFAULT));
            }

            if( message.title!=null )builder.setContentTitle(message.title); // 1行目
            if( message.body!=null )builder.setContentText(message.body); // 2行目
            builder.setWhen(System.currentTimeMillis()); // タイムスタンプ（現在時刻、メール受信時刻、カウントダウンなどに使用）
            builder.setAutoCancel(true);

            // 起動アクティビティ
            try {
                if (Device.getInstance().notification_launchActivity != null) {
                    // 例外が発生する可能性有り
                    builder.setContentIntent(createPendingIntent(context, Device.getInstance().notification_launchActivity, requestId, message));
                } else {
                    // 例外の可能性は基本は無い
                    builder.setContentIntent(createPendingIntent(context, getMainActivityName(), requestId, message));
                }
            } catch ( Exception e ) {
                try {
                    // 例外を受け取ったときは main activity を対象とする
                    builder.setContentIntent(createPendingIntent(context, getMainActivityName(), requestId, message));
                }catch ( Exception f ) {
                    // 通知は表示するが起動アクティビティが無い状態 / 基本ありえない
                    f.printStackTrace();
                }
            }

            // リッチ通知の判定
            if (!TextUtils.isEmpty(message.image)) {
                // URL が存在しているとき

                try {
                    Bitmap bmp = DownloadClient.downloadAndConvertToRichImage(message.image);
                    if( bmp==null )throw new Exception( "image error");

                    NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle(builder);
                    style.bigPicture(bmp);
                    style.bigLargeIcon((Bitmap) null);
                    if( message.body!=null ) style.setSummaryText(message.body);

                    builder.setLargeIcon(bmp);

                } catch( Exception e ) {
                    e.printStackTrace();
                }
            }

            NotificationManagerCompat.from(context).notify(requestId, builder.build());

        }catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /***
     * 割り込み通知用のUI. Activity を生成する
     * @param message
     * @param context
     * @param option
     */
    private void notifyMessageSyncWithRichUI( final NotificationMessage message, Context context, BDashPopupCustomOption option) {
        try {
            // カスタムペイロードをデコードし、buttonsの情報を取得する
            NotificationButton notificationButton = new NotificationButton();
            if (SDKConfig.APP_BDASH_FCM_API_V1.equals(message.fcm_api)) {
                // HTTPv1の場合
                if (message.custom_payload != null && !message.custom_payload.isEmpty()) {
                    message.custom_payload = new String(Base64.decode(message.custom_payload, Base64.DEFAULT));

                    notificationButton = new Gson().fromJson(message.custom_payload, NotificationButton.class);
                }
            } else {
                // FCM レガシーの場合
                notificationButton.buttons = new ArrayList<>();
                for (NotificationButton buttons : message.buttons) {
                    NotificationButton.ButtonElement buttonElement = new NotificationButton.ButtonElement(
                            buttons.number, buttons.notification_param, buttons.label);
                    notificationButton.buttons.add(buttonElement);
                }
            }

            Intent intent = createIntent(context, "", message);
            intent.setClass(context, BDashPopupActivity.class);

            if (message.image != null && !message.image.isEmpty()) {
                try {
                    Bitmap bitmap = DownloadClient.downloadAndConvertToRichImage(message.image);
                    if (bitmap != null) {
                        writeBitmapToCache(context, bitmap);
                    } else {
                        throw new Exception( "image error");
                    }
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }

            //割り込み通知専用ペイロード
            intent.putExtra(BDashPopupActivity.INTENT_OVERLAY, option.isOverlay);

            String mainButtonLabel = null;
            String subButtonLabel = null;
            for (NotificationButton.ButtonElement buttonElement : notificationButton.buttons) {
                if (buttonElement.number == 1) {
                    subButtonLabel = buttonElement.label;
                    intent.putExtra(LAUNCH_EXTRA_SUB_PARAM, buttonElement.notification_param);
                } else if (buttonElement.number == 2) {
                    mainButtonLabel = buttonElement.label;
                    intent.putExtra(LAUNCH_EXTRA_MAIN_PARAM, buttonElement.notification_param);
                }
            }

            String nameButtonSub = subButtonLabel == null || subButtonLabel.isEmpty() ? option.nameButton02 : subButtonLabel;
            intent.putExtra(BDashPopupActivity.INTENT_NAME_BUTTON_SUB, nameButtonSub);

            String nameButtonMain = mainButtonLabel == null || mainButtonLabel.isEmpty() ? option.nameButton01 : mainButtonLabel;
            intent.putExtra(BDashPopupActivity.INTENT_NAME_BUTTON_MAIN, nameButtonMain);

            Integer buttonLayoutType = notificationButton.buttons.size() == 1 ? BDashPopupCustomOption.ONE_BUTTON_LAYOUT :BDashPopupCustomOption.TWO_BUTTON_LAYOUT;
            intent.putExtra(BDashPopupActivity.INTENT_BUTTON_LAYOUT, buttonLayoutType);

            // 起動
            context.startActivity(intent);
        } catch( Throwable e ) {
            // ここのエラーは通常ありえない.ローカル内の Activity Not Found は開発段階のみで起こり得る
            e.printStackTrace();
        }
    }

    static Intent createIntent(Context context, String name, NotificationMessage message) {
        Intent intent = new Intent();
        intent.setClassName(context, name);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(LAUNCH_TITLE, message.title);
        intent.putExtra(LAUNCH_MESSAGE, message.body);
        intent.putExtra(LAUNCH_CUSTOM_PAYLOAD, message.custom_payload);
        intent.putExtra(LAUNCH_DID, message.dId);
        intent.putExtra(LAUNCH_BDID, message.bdId);
        intent.putExtra(LAUNCH_JP_CO_DATAX, message.jp_co_fscratch);
        intent.putExtra(LAUNCH_IMAGE, message.image);
        intent.putExtra(LAUNCH_TYPE, message.notification_type_android);
        intent.putExtra(LAUNCH_NOTIFICATION, 1);

        // FCM legacy
        intent.putExtra(LAUNCH_MESSAGE_ID, message.message_id);
        intent.putExtra(LAUNCH_EXTRA_PARAM, message.param);
        intent.putExtra(LAUNCH_EXTRA_NOTIFICATION_PARAM, message.notification_param);
        intent.putExtra(LAUNCH_ID, message.id);
        return intent;
    }

    private static PendingIntent createPendingIntent( Context context, String name, int requestId, NotificationMessage message) {
        Intent intent = createIntent(context, name, message);
        PendingIntent pendingIntent;
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.S ) {
            pendingIntent = PendingIntent.getActivity(context, requestId, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(context, requestId, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        return pendingIntent;
    }

    /***
     * アプリケーションに設定されているアイコンを取得する
     * @param c
     * @return
     */
    private static int getNotificationSmallIcon(Context c){
        return DeviceUtil.getApplicationInfo(c).icon;
    }

    /**
     * バイブレーションのパターンを取得する
     * @return
     */
    private static long[] getVibrationPattern(){
        return new long[]{100, 300, 500, 200};
    }

    /***
     * metadata の ResourceIDを ImagePack クラスとして返す
     * @param context
     * @param resourceKey
     * @return ImagePack object. null is 'parse error' or `not found bitmap`.
     */
    private static ImagePack resource2Bitmap( Context context, int resourceKey, boolean isCreateBitmap ){

        ImagePack result = new ImagePack();
        result.resourceId = resourceKey;
        if( isCreateBitmap ) {
            result.image = BitmapFactory.decodeResource(context.getResources(), result.resourceId);
            if (result.image == null) {
                return null;
            }
        }
        return result;
    }

    private static class ImagePack {
        Bitmap image;
        int resourceId;
    }

    /**
     * アプリがフォアグラウンドかの判定
     * @param context
     * @return
     */
    private static Boolean isForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcess = am.getRunningAppProcesses();
        for(ActivityManager.RunningAppProcessInfo processInfo : runningProcess) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    /**
     * 一時保存用ファイルへ画像データを保存
     * @param context
     * @param bitmap
     */
    private void writeBitmapToCache(Context context, Bitmap bitmap) {
        File cacheFile = new File(context.getCacheDir(), CACHE_NAME);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(cacheFile);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
