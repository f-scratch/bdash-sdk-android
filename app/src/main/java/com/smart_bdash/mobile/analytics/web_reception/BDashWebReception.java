package com.smart_bdash.mobile.analytics.web_reception;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;

import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.smart_bdash.mobile.analytics.R;
import com.smart_bdash.mobile.analytics.connect.ConnectClient;
import com.smart_bdash.mobile.analytics.connect.ConnectClientController;
import com.smart_bdash.mobile.analytics.connect.ConnectType;
import com.smart_bdash.mobile.analytics.connect.IConnectAsyncResponse;
import com.smart_bdash.mobile.analytics.connect.RequestParam;
import com.smart_bdash.mobile.analytics.model.config.SDKConfig;
import com.smart_bdash.mobile.analytics.util.DeviceUtil;
import com.smart_bdash.mobile.analytics.util.LogUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * B-Dash のアプリ接客をコントロールするクラス<br>
 *
 * @author dataX on 2018/09/06.
 */

public class BDashWebReception extends DialogFragment {

    // Fragment 関連
    /** savedInstanceState から popup インスタンスを取得する際のタグ名 */
    public final static String BDASH_FRAGMENT_TAG  = "--bdash-webview-tag";
    private final static String BDASH_SAVE_CURRENT = "--bdash-save-current";
    private final static String BDASH_SAVE_REPORT  = "--bdash-save-report";

    // Javascript 関数
    private final static String JS_FUNC_SUCCESS = "successWebview";
    private final static String JS_FUNC_CLOSE   = "onClose";

    // Schema
    private final static String SCHEMA_INTERNAL = "internal://";
    private final static String SCHEMA_WEBVIEW  = "webview://";
//    private final static String SCHEMA_POPUP    = "popup://";
    private final static String SCHEMA_EXTERNAL = "external://";
//    private final static String SCHEMA_COMMAND  = "command://";

    // Scheme EventType
    /** 内部遷移イベント */
    public final static int EVENT_INTERNAL = 0;
    /** WebView遷移イベント */
    public final static int EVENT_WEBVIEW  = 1;
    /** 外部アプリ起動イベント */
    public final static int EVENT_EXTERNAL = 2;
    /** 不明なイベント */
    public final static int EVENT_UNKNOWN = -1;


    // favicon
    private final static String FAVICON_PATH    = "/favicon.ico";

    // BDash Command
    private final static String COMMAND_SEPARATOR = "/"; // 区切り文字
    private final static String COMMAND_CLOSE = "close"; // 閉じる
    private final static String COMMAND_COPY  = "copy";  // コピー

    /**
     * Web接客で発生したイベントを受け取るリスナーのインターフェイス
     */
    public interface EventListener {
        /**
         * @see BDashWebReception#EVENT_INTERNAL
         * @see BDashWebReception#EVENT_WEBVIEW
         * @param eventType ポップアップ内で発生した遷移イベント
         * @param parameter ポップアップ内で発生したイベントのパラメーター
         */
        void    onAction(int eventType, HashMap<String, String> parameter);
    }
    private EventListener eventListener;

    private Handler handler = new Handler();

    private final static int PROGRESS_INIT     = 0;    // 初期状態
    private final static int PROGRESS_UPDATE   = 1;    // Update 確認状態
    private final static int PROGRESS_CONFIRM  = 2;    // 確認状態
    private final static int PROGRESS_SHOWN    = 3;    // 表示された状態
    private final static int PROGRESS_EXCLUDED = 4;    // 条件を満たせなかった状態
    private final static int PROGRESS_UPDATE_ERROR = 5;// Update エラ－が発生した状態
    private final static int PROGRESS_NEXT_TASK = 6;// View を再生成する必要がある状態

    private ConnectClientController controller;

    // 進捗状況
    private int progressStatus = PROGRESS_INIT;

    // 内部パラメーター
    private boolean webview_isPageStarted;
    private String webview_failingUrl;

    // ポップアップがクローズされたか
    private boolean isDismiss;


    // PopupView の計算後の高さ
    private int viewHeight;

    // ポップアップ全体とWebViewとのパディング(上)
    private int paddingTopSize;
    // ポップアップ全体とWebViewとのパディング(下)
    private int paddingBottomSize;
    // ポップアップ全体とWebViewとのパディング(左)
    private int paddingLeftSize;
    // ポップアップ全体とWebViewとのパディング(右)
    private int paddingRightSize;

    private final static String KEY_CLOSE_BUTTON_VERTICAL_ALIGN = "closeButtonVerticalAlign";
    private final static String KEY_CLOSE_BUTTON_HORIZONTAL_ALIGN = "closeButtonHorizontalAlign";
    private final static String KEY_CLOSE_BUTTON_HEIGHT = "closeButtonHeight";
    private final static String KEY_CLOSE_BUTTON_WIDTH = "closeButtonWidth";

    //アスペクト比を保持するための比率
    private float wideRatio = 1;

    // キュー
    private ArrayList<BDashReport> updateQueue = new ArrayList<>();
    private ArrayList<BDashReport> trackingQueue = new ArrayList<>();
    FragmentManager fragmentManager;
    Activity context;

    // 現在の設定情報
    private WebReceptionSettingsResponse current_webReception;
    private BDashReport current_report;

    // デバッグ用(本番も導入)
    private BDashWebReceptionController._DebugLogMessage debugListener;

    // WebView
    private WebView webView;

    // アプリ接客が埋め込み形式か
    private boolean isEmbedded;

    // アプリ接客の表示サイズ指定
    public final static String POPUP_SIZE_UNIT_AUTO = "auto";  // 自動設定
    public final static String POPUP_SIZE_UNIT_VW = "vw";  // 比率指定
    public final static String POPUP_SIZE_UNIT_PX = "px";  // 固定値指定

    private String sizeUnit;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 埋め込み形式アプリ接客のデータを取得するリスナー
     */
    public interface WebReceptionListener {
        void onDataReceived(String webReceptionData);
    }
    private WebReceptionListener webReceptionListener;

    private WebViewClient webViewClient = new WebViewClient(){


        // 新しいURLが指定されたときの処理を定義
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

            BDashSchema schema = new BDashSchema();
            String url = request.getUrl().toString();
            switch(schema.parse(url)){
            case SCHEMA_INTERNAL:
                printConsole( ">>internal click: " + schema.getParam());
                notifyEventListener(EVENT_INTERNAL, schema.getCoordinationParam());
                closeMessage();
                break;
            case SCHEMA_WEBVIEW:
                printConsole( ">>webview click: " + schema.getParam());
                notifyEventListener(EVENT_WEBVIEW, schema.getCoordinationParam());
                closeMessage();
                break;
            case SCHEMA_POPUP:
                printConsole( ">>popup click: " + schema.getParam());
                // javascript: / file: などの危険なスキームを弾くため http / https のみ許可する
                if( isAllowedHttpUrl(schema.getParam()) ){
                    webView.loadUrl(schema.getParam());
                } else {
                    LogUtil.s(">> popup: 許可されていないスキームのため読み込みません: " + schema.getParam());
                }
                break;
            case SCHEMA_EXTERNAL:
                printConsole(">>external click: " + schema.getParam());
                launchBrowser(schema.getParam());
                closeMessage();
                break;
            case SCHEMA_COMMAND:
                printConsole(">>command click: " + schema.getParam());
                commandSchema(schema);
                break;
            default:
                return false;
            }

            return true;
        }

        /***
         * 証明証エラー
         * @param view
         * @param handler
         * @param error
         */
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Context context = getActivity();
            if( context!=null ) {
                AlertDialog dialog = BDashWebReceptionUtil.createSslErrorDialog(context, null);
                dialog.show();
            }

            // 問答無用で切断
            handler.cancel();
        }

        /**
         * WebViewのページ読み込み開始
         * @param view コールバックWebView
         * @param url 読み込むURL
         * @param favicon 該当ページのfavicon
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            // WebView 開始フラフ
            webview_isPageStarted = true;

            if(TextUtils.equals(url, webview_failingUrl)){
                // エラーページなので何もしない
                return;
            }
        }

        /**
         * [OS依存]
         * @param view
         * @param request
         * @param error
         */
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            LogUtil.s( "onReceivedError");
            onConnect_WebViewStatusCodeError();
        }

        // 互換性のためか API26 現在、上記の onReceiveError から、非推奨の下記がコールバックされる
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            if(webview_isPageStarted) {
                webview_failingUrl = failingUrl;
            }
            LogUtil.s( "onReceivedError deprecated");
        }

        /**
         * [OS依存] Android 6.x 以降 ステータスコードエラー
         * @param view
         * @param request
         * @param errorResponse
         */
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);

            // 取得しに行った URL 以外で http エラーが発生したとき
            if( !request.getUrl().toString().equals(current_webReception.getCurrent().getUrl()) ) {
                LogUtil.s( "onReceivedHttpError: " + errorResponse.getStatusCode());
                // URL のクエリ・フラグメントに秘匿パラメータが乗りうるため、host + path のみ出力する
                LogUtil.s("url: " + LogUtil.maskUrl(request.getUrl().toString()));
                return ;
            }

            if( !request.getUrl().toString().endsWith(FAVICON_PATH) ) {
                LogUtil.s("################################" );
                LogUtil.s("onReceivedHttpError: " + errorResponse.getStatusCode());
                // URL のクエリ・フラグメントに秘匿パラメータが乗りうるため、host + path のみ出力する
                LogUtil.s("url: " + LogUtil.maskUrl(request.getUrl().toString()));

                // ステータスコードエラーの場合 WebView を表示しない
                onConnect_WebViewStatusCodeError();
            }else {
                LogUtil.s( ">>ignore favicon error");
            }
        }

        /**
         * WebViewのページ読み込み完了
         * @param view コールバックWebView
         * @param url ページのURL
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            LogUtil.s( ">>onPageFinished: " + url );

            if (isEmbedded) {
                notifyHtmlSourceByJavascript(view);
            } else {
                // Javascript を実行して戻り値から処理を継続する
                getStatusCodeByJavascript(view);
            }
        }
    };



    /**
     * デバッグ用
     * @param str
     */
    private void printConsole( String str ) {
        if( str==null )return;

        if( debugListener!=null )debugListener.onMessage(str);
        else LogUtil.s(str);
    }

    /**
     * JavaScript を実行し、結果をコールバックで受け取る
     * @param webView 対象 WebView
     * @param script 実行する JavaScript
     * @param callback 実行結果のコールバック
     */
    private void evaluateJavascript(WebView webView, String script, ValueCallback<String> callback) {
        if( webView==null )return;

        try {
            webView.evaluateJavascript(script, callback);
        } catch (Exception e) {
            // Null Reference で 実際にクラッシュ発生
        }
    }

    /**
     * JavaScript の文字列戻り値を Java の文字列へ変換する
     * @param value JavaScript の戻り値
     * @return デコード済み文字列
     */
    private String decodeJavascriptString(String value) {
        if( value==null || TextUtils.equals(value, "null") )return null;

        try {
            return new Gson().fromJson(value, String.class);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * HTML の outerHTML を取得し、埋め込み形式のリスナーへ通知する
     * @param webView 対象 WebView
     */
    private void notifyHtmlSourceByJavascript(WebView webView) {
        evaluateJavascript(webView, "document.getElementsByTagName('html')[0].outerHTML", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                // 埋め込み形式のアプリ接客ではポップアップは表示しない
                closeMessage();

                notifyWebReceptionListener(decodeJavascriptString(value));
            }
        });
    }


    /**
     * Web接客インスタンスの作成
     * @return
     */
    public static BDashWebReception create(){
        BDashWebReception dialog = new BDashWebReception();
        return dialog;
    }

    /**
     * HTML の js を実行しステータスコードを判定する
     * @param webView
     */
    void getStatusCodeByJavascript( WebView webView ) {
        // 状態を確認する
        synchronized (this){
            int status = getProgressStatus();
            if( status == PROGRESS_NEXT_TASK || status == PROGRESS_EXCLUDED || status == PROGRESS_INIT ) {
                LogUtil.s( ">>破棄モードなので javascript を実行させません");
                return ;
            }
        }


        if( TextUtils.equals(current_webReception.getCurrent().forceShow, "true" )) {
            LogUtil.s( ">>強制表示モード forceShow");
            onPageFinished();
            return ;
        }

        // 関数の存在チェック
        evaluateJavascript(webView, "typeof " + JS_FUNC_SUCCESS + " == 'function'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                final boolean isExist = Boolean.valueOf(value);

                printConsole( "### func " + JS_FUNC_SUCCESS + "() is exist: " + value );
                if( isExist ) {
                    evaluateJavascript(webView, JS_FUNC_SUCCESS + "()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            onPageFinished();
                        }
                    });
                } else {
                    onConnect_WebViewStatusCodeError();
                }
            }
        });
    }


    /**
     * HTML の js / onClose() を実行する
     * @param webView
     */
    void closeByJavascript( WebView webView ) {
        LogUtil.s( ">>closeByJavascript");
        evaluateJavascript(webView, "typeof " + JS_FUNC_CLOSE + " == 'function'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                final boolean isExist = Boolean.valueOf(value);

                printConsole( "### func " + JS_FUNC_CLOSE + "() is exist: " + value );
                if( isExist ) {
                    evaluateJavascript(webView, JS_FUNC_CLOSE + "()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            dismiss();
                        }
                    });
                } else {
                    dismiss();
                }
            }
        });
    }

    /**
     * 表示準備が整ったことを示す
     */
    void onPageFinished(){
        if( isDismiss ) {
            LogUtil.s(">>ページ表示が完了する前に閉じられました");
            return ;
        }

        Dialog dialog = getDialog();
        if( dialog!=null ){
            try {
                View v = dialog.getWindow().getDecorView();
                v.setVisibility(View.VISIBLE);

                // アニメーション処理
                if (current_webReception.getCurrent().getEffect() == WebReceptionSettings.ANIM_FADE_IN) {
                    Animation animation = createFadeInAnimation(current_webReception.getCurrent().getEffectDuration());
                    webView.startAnimation(animation);
                    //playSlideAnimation(webView, 5000);
                    LogUtil.s(">>start animation >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                }
            }catch( Exception e ) {
                // View が破棄されたあとに null pointer が発生してここに来る可能性がある
                e.printStackTrace();
            }
        }
        // キューをクリアする
        updateQueue.clear();

        // 正常に表示された
        setProgressStatus(PROGRESS_SHOWN);
    }

    /**
     * Update で通信エラーが発生
     */
    void onConnect_UpdateError(){
        LogUtil.s( ">>onConnect_UpdateError");

        clearUpdateQueue();
        setProgressStatus(PROGRESS_UPDATE_ERROR);
    }

    /**
     * WebView で通信エラーが発生
     */
    void onConnect_WebViewError(){
        LogUtil.s( ">>onConnect_WebViewError");
        synchronized (updateQueue) {
            if( updateQueue.size() != 0 ) {
                LogUtil.s( "updateQueue があります");
                setProgressStatus(PROGRESS_NEXT_TASK);
            } else {
                LogUtil.s( "条件を満たせませんでした");

                // 条件を満たせなかった
                setProgressStatus(PROGRESS_EXCLUDED);
            }
        }
        if( webView!=null ) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
        }

        boolean isResumed = isResumed();
        LogUtil.s(">>Fragment is resumed(" + isResumed + ")");
        if (isResumed) {
            dismiss();
        }
    }

    /**
     * WebView ステータスコードが 200 ではなかったとき
     * キューに処理があるならそれを実施する
     */
    void onConnect_WebViewStatusCodeError(){

        onConnect_WebViewError();
    }


    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            BDashReport report;
            synchronized (updateQueue) {
                report = updateQueue.remove(0);
            }
            requestWebReception(report, BDashWebReception.this);
        }
    };

    private Runnable bootTrackingRunnable = new Runnable() {
        @Override
        public void run() {
            BDashReport report;
            synchronized (trackingQueue) {
                report = trackingQueue.remove(0);
            }
            requestWebReception(report, BDashWebReception.this);
        }
    };

    /**
     * WebReception API の Response 処理
     * @param response
     */
    private void onResponse_WebReceptionAPI( BDashReport report, String response ){
        try {
            // レスポンス本文は秘匿情報を含みうるため、文字数のみ出力する
            LogUtil.s(">> webReception response: " + LogUtil.maskData(response));

            Gson gson = BDashWebReceptionUtil.getDefaultGson();
            WebReceptionSettingsResponse current = gson.fromJson(response, WebReceptionSettingsResponse.class);
            if (current == null || current.getCurrent()==null || current.getCurrent().getUrl()==null ) {
                // json parse error. or url is not exist.
                LogUtil.s(">>json parse error onResponse_WebReceptionAPI");
                onConnect_UpdateError();
                return;
            }

            current_report = report;
            current_webReception = current;

            // update中に閉じられた 又は json が不正
            if (isDismiss() || getProgressStatus() == PROGRESS_UPDATE_ERROR ) {
                LogUtil.s(">>abort onResponse_WebReceptionAPI");
                onConnect_UpdateError();
                return;
            }

            // ポップアップがまだ作られていない
            if (isCreatedDialog() == false) {
                // 処理を継続する
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            showPopup();
                        }catch( Exception e ) {
                            // ここにくるのはホーム画面やアプリ内の画面遷移が行われたとき
                            e.printStackTrace();

                            // このタイミングで発生した場合は、破棄とする
                            onConnect_UpdateError();
                        }
                    }
                });

            } else {
                // memo: ポップアップがあるときに来ても意味がないので何もしない
                LogUtil.s( ">>popup is created. ");
            }
        }catch( Exception e ) {
            // ここに来た場合も考慮する. json parse error / onDestroy などで来る
            e.printStackTrace();

            // Update error
            onConnect_UpdateError();
        }
    }


    /**
     * レポートデータを送る
     * @param report
     */
    public void report( BDashReport report ) {
        LogUtil.s( ">>report function()");
        report.accessType = BDashReport.ACCESS_TYPE_TRACKING;
        synchronized (bootTrackingRunnable) {
            trackingQueue.add(report);
        }
        startTrackingConnectThread();
    }


    /**
     * 顧客情報 取得通信を行う
     */
    private void update( BDashReport report ){
        LogUtil.s( ">>update function()");

        pushUpdateQueue(report);

        startUpdateConnectThread();
    }

    private void startUpdateConnectThread() {

        BDashWebReceptionController.getInstance().getThreadPoolExecutor().submit(updateRunnable);
    }

    private void startTrackingConnectThread() {

        BDashWebReceptionController.getInstance().getThreadTrackingPoolExecutor().submit(bootTrackingRunnable);
    }


    /**
     * 実際の https 通信を行う
     * @param report
     */
    static void requestWebReception( BDashReport report, final BDashWebReception webReception){

        String request = null;
        try {
            request = com.smart_bdash.mobile.analytics.util.LogicUtil.createJsonRequestWithCommonParameter(report);
        }catch( Exception e ) {
            e.printStackTrace();
        }

        RequestParam param = ConnectClient.getDefaultRequestParam();
        param.param_str = report.accessType;
        param.param_obj = report;

        ConnectType type = ConnectType.API_WEB_RECEPTION_SETTING;
        // memo: private 変数に値があるときはデバッグモード
        if( report.debugConnectUrl != null ){
            type = ConnectType.DEBUG_API;
            param.setConcatUrl(report.debugConnectUrl);
            LogUtil.s(">>> 検証用の URL が指定されました>>>>>>>>>>>>>>>>>>>>>>");
            // URL のクエリ・フラグメントに秘匿パラメータが乗りうるため、host + path のみ出力する
            LogUtil.s( LogUtil.maskUrl(report.debugConnectUrl) );
        }

        ConnectClientController controller;
        if( webReception!=null ){
            // リクエストボディは識別子等を含むため、秘匿キーをマスクして出力する
            webReception.printConsole(LogUtil.maskJson(request));
            controller = webReception.getConnectController();
        }else{
            // リクエストボディは識別子等を含むため、秘匿キーをマスクして出力する
            LogUtil.s(LogUtil.maskJson(request));
            controller = new ConnectClientController();
        }

        ConnectClient client = controller.connect(new IConnectAsyncResponse() {
            @Override
            public void onConnect(ConnectClient connectClient) throws Exception {
            }

            @Override
            public void onPostExecuteImpl(ConnectClient connectClient, Throwable throwable) throws Exception {
                LogUtil.s( ">>accessType: " + connectClient.getRequestParam().param_str );

                BDashReport report = (BDashReport) connectClient.getRequestParam().param_obj;

                // Update 通信のとき
                if( TextUtils.equals(BDashReport.ACCESS_TYPE_UPDATE, connectClient.getRequestParam().param_str )) {
                    if( throwable == null ) {
                        // 取得できた
                        webReception.onResponse_WebReceptionAPI(report, connectClient.getResponse());
                    } else {
                        // エラー発生した時
                        webReception.onConnect_UpdateError();
                    }
                } else {
                    LogUtil.s("status: " + connectClient.getResponseCode() );
                    if( throwable == null ) {
                        // レスポンス本文は秘匿情報を含みうるため、文字数のみ出力する
                        LogUtil.s(LogUtil.maskData(connectClient.getResponse()) );
                    } else{
                        LogUtil.s( throwable.toString() );
                    }

                }

            }
        }, type, request, param);
    }

    /**
     * 通信コントローラーの取得
     * @return ConnectClientController
     */
    private synchronized ConnectClientController getConnectController(){
        if( controller==null ){
            controller = new ConnectClientController();
        }
        return controller;
    }

    /**
     * Viewサイズを計算する
     */
    WindowManager.LayoutParams calculateViewSize(){
        LogUtil.s(">> Popup view size calculate.");
        if (context == null) {
            context = getActivity();
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14(SDK 34)以降、scaledDensity が非推奨のため
            LogUtil.s( String.format(">>scale: %f (SDK 34 or Over)", TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, metrics)) );
        } else {
            LogUtil.s( String.format(">>scale: %f", metrics.density) );
        }

        WindowManager.LayoutParams layoutParams = getDialog().getWindow().getAttributes();

        adjustViewSize(metrics, layoutParams);

        return layoutParams;
    }

    /**
     * レイアウトを更新する
     */
    void updateLayout(){
        LogUtil.s(">> Layout update.");

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        try {
            WindowManager.LayoutParams decorParams = (WindowManager.LayoutParams) getDialog().getWindow().getAttributes();
            ViewGroup rootView = (ViewGroup) getDialog().getWindow().getDecorView().findViewById(R.id.com_smart_bdash_mobile_rootView);
            View closeButton = getCloseButtonView(rootView);
            WebView webView    = (WebView)rootView.findViewById(R.id.com_smart_bdash_mobile_reception_webView);
            ViewGroup webViewFrame = (ViewGroup)rootView.findViewById(R.id.com_smart_bdash_mobile_reception_webViewFrame);
            WebReceptionSettings current = current_webReception.getCurrent();

            int webViewGravityTop = 0;
            int webViewGravityBottom = 0;
            int webViewGravityRight = 0;
            int webViewGravityLeft = 0;

            // HWアクセラレーターをオフにした上で透過設定にする
            webView.setBackgroundColor(0x0);

            if (current.validateCloseButton()) {
                HashMap<String, Integer> closeButtonInfo = getCloseButtonInfo(current, metrics);

                int closeButtonHeight = closeButtonInfo.get(KEY_CLOSE_BUTTON_HEIGHT);
                int closeButtonWidth = closeButtonInfo.get(KEY_CLOSE_BUTTON_WIDTH);
                FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(closeButtonWidth, closeButtonHeight);

                int verticalAlign = closeButtonInfo.get(KEY_CLOSE_BUTTON_VERTICAL_ALIGN);
                int horizontalAlign = closeButtonInfo.get(KEY_CLOSE_BUTTON_HORIZONTAL_ALIGN);

                if (verticalAlign > 0) {
                    closeButtonParams.topMargin = verticalAlign;
                } else {
                    // adjustViewSize にて閉じるボタンがコンテンツの上辺を超えた場合、超過分拡張されるためマージンを追加しない
                    closeButtonParams.topMargin = 0;
                    webViewGravityBottom = Gravity.BOTTOM;
                }
                if (horizontalAlign > 0) {
                    closeButtonParams.leftMargin = horizontalAlign;
                } else {
                    // adjustViewSize にて閉じるボタンがコンテンツの左辺を超えた場合、超過分拡張されるためマージンを追加しない
                    closeButtonParams.leftMargin = 0;
                    webViewGravityRight = Gravity.RIGHT;
                }

                closeButton.setLayoutParams(closeButtonParams);

                if (paddingBottomSize > 0) {
                    // 閉じるボタンがコンテンツの下辺を超過する場合
                    webViewGravityTop = Gravity.TOP;
                }
                if (paddingRightSize > 0) {
                    // 閉じるボタンがコンテンツの右辺を超過する場合
                    webViewGravityLeft = Gravity.LEFT;
                }
            } else {
                paddingTopSize = closeButton.getHeight() / 2;
                paddingRightSize = closeButton.getHeight() / 2;
                paddingBottomSize = 0;
                paddingLeftSize = 0;
                webViewGravityBottom = Gravity.BOTTOM;
                webViewGravityLeft = Gravity.LEFT;
            }

            LogUtil.s( String.format("decorView  w=%d h=%d  y=%d", decorParams.width, decorParams.height, decorParams.y ));
            LogUtil.s( String.format("closeButton  w=%d h=%d", closeButton.getWidth(), closeButton.getHeight()));

            int decor_width = decorParams.width;

            int webView_width  = decor_width - (paddingRightSize + paddingLeftSize);
            int webView_height;
            if (sizeUnit.equals(POPUP_SIZE_UNIT_PX)) {
                Point point = getPopupSizePx(metrics, current);
                webView_height = point.y;
            } else {
                webView_height = (int)(webView_width * current.getHeight());
            }
            // setLayerType の仕様(挙動)に対応
            webView_height = (int)adjustTranslateViewSize(webView_height,metrics);

            LogUtil.s(">> check webView size, width: " + webView_width + "px, height: " + webView_height + "px");

            viewHeight = webView_height + paddingTopSize + paddingBottomSize;

            decorParams.height = viewHeight;
            getDialog().getWindow().setAttributes(decorParams);


            FrameLayout.LayoutParams lp;
            lp = (FrameLayout.LayoutParams) webViewFrame.getLayoutParams();
            LogUtil.s( String.format("webViewFrame  w=%d h=%d", webViewFrame.getWidth(), webViewFrame.getHeight()));
            lp.width = webView_width;
            if (current.validateCloseButton()) {
                lp.height = viewHeight - (paddingTopSize + paddingBottomSize);
            } else {
                lp.height = viewHeight;
            }
            lp.gravity = webViewGravityTop | webViewGravityBottom | webViewGravityRight | webViewGravityLeft;

            webViewFrame.setLayoutParams(lp);

            LogUtil.s( String.format("      decor_width: %d", decor_width ));
            LogUtil.s( String.format("      webView_width: %d", webView_width ));
            LogUtil.s( String.format("      webView_height: %d", webView_height ));
            LogUtil.s( String.format("      viewHeight: %d", viewHeight ));

            //ここでLinearLayout内の全てのViewに対して高さ調整を行う(pixel計算)
            for( int i=0 ; i< webViewFrame.getChildCount() ; i++ ) {
                View target = webViewFrame.getChildAt(i);

                ViewGroup.MarginLayoutParams mlp;
                mlp = (ViewGroup.MarginLayoutParams)target.getLayoutParams();
                if( target instanceof WebView ){
                    mlp.height = webView_height;
                } else {
                    if( i==0 ) {
                        // 先頭
                        if (current.validateCloseButton()) {
                            mlp.height = 0;
                        } else {
                            mlp.height = paddingTopSize;
                        }
                    }
                }
                mlp.topMargin = mlp.bottomMargin = 0;
                mlp.rightMargin = 0;
                mlp.leftMargin= 0;
                target.setLayoutParams(mlp);

                if( target instanceof WebView ) {
                    LogUtil.s( "webView width: " + mlp.width + "  height: " + mlp.height);

                    WebViewRequest req = new WebViewRequest();
                    req.copy(current_report);
                    String postData = BDashWebReceptionUtil.getDefaultGson().toJson(req);
                    // リクエストボディは識別子等を含むため、秘匿キーをマスクして出力する
                    LogUtil.s(">> postData: " + LogUtil.maskJson(postData));

                    if( current.forceShow==null ) {
                        LogUtil.s(">> POST Request");
                        getWebViewContent(current.getUrl(), postData, (WebView) target);
                    }else{
                        LogUtil.s(">> GET Request");
                        ((WebView) target).loadUrl(current.getUrl());
                    }
                }
            }
        }catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Web接客のコンテンツを取得＆WebViewに表示
     */
    private void getWebViewContent(String url, String postData, WebView webView) {
        if (url.contains("/v2/")) {
            LogUtil.s(">> getWebViewContent Url has /v2/");
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(
                    postData,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    LogUtil.s(">> getWebViewContent onFailure");
                    LogUtil.s(">> exception: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        LogUtil.s(">> getWebViewContent response error");
                        LogUtil.s(">> error code: " + response.code());
                        return;
                    }

                    final String htmlContent = response.body().string();
                    LogUtil.s(">> getWebViewContent success");

                    mainHandler.post(() ->
                            webView.loadDataWithBaseURL(
                                    url,
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                            )
                    );
                }
            });
        } else {
            LogUtil.s(">> getWebViewContent Url does not have /v2/");
            postData = BDashWebReceptionUtil.URLEncode(postData);
            // URLエンコード後は秘匿情報を含みうるため、文字数のみ出力する
            LogUtil.s(">> postData(encode): " + LogUtil.maskData(postData));

            webView.postUrl(url, postData.getBytes());
        }
    }

    /**
     * Viewサイズを調整
     * @param metrics 端末ディスプレイ情報
     * @param layoutParams ポップアップレイアウト情報
     */
    void adjustViewSize(DisplayMetrics metrics, WindowManager.LayoutParams layoutParams) {
        LogUtil.s(">> View size adjust.");
        WebReceptionSettings current = current_webReception.getCurrent();

        int gravity = current.getHorizontalAlign() | current.getVerticalAlign();
        int ratioWidth = current.getWidth();
        float ratioHeight = current.getHeight();
        int verticalMargin = (int)DeviceUtil.getDisplayPxSize(current.getVerticalMargin(), metrics);
        int horizontalMargin = (int)DeviceUtil.getDisplayPxSize(current.getHorizontalMargin(), metrics);

        float dialogWidth;
        float dialogHeight;
        // ポップアップサイズ
        if (sizeUnit.equals(POPUP_SIZE_UNIT_PX)) {
            // 表示サイズの単位が"px"
            Point point = getPopupSizePx(metrics, current);
            dialogWidth = point.x;
            dialogHeight = point.y;
        } else {
            // 表示サイズの単位が"vw"
            dialogWidth = metrics.widthPixels * ratioWidth / 100f;
            dialogHeight  = dialogWidth * ratioHeight;
        }

        HashMap<String, Integer> closeButtonInfo = getCloseButtonInfo(current, metrics);
        // 閉じるボタンの表示位置(縦)
        int buttonVerticalAlign = closeButtonInfo.get(KEY_CLOSE_BUTTON_VERTICAL_ALIGN);
        // 閉じるボタンの表示位置(横)
        int buttonHorizontalAlign = closeButtonInfo.get(KEY_CLOSE_BUTTON_HORIZONTAL_ALIGN);
        // 閉じるボタンサイズ
        int buttonWidth = closeButtonInfo.get(KEY_CLOSE_BUTTON_WIDTH);
        int buttonHeight = closeButtonInfo.get(KEY_CLOSE_BUTTON_HEIGHT);

        paddingTopSize = 0;
        paddingBottomSize = 0;
        paddingLeftSize = 0;
        paddingRightSize = 0;

        if (buttonVerticalAlign < 0) {
            // マイナス値の場合、閉じるボタンがコンテンツの上にはみ出すため高さを追加
            LogUtil.s(">>  CloseButton is above the content.");
            paddingTopSize = buttonVerticalAlign * -1;
            dialogHeight = dialogHeight + paddingTopSize;
            LogUtil.s(">>    dialogHeight: " + dialogHeight + ", padding(top): " + paddingTopSize);
        } else if ((int)dialogHeight < (buttonVerticalAlign + buttonHeight)) {
            // 上辺からの差分 + ボタンの高さ がコンテンツの高さを超える場合、下にはみ出すため高さを追加
            LogUtil.s(">>  CloseButton is below the content.");
            paddingBottomSize = buttonVerticalAlign + buttonHeight - (int)dialogHeight;
            dialogHeight = dialogHeight + paddingBottomSize;
            LogUtil.s(">>  dialogHeight: " + dialogHeight + ", padding(bottom): " + paddingBottomSize);
        }
        if (buttonHorizontalAlign < 0) {
            // マイナス値の場合、閉じるボタンがコンテンツの左にはみ出すため幅を追加
            LogUtil.s(">>  CloseButton is to the left of the content.");
            paddingLeftSize = buttonHorizontalAlign * -1;
            dialogWidth = dialogWidth + paddingLeftSize;
            LogUtil.s(">>    dialogWidth: " + dialogWidth + ", padding(left): " + paddingLeftSize);
        } else if ((int)dialogWidth < (buttonHorizontalAlign + buttonWidth)) {
            LogUtil.s(">>  CloseButton is to the right of the content.");
            // 左辺からの差分 + ボタンの幅 がコンテンツの幅を超える場合、右にはみ出すため幅を追加
            paddingRightSize = buttonHorizontalAlign + buttonWidth - (int)dialogWidth;
            dialogWidth = dialogWidth + paddingRightSize;
            LogUtil.s(">>    dialogWidth: " + dialogWidth + ", padding(right): " + paddingRightSize);
        }

        // setLayerType の仕様(挙動)に対応
        dialogHeight = adjustTranslateViewSize(dialogHeight , metrics);
        //adjustTranslateViewSizeでViewの高さを調整したのでアスペクト比を合わせるために横幅も調整
        dialogWidth = dialogWidth * wideRatio;

        layoutParams.width  = (int)dialogWidth;
        layoutParams.height = (int)dialogHeight;
        layoutParams.x = 0;
        layoutParams.y = 0;

        if ((gravity & Gravity.LEFT) == Gravity.LEFT ||
                (gravity & Gravity.RIGHT) == Gravity.RIGHT ) {
            layoutParams.x = horizontalMargin;
        } else if ((gravity & Gravity.CENTER_HORIZONTAL) == Gravity.CENTER_HORIZONTAL) {
            // 現状 center のときには必ず margin は 0 として渡される. また if 文は else if として書いたとき正常に動く
            layoutParams.width  = layoutParams.width - horizontalMargin;
        }

        if ((gravity & Gravity.TOP) == Gravity.TOP ||
                (gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            layoutParams.y = verticalMargin;
        } else if ((gravity & Gravity.CENTER_VERTICAL) == Gravity.CENTER_VERTICAL) {
            // 現状 center のときには必ず margin は 0 として渡される. また if 文は else if として書いたとき正常に動く
            layoutParams.height = layoutParams.height - verticalMargin;
        }

        layoutParams.gravity = gravity;

        viewHeight = layoutParams.height;
        LogUtil.s(String.format(">> adjustedView viewHeights: %d  width: %d", layoutParams.height, layoutParams.width));
    }

    /***
     * gravityがcenterの時はバツボタン含めない状態で中央に持ってきてるように見せる(iOSと同様の見た目にしなくてはいけないため)
     */
    void adjustDialogMarginWhenCenter(){
        LogUtil.s(">> Adjust when the popup is created.");
        WebReceptionSettings current = current_webReception.getCurrent();
        if (!current.validateCloseButton()) {
            if (paddingTopSize == 0 || paddingRightSize == 0) {
                // 閉じるボタンのサイズが取得できていない場合は戻る
                LogUtil.s(">>  not adjustable button return!");
                return;
            }
        } else {
            if (paddingTopSize == 0 && paddingBottomSize == 0 &&
                    paddingRightSize == 0 && paddingLeftSize == 0) {
                // 閉じるボタンがコンテンツに完全に重なる場合は戻る
                LogUtil.s(">>  adjustable button return!");
                return;
            }
        }

        if(current.getHorizontalAlign() != Gravity.CENTER_HORIZONTAL && current.getVerticalAlign() != Gravity.CENTER_VERTICAL){
            //horizontalとverticalのどちらもcenter扱いでない場合は戻る
            return;
        }

        WindowManager.LayoutParams layoutParams = getDialog().getWindow().getAttributes();

        //移動量は実際の差(閉じるボタンサイズ)の1/2の量で良い
        int horizontalMarginSize = paddingRightSize / 2;
        if(current.getHorizontalAlign() == Gravity.CENTER_HORIZONTAL){
            layoutParams.x = horizontalMarginSize;
        }

        int verticalMarginSize = paddingTopSize / 2;
        if(current.getVerticalAlign() == Gravity.CENTER_VERTICAL){
            //画像座標原点のため、上に移動させるのに符号反転する
            layoutParams.y = -(verticalMarginSize);
        }

        getDialog().getWindow().setAttributes(layoutParams);
    }

    /***
     * 透過設定を有効にしていると画面高さを超えた View を作成すると、常に透過になってしまうので調整する
     *
     * また横向きの際はiOSと挙動を合わせるため、画面の縦サイズからはみ出た分を倍率として保持しておき、アスペクト比を維持する
     */
    float adjustTranslateViewSize( float dialogHeight, DisplayMetrics metrics ) {

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            //横画面の処理
            if(dialogHeight > metrics.heightPixels){
                wideRatio = metrics.heightPixels / dialogHeight;
                dialogHeight = dialogHeight * wideRatio;
            }else{
                wideRatio = 1;
            }
        }
        else {
            //縦画面の処理
            if (dialogHeight > metrics.heightPixels) {
                dialogHeight = metrics.heightPixels;
            }
            wideRatio = 1;
        }
        return dialogHeight;
    }

    /**
     * 閉じるボタンの情報(サイズ・表示位置)を算出する
     * @param current 現在のWeb接客コンテンツ情報
     * @param metrics 端末のディスプレイ情報
     * @return 閉じるボタン情報
     */
    private HashMap<String, Integer> getCloseButtonInfo(WebReceptionSettings current, DisplayMetrics metrics) {
        LogUtil.s(">> getCloseButtonInfo called!!!");
        HashMap<String, Integer> buttonInfo = new HashMap<>();

        float dialogWidth;
        float dialogHeight;
        if (this.sizeUnit.equals(POPUP_SIZE_UNIT_PX)) {
            Point point = getPopupSizePx(metrics, current);
            dialogWidth = point.x;
            dialogHeight = point.y;
        } else {
            dialogWidth = metrics.widthPixels * current.getWidth() / 100f;
            dialogHeight = dialogWidth * current.getHeight();
        }
        LogUtil.s(">>  popup size width: " + dialogWidth + ", height: " + dialogHeight);

        // 閉じるボタンの表示位置(横)
        int buttonHorizontalAlign = Math.round(dialogWidth * current.getCloseButtonHorizontalAlign() / 100);
        // 閉じるボタンの表示位置(縦)
        int buttonVerticalAlign = Math.round(dialogWidth * current.getCloseButtonVerticalAlign() / 100);
        int buttonWidth = (int)(dialogWidth * current.getCloseButtonWidth() / 100);
        int buttonHeight = (int)(buttonWidth * current.getCloseButtonHeight());
        LogUtil.s(">>  closeButton coordinate x: " + buttonVerticalAlign + ", y: " + buttonHorizontalAlign);
        LogUtil.s(">>  closeButton width: " + buttonWidth);
        LogUtil.s(">>              height: " + buttonHeight);

        buttonInfo.put(KEY_CLOSE_BUTTON_HORIZONTAL_ALIGN, buttonHorizontalAlign);
        buttonInfo.put(KEY_CLOSE_BUTTON_VERTICAL_ALIGN, buttonVerticalAlign);
        buttonInfo.put(KEY_CLOSE_BUTTON_WIDTH, buttonWidth);
        buttonInfo.put(KEY_CLOSE_BUTTON_HEIGHT, buttonHeight);

        return buttonInfo;
    }

    /**
     * フェードインアニメーションを作成する
     * @param duration
     * @return
     */
    Animation createFadeInAnimation( int duration ){
        Animation anim = new AlphaAnimation(0, 1.0f);
        anim.setDuration(duration);
        anim.setFillAfter(true);
        return anim;
    }

    /**
     * <pre>
     * 顧客提供API
     *  - Web接客で発生したイベントを受け取るリスナー
     * </pre>
     */
    public synchronized void setEventListener( EventListener listener ){
        this.eventListener = listener;
    }

    /**
     * イベントリスナーへアクションを通知する
     * @param eventType
     * @param param
     */
    private synchronized void notifyEventListener( int eventType, HashMap<String,String> param ){
        if( eventListener!=null ) {
            eventListener.onAction(eventType, param);
        }
    }

    /**
     * <pre>
     * WebViewリクエストAPI
     * - Web接客のコンテンツのページ情報を受け取るリスナー
     * </pre>
     * @param listener リスナー
     */
    public synchronized void setWebReceptionListener( WebReceptionListener listener ) {
        this.webReceptionListener = listener;
    }

    /**
     * リスナーへページ情報を通知する
     * @param webReceptionData ページ情報(HTML)
     */
    private synchronized void notifyWebReceptionListener( String webReceptionData ) {
        if (webReceptionListener != null) {
            webReceptionListener.onDataReceived(webReceptionData);
        }
    }

    /**
     * 指定の URL が http / https スキームであるかを判定する.<br>
     *  ・javascript: / file: / content: / intent: などの危険なスキームを弾くために使用する<br>
     *  ・null・空文字・パース不能な場合は false を返す
     * @param url 判定対象の URL
     * @return http または https の場合のみ true
     */
    private boolean isAllowedHttpUrl( String url ){
        if( TextUtils.isEmpty(url) ){
            return false;
        }
        try {
            String scheme = Uri.parse(url).getScheme();
            if( scheme == null ){
                return false;
            }
            scheme = scheme.toLowerCase();
            return "http".equals(scheme) || "https".equals(scheme);
        }catch( Exception e ){
            return false;
        }
    }

    /**
     * ブラウザ起動
     * @param url
     */
    private void launchBrowser( String url ){
        if( !isAllowedHttpUrl(url) ){
            LogUtil.s(">> launchBrowser: 許可されていないスキームのため起動しません: " + url);
            return;
        }
        try {
            Uri uri = Uri.parse(url);
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(i);
        }catch( Exception e ){
            e.printStackTrace();
        }
    }

    /**
     * <pre>
     * コマンドスキームを解析し実行する
     *
     * [データ形式]
     *  command://[コマンド名]
     * </pre>
     * @param schema
     */
    private void commandSchema( BDashSchema schema ){
        String param = schema.getParam();

        // null 時は何もしない
        if ( param == null ){
            return;
        }

        // クローズ コマンド
        if( TextUtils.equals(param, COMMAND_CLOSE) ){
            closeMessage();
        }
        // コピー コマンド
        else if( param.startsWith(COMMAND_COPY + COMMAND_SEPARATOR) ) {
            String data =  BDashWebReceptionUtil.URLDecode(
                 param.substring(new String(COMMAND_COPY + COMMAND_SEPARATOR).length())
            );
            ClipData clip = DeviceUtil.copyToClipboard( context, data, data) ;

            String message = String.format("%s %s", clip.getDescription().getLabel(), context.getResources().getString(R.string.com_smart_bdash_mobile_command_copied));
            Toast.makeText( context, message, Toast.LENGTH_LONG).show();
            LogUtil.s( message );
        }
    }

    /**
     * <pre>
     * 顧客提供API(〜ver 6.3.1)
     *  - Web接客のポップアップを表示する
     * </pre>
     * @param report BDashReport データ
     * @param context Activity インスタンス
     * @param manager FragmentManager インスタンス
     * @return this
     */
    public BDashWebReception showMessage(BDashReport report, Activity context, FragmentManager manager) {
        return showMessage(report, context, manager, POPUP_SIZE_UNIT_AUTO);
    }

    /**
     * <pre>
     * 顧客提供API
     *  - Web接客のポップアップを表示する(ver 6.4.0〜)
     * </pre>
     * @param report BDashReport データ
     * @param context Activity インスタンス
     * @param manager FragmentManager インスタンス
     * @param sizeUnit ポップアップのサイズ指定("vw", "px", "auto")
     * @return this
     */
    public BDashWebReception showMessage(BDashReport report, Activity context, FragmentManager manager, String sizeUnit ) {

        // 引数が要件を満たしていない場合何もしない
        if( report == null || context == null || manager == null ) {
            return this;
        }

        if( report.debugConnectUrl != null && !report.debugConnectUrl.startsWith("http") ) {
            LogUtil.s( ">>デバッグ接続先が URL でないので何もせず終了します");
            return this;
        }


        synchronized (this) {
            // 表示されているか、次の連続タスクの準備中のとき
            if (getProgressStatus() == PROGRESS_SHOWN || getProgressStatus() == PROGRESS_NEXT_TASK) {
                return this;
            }

            // アプリ接客のサイズ指定を設定
            if (sizeUnit == null) {
                this.sizeUnit = POPUP_SIZE_UNIT_AUTO;
            } else {
                switch(sizeUnit) {
                    case POPUP_SIZE_UNIT_PX:
                        this.sizeUnit = POPUP_SIZE_UNIT_PX;
                        break;
                    case POPUP_SIZE_UNIT_VW:
                        this.sizeUnit = POPUP_SIZE_UNIT_VW;
                        break;
                    default:
                        this.sizeUnit = POPUP_SIZE_UNIT_AUTO;
                }
            }

            // UI コントロール変数の保持. 常に上書きで問題ない
            this.context = context;
            this.fragmentManager = manager;

            // 状態のリセットを行う
            if (getProgressStatus() == PROGRESS_UPDATE_ERROR) {
                // Update の通信エラーについては初期状態に戻す
                setProgressStatus(PROGRESS_INIT);
            }
            if (getProgressStatus() == PROGRESS_EXCLUDED) {
                // 条件を満たせなかったについては初期状態に戻す
                setProgressStatus(PROGRESS_INIT);
            }

            int status = getProgressStatus();
            if (status == PROGRESS_INIT) {
                // 初期化する
                clearUpdateQueue();
                isDismiss = false;
                isEmbedded = false;
                setProgressStatus(PROGRESS_UPDATE);
                update(report);
                return this;
            }
            LogUtil.s( "progressStatus: " + getProgressStatus() );
        }


        // 引数情報をすべてキューにする
        pushUpdateQueue(report);

        return this;
    }


    /**
     * キューへレポート情報を挿入
     * @param report
     */
    private void pushUpdateQueue( BDashReport report ){
        report.accessType = BDashReport.ACCESS_TYPE_UPDATE;
        synchronized (updateQueue) {
            updateQueue.add(report);
            LogUtil.s(">>>push queue count=" + updateQueue.size());
        }
    }

    /**
     * キューをクリア
     */
    private void clearUpdateQueue(){
        synchronized (updateQueue) {
            updateQueue.clear();
        }
    }


    /**
     * ポップアップを表示する
     *  - Fragment Show を実際に行う
     */
    private synchronized void showPopup(){
        if( isDismiss() ) {
            LogUtil.s( ">>abort showPopup");
            setProgressStatus(PROGRESS_EXCLUDED);
            return ;
        }

        createForcePopup();
    }

    /**
     * ポップアップを強制的に作成する
     */
    private synchronized void createForcePopup(){
        // receptions データの設定
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList( BDASH_SAVE_CURRENT, current_webReception.receptions);

        this.setArguments(bundle);
        this.setCancelable(false);
        this.show(fragmentManager, BDASH_FRAGMENT_TAG );

        // 表示状態のリセット
        isDismiss = false;

        setProgressStatus(PROGRESS_CONFIRM);

        LogUtil.s( ">>popup を作成します: "  );
    }

    /**
     * <pre>
     * 顧客提供API
     *  - Web接客のポップアップを安全に閉じます
     * </pre>
     */
    public void closeMessage(){
        synchronized (this) {
            // Update 中は通信エラーに状態を変える
            if( getProgressStatus() == PROGRESS_UPDATE ) {
                setProgressStatus(PROGRESS_UPDATE_ERROR);
            }
        }
        try {
            dismiss();
        }catch( Exception e ) {
        }
    }

    /**
     *　ダイアログの状態を保持
     * @param progress
     */
    private synchronized void setProgressStatus( int progress ) {
        progressStatus = progress;
        LogUtil.s( ">>>>progressStatus: " + progress);
    }

    /**
     * ダイアログの状態を取得
     * @return
     */
    public synchronized int getProgressStatus(){
        return progressStatus;
    }


    /**
     * ポップアップのクローズ状態を取得
     * @return
     */
    public boolean isDismiss(){
        return isDismiss;
    }


    /**
     * 表示中かを返す / progressStatus とは別
     * @return
     */
    public boolean isShowing(){
        int status = getProgressStatus();
        return status == PROGRESS_SHOWN;
    }

    /**
     * 埋め込み形式のアプリ接客を取得する
     * @param report BDashレポート
     * @param context コンテキスト
     * @param manager Fragmentマネージャー
     * @return
     */
    public BDashWebReception getWebReception(BDashReport report, Activity context, FragmentManager manager) {
        // 引数が要件を満たしていない場合何もしない
        if( report == null || context == null || manager == null ) {
            return this;
        }

        if( report.debugConnectUrl != null && !report.debugConnectUrl.startsWith("http") ) {
            LogUtil.s( ">>デバッグ接続先が URL でないので何もせず終了します");
            return this;
        }

        synchronized (this) {
            if (getProgressStatus() == PROGRESS_SHOWN || getProgressStatus() == PROGRESS_NEXT_TASK) {
                // 表示中or連続タスクの準備中の時
                return this;
            }

            // アプリ接客のサイズ指定を設定(埋め込みの場合、表示先のWebViewはアプリに依存するためAUTOにする)
            this.sizeUnit = POPUP_SIZE_UNIT_AUTO;

            // 通常のWeb接客の時と同様にUIコントロール変数を上書き
            this.context = context;
            this.fragmentManager = manager;

            // 状態のリセット
            if (getProgressStatus() == PROGRESS_UPDATE_ERROR || getProgressStatus() == PROGRESS_EXCLUDED) {
                setProgressStatus(PROGRESS_INIT);
            }

            if (getProgressStatus() == PROGRESS_INIT) {
                // 初期化処理
                clearUpdateQueue();
                isDismiss = false;
                isEmbedded = true;
                setProgressStatus(PROGRESS_UPDATE);
                update(report);
                return this;
            }
        }

        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.s(">>onCreate reception.");

        // 引数取得
        ArrayList<WebReceptionSettings> webReception= getArguments().getParcelableArrayList(BDASH_SAVE_CURRENT);
        current_webReception = new WebReceptionSettingsResponse(webReception);

        if( savedInstanceState != null ) {
            ArrayList<WebReceptionSettings> savedWebReceptions = savedInstanceState.getParcelableArrayList(BDASH_SAVE_CURRENT);
            if( savedWebReceptions != null ) {
                boolean result = savedWebReceptions.get(0).equals(current_webReception.receptions);
                LogUtil.s(">>一致するか？: " + result);
            }
            current_report = (BDashReport)savedInstanceState.getSerializable(BDASH_SAVE_REPORT);
        }

        int dialogStyle = current_webReception.getCurrent().hasFilter() ? R.style.BDashSDK_WebPopupModalTheme : R.style.BDashSDK_WebPopupTheme;
        setStyle(DialogFragment.STYLE_NO_TITLE, dialogStyle);

        LogUtil.s(">>  check sizeUnit: " + this.sizeUnit);
        // sizeUnitが"auto"の場合、ポップアップ表示前にサイズの単位を決定する
        if (sizeUnit.equals(POPUP_SIZE_UNIT_AUTO)) {
            boolean isTablet = DeviceUtil.isTablet(context);
            if (isTablet) {
                LogUtil.s(">> This device is Tablet.(px)");
                sizeUnit = POPUP_SIZE_UNIT_PX;
            } else {
                LogUtil.s(">> This device is Phone.(vw)");
                sizeUnit = POPUP_SIZE_UNIT_VW;
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        LogUtil.s(">>onCreateView reception.");

        LayoutInflater activityInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // SDKバージョン取得
        LogUtil.s(">> check SDK version: " + SDKConfig.SDK_VERSION);
        View view = activityInflater.inflate(R.layout.com_smart_bdash_mobile_reception_fragment_webview, container, false);
        ImageView closeButtonView = getCloseButtonView(view);

        WebReceptionSettings closeButtonSettings = current_webReception.getCurrent();
        String closeButtonSrc = closeButtonSettings.getCloseButtonSrc();
        LogUtil.s(">>  close button src: " + closeButtonSrc);
        if (closeButtonSrc != null && closeButtonSettings.validateCloseButton()) {
            // デフォルトの閉じるボタンを描画しない場合、新たに閉じるボタンをサーバーから取得＆描画する
            setButtonSrcToImageView(closeButtonView, closeButtonSrc);
        }

        closeButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 右上クローズボタンの処理/ 行動：エンドユーザー
                if( webView == null ){
                    dismiss();
                } else {
                    closeByJavascript(webView);
                }
            }
        });

        webView = view.findViewById(R.id.com_smart_bdash_mobile_reception_webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setWebViewClient(webViewClient);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        // file:// / content:// へのアクセスを無効化し、ローカルファイルの読取を防ぐ
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        // Cookie の許諾
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // デバッグ用だが本番も埋め込み. 検証アプリ以外では成り立つことがない
        if( debugListener!=null ) {
            webView.setWebChromeClient(new WebChromeClient(){
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    int pos = consoleMessage.sourceId().lastIndexOf("/");
                    String source;
                    if( pos == -1 )source = consoleMessage.sourceId();
                    else source = consoleMessage.sourceId().substring(pos);

                    debugListener.onMessage(String.format("[%s(%d)] ", source, consoleMessage.lineNumber()) + consoleMessage.message());
                    return true;
                }
            });
        }

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        LogUtil.s(">>onViewCreated reception.");
        super.onViewCreated(view, savedInstanceState);

        Dialog dialog = getDialog();
        // 枠線を完全に削除する
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (current_webReception.getCurrent().hasAllowClick()) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        }

        // AttributeからLayoutParamsを求める
        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();

        View v = dialog.getWindow().getDecorView();
        v.setPadding( 0,0,0,0);
        v.setVisibility(View.INVISIBLE);

        final View closeButton = getCloseButtonView(v);
        closeButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateLayout();
                adjustDialogMarginWhenCenter();

                closeButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        layoutParams = calculateViewSize();

        //LayoutParamsをセットする
        dialog.getWindow().setAttributes(layoutParams);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if( current_webReception != null ) {
            LogUtil.s(">>BDashWebReception::onSavedInstanceState");
            outState.putParcelableArrayList(BDASH_SAVE_CURRENT, current_webReception.receptions);
        }
        // report 状態を引き継げるようにする
        if( current_report != null ){
            outState.putSerializable(BDASH_SAVE_REPORT, current_report);
        }
    }

    /**
     * クリーンナップ処理
     */
    private void onCleanup(){
        isDismiss = true;
        setProgressStatus(PROGRESS_INIT);
        webview_isPageStarted = false;
        webView = null;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        LogUtil.s( ">>onDismiss: getProgressStatus()=" + getProgressStatus() );

        int progressStatus = getProgressStatus();

        // クリーンナップ処理
        onCleanup();

        if( progressStatus == PROGRESS_NEXT_TASK ){
            LogUtil.s( ">>next task");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (updateQueue) {
                        if(!updateQueue.isEmpty()) {
                            LogUtil.s( ">>run force update");
                            isDismiss = false;
                            setProgressStatus(PROGRESS_UPDATE);
                            startUpdateConnectThread();
                        }
                    }

                }
            });
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface _dialog) {
        super.onCancel(_dialog);
        LogUtil.s("cancel");
        dismiss();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LogUtil.s("BDashWebReception::onConfigurationChanged");

        // 画面回転したときはエフェクトは強制的に無効化する
        if( current_webReception!=null ) {
            current_webReception.getCurrent().effect = null;
        }

        // View 自体のサイズを計算し設定をする
        getDialog().getWindow().setAttributes(calculateViewSize());
        updateLayout();
        adjustDialogMarginWhenCenter();
    }

    /**
     * ダイアログが作られているか
     * @return
     */
    private boolean isCreatedDialog(){
        return webView!=null;
    }

    /**
     * Web接客のbaseUrlを取得
     * @return Web接客のbaseURL
     */
    public String getWebReceptionUrl() {
        return ConnectType.getWebViewBaseUrl();
    }

    /**
     * イベントタイプを取得
     * @param request リクエスト
     * @return イベントタイプ
     */
    public int getEventType(WebResourceRequest request) {
        String url = request.getUrl().toString();
        int eventType;

        if (url.startsWith(SCHEMA_INTERNAL)) {
            eventType = EVENT_INTERNAL;
        } else if (url.startsWith(SCHEMA_WEBVIEW)) {
            eventType = EVENT_WEBVIEW;
        } else if (url.startsWith(SCHEMA_EXTERNAL)) {
            eventType = EVENT_EXTERNAL;
        } else {
            eventType = EVENT_UNKNOWN;
        }

        return eventType;
    }

    /**
     * WebViewのイベント情報を取得
     * @param request リクエスト
     * @return イベント情報(key: リクエスト内のキー値、value: 整形したパラメータ)
     */
    public static HashMap<String, String> getEventInfo(WebResourceRequest request) {
        BDashSchema schema = new BDashSchema();
        String url = request.getUrl().toString();
        HashMap<String, String> eventInfo;

        switch(schema.parse(url)) {
            case SCHEMA_INTERNAL:
            case SCHEMA_WEBVIEW:
                eventInfo = schema.getCoordinationParam();
                break;
            case SCHEMA_EXTERNAL:
                eventInfo = new HashMap<>();
                eventInfo.put("url", schema.getParam());
                break;
            default:
                // popup/command/unknown
                eventInfo = new HashMap<>();
                eventInfo.put("param", schema.getParam());
        }

        return eventInfo;
    }

    /**
     * 閉じるボタンの取得
     * @param rootView
     * @return
     */
    private ImageView getCloseButtonView(View rootView) {
        ImageView closeButton;
        if (current_webReception.getCurrent().validateCloseButton()) {
            LogUtil.s(">>  close button can adjustment.");
            closeButton = rootView.findViewById(R.id.com_smart_bdash_mobile_reception_closeButton_adjustable);
        } else {
            LogUtil.s(">>  close button can not adjustment.");
            closeButton = rootView.findViewById(R.id.com_smart_bdash_mobile_reception_closeButton);
        }

        if (closeButton.getVisibility() == View.GONE) {
            closeButton.setVisibility(View.VISIBLE);
        }

        return closeButton;
    }

    /**
     * アプリ接客のサイズを取得(px)
     * @param metrics ディスプレイ情報
     * @param current ポップアップ設定
     * @return
     */
    private Point getPopupSizePx(DisplayMetrics metrics, WebReceptionSettings current) {
        LogUtil.s(">>getPopupSizePx called!!!");
        Point point = new Point(0, 0);
        float widthPx = (float) current.getWidthPx();
        float heightPx = (float) current.getHeightPx();

        if (widthPx == 0 || heightPx == 0) {
            // 数値が入っていない(もしくは異常値である)場合、レスポンスの"width"/"height"から算出
            float widthRatio = current.getWidth() / 100f;
            widthPx = metrics.widthPixels * widthRatio;
            LogUtil.s(">>  widthPx is 0! set widthPx: " + widthPx);
            LogUtil.s(">>  check metrics.widthPixels: " + metrics.widthPixels);

            float heightRatio = current.getHeight();
            heightPx = widthPx * heightRatio;
            LogUtil.s(">>  widthPx is 0! set widthPx: " + heightPx);
            LogUtil.s(">>  check heightRatio: " + heightRatio);
        } else {
            // ポップアップの高さが幅の3倍より大きい場合、高さの最大値を幅の3倍の値まで短縮
            // "vw"にて高さの係数("height")の最大値が3.0のため、"px"の時も合わせる
            if (heightPx > widthPx * 3f) {
                heightPx = widthPx * 3f;
            }

            // APIレスポンス中の"widthPx"/"heightPx"で渡されるサイズ(px)をそのまま表示すると端末毎の
            // ピクセル密度の影響を受けるため補正を入れる
            widthPx = DeviceUtil.getDisplayPxSize(Math.round(widthPx), metrics);
            heightPx = DeviceUtil.getDisplayPxSize(Math.round(heightPx), metrics);
        }

        point.x = (int)widthPx;
        point.y = (int)heightPx;

        LogUtil.s(">>  popup size(px) width: " + point.x + "px, height: " + point.y + "px");
        return point;
    }

    private void setButtonSrcToImageView(ImageView imageView, String url) {
        executor.execute(() -> {
            LogUtil.s(">>> [setButtonSrcToImageView] get close button source.");
            LogUtil.s(">>>   check url: " + url);
            Bitmap buttonSrc = getCloseButtonSrc(url);

            mainHandler.post(() -> {
                if (buttonSrc != null) {
                    LogUtil.s(">>> [setButtonSrcToImageView] set close button source.");
                    imageView.setImageBitmap(buttonSrc);
                } else {
                    // URLよりBitmapが取得できなかった場合、デフォルトの画像を表示
                    LogUtil.s(">>> [setButtonSrcToImageView] set default close button.");
                    imageView.setImageResource(R.drawable.com_smart_bdash_mobile_close_100x100);
                }
            });
        });
    }

    private Bitmap getCloseButtonSrc(String urlStg) {
        try {
            URL imageUrl = new URL(urlStg);
            InputStream imageIs;
            imageIs = imageUrl.openStream();
            return BitmapFactory.decodeStream(imageIs);
        } catch (Exception e) {
            return null;
        }
    }
}
