package com.f_scratch.bdash.mobile.analytics;

import android.content.Context;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.f_scratch.bdash.mobile.analytics.config.TestSettings;
import com.f_scratch.bdash.mobile.analytics.connect.ConnectClient;
import com.f_scratch.bdash.mobile.analytics.connect.ConnectClientController;
import com.f_scratch.bdash.mobile.analytics.connect.ConnectType;
import com.f_scratch.bdash.mobile.analytics.connect.IConnectAsyncResponse;
import com.f_scratch.bdash.mobile.analytics.connect.RequestParam;
import com.f_scratch.bdash.mobile.analytics.helper.facade.BDashWebReceptionAccess;
import com.f_scratch.bdash.mobile.analytics.helper.util.LogUtil;
import com.f_scratch.bdash.mobile.analytics.model.Device;
import com.f_scratch.bdash.mobile.analytics.util.LogicUtil;
import com.f_scratch.bdash.mobile.analytics.web_reception.BDashReport;
import com.f_scratch.bdash.mobile.analytics.web_reception.BDashWebReceptionController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class WebReceptionTest {

    private static Context context;

    final private static String STG_WEB_RECEPTION_URL = "https://receptions.bdash.works/mobile/receptions";

    /***
     * テストクラスの初期化
     */
    @BeforeClass
    public static void initializeTestClass() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        bufferClear();
    }

    /**
     * コントローラー初期化(boot event)
     *
     */
    @Test
    public void testcase_init() {
        Device.getInstance().appId = TestSettings.STG.WebReception.appId;
        Device.getInstance().accountId = TestSettings.STG.WebReception.accountId;
        Device.getInstance().dataViewIds = TestSettings.STG.WebReception.dataView;
        BDashWebReceptionController.getInstance().init(context);
        try {
            Thread.sleep(3000);
        } catch (Exception ignored) {
        }
    }

    /**
     * <pre>STG 環境での「Web接客 顧客設定 取得API」の検証
     * アクション
     *  - トマト(1000)/りんご(1001)/バナナ(1002) をまとめて取得
     * 期待値
     *  - 例外が発生せず 200 が帰ってくること
     *  - 件数が 1件以上あること
     * </pre>
     */
    @Test
    public void testCase_connectWebReceptionSettingAPI_byStg() throws Exception {
        run_connectWebReceptionSettingAPI(
                STG_WEB_RECEPTION_URL,
                TestSettings.STG.WebReception.appId,
                TestSettings.STG.WebReception.accountId,
                TestSettings.STG.WebReception.dataView,
                new String[]{"121234"}
        );
    }

    private void run_connectWebReceptionSettingAPI(String url, String appId, String accountId, String dataViewIds, String[] targets) throws Exception {
        LogUtil.s(">>testCase_connect");

        BDashReport report = new BDashReport();
        report.targets = targets;
        Device.getInstance().appId = appId;
        Device.getInstance().accountId = accountId;
        Device.getInstance().dataViewIds = dataViewIds;
        BDashWebReceptionAccess.set_BDashReport_setAccessType(report, BDashWebReceptionAccess.get_BDashReport_AccessTypeUpdate(report));

        String request = LogicUtil.createJsonRequestWithCommonParameter(report);

        ConnectClientController c = new ConnectClientController();
        RequestParam param = ConnectClient.getDefaultRequestParam();
        param.setConcatUrl(url);
        c.connect(new IConnectAsyncResponse() {
            @Override
            public void onConnect(ConnectClient client) throws Exception {
            }

            @Override
            public void onPostExecuteImpl(ConnectClient client, Throwable exception) throws Exception {
                if (exception == null) {
                    LogUtil.s(client.getResponse());
                } else {
                    exception.printStackTrace();
                }

                assertEquals(exception == null, true);
                assertEquals(client.getResponseCode() == 200, true);

                Gson gson = getDefaultGson();

                // 受信クラスを設定
                Class<?> clazz = Class.forName("com.f_scratch.bdash.mobile.analytics.web_reception.WebReceptionSettingsResponse");
                Object current = gson.fromJson(client.getResponse(), clazz);
                assertThat(current).as("json is nut found.").isNotNull();

                // 件数が1件以上あるか確認
                Method existCurrent = clazz.getDeclaredMethod("existCurrent");
                existCurrent.setAccessible(true);
                assertThat((Boolean) existCurrent.invoke(current)).as("データが1件もありません").isTrue();

            }
        }, ConnectType.DEBUG_API, request, param);

        c.waitConnect();
    }

    public static Gson getDefaultGson() {
        return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    }

    private static void bufferClear() {
        EventLogManager manager = EventLogManager.getInstance();

        while (manager.getEventSize() > 0) {
            manager.lock();
            manager.unlockCommit();
        }
        manager.save();
    }
}
