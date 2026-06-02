package com.f_scratch.bdash.mobile.analytics;

import android.content.Context;

import com.f_scratch.bdash.mobile.analytics.model.Device;
import com.f_scratch.bdash.mobile.analytics.model.User;
import com.f_scratch.bdash.mobile.analytics.model.config.SDKConfig;
import com.f_scratch.bdash.mobile.analytics.util.LogicUtil;
import com.f_scratch.bdash.mobile.analytics.util.StorageUtil;

import java.lang.reflect.Field;

/**
 * SDK マネージャークラス<br>
 *  BDash SDK ライブラリにシングルトンで存在する<br>
 *  ・共通初期化処理や SDK 呼び出し側のオブジェクトを管理します<br>
 *  ・可視性として package private に変えたいが、Context を他のクラスで参照するので要検討<br>
 *
 *
 */
public class MobileSDKManager {

    private static MobileSDKManager instance;

    /** SDK バージョン */
    private static final String SDK_VERSION = SDKConfig.SDK_VERSION;
    /** アプリケーション Context */
    private Context applicationContext;

    /***
     *
     * @param applicationContext
     */
    private MobileSDKManager(Context applicationContext, String customId){
        init(applicationContext, customId);
    }

    /***
     * 内部初期化用インターフェイス
     * @return
     */
    public static MobileSDKManager getInstance_needCreate(Context applicationContext, String customId) {
        if( instance == null ) {
            instance = new MobileSDKManager(applicationContext, customId);
        }
        return instance;
    }

    /***
     *
     * @return
     */
    public static MobileSDKManager getInstance() {
        return instance;
    }


    /***
     * SDK の共通初期化処理
     * @param applicationContext
     */
    protected void init(Context applicationContext, String customId){
        this.applicationContext = applicationContext;

        // ストレージの初期化
        StorageUtil.initialize(applicationContext);

        // デバイス情報の初期化
        LogicUtil.initDeviceInfo(Device.getInstance(), applicationContext, customId);


        // ユニークID のチェック
        if (User.getInstance().hasUniqueId() == false) {
            User.getInstance().setUniqueId(LogicUtil.generateUUID());
            try {
                User.getInstance().save();
            } catch (Exception e) {
                // ここで保存できないのは致命的だが、
                // 100byte に満たないデータを保存できないのは SDK ではどうしようも無い
            }
        }

    }

    /***
     *
     * @return
     */
    public Context getContext(){
        return applicationContext;
    }


    /***
     * SDK バージョンの取得
     * @return
     */
    public static String getSdkVersion(){
        return SDK_VERSION;
    }


    /***
     * 以降、サンプルアプリ専用の環境確認
     */
    private static String Class_ConnectConfig = "com.f_scratch.bdash.mobile.analytics.connect.ConnectConfig";

    private static String getConnectConfigFieldValue( String name ) throws Exception{
        Class clazz = Class.forName(Class_ConnectConfig);

        Field target = clazz.getDeclaredField(name);
        target.setAccessible(true);
        return (String) target.get(clazz);
    }


    public static boolean isITEnvironmentTracker() throws Exception{
        if( getConnectConfigFieldValue("API_URL").startsWith("https://it-") ) {
            return true;
        }
        return false;
    }

    public static boolean isITEnvironmentNotification() throws Exception{
        if( getConnectConfigFieldValue("API_TOKEN_URL").startsWith("https://it-") ) {
            return true;
        }
        return false;
    }


}
