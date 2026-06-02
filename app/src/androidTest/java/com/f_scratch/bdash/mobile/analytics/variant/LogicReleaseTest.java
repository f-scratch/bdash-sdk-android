package com.f_scratch.bdash.mobile.analytics.variant;

import android.content.Context;

import com.f_scratch.bdash.mobile.analytics.BuildConfig;
import com.f_scratch.bdash.mobile.analytics.Tracker;
import com.f_scratch.bdash.mobile.analytics.helper.util.LogUtil;
import com.f_scratch.bdash.mobile.analytics.model.Device;
import com.f_scratch.bdash.mobile.analytics.model.User;

import org.junit.Assert;

import static org.junit.Assert.*;

/**
 * SDK リリース前にテストさせる<br>
 *  ・このクラスがグリーンにならない場合、不備があると判断<br>
 *  ・呼び出しは CoreReleaseTest.testSdkForRelease() にて
 */
public class LogicReleaseTest {

    private static Tracker tracker;
    private static Context context;

    /***
     * ・テストコードの初期化<br>
     * @param con コンテキスト
     */
    public static void init( Context con) {
        context = con;
        tracker = Tracker.getInstance(context);

        Device.getInstance().appId     = "XXXXX-FS-APP";
        Device.getInstance().accountId = "BD-AST000";
    }

    /***
     * ビジターID のテストケース<br>
     *  [期待値]<br>
     *   UUID と visitorId が等しいこと
     */
    public static void testCase_visitorId(){
        LogUtil.s("     UUID: " + User.getInstance().getUniqueId() );
        LogUtil.s("visitorId: " + tracker.getVisitorId());

        assertEquals(tracker.getVisitorId(), User.getInstance().getUniqueId());
    }


    /***
     * <pre>すべてのビルド環境が「リリース環境」かどうかを検証する
     * 期待値
     *  - assert ですべてに合格すること
     * </pre>
     */
    public static void testCase_buildVariants() throws Exception{

        testCase_connectConfig();

        assertEquals(com.f_scratch.bdash.mobile.analytics.util.LogUtil.isDebuggable(), false);
        LogUtil.s(">>debug log は off です");
    }

    /***
     * <pre>すべてのビルド環境が「開発環境」かどうかを検証する
     * 期待値
     *  - assert ですべてに合格すること
     * </pre>
     */
    public static void testCase_buildVariantsByDevelop() throws Exception{

        testCase_connectConfigByDevelop();

        assertEquals(com.f_scratch.bdash.mobile.analytics.util.LogUtil.isDebuggable(), true);
        LogUtil.s(">>debug log は on です");
    }

    /**
     * <pre>通信環境が「リリース環境」かどうかを検証する
     * 期待値
     *  - assert ですべてに合格すること
     * </pre>
     */
    public static void testCase_connectConfig() throws Exception{

        assertEquals(getAPI_WebReceptionSettingUrl(), "https://receptions.smart-bdash.com/mobile/v2/receptions");
        LogUtil.s(">>Web接客 顧客情報取得 API 環境は本番です");

        assertEquals(getAPI_WebReceptionViewUrl(), "https://receptions.smart-bdash.com/v2/");
        LogUtil.s(">>Web接客 WebView API 環境は本番です");

        assertEquals(getAPIUrl(), "https://trackersdk.smart-bdash.com/v2/tracking");
        LogUtil.s(">>Tracker API 環境は本番です");

        assertEquals(getAPITokenUrl(), "https://mobile.smart-bdash.com/v2/notification");
        LogUtil.s(">>Notification Token API 環境は本番です");
    }

    /**
     * <pre>通信環境が「検証環境」かどうかを検証する
     * 期待値
     *  - assert ですべてに合格すること
     * </pre>
     */
    public static void testCase_connectConfigByDevelop() throws Exception{
        assertEquals(getAPI_WebReceptionSettingUrl(), "https://receptions.bdash.works/mobile/v2/receptions");
        LogUtil.s(">>Web接客 顧客情報取得 API 環境は STG です");

        assertEquals(getAPI_WebReceptionViewUrl(), "https://receptions.bdash.works/v2/");
        LogUtil.s(">>Web接客 WebView API 環境は STG です");

        assertEquals(getAPIUrl(), "https://trackersdk.bdash.works/v2/tracking");
        LogUtil.s(">>Tracker API 環境は STG です");

        assertEquals(getAPITokenUrl(), "https://mobile.bdash.works/v2/notification");
        LogUtil.s(">>Notification Token API 環境は STG です");
    }


    /**
     * <pre>SDK バージョンが「リリース環境」かどうかを検証する
     * 期待値
     *  - assert ですべてに合格すること
     * </pre>
     */
    public static void testCase_versionCheck() {
        Assert.assertEquals( "Sdk Version が Release 向けではありません", BuildConfig.SDK_VERSION, BuildConfig.SDK_RELEASE_VERSION);
        LogUtil.s(String.format(">>Sdk Version は %s です", BuildConfig.SDK_VERSION) );
    }

    /***
     * 以下リフレクション
     * @return
     * @throws Exception
     */
    private static String getAPIUrl() throws Exception {
        Class<?> clazz = Class.forName("com.f_scratch.bdash.mobile.analytics.connect.ConnectConfig");
        return (String)clazz.getField("API_URL").get(clazz);
    }

    private static String getAPITokenUrl() throws Exception {
        Class<?> clazz = Class.forName("com.f_scratch.bdash.mobile.analytics.connect.ConnectConfig");
        return (String)clazz.getField("API_TOKEN_URL").get(clazz);
    }

    private static String getAPI_WebReceptionSettingUrl() throws Exception {
        Class<?> clazz = Class.forName("com.f_scratch.bdash.mobile.analytics.connect.ConnectConfig");
        return (String)clazz.getField("API_WEB_RECEPTION_SETTING_URL").get(clazz);
    }

    private static String getAPI_WebReceptionViewUrl() throws Exception {
        Class<?> clazz = Class.forName("com.f_scratch.bdash.mobile.analytics.connect.ConnectConfig");
        return (String)clazz.getField("API_WEB_RECEPTION_VIEW_URL").get(clazz);
    }
}
