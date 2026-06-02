package com.f_scratch.bdash.mobile.analytics;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.f_scratch.bdash.mobile.analytics.connect.ConnectClient;
import com.f_scratch.bdash.mobile.analytics.connect.ConnectClientController;
import com.f_scratch.bdash.mobile.analytics.connect.ConnectType;
import com.f_scratch.bdash.mobile.analytics.connect.IConnectAsyncResponse;
import com.f_scratch.bdash.mobile.analytics.connect.RequestParam;
import com.f_scratch.bdash.mobile.analytics.model.User;
import com.f_scratch.bdash.mobile.analytics.model.config.JsonKey;
import com.f_scratch.bdash.mobile.analytics.notification.BDashNotification;
import com.f_scratch.bdash.mobile.analytics.util.LogUtil;
import com.f_scratch.bdash.mobile.analytics.util.LogicUtil;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * トラッカークラス<br>
 * <br>
 * ・各種データログのトラッキングは本クラスで行います。<br>
 * ・プッシュ通知は {@link com.f_scratch.bdash.mobile.analytics.notification.BDashNotification BDashNotification} を参照してください。<br>
 *
 * @author FromScratch
 */
public class Tracker {

    private static Tracker instance;
    private MobileSDKManager sdkManager;
    private ConnectClientController connectController = new ConnectClientController();

    /**
     * SDK Tracker パラメーター
     **/
    private String screenName;      // スクリーン名
    private String loginUser;       // ユーザー名
    private String relationalKey;   // 外部キー
    private String relationalValue; // 外部バリュー
    private String bootType;        // 起動タイプ
    private String bootValue;       // 起動バリュー
    private HashMap<String, String> userMap;

    /**
     * SDK 内部 パラメーター
     */
    private boolean requestSync;    // sync をリクエスト済みかどうか
    private Handler handler;        // handler
    private Thread sendThread;
    private boolean sendThreadAlive;
    private int sendThreadNoneEventCount;

    /**
     * SDK 内部定数
     */
    private final static String THREAD_NAME_SEND = "bdash-send-thread";
    private final static int THREAD_ALIVE_LINE = 10;

    /**
     * 以降、デバッグ計測用のパラメーター
     */
    private long m_timeAd;


    /**
     * 起動タイプ<br>
     *
     * @see Tracker#setBootType(String)
     * @see Tracker#setBootType(String, String)
     */
    public static class BootType {
        private static final String BOOT_FIRST = "boot";   // 内部的な起動バリュー値
        /**
         * アプリがスキーム起動された事を示す
         */
        public static final String BOOT_SCHEMA = "schema";
        /**
         * プッシュ通知から起動されたことを示す
         */
        public static final String BOOT_PUSH = "push";
        /**
         * その他(自由定義)から起動されたことを示す
         */
        public static final String BOOT_OTHER = "other";
    }

    private static class BootValue {
        String dId;
        String bdId;

        public BootValue(String dId, String bdId) {
            this.dId = dId;
            this.bdId = bdId;
        }
    }

    /**
     *　Trackerクラスのコンストラクタ
     */
    private Tracker(Context context, String uuId) {
        init(context, uuId);
    }

    /**
     * customId を指定する I/F は非推奨
     *
     * @param context Application Context
     * @return Tracker インスタンス
     */
    @Deprecated
    public static synchronized Tracker getInstance(Context context, String uuid) {
        if (instance == null) {
            instance = new Tracker(context, uuid);
        }
        return instance;
    }

    public static synchronized Tracker getInstance(Context context) {
        return getInstance(context, (String) null);
    }

    /**
     * プッシュ通知からの起動時に呼び出される
     * 起動時のパラメータをもとにbootType,bootValueを設定したTrackerインスタンスを返す
     *
     * @param context Application Context
     * @param intent 起動時のパラメータ
     * @return Trackerインスタンス
     */
    public static synchronized Tracker getInstance(Context context, Intent intent) {
        Tracker tracker = getInstance(context);

        // 起動時のパラメータからdID,bdIDを取得
        String dId = intent.getStringExtra(BDashNotification.LAUNCH_DID);
        String bdId = intent.getStringExtra(BDashNotification.LAUNCH_BDID);

        // 取得した値をもとにjsonを作成し、bootValueに設定
        BootValue bootValue = new BootValue(dId, bdId);
        String bootValueJson = new Gson().toJson(bootValue);
        tracker.setBootType(BootType.BOOT_PUSH, bootValueJson);
        LogUtil.s("setBootType bootValue:" + bootValueJson);

        return tracker;
    }

    /**
     * BDash の通知コントロールクラスを生成し取得します
     *
     * @return BDashNotification
     */
    public BDashNotification getNotification() {
        return BDashNotification.getInstance(sdkManager.getContext());
    }

    /**
     * 初期化
     */
    private void init(Context context, String uuid) {
        // 必ず、公開外部クラスのイニシャライズ処理の先頭に記載
        sdkManager = MobileSDKManager.getInstance_needCreate(context.getApplicationContext(), uuid);

        // 起動タイプを設定します
        setBootType(BootType.BOOT_FIRST);

        // adID は毎回取得する
        m_timeAd = System.currentTimeMillis();
        Thread localThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(sdkManager.getContext());
                    if (info.isLimitAdTrackingEnabled()) {
                        // お作法として、ユーザー設定を優先する
                        User.getInstance().setAdId(null);
                    } else {
                        User.getInstance().setAdId(info.getId());
                    }
                    if (BuildConfig.DEBUG) {
                        m_timeAd = System.currentTimeMillis() - m_timeAd;
                        LogUtil.s(String.format("広告IDの取得に %dms かかりました。", m_timeAd));
                    }

                    User.getInstance().can_use_adId = true;
                    User.getInstance().save();
                } catch (Throwable e) {
                    // AdID が取れないケース。どうしようもない
                    // IOException:  GooglePlayServicesへの接続が失敗した
                    // IllegalStateException:  メインスレッドで処理が呼ばれた
                    // GooglePlayServicesNotAvailableException: GooglePlayがデバイスにインストールされていない時
                    // GooglePlayServicesRepairableException: GooglePlayServicesを使う上でエラーが起きた時
                }
            }
        });
        localThread.start();

        // 100ms 秒最大ウェイトをかける.
        // ほとんどの場合 10-100ms の間に取得完了するが、取れない場合は同じセッションの中で adId が空から値が入るなどある
        try {
            localThread.join(100);
        } catch (Exception e) {
        }
    }

    /**
     * ログインユーザーを設定します
     *
     * @param user
     */
    public void setLoginUser(String user) {
        this.loginUser = user;
    }

    /**
     * スクリーン名を設定します
     *
     * @param name 画面名
     */
    public void setScreenName(String name) {
        this.screenName = name;
    }


    /**
     * 起動タイプを設定
     *
     * @param type Tracker.BootType クラスの定数を指定します
     */
    public void setBootType(String type) {
        setBootType(type, null);
    }

    /**
     * 起動タイプを設定
     *
     * @param type Tracker.BootType クラスの定数を指定します
     */
    public void setBootType(String type, String value) {
        this.bootType = type;
        this.bootValue = value;
    }


    /**
     * リレーショナルキーを設定します
     *
     * @param key
     */
    public void setRelationalKey(String key) {
        this.relationalKey = key;
    }

    /**
     * リレーショナルキー＆バリューを設定します
     *
     * @param key
     */
    public void setRelationalKey(String key, String value) {
        this.relationalKey = key;
        this.relationalValue = value;
    }


    /**
     * 開発者が定義するユーザー情報のキーバリュー(マップ)を設定します。
     *
     * @param map
     */
    public void setUserMap(HashMap<String, String> map) {
        this.userMap = map;
    }

    /**
     * ビジターID を取得
     *
     * @return
     */
    public String getVisitorId() {
        // 2016/09/28. 「50b4ba4: 0414納品用修正」のコードに戻す
        return User.getInstance().getUniqueId();
    }

    /**
     *
     */
    private Runnable sendMainTask = new Runnable() {
        @Override
        public void run() {
            long time = System.currentTimeMillis();
            EventLogManager.getInstance().save();

            // バッファの件数を調べる
            ArrayList<HashMap<String, Object>> events = EventLogManager.getInstance().getEventLogs();
            LogUtil.s(String.format("ログデータは %d 件あります", events.size()));

            if (events.size() >= EventLogManager.THRESHOLD_SEND_BUFFER) {
                // 送信するべき状態なら送信する

                EventLogManager.getInstance().lock();
                post(events, null);
            }
        }
    };

    /**
     *
     */
    private Runnable sendWorkTask = new Runnable() {
        @Override
        public void run() {
            EventLogManager.getInstance().saveWorkOnly();
        }
    };

    /**
     *
     */
    private Runnable sendTask = new Runnable() {
        @Override
        public void run() {
            sendThreadAlive = true;
            sendThreadNoneEventCount = 0;

            // 最大 THREAD_ALIVE_LINE 秒間常駐するスレッド
            while (true) {
                int task = getSendTask();
                if (task == TASK_UNKNOWN) {

                    // ここのロック対象は実行スレッド
                    synchronized (sendThread) {
                        try {
                            /***
                             * 本当は永遠と常駐したいけど、以下の理由により 1000ms とする
                             *  ・SDK という立場上、永遠に常駐するのはどうなの
                             *  ・スレッド生成は内部的なしきい値を超えたときのみ生成されるので、
                             *    ほとんどの場合、生成されない
                             *  ・1秒としたのは単純に「長すぎず短すぎず」生成してから 10秒後に自身を終了させる
                             *    ロジックを組み込むため
                             */
                            sendThread.wait(1000);
                        } catch (InterruptedException e) {
                            // 割り込みが発生したら即実行
                            continue;
                        } catch (IllegalMonitorStateException e) {
                            // ロジックｴﾗｰのため、詳細ログを出力する
                            e.printStackTrace();
                        }
                    }
                    // ここのロック対象は send タスク
                    synchronized (sendTask) {
                        // スレッド終了を判定するカウンターを増やし、規定回数に達したら終了
                        sendThreadNoneEventCount++;
                        if (sendThreadNoneEventCount >= THREAD_ALIVE_LINE) {
                            // Thread.isAlive だとラグが考えられるため、自前で管理する
                            sendThreadAlive = false;
                            LogUtil.s(">>thread exit >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                            break;
                        }
                    }
                    continue;
                }
                LogUtil.s(">>thread run");

                if (task == TASK_MAIN_SEND) {
                    sendMainTask.run();
                } else {
                    sendWorkTask.run();
                }
                LogUtil.s(">>thread run end");
            }
        }
    };
    private ArrayList<Integer> sendCommand = new ArrayList<Integer>();
    private final static int TASK_UNKNOWN = -1;
    private final static int TASK_MAIN_SEND = 0;
    private final static int TASK_WORK_SEND = 1;

    private synchronized void addSendTask(int type) {
        int size = sendCommand.size();
        if (size != 0) {
            // 直前のと重複した場合は、何もしない
            if (sendCommand.get(size - 1) == type) return;
        }
        sendCommand.add(type);
    }

    /**
     * 現在のタスクを取得する
     *
     * @return
     */
    private synchronized int getSendTask() {
        if (sendCommand.size() == 0) {
            return TASK_UNKNOWN;
        }
        return sendCommand.remove(0);
    }

    /**
     * 送信用のスレッドを生成する
     */
    private void createSendThread() throws Exception {
        sendThread = new Thread(sendTask, THREAD_NAME_SEND);
        sendThread.start();
        Thread.yield();

        // フラグ true まで待った方が確実
        while (!sendThreadAlive) {
            Thread.yield();
        }
    }

    /**
     * データ送信を非同期で実行する
     */
    public void send(HashMap<String, Object> build) {

        // パラメーターの設定
        build.put(JsonKey.KEY_SCREEN_NAME, this.screenName);
        build.put(JsonKey.KEY_LOGIN_USERID, this.loginUser);
        build.put(JsonKey.KEY_RELATIONAL, this.relationalKey);
        build.put(JsonKey.KEY_RELATIONAL_VALUE, this.relationalValue);
        build.put(JsonKey.KEY_BOOT_TYPE, this.bootType);
        build.put(JsonKey.KEY_BOOT_VALUE, this.bootValue);
        build.put(JsonKey.KEY_USER_MAP, this.userMap);

        // リセット
        this.screenName = this.loginUser = this.relationalKey = this.relationalValue = null;
        this.bootType = this.bootValue = null; // boot系
        this.userMap = null;

        // 以降は、排他制御を行う
        synchronized (this) {
            EventLogManager.getInstance().addEventLog(build);

            if (EventLogManager.getInstance().isLock() == false) {

                int size = EventLogManager.getInstance().getEventSize();
                if (size < 300) {
                    // 300件未満は件数が少ないので即実行
                    sendMainTask.run();

                } else {
                    // 別スレッドで実行する場合は、スレッド生成前にロックフラグを立てておく
                    EventLogManager.getInstance().lock();

                    // タスクを積む
                    addSendTask(TASK_MAIN_SEND);

                    try {
                        if (sendThread == null) {
                            createSendThread();
                        } else {
                            synchronized (sendTask) {
                                if (!sendThreadAlive) {
                                    // スレッドが終了状態のとき又は終了に移行しているとき
                                    createSendThread();
                                } else {
                                    // スレッドがまだ生存しているとき
                                    sendThreadNoneEventCount = 0;
                                }
                            }
                        }
                        synchronized (sendThread) {
                            try {
                                sendThread.notify();
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception e) {
                        // スレッドの生成に失敗した可能性を考慮する

                        // 直前のタスクを取り消す
                        getSendTask();

                        // 現在のスレッドで実行する
                        sendMainTask.run();
                    }
                }
            } else {
                if (sendThread == null) {
                    sendWorkTask.run();
                } else {
                    boolean notify = false;
                    synchronized (sendTask) {
                        if (sendThreadAlive) {
                            // スレッドがまだ生存しているとき
                            sendThreadNoneEventCount = 0;
                            addSendTask(TASK_WORK_SEND);
                            notify = true;
                        } else {
                            sendWorkTask.run();
                        }
                    }
                    if (notify) {
                        synchronized (sendThread) {
                            try {
                                sendThread.notify();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }
        LogUtil.s(">>send exit.");
    }

    /**
     * データの同期を行う
     */
    public synchronized void sync() {
        // すでに sync リクエストを処理を受け付けている
        if (requestSync) {
            return;
        }

        ArrayList<HashMap<String, Object>> events = EventLogManager.getInstance().getEventLogs();
        LogUtil.s(String.format("ログデータは %d 件あります", events.size()));
        if (events.size() == 0) {
            LogUtil.s(">>sync を実施する必要はありません");
            return;
        }
        Runnable run = new Runnable() {
            @Override
            public void run() {
                requestSync = false;
                LogUtil.s(">>sync end.");
            }
        };
        if (EventLogManager.getInstance().isLock() == false) {
            // ロック中でないなら処理実施
            EventLogManager.getInstance().lock();
            requestSync = true;
            post(events, run);
        } else {
            // send 中などに呼ばれた場合、遅延実行する
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            // 自身を一定時間後に呼び出してもらう
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sync();
                }
            }, 500);
        }
    }


    /**
     * リクエストデータをサーバーに POST する
     *
     * @param requestData
     * @param connectEndRunnable
     */
    private void post(ArrayList<HashMap<String, Object>> requestData, Runnable connectEndRunnable) {
        try {
            String request = LogicUtil.createJsonRequest(JsonKey.createJsonDeviceFields(), requestData);
            RequestParam param = ConnectClient.getDefaultRequestParam();
            param.param_run_callback = connectEndRunnable;
            ConnectClientController con = getConnectController();
            con.connect(new IConnectAsyncResponse() {
                @Override
                public void onConnect(ConnectClient client) throws Exception {
                }

                @Override
                public void onPostExecuteImpl(ConnectClient client, Throwable exception) throws Exception {
                    LogUtil.s("  exception: " + (exception != null ? exception : "none"));
                    LogUtil.s("  stateCode: " + client.getResponseCode());

                    if (exception == null) {
                        EventLogManager.getInstance().unlockCommit();
                    } else {
                        EventLogManager.getInstance().unlock();
                    }
                    EventLogManager.getInstance().save();

                    if (client.getRequestParam().param_run_callback != null) {
                        try {
                            client.getRequestParam().param_run_callback.run();
                        } catch (Exception e) {
                            // 呼び出し先での例外(クラッシュ)を防ぐ
                            e.printStackTrace();
                        }
                    }
                }
            }, ConnectType.API_POST, request, param);

        } catch (Exception e) {
            // 何かしらの冷害が起きたときは、通信エラーと同じ処理にする
            EventLogManager.getInstance().unlock();
            EventLogManager.getInstance().save();

            if (connectEndRunnable != null) {
                try {
                    connectEndRunnable.run();
                } catch (Exception f) {
                    // 呼び出し先での例外(クラッシュ)を防ぐ
                }
            }
        }
    }

    /**
     * ConnectController を取得する
     *
     * @return
     */
    private ConnectClientController getConnectController() {
        return connectController;
    }
}
