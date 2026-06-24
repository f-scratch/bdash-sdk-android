package com.smart_bdash.mobile.analytics;

import android.content.Context;

import com.smart_bdash.mobile.analytics.model.Device;
import com.smart_bdash.mobile.analytics.model.User;
import com.smart_bdash.mobile.analytics.model.config.SDKConfig;
import com.smart_bdash.mobile.analytics.util.LogicUtil;
import com.smart_bdash.mobile.analytics.util.StorageUtil;

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

    /**
     * 広告ID(adId)収集に対するユーザー同意状態<br>
     *  ・default==false（同意が明示されるまで adId を取得しない）
     */
    private boolean userConsentGranted = false;

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


    /**
     * 広告ID(adId)収集に対するユーザー同意を設定します<br>
     *  ・ホストアプリは、プライバシーポリシー等に基づきユーザーから同意を取得した後に true を設定してください<br>
     *  ・同意（true）が設定された場合のみ、次回の SDK 初期化時に adId の取得・送信を行います<br>
     *  ・同意を取り消す（false）場合は、保持している adId をクリアします
     *
     * @param hasConsent ユーザーが adId 収集に同意した場合は true
     */
    public static void setUserConsent(boolean hasConsent) {
        if( instance == null ) {
            // SDK 未初期化。Tracker.getInstance() 等での初期化後に再設定してください
            return;
        }
        instance.userConsentGranted = hasConsent;

        if( !hasConsent ) {
            // 同意が取り消されたので保持している adId をクリアする
            User.getInstance().setAdId(null);
            User.getInstance().setCanUseAdId(false);
            try {
                User.getInstance().save();
            } catch( Exception e ) {
                // 保存失敗時もメモリ上の adId はクリア済みのため送信はされない
            }
        }
    }

    /**
     * 広告ID(adId)収集に対するユーザー同意状態を取得します
     *
     * @return 同意済みの場合は true（デフォルトは false）
     */
    public boolean hasUserConsent() {
        return userConsentGranted;
    }


    /***
     * SDK バージョンの取得
     * @return
     */
    public static String getSdkVersion(){
        return SDK_VERSION;
    }

}
