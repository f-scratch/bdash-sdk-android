package com.f_scratch.bdash.mobile.analytics;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.f_scratch.bdash.mobile.analytics.connect.factory.MockFailedConnectClientControllerCreator;
import com.f_scratch.bdash.mobile.analytics.helper.util.LogUtil;
import com.f_scratch.bdash.mobile.analytics.helper.facade.ConnectAccess;
import com.f_scratch.bdash.mobile.analytics.helper.facade.NotificationAccess;
import com.f_scratch.bdash.mobile.analytics.model.Device;
import com.f_scratch.bdash.mobile.analytics.notification.BDashBusyException;
import com.f_scratch.bdash.mobile.analytics.notification.BDashNotification;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * <pre>
 * 通知周りのテストケース
 *
 * </pre>
 * Created by fujimaru on 2016/12/12.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationTest {

    private static Context context;
    private static BDashNotification notification;

    final private static String TOKEN_API_STG_URL = "https://mobile.bdash.works/notification";

    /**
     * テストクラスの初期化
     */
    @BeforeClass
    public static void initializeTestClass() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * テストメソッド実行前処理
     */
    @Before
    public void setup() {
        LogUtil.s(">>>>" + getClass().getSimpleName() + "::Setup");

        notification = BDashNotification.getInstance(context);

        try {
            String url = ConnectAccess.getTokenAPI_URL();

            if (url.equals(TOKEN_API_STG_URL)) {
                // STG 環境
                Device.getInstance().appId = "bd-Mh96PzeRJ69XG9uKnxQdXLi80ms0OUrXT";
                Device.getInstance().accountId = "BD-K35Y4G";
                Device.getInstance().dataViewIds = "2";
            } else {
                // Dev環境
                Device.getInstance().appId = "XXXXX-FS-APP";
                Device.getInstance().accountId = "XXXXX-FS-ACCOUNT";
            }
        } catch (Exception e) {
            assertTrue("Token URL Error.", false);
        }

    }

    /**
     * テストメソッド実行後処理
     */
    @After
    public void tearDown() {
        while (notification.isProcessing()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        notification.clearStateListener();
        try {
            NotificationAccess.setHookConnectResponse(notification, null);

            NotificationAccess.instanceReset(notification);
        } catch (Exception e) {
        }
    }


    /**
     * <pre>
     * トークンの登録処理API を２回コールした場合、２回めで例外が起きること
     *
     *   [テスト観点]
     *   　・register/cancel は処理中に連続で呼ばれることが出来ない
     *   [確認方法]
     *   　・try/catch にて例外が発生したかを確認する(BDashBusyException)</pre>
     *
     * @throws Exception BDashBusyException(トークン登録/キャンセルプロセス中に別のプロセス開始を受信した際に発生する例外)
     */
    @Test
    public void testCase_registerRegisterFailed() throws Exception {

        notification.registerNotification();
        try {
            notification.registerNotification();
            assertTrue("例外が発生しませんでした", false);
        } catch (Exception e) {
            LogUtil.s(">> success exception.");
            LogUtil.s(">> exception: " + e);
        }
    }

    /**
     * トークンのキャンセル処理API を２回コールした場合、２回めで例外が起きること<pre>
     *
     *   [テスト観点]
     *   　・register/cancel は処理中に連続で呼ばれることが出来ない
     *   [確認方法]
     *   　・try/catch にて例外が発生したかを確認する(BDashBusyException)</pre>
     *
     * @throws Exception BDashBusyException(トークン登録/キャンセルプロセス中に別のプロセス開始を受信した際に発生する例外)
     */
    @Test
    public void testCase_cancelCancelFailed() throws Exception {

        notification.cancelNotification();
        try {
            notification.cancelNotification();
            assertTrue("例外が発生しませんでした", false);
        } catch (Exception e) {
            LogUtil.s(">> success exception.");
            LogUtil.s(">> exception: " + e);
        }
    }

    /**
     * <pre>
     * トークンの登録処理API を処理中に cancel をコールした場合例外が起きること
     *
     *   [テスト観点]
     *   　・register/cancel は処理中に連続で呼ばれることが出来ない
     *   [確認方法]
     *   　・try/catch にて例外が発生したかを確認する(BDashBusyException)</pre>
     *
     * @throws Exception BDashBusyException(トークン登録/キャンセルプロセス中に別のプロセス開始を受信した際に発生する例外)
     */
    @Test
    public void testCase_registerCancelFailed() throws Exception {

        notification.registerNotification();
        try {
            notification.cancelNotification();
            assertTrue("例外が発生しませんでした", false);
        } catch (Exception e) {
            LogUtil.s(">> success exception.");
            LogUtil.s(">> exception: " + e);
        }
    }

    /**
     * <pre>
     * トークンのキャンセル処理API を処理中に register をコールした場合例外が起きること
     *
     *   [テスト観点]
     *   　・register/cancel は処理中に連続で呼ばれることが出来ない
     *   [確認方法]
     *   　・try/catch にて例外が発生したかを確認する(BDashBusyException)</pre>
     *
     * @throws Exception BDashBusyException(トークン登録/キャンセルプロセス中に別のプロセス開始を受信した際に発生する例外)
     */
    @Test
    public void testCase_cancelRegisterFailed() throws Exception {

        notification.cancelNotification();
        try {
            notification.registerNotification();
            assertTrue("例外が発生しませんでした", false);
        } catch (Exception e) {
            LogUtil.s(">> success exception.");
            LogUtil.s(">> exception: " + e);
        }
    }

    /**
     * <pre>
     * トークンの登録処理API を1回コールし、リスナー登録した2つのクラスに onError(ERROR_FCM_READY) の通知が来ること
     * トークンの中身に適当な値を入れるとonError(ERROR_FCM_READY)が走る
     *
     *   [テスト観点]
     *   　・register/cancel のリスナーイベント定義
     *   [確認方法]
     *   　・リスナー処理にて例外が発生するかを確認する</pre>
     *
     * @throws Exception
     */
    private int _listener_onError = 0;

    @Test
    public void testCase_registerInOnErrorLister() throws Exception {
        NotificationAccess.setConnectControllerCreator(notification, new MockFailedConnectClientControllerCreator());

        //確認したいエラーによってトークンの中身を適当な値かnullのどちらかに変える
        final String token = null;
        notification.addStateListener(new BDashNotification.NotificationStateListener() {
            @Override
            public void onEnable() {
            }

            @Override
            public void onDisable() {
            }

            @Override
            public void onError(int errType, Throwable e) {
                if (token == null) {
                    assertEquals("エラー種別が異なります", errType, ERROR_FCM_READY);
                } else {
                    assertEquals("エラー種別が異なります", errType, ERROR_CONNECT);
                }
                _listener_onError++;
            }
        });

        notification.addStateListener(new BDashNotification.NotificationStateListener() {
            @Override
            public void onEnable() {
            }

            @Override
            public void onDisable() {
            }

            @Override
            public void onError(int errType, Throwable e) {
                if (token == null) {
                    assertEquals("エラー種別が異なります", errType, ERROR_FCM_READY);
                } else {
                    assertEquals("エラー種別が異なります", errType, ERROR_CONNECT);
                }
                _listener_onError++;
            }
        });

        notification.registerNotification();
        while (notification.isProcessing()) {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {
            }
        }

        assertThat(_listener_onError)
                .as(String.format("件数(%d)が異なります", _listener_onError))
                .isEqualTo(2);
    }

    /**
     * <pre>
     * トークンの登録処理API を1回コールし、処理完了後２回目のコールで例外が起きないこと
     *
     *   [テスト観点]
     *   　・register/cancel の呼び出し処理が完結している場合、継続して register/cancel がコールできる
     *   [確認方法]
     *   　・try/catch にて</pre>
     *
     * @throws Exception
     */
    private boolean _registerWaitCancel_wait;

    @Test
    public void testCase_registerWaitRegister() throws Exception {
        _registerWaitCancel_wait = true;
        notification.addStateListener(new BDashNotification.NotificationStateListener() {
            @Override
            public void onEnable() {
                finish();
            }

            @Override
            public void onDisable() {
                finish();
            }

            @Override
            public void onError(int errType, Throwable e) {
                finish();
            }

            void finish() {
                _registerWaitCancel_wait = false;
            }
        });
        notification.registerNotification();
        try {
            while (_registerWaitCancel_wait) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
            LogUtil.s(">> lister finish.");
            notification.registerNotification();
            LogUtil.s(">> success.");
        } catch (Exception e) {
            assertTrue("例外が発生しました", false);
        }
    }

    /**
     * <pre>
     * トークンのキャンセル処理API を1回コールし、処理完了後２回目のコールで例外が起きないこと
     *
     *   [テスト観点]
     *   　・register/cancel の呼び出し処理が完結している場合、継続して register/cancel がコールできる
     *   [確認方法]
     *   　・try/catch にて</pre>
     *
     * @throws Exception
     */
    private boolean _cancelWaitCancel_wait;

    @Test
    public void testCase_cancelWaitCancel() throws Exception {
        _cancelWaitCancel_wait = true;
        notification.addStateListener(new BDashNotification.NotificationStateListener() {
            @Override
            public void onEnable() {
                finish();
            }

            @Override
            public void onDisable() {
                finish();
            }

            @Override
            public void onError(int errType, Throwable e) {
                finish();
            }

            void finish() {
                _cancelWaitCancel_wait = false;
            }
        });
        notification.cancelNotification();
        try {
            while (_cancelWaitCancel_wait) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
            LogUtil.s(">> lister finish.");
            notification.cancelNotification();
            LogUtil.s(">> success.");
        } catch (Exception e) {
            assertTrue("例外が発生しました", false);
        }
    }

    /**
     * <pre>
     * トークンの登録処理API を先に1回コールし、処理実施中に別スレッドからキャンセル処理API を呼び BDashBusyException 例外が起きること
     *
     *   [テスト観点]
     *   　・register/cancel は処理中に連続で呼ばれることが出来ない
     *   　・register/cancel をマルチスレッド環境で呼び出しクラッシュしないこと
     *   [確認方法]
     *   　・try/catch にて</pre>
     *
     * @throws Exception
     */
    @Test
    public void testCase_multiThread_registerCancel() throws Exception {
        // 先にコードブロックを生成しておく
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    notification.cancelNotification();
                    assertTrue("例外が発生しませんでした", false);
                } catch (BDashBusyException e) {
                    LogUtil.s(">> success exception.");
                } catch (Exception e) {
                    assertTrue("未知の例外が発生しました", false);
                }
            }
        });
        notification.registerNotification();
        thread.start();
        thread.join();
    }

    /**
     * <pre>
     * トークンの登録処理API を先に1回コールし、処理完了後に別スレッドから再度登録処理API を呼び BDashBusyException 例外が起きないこと
     *
     *   [テスト観点]
     *   　・register/cancel を別々のスレッドから呼び出してもエラーハンドリングができる
     *   　・register/cancel をマルチスレッド環境で呼び出しクラッシュしないこと
     *   [確認方法]
     *   　・try/catch にて</pre>
     *
     * @throws Exception
     */
    int _testCase_multiThread_registerWaitRegister = 0;

    @Test
    public void testCase_multiThread_registerWaitRegister() throws Exception {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    notification.registerNotification();
                } catch (Exception e) {
                    assertTrue("未知の例外が発生しました", false);
                }
                _testCase_multiThread_registerWaitRegister = 1;

            }
        });
        notification.addStateListener(new BDashNotification.NotificationStateListener() {
            @Override
            public void onEnable() {
                thread.start();
            }

            @Override
            public void onDisable() {
                thread.start();
            }

            @Override
            public void onError(int errType, Throwable e) {
                thread.start();
            }
        });
        notification.registerNotification();
        while (!thread.isAlive()) {
            if (_testCase_multiThread_registerWaitRegister != 0) break;
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        thread.join();
    }
}
