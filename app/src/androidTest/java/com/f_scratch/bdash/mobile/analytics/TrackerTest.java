package com.f_scratch.bdash.mobile.analytics;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.f_scratch.bdash.mobile.analytics.helper.facade.TrackerAccess;
import com.f_scratch.bdash.mobile.analytics.helper.util.LogUtil;
import com.f_scratch.bdash.mobile.analytics.model.Device;
import com.f_scratch.bdash.mobile.analytics.model.config.JsonKey;
import com.f_scratch.bdash.mobile.analytics.util.StorageUtil;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URLEncoder;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * BDash Mobile SDK のテスト<br>
 * ・"testCase_" から始まるメソッドが各単体テスト項目のメソッド<br>
 * ・パラメータについては「SDK仕様・JSONパラメーター資料.xlsx」を参照する<br>
 */
@RunWith(AndroidJUnit4.class)
public class TrackerTest {

    private static Tracker tracker;

    /***
     * テストクラスの初期化
     */
    @BeforeClass
    public static void initializeTestClass() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        tracker = Tracker.getInstance(context);

        bufferClear();
    }

    /***
     * テストメソッド実行前処理
     */
    @Before
    public void setup() {
        LogUtil.s(">>>>" + getClass().getSimpleName() + "::Setup");
        bufferClear();
    }

    /***
     * ScreenViewBuilder の検証<br>
     * ログデータを1件生成して通信を行う<br>
     * [期待値]<br>
     * ・通信パラメーターに問題ないこと<br>
     * ・Tracking データが1件であること<br>
     * [エビデンス]<br>
     * ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_ScreenViewBuilder01() throws Exception {
        LogUtil.s(">>testCase_ScreenViewBuilder01");
        bufferClear();
        screenBuilder_add();
        run_sync();
    }

    /***
     * ScreenViewBuilder の検証<br>
     * ログデータを送信しきい値の個数生成して通信を行う<br>
     * [期待値]<br>
     * ・通信パラメーターに問題ないこと<br>
     * ・Tracking データが1件であること<br>
     * [エビデンス]<br>
     * ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_ScreenViewBuilder02() throws Exception {
        LogUtil.s(">>testCase_ScreenViewBuilder02");
        bufferClear();
        for (int i = 0; i < 10; i++) {
            screenBuilder_add();
        }
        run_sync();
    }

    /***
     * ScreenViewBuilder の検証<br>
     * ログデータを一度に送れる最大件数の分生成して通信を行う<br>
     * [期待値]<br>
     * ・通信パラメーターに問題ないこと<br>
     * ・Tracking データが1件であること<br>
     * [エビデンス]<br>
     * ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_ScreenViewBuilder03() throws Exception {
        LogUtil.s(">>testCase_ScreenViewBuilder03");
        bufferClear();
        for (int i = 0; i < 100; i++) {
            screenBuilder_add();
        }
        run_sync();
    }

    /***
     * EventBuilder の検証<br>
     *  ・各パラメーターが許諾している最大長を設定して1件データとして通信を行う<br>
     *  [期待値]<br>
     *  ・通信パラメーターに問題ないこと<br>
     *  ・Tracking データが1件であること<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_EventBuilder01() throws Exception {
        LogUtil.s(">>testCase_EventBuilder01");
        bufferClear();
        eventBuilder_setMaxLength_add();
        run_sync();
    }

    /***
     * EventBuilder の検証<br>
     *  ・各パラメーターが許諾している最大長を設定して送信しきい値(10件)データとして通信を行う<br>
     *  [期待値]<br>
     *  ・通信パラメーターに問題ないこと。複数件でのパラメーターチェック<br>
     *  ・Tracking データが10件であること<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_EventBuilder02() throws Exception {
        LogUtil.s(">>testCase_EventBuilder02");
        bufferClear();
        for (int i = 0; i < 10; i++) {
            eventBuilder_setMaxLength_add();
        }
        run_sync();
    }

    /***
     * EventBuilder の検証<br>
     *  ・各パラメーターが許諾している最大長を設定して一度に送れる最大件数(100件)データとして通信を行う<br>
     *  [期待値]<br>
     *  ・Tracking データが100件であること<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_EventBuilder03() throws Exception {
        LogUtil.s(">>testCase_EventBuilder03");
        bufferClear();
        for (int i = 0; i < 100; i++) {
            eventBuilder_add();
        }
        run_sync();
    }

    /***
     * ExceptionBuilder の検証<br>
     *  ・各パラメーターが許諾している最大長を設定して1件データとして通信を行う<br>
     *  [期待値]<br>
     *  ・通信パラメーターに問題ないこと<br>
     *  ・Tracking データが1件であること<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_ExceptionBuilder01() throws Exception {
        LogUtil.s(">>testCase_ExceptionBuilder01");
        bufferClear();
        exceptionBuilder_setMaxLength_add();
        run_sync();
    }

    /***
     * ExceptionBuilder の検証<br>
     *  ・各パラメーターが許諾している最大長を設定して送信しきい値(10件)データとして通信を行う<br>
     *  [期待値]<br>
     *  ・通信パラメーターに問題ないこと。複数件でのパラメーターチェック<br>
     *  ・Tracking データが10件であること<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_ExceptionBuilder02() throws Exception {
        LogUtil.s(">>testCase_ExceptionBuilder02");
        bufferClear();
        for (int i = 0; i < 10; i++) {
            exceptionBuilder_setMaxLength_add();
        }
        run_sync();
    }

    /***
     * ExceptionBuilder の検証<br>
     *  ・各パラメーターが許諾している最大長を設定して一度に送れる最大件数(100件)データとして通信を行う<br>
     *  [期待値]<br>
     *  ・Tracking データが100件であること<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_ExceptionBuilder03() throws Exception {
        LogUtil.s(">>testCase_ExceptionBuilder03");
        bufferClear();
        for (int i = 0; i < 100; i++) {
            exceptionBuilder_add();
        }
        run_sync();
    }

    /***
     * 仕様を満たさない通信データの検証<br>
     *  ・Tracker.send() にユーザーが定義した適当な Map データをいれ送信する<br>
     *  [期待値]<br>
     *  ・サーバーがバグらないこと<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_SendError() throws Exception {
        LogUtil.s(">>testCase_SendError");

        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        HashMap<String, Object> userMap = new HashMap<String, Object>();
        userMap.put("errorTest", "asdaSADA");
        userMap.put("errorDevice", "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ");
        userMap.put("errorHoge", "aaaaaaaaa");

        tracker.send(userMap);
        run_sync();
    }

    /***
     * send メソッドの検証
     *  ・ScreenViewBuilder に連番のスクリーン名を設定し、 送信しきい値を満たすまで send コールする<br>
     *    また1件目データだけ下記の API で準備を行う<br>
     *  [準備]<br>
     *  ・setLoginUser() で任意の文字列を設定する<br>
     *  ・setRelationalKey() で任意の文字列を設定する<br>
     *  ・setUserMap() で任意のキーバリューを1件設定する<br>
     *  [期待値]<br>
     *  ・通信が1回行われること<br>
     *  ・1件目のデータに bootType が入っていること<br>
     *  ・1件目のデータに loginUserId が入っていること<br>
     *  ・1件目のデータに relationalKey, relationalValue  が入っていること<br>
     *  ・1件目のデータに userMap が入っていること<br>
     *  ・Tracing データが10件であること<br>
     *  ・スクリーンネームが連番であること<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_sendScreenView() throws Exception {
        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        HashMap<String, String> map = new HashMap<>();
        map.put("userKey", "userValue");

        tracker.setLoginUser("myuser");
        tracker.setRelationalKey("mykey");
        tracker.setUserMap(map);

        for (int i = 0; i < 10; i++) {
            tracker.setScreenName(String.format("画面その%d", i + 1));
            tracker.send(new ScreenViewBuilder().build());
        }
        TrackerAccess.getConnectController(tracker).waitConnect();
        assertEquals("networkError", getBufferSize(), 0);
    }

    /***
     * sync メソッドの検証
     *  ・ScreenViewBuilder に連番のスクリーン名を設定し send を3回コールする<br>
     *    その後 sync() を呼び出す<br>
     *  [期待値]<br>
     *  ・通信が1回行われること<br>
     *  ・1件目のデータに bootType が入っていること<br>
     *  ・Tracing データが3件であること<br>
     *  ・スクリーンネームが連番であること<br>
     *  [エビデンス]<br>
     *  ・リクエスト(又はレスポンス)ログデータとして残す
     */
    @Test
    public void testCase_sync() throws Exception {
        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        for (int i = 0; i < 3; i++) {
            tracker.setScreenName(String.format("画面その%d", i + 1));
            tracker.send(new ScreenViewBuilder().build());
        }
        tracker.sync();
        TrackerAccess.getConnectController(tracker).waitConnect();
        assertEquals("networkError", getBufferSize(), 0);
    }

    /***
     * send メソッドの異常ケースの検証
     *  ・ScreenViewBuilder に連番のスクリーン名を設定し send を1万回コールする<br>
     *
     *  [期待値]<br>
     *  ・SDK がクラッシュしないこと<br>
     *  ・連番データがサーバーに届くこと<br>
     */
    @Test
    public void testCase_SendLoop() throws Exception {
        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        for (int i = 0; i < 1000; i++) {
            tracker.setScreenName(String.format("異常系%d", i + 1));
            tracker.send(new ScreenViewBuilder().build());
        }
        TrackerAccess.getConnectController(tracker).waitConnect();
    }

    /***
     * 文字コードの検証<br>
     *  ・URLエンコードや特殊文字を入力してサーバーにデータを UTF-8 で送る
     *  [期待値]<br>
     *  ・文字化けしないこと
     */
    @Test
    public void testCase_CharEscape() throws Exception {

        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        try {
            String encode = URLEncoder.encode(String.format("テスト"), "UTF-8");
            LogUtil.s(" encode: " + encode);
            tracker.setScreenName(encode);
            tracker.setRelationalKey("\uD867\uDE3Dテスト");
            tracker.setLoginUser("\n\"\\\\x22display \\x22");


            tracker.send(new EventBuilder().setEventValue("yj-3ry3e://cb#access_token=Y9t9xjtkt6YOZ1oTBQpEHKCPuWKbolcMR9auHFIK1kD_KiLW6m85pnOS7ilg5xIuxVPH3LT2oTVcliyG0ceNIB2hAIcbAAlDdDmeCU1zM08Z9ivACHb60cE_XaX9sWo8y4kgD_xhDwtJ4MjhoTpq22Sai1Kj5pLT3NhxewZ5wrIBB5KrEdYT9KXvN8ZF0d26ieBXYVg2xrEIySFaO0SGfaBiOelmxE2ngWFFc6z8ByIxmyTSjaWPgttu21zjSk7zWeKu9XnX3nxzdNmJZpQsfIZf.rph7GEpNg3QaK2ABBtWacgVnt10FVeIa5WMC1KzES0VgRfCprrNEGTXiunv.29cFQNddfDPZhC788avHZtyfLA83JMzMpzuFPJj9CHFdd.2Bd7kTVVNYxNbiOM.0lgsolXMtzod3shlLKb6NFq8.nMIGBFaJ.HvIsaQsZ6YAqwjleWrYxEJooeIpNWCaMwGabSE8MZucpFFXUknNivjJWtESsmEXJ2H6BAHSxPO21XVWRio3cgMfPbszdVAZ6rR_gwjO1n7MzEBMn54o.k2I6wdExsxCQQ1EdmgSxV1hMed7EhNau036_PlCpz61LUcE3J_VFYVWW1dBg--&token_type=bearer&expires_in=3600&state=44GC44GC54Sh5oOF&id_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvYXV0aC5sb2dpbi55YWhvby5jby5qcCIsInVzZXJfaWQiOiJYQlMyQUQyWFhGQUNTUENHNFBTT1pOV1A3WSIsImF1ZCI6ImRqMHphaVpwUFUxd2VYVjZjRmRQTUVoeVp5WnpQV052Ym5OMWJXVnljMlZqY21WMEpuZzlZamstIiwiaWF0IjoxNDYwOTQ5NDczLCJleHAiOjE0NjMzNjg2NzMsIm5vbmNlIjoiVTBGTlRDQnBjeUJFWldGa0xnPT0ifQ.TeihJLPKXnUMFh3NVmoOq3y-S9VpHbbTBNmxbckoTTc").build());
            tracker.sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        TrackerAccess.getConnectController(tracker).waitConnect();
    }

    /***
     * send() 実行中の sync() を呼び出しの検証<br>
     *  [期待値]<br>
     *   ・TestClass としては send 中の sync は Looper が存在しないので例外が出る.<br>
     *       => 想定のフローとして進んでいるのでOK<br>
     *  [備考]<br>
     *   ・検証アプリでも同等の検査をすること<br>
     */
    @Test
    public void testCase_SendSync() throws Exception {

        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        try {
            for (int i = 0; i < 10; i++) {
                tracker.setScreenName(String.format("画面%d", i + 1));
                tracker.send(new ScreenViewBuilder().build());
            }
            // send 中の sync は TestClass では Looper が存在しないので例外が出る => 例外が出ると想定のフローとして進んでいるのでOK
            tracker.sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        TrackerAccess.getConnectController(tracker).waitConnect();
    }

    /***
     * ストレージの検証<br>
     *  ・10件データを生成してファイルサイズを調べる<br>
     *  ・100件データを生成してファイルサイズを調べる<br>
     *  ・1000件データを生成してファイルサイズを調べる<br>
     *  [エビデンス]<br>
     *  ・ログデータとして残す
     */
    @Test
    public void testCase_StorageSizeCheck() {
        testCase_StorageSizeCheck(10);
        testCase_StorageSizeCheck(100);
        testCase_StorageSizeCheck(1000);
    }

    private void testCase_StorageSizeCheck(int count) {
        bufferClear();
        for (int i = 0; i < count; i++) {
            eventBuilder_setMaxLength_add();
        }

        saveEventLog();
        int size = StorageUtil.getFileSize(StorageUtil.FileType.EVENT_LOGS);
        LogUtil.s(String.format("filesize(%4d件): %4dKB(%d)", count, size / 1024, size));
        if (size / 1024 / 1024 > 0) {
            LogUtil.s(String.format(" >>%dMB", size / 1024 / 1024));
        }
        bufferClear();
        saveEventLog();
    }

    /**
     * ScreenViewBuilder を利用してログデータを追加する
     */
    private void screenBuilder_add() {
        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        ScreenViewBuilder builder = new ScreenViewBuilder();
        HashMap<String, String> userMap = new HashMap<>();

        addTestData(builder.build(), userMap);
    }

    /**
     * EventBuilder を利用してログデータを追加する
     */
    private void eventBuilder_add() {
        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        EventBuilder builder = new EventBuilder()
                .setActionName("ACTION_NAME")
                .setEventValue("EVENT_VALUE")
                .setCategoryName("CATEGORY_NAME")
                .setLabel("LABEL");
        HashMap<String, String> userMap = new HashMap<>();

        addTestData(builder.build(), userMap);
    }

    /***
     * EventBuilder を利用して、各パラメーターが許諾している最大長を設定してログデータを追加する
     */
    private void eventBuilder_setMaxLength_add() {
        // accountId を 128byte で設定
        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        HashMap<String, String> eventMap = new HashMap<>();
        HashMap<String, String> userMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            eventMap.put(String.format("EVENT_KEY%d", i), String.format("EVENT_VAL%d", i));
            userMap.put(String.format("USER_KEY%d", i), String.format("USER_VAL%d", i));
        }

        EventBuilder builder = new EventBuilder()
                .setActionName("ACTION_NAME")
                .setEventValue("EVENT_VALUE")
                .setCategoryName("CATEGORY_NAME")
                .setLabel("LABEL")
                .setEventMaps(eventMap);

        addTestData(builder.build(), userMap);
    }

    /***
     * ExceptionBuilder を利用してログデータを追加する
     */
    private void exceptionBuilder_add() {
        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        ExceptionBuilder builder = new ExceptionBuilder()
                .setFatal(true)
                .setName("CRASH_NAME")
                .setDescription("CRASH_DESCRIPTION");
        HashMap<String, String> userMap = new HashMap<>();

        addTestData(builder.build(), userMap);
    }

    /***
     * EventBuilder を利用して、各パラメーターが許諾しているミニマムを設定してログデータを追加する
     */
    private void exceptionBuilder_setMaxLength_add() {
        // accountId を 128byte で設定
        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        HashMap<String, String> userMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            userMap.put(String.format("USER_KEY%032d", i), String.format("USER_VAL%0128d", i));
        }

        ExceptionBuilder builder = new ExceptionBuilder()
                .setFatal(true)
                .setName("CRASH_NAME")
                .setDescription("CRASH_DESCRIPTION");

        addTestData(builder.build(), userMap);
    }

    /***
     * send スレッドの検証
     */
    @Test
    public void testCase_Thread() {
        for (int i = 0; i < 700; i++) {
            exceptionBuilder_setMaxLength_add();
        }

        for (int i = 0; i < 5; i++) {
            LogUtil.s(String.format("%d件目のsend", i + 1));
            tracker.send(new ScreenViewBuilder().build());

            // 10秒
            try {
                Thread.sleep(1000 * 10);
                if (i > 3) Thread.sleep(1000 * 2);
            } catch (Exception ignored) {
            }
        }
    }

    /***
     * EventBuilder の userMap/eventMap の出力順番の確認
     * ログを見て目視で確認する
     */
    @Test
    public void testCase_hashMapData() throws Exception {
        LogUtil.s(">>testCase_EventBuilder04");

        Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
        Device.getInstance().accountId = "BD-K35Y4G";

        for (int m = 0; m < 2; m++) {
            HashMap<String, String> eventMap = new HashMap<String, String>();
            HashMap<String, String> userMap = new HashMap<String, String>();
            for (int i = 0; i < 6; i++) {
                eventMap.put(String.format("ekey%d", i), String.format("%d", i));
                userMap.put(String.format("ukey%d", i), String.format("%d", i));
            }
            EventBuilder builder = new EventBuilder();
            builder.setEventMaps(eventMap);
            tracker.setUserMap(userMap);
            tracker.send(builder.build());
            run_sync();
        }
    }

    private void run_sync() throws Exception {
        run_sync(0);
    }

    private void run_sync(int size) throws Exception {
        if (size < 0) size = 0;

        if (!EventLogManager.getInstance().isLock()) {
            tracker.sync();
        } else {
            try {
                Thread.sleep(5000);
            } catch (Exception ignored) {
            }
        }
        TrackerAccess.getConnectController(tracker).waitConnect();

        assertThat(getBufferSize())
                .as("networkError")
                .isEqualTo(size);
    }

    private static void bufferClear() {
        EventLogManager manager = EventLogManager.getInstance();

        while (manager.getEventSize() > 0) {
            manager.lock();
            manager.unlockCommit();
        }
        manager.save();
    }

    private static void saveEventLog() {
        try {
            EventLogManager.getInstance().save();
        } catch (Exception ignored) {
        }
    }

    private int getBufferSize() {
        return EventLogManager.getInstance().getEventSize();
    }

    private static void addTestData(HashMap<String, Object> build, HashMap<String, String> userMap) {
        // パラメーターの設定
        build.put(JsonKey.KEY_SCREEN_NAME, "MY_SCREEN");
        build.put(JsonKey.KEY_LOGIN_USERID, "LOGIN_USER");
        build.put(JsonKey.KEY_RELATIONAL, "RELATION_KEY");
        build.put(JsonKey.KEY_BOOT_TYPE, Tracker.BootType.BOOT_PUSH);
        if (userMap != null && !userMap.isEmpty()) {
            build.put(JsonKey.KEY_USER_MAP, userMap);
        }

        EventLogManager.getInstance().addEventLog(build);
    }
}