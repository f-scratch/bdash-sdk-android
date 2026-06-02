package com.f_scratch.bdash.mobile.analytics.web_reception;

import android.content.Context;

import com.f_scratch.bdash.mobile.analytics.MobileSDKManager;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Web接客の初期化や BDashWebReception インスタンス生成を管理するコントローラー
 */
public class BDashWebReceptionController {

    /** シングルトン */
    private static BDashWebReceptionController instance;

    /** スレッドプール */
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1,1,60,TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
    private ThreadPoolExecutor threadTrackingExecutor = new ThreadPoolExecutor(1,1,60,TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());

    /** 初期化フラグ */
    private boolean isInitialized;


    // デバッグ用インターフェイス / public だがアンダーバー開始とし内部インターフェイスであることを示唆
    public interface _DebugLogMessage {
        void onMessage(String str);
    }

    /**
     * 非公開コンストラクタ
     */
    private BDashWebReceptionController(){
        this.threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    /**
     * Web接客のコントローラーを取得する
     * @return
     */
    public static synchronized BDashWebReceptionController getInstance() {
        if( instance == null ) {
            instance = new BDashWebReceptionController();
        }
        return instance;
    }

    /**
     * 公開API
     *  - コントローラーを初期化する
     */
    public synchronized void init( Context context ){
        if( isInitialized ){
            return ;
        }
        MobileSDKManager.getInstance_needCreate(context.getApplicationContext(), null);

        // デバッグの向き先は無効(null)で Boot のトラッキングレポートを実施
        connectByBoot(null);

        isInitialized = true;
    }

    /**
     * 公開API
     *  - Web接客のポップアップを生成する
     * @return BDashWebReception
     */
    public BDashWebReception newPopup(){
        return BDashWebReception.create();
    }


    /**
     * Boot のトラッキングレポート通信を行う
     * @param url
     */
    void connectByBoot( String url ){
        BDashReport report = new BDashReport();
        report.accessType = BDashReport.ACCESS_TYPE_BOOT;
        report.debugConnectUrl = url;
        BDashWebReception.requestWebReception(report, null);
    }

    /**
     * Update 用の Executor を返す
     * @return
     */
    ThreadPoolExecutor getThreadPoolExecutor(){
        return threadPoolExecutor;
    }

    /**
     * トラッキングレポート用の Executor を返す
     * @return
     */
    ThreadPoolExecutor getThreadTrackingPoolExecutor(){
        return threadTrackingExecutor;
    }

}
