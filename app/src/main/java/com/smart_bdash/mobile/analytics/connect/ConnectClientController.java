package com.smart_bdash.mobile.analytics.connect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.smart_bdash.mobile.analytics.MobileSDKManager;

import java.util.ArrayList;

/**
 * 通信コントローラー
 *
 * @author fujimaru
 */
public class ConnectClientController extends IConnectClientController implements ConnectClientObserver {

    // 同期通信用、管理変数
    private ArrayList<ConnectClient> connectClients;
    private ArrayList<ClientThread> connectThreads;

    /***
     * コンストラクタ
     */
    public ConnectClientController() {
        connectClients = new ArrayList<ConnectClient>();
        connectThreads = new ArrayList<ClientThread>();
    }

    public ConnectClient connect(IConnectAsyncResponse response, ConnectType type, String send) {
        return connect(response, type, send, ConnectClient.getDefaultRequestParam());
    }

    public ConnectClient connect(IConnectAsyncResponse response, ConnectType type, String send, RequestParam param) {
        return connect_native(response, type, send, param);
    }

    protected ConnectClient connect_native(IConnectAsyncResponse response, ConnectType type, String send, RequestParam param) {
        String url = ConnectConfig.getUrl(type);

        ConnectClient client = new ConnectClient(type, url, send, param);
        client.setCallBack(response);
        client.addObserver(this);

        ClientThread ct = new ClientThread(client);
        synchronized (this) {
            connectClients.add(client);
            connectThreads.add(ct);
        }
        ct.start();
        return client;
    }

    /***
     * 通信スレッドを待つ
     */
    public void waitConnect() {
        synchronized (this) {
            if (connectThreads.size() == 0) {
                return;
            }
        }
        while (true) {
            synchronized (this) {
                if (connectThreads.size() == 0) {
                    return;
                }
            }
            try {
                Thread.yield();
            } catch (Exception e) {
            }
        }
    }

    /**
     * スレッドの終了を待つ
     */
    private void waitThreadExit() {
        try {
            for (int i = 0; i < connectThreads.size(); i++) {
                ClientThread ct = connectThreads.get(i);
                try {
                    ct.join(2000);
                } catch (InterruptedException e) {
                }
            }
        } catch (Exception e) {
            // 基本来るはずない
        }
    }

    @Override
    public void update(ConnectClient client) {
        int num = -1;
        synchronized (this) {
            num = connectClients.indexOf(client);
            if (num != -1) {
                connectClients.remove(client);
            }
        }
        if (num != -1) {
            exitClientThread(client);
        }
    }

    /***
     * 通信用のスレッドを管理変数から削除(終了)する
     * @param client
     */
    private void exitClientThread(ConnectClient client) {
        synchronized (this) {
            for (ClientThread i : connectThreads) {
                if (i.match(client)) {
                    connectThreads.remove(i);
                    break;
                }
            }
        }
    }

    /***
     * 管理している同期通信を全てキャンセルする
     */
    public synchronized boolean connectCancel(boolean isUserCancel) {
        boolean isCanceled = connectCancelAsync(isUserCancel);
        waitThreadExit();
        return isCanceled;
    }

    /***
     * 管理している同期通信を非同期に全てキャンセルする
     */
    public synchronized boolean connectCancelAsync(boolean isUserCancel) {
        int isCanceled = 0;

        // connectClient インスタンスの通信状態
        if (connectClients != null && connectClients.size() > 0) {
            for (int i = 0; i < connectClients.size(); i++) {
                ConnectClient work = connectClients.get(i);
                if (work != null) {
                    if (work.cancel(isUserCancel)) {
                        isCanceled++;
                    }
                }
            }
        }
        return isCanceled != 0;
    }

    /**
     * ネットワーク状態をチェックする 接続可能(true) / 接続不可(false)
     *
     * @return ret boolean
     */
    public static boolean checkOnLine() {
        boolean ret = false;

        ConnectivityManager cm =
                (ConnectivityManager) MobileSDKManager.getInstance().getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            // 接続状態を返す
            ret = cm.getActiveNetworkInfo().isConnected();
        } else {
            ret = false;
        }
        return ret;
    }

    /***
     * 実際の通信を行うスレッド
     *
     * @author fujimaru
     */
    private class ClientThread implements Runnable {
        private ConnectClient client;
        private Thread thread;

        public ClientThread(ConnectClient client) {
            this.client = client;
        }

        public boolean match(ConnectClient client) {
            if (this.client.equals(client)) {
                return true;
            }
            return false;
        }

        public void start() {
            thread = new Thread(this);
            thread.start();
        }

        public void join() throws InterruptedException {
            join(0);
        }

        public void join(long millis) throws InterruptedException {
            if (thread != null) {
                thread.join(millis);
            }
        }

        public void run() {
            if (checkOnLine()) {
                // 優先順位はバックグラウンド
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                client.start();
            } else {
                // connect error
                client.setError(new NetworkNotFoundException());
                client.setResponseCode(500);    // ひとまず InternalServerError とする
                try {
                    client.getCallBack().onPostExecuteImpl(client, client.getError());
                } catch (Exception e) {
                    // ここの例外は無視.呼び出し先でｸﾗｯｼｭしたとき
                }
                update(client);
            }
        }
    }
}
