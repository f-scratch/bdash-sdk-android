package com.smart_bdash.mobile.analytics.connect;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

import com.smart_bdash.mobile.analytics.MobileSDKManager;
import com.smart_bdash.mobile.analytics.util.LogUtil;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/***
 * 通信クライアント<br>
 *  ・HttpClient を内部で利用。Android5.1 以降は非推奨になっているので<br>
 *   今後は HttpURLConnection に置き換える方向
 * @author fujimaru
 */
public class ConnectClient {

    // ##### 定数 #####
    private static int TIME_OUT = 1000 * 5; // デフォルトのタイムアウト値
    private static int READ_TIME_OUT = 1000 * 30;

    // HTTPステータスコード
    public static final int NG = -1;
    public static final int SUCCESS = 200;

    // default connect encoding
    public static final String UTF_8 = "UTF-8";

    // リクエストパラメーター
    private RequestParam requestParam;

    // ##### オブザーバー関連 #####
    private ArrayList<ConnectClientObserver> observers = new ArrayList<ConnectClientObserver>();

    /**
     * オブザーバーを追加する
     *
     * @param observer
     */
    public void addObserver(ConnectClientObserver observer) {
        observers.add(observer);
    }

    /**
     * オブザーバーを削除する
     *
     * @param observer
     */
    public void deleteObserver(ConnectClientObserver observer) {
        observers.remove(observer);
    }

    /**
     * 変更が起きた場合に通知する
     */
    public void notifyObservers() {
        Iterator<ConnectClientObserver> it = observers.iterator();
        while (it.hasNext()) {
            ConnectClientObserver o = (ConnectClientObserver) it.next();
            o.update(this);
        }
    }

    // ##### ConnectClient関連 #####
    private ConnectType type;
    private String url;
    private String requestData;
    private String responseStr;
    private byte[] responseBin;
    private int responseCode;
    private boolean isUserCancel, isExecCancel, isExecuteLocalTask;
    private boolean isStart, isConnectEnd;
    private Throwable error, requestError;
    private Request okHttpGetRequest;
    private Request okHttpPostRequest;
    private OkHttpClient okHttpClient;
    private Call okHttpCall;
    private Response okHttpResponse;
    private IConnectAsyncResponse callback;

    /**
     * コンストラクタ
     */
    public ConnectClient(ConnectType type, String url, String request, RequestParam param) {
        use(type, url, request, param);
    }

    public ConnectClient(ConnectType type, String url) {
        use(type, url, null, getDefaultRequestParam());
    }

    /***
     * 初期化やパラメーターを変えて再利用する場合やなどのときにコールする
     */
    public void use(ConnectType type, String url, String request, RequestParam param) {
        if (type.ordinal() > ConnectType._POST_END.ordinal()) {
            param.option = ConnectType.Option.RESULT_STRING;
            param.method = RequestParam.GET_METHOD;
        }
        setType(type);
        setRequest(request);
        setRequestParam(param);
        if (requestParam != null) {
            String concat_url = requestParam.concat_url;
            if (concat_url != null) {
                url = url + concat_url;
            }
        }
        setUrl(url);

        use();
    }

    /***
     * 初期化やパラメーターを変えて再利用する場合やなどのときにコールする
     */
    public void use() {
        setResponse(null);
        setError(null);
        setResponseCode(NG);
        isUserCancel = false;
        isExecCancel = false;
        isConnectEnd = false;
    }

    /***
     * リクエストパラメーターを設定する
     * @param param
     */
    public void setRequestParam(RequestParam param) {
        if (param != null) {
            this.requestParam = param;
        } else {
            this.requestParam = getDefaultRequestParam();
        }
    }

    /***
     * コールバックを設定する
     * @param callback
     */
    public void setCallBack(IConnectAsyncResponse callback) {
        this.callback = callback;
    }

    /***
     *
     * @return
     */
    public IConnectResponse getCallBack() {
        return callback;
    }

    /***
     * コールバックを持っているか？
     * @return
     */
    public boolean hasCallBack() {
        return callback != null;
    }

    /***
     * コールバックメソッドを呼び出す
     */
    public void callback() {
        if (error == null) {
            try {
                callback.onConnect(this);
            } catch (Exception e) {
                // ここは何もしない
            }
        }

        try {
            callback.onPostExecuteImpl(this, error);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            // ここは何もしない
        }
    }

    /***
     * 現在のリクエストパラメーターを取得する
     * @return
     */
    public RequestParam getRequestParam() {
        return requestParam;
    }

    /***
     * デフォルトのリクエストパラムを取得する
     * @return
     */
    public static RequestParam getDefaultRequestParam() {
        RequestParam param = new RequestParam();
        param.connect_timeout = TIME_OUT;
        param.connect_read_timeout = READ_TIME_OUT;
        param.method = RequestParam.POST_METHOD;
        param.option = ConnectType.Option.RESULT_STRING;
        param.setMultiPart(false);
        param.useUserCancel = true;
        param.setMimeType(RequestParam.MIME_JSON);
        return param;
    }

    /***
     * 通信キャンセル
     * @param isUserCancel
     * @return
     */
    public synchronized boolean cancel(boolean isUserCancel) {
        // すでにキャンセルリクエストは受け付けてある
        if (isConnectEnd) {
            LogUtil.s(" => connecttin end. ignore cancel request.");
            return false;
        }
        LogUtil.s(" => to connecttin cancel.");
        boolean isCancelExec = false;

        // HttpClient の場合
        if (okHttpPostRequest != null) {
            try {
                isCancelExec = true;
                synchronized (requestParam) {
                    if(okHttpPostRequest != null){
                        if(!okHttpCall.isCanceled()){
                            okHttpCall.cancel();
                        }
                    }
                    if(okHttpGetRequest != null){
                        if(!okHttpCall.isCanceled()){
                            okHttpCall.cancel();
                        }
                    }
                }
                if (okHttpClient != null) {
                    okHttpClient.dispatcher().executorService().shutdown();
                }
            } catch (Exception e) {
                LogUtil.s("connect abort exception. " + e);
            }
            synchronized (requestParam) {
                this.okHttpGetRequest = null;
                this.okHttpPostRequest = null;
                this.isUserCancel = isUserCancel;
            }
        }
        // キャンセル処理をリクエストした
        this.isExecCancel = true;
        return isCancelExec;
    }

    /**
     * 通信開始
     */
    public void start() {
        isStart = true;

        long time = System.currentTimeMillis();
        try {
            sendTextClient(url, requestData);
        } catch (HttpStatusCodeException e) {
            setError(e);
            close();
        } catch (SocketException e) {
            LogUtil.s("------------------------");
            LogUtil.s("connectClient start() socketException: " + e.toString());
            LogUtil.s("------------------------");
            setError(e);
            close();
        } catch (Throwable e) {
            e.printStackTrace();
            setError(e);
            close();
        }
        // debug code //////////////////////////////////////////
        if (LogUtil.isDebuggable()) {
            try {
                time = System.currentTimeMillis() - time;
                LogUtil.s("  connect execute time: " + time);
            } catch (Exception e) {
                LogUtil.s("debug error=>" + e);
            }
        }
        ////////////////////////////////////////////////////////
        if (hasCallBack()) {
            callback();
        }

        // notify
        notifyObservers();

        // すべて終了
        isConnectEnd = true;
    }

    /***
     * 再度開始する
     */
    public void reStart() {
        use();
        start();
    }

    /***
     * ストリームから解析を行う
     * @param in
     * @param buffer_size
     * @throws Throwable
     */
    private void parseSendResult(InputStream in, int buffer_size) throws Throwable {
        if (requestParam.option == ConnectType.Option.RESULT_STRING) {
            // 送信結果取得
            BufferedReader rd = new BufferedReader(new InputStreamReader(in, UTF_8));
            String line;
            StringBuffer sb = null;
            try {
                sb = new StringBuffer(buffer_size);

                while ((line = rd.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (Throwable e) {
                rd.close();

                if (sb != null) {
                    sb.setLength(0);
                }
                throw e;
            }
            rd.close();

            // 得られたものを解析する
            responseStr = sb.toString();
        } else {
            int rd;
            ByteArrayOutputStream out = new ByteArrayOutputStream(buffer_size);
            while ((rd = in.read()) != -1) {
                out.write(rd);
            }
            responseBin = out.toByteArray();
            in.close();
        }
    }


    // ##### Getter & Setter #####
    public ConnectType getType() {
        return type;
    }

    public void setType(ConnectType type) {
        this.type = type;
    }

    public String getRequest() {
        return requestData;
    }

    public void setRequest(String data) {
        this.requestData = data;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    /***
     * 例外が発生したかを返す
     * @return
     */
    public boolean hasError() {
        return error != null;
    }

    /***
     * 例外情報を取得する
     * @return
     */
    public Throwable getError() {
        return error;
    }

    /***
     * 例外を設定する
     * @param error
     */
    public void setError(Throwable error) {
        this.error = error;
    }

    /***
     * HTTP レスポンスコードを返す
     * @return int
     */
    public int getResponseCode() {
        return responseCode;
    }

    protected void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /***
     * レスポンスを取得(txt)
     * @return
     */
    public String getResponse() {
        return responseStr;
    }

    /***
     * レスポンスを取得(binary)
     * @return
     */
    public byte[] getResponseBinary() {
        return responseBin;
    }


    public void setResponse(String response) {
        this.responseStr = response;
    }


    /***
     * ユーザーがキャンセルしたか？
     * @return
     */
    public boolean isUserCancel() {
        return isUserCancel;
    }


    /***
     * [内部メソッド] コネクションのキャンセル状態を調べる
     * @throws SocketException
     */
    private synchronized void checkCancelState() throws Exception {
        if (isExecCancel) {
            isUserCancel = true;
            isExecCancel = false;
            try {
                if (okHttpClient != null) {
                    okHttpClient.dispatcher().executorService().shutdown();
                    okHttpClient = null;
                }
            } catch (Exception e) {
                // ここの例外は無視.
            }
            throw new HttpAbortException();
        }
    }


    /***
     * 新形式の HTTP 通信
     * @param sendUrl
     * @param sendText
     * @throws SocketException
     * @throws IOException
     * @throws Exception
     */
    private void sendTextClient(String sendUrl, String sendText) throws SocketException, IOException, Throwable {
        //クライアントの生成を行う
        //タイムアウトの設定もここで行う
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(requestParam.getConnectTimeOut(), TimeUnit.MILLISECONDS)
                .readTimeout(requestParam.getConnectReadTimeOut(), TimeUnit.MILLISECONDS)
                .build();

        /****
         * POST でリダイレクト自動処理するときは… setRedirectStrategy() で設定する
         */
        LogUtil.s(sendUrl);

        // お客さんように
        LogUtil.s(sendText);

        // ヘッダーの設定
        Request.Builder requestBuilder = new Request.Builder();
        if (type.equals(ConnectType.OTHER_GET)) {

        } else {
            requestBuilder.header("User-Agent", String.format("bdash-sdk / ver=%s", MobileSDKManager.getSdkVersion()));
        }

        String if_modified_since = getRequestParam().if_modified_since;
        //GET POST共通リクエスト部分の準備
        requestBuilder.url(sendUrl);
        requestBuilder.header("Pragma", "No-cache");
        requestBuilder.header("Accept-Encoding", "gzip");
        if (if_modified_since != null) {
            requestBuilder.header("if-Modified-Since", if_modified_since);
        }

        if (requestParam.getRequestMethod().equals(RequestParam.GET_METHOD)) {
            // GET リクエスト
            okHttpGetRequest = requestBuilder.build();
        } else {
            // POST リクエスト
            // リクエストボディの設定
            MediaType MIMEType= MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(sendText, MIMEType);
            requestBuilder.post(requestBody);

            // 通常のリクエスト
            okHttpPostRequest = requestBuilder.build();
        }

        // 中断チェック
        checkCancelState();

        // ローカルタスクを非同期で実行
        requestError = null;
        okHttpResponse = null;
        okHttpCall = null;
        isExecuteLocalTask = true;
        Runnable task = new Runnable() {
            public void run() {
                try {
                    Request request = null;
                    synchronized (requestParam) {
                        if (okHttpGetRequest == null) {
                            request = okHttpPostRequest;
                        } else {
                            request = okHttpGetRequest;
                        }
                    }
                    if (request != null) {
                        okHttpCall = okHttpClient.newCall(request);
                        okHttpResponse = okHttpCall.execute();
                    }
                } catch (Exception e) {
                    //Callアップロード中にcancelを叩くとIOExceptionがスローされるので、この部分でCallがキャンセルされたかどうか確認する
                    if(okHttpCall.isCanceled())
                        return;
                    requestError = e;
                }
                isExecuteLocalTask = false;
            }
        };
        Thread connectThread = new Thread(task, "connectWait");
        connectThread.start();

        {
            String date = String.format("%d/%02d/%02d %02d:%02d:%02d"
                    , Calendar.getInstance().get(Calendar.YEAR)
                    , Calendar.getInstance().get(Calendar.MONTH) + 1
                    , Calendar.getInstance().get(Calendar.DATE)
                    , Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    , Calendar.getInstance().get(Calendar.MINUTE)
                    , Calendar.getInstance().get(Calendar.SECOND)
            );
            LogUtil.s(String.format("[%s] 通信開始しました", date));
            LogUtil.s(String.format("getConnectReadTimeOut: %d", requestParam.getConnectReadTimeOut()));
        }

        // キャンセルが実行されていないとき、かつ、ローカルタスクが実行中の時
        while (isExecCancel == false && isExecuteLocalTask) {
            try {
                Thread.yield();
            } catch (Exception e) {
            }
        }
        {
            String date = String.format("%d/%02d/%02d %02d:%02d:%02d"
                    , Calendar.getInstance().get(Calendar.YEAR)
                    , Calendar.getInstance().get(Calendar.MONTH) + 1
                    , Calendar.getInstance().get(Calendar.DATE)
                    , Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    , Calendar.getInstance().get(Calendar.MINUTE)
                    , Calendar.getInstance().get(Calendar.SECOND)
            );
            LogUtil.s(String.format("[%s] 通信終了しました", date));
        }

        // レスポンスが取得できなかった
        if (okHttpResponse == null) {
            if (requestError != null) {
                throw requestError;
            }
            throw new SocketException("http connect error.");
        }

        // 中断チェック
        checkCancelState();

        //ステータスコードの確認
        int statusCode = okHttpResponse.code();
        setResponseCode(statusCode);

        if (statusCode != SUCCESS) {
            // 200 以外は失敗と見なす
            if (okHttpPostRequest != null) {
                okHttpCall.cancel();
            }
            if (okHttpClient != null) {
                okHttpClient.dispatcher().executorService().shutdown();
            }
            throw new HttpStatusCodeException(statusCode);
        }

        // 中断チェック
        checkCancelState();

        // Content-Length の解析
        int size = 1024;
        String headerValue = okHttpResponse.header("Content-Length", null);
        if (headerValue != null) {
            int work = Integer.parseInt(headerValue);
            if (work > size) {
                size = work;
            }
        }

        InputStream in = null;
        String encodingValue = okHttpResponse.header("Accept-Encoding", null);
        if (encodingValue != null && encodingValue.equalsIgnoreCase("gzip")) {
            in = new GZIPInputStream(okHttpResponse.body().byteStream());
        }
        if (in == null) {
            in = okHttpResponse.body().byteStream();
        }

        // レスポンスの解析
        parseSendResult(in, size);

        // join
        connectThread.join();

        // 中断チェック
        checkCancelState();

        // 正常終了
        okHttpClient.dispatcher().executorService().shutdown();
    }

    /***
     * 内部のクローズ処理/例外をキャッチしたとき用にメソッド化
     */
    private void close() {
        if (okHttpClient != null) {
            synchronized (this) {
                try {
                    if (okHttpClient != null) {
                        okHttpClient.dispatcher().executorService().shutdown();
                        okHttpClient = null;
                    }
                } catch (Exception f) {
                    // ここでの例外はどうしょうもないし、発生しても fatal では無い
                }
            }
        }
    }

    /***
     * 通信処理が開始されたかどうか
     * @return
     */
    public boolean isStartedConnect() {
        return isStart;
    }

}
