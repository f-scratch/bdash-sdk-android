package com.smart_bdash.mobile.analytics;

import com.smart_bdash.mobile.analytics.util.LogUtil;
import com.smart_bdash.mobile.analytics.util.StorageUtil;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * イベントログ管理クラス<br>
 *  ・本クラスは内部実装用クラスです。<br>
 *  ・イベントログデータを管理します。
 *
 * @author FromScratch
 */
class EventLogManager {

    public static final int THRESHOLD_SEND_BUFFER = 10;     // 送信を判定するバッファサイズ 10件予定
    public static final int MAX_SEND_BUFFER       = 100;    // 一度に送信するデータ件数
    public static final int MAX_STORAGE_BUFFER    = 1000;   // ストレージに保存する最大件数

    private static EventLogManager instance;
    private ArrayList<HashMap<String,Object>> eventLogs = new ArrayList<HashMap<String,Object>>();
    private ArrayList<HashMap<String,Object>> lockBuffer= new ArrayList<HashMap<String,Object>>();
    private boolean lock;

    private EventLogManager(){
        load();
    }

    /***
     * インスタンスを取得<br>
     *  ・この段階で排他制御していないのは、わざと(明示的に)してる<br>
     *  ・複数のスレッドから同時に呼び出されることは無い
     * @return EventLogManager
     */
    public static EventLogManager getInstance(){
        if( instance==null ){
            instance = new EventLogManager();
        }
        return instance;
    }

    /***
     * イベントログを取得する。内容は参照で返されることがあるので、利用先で編集する場合は copy する必要が有ります。
     * @return
     */
    public synchronized ArrayList<HashMap<String,Object>> getEventLogs(){
        // 送信しきい値内の場合は、そのまま参照を返す
        if( eventLogs.size() <= MAX_SEND_BUFFER ) {
            return eventLogs;
        }
        LogUtil.s( String.format("送信データ数のしきい値(%d/%d件)をオーバーしているので切り詰めて送信します", eventLogs.size(), MAX_SEND_BUFFER));
        // 送信しきい値を超えている場合は、しきい値につめて返す
        ArrayList<HashMap<String,Object>> result = new ArrayList<HashMap<String,Object>>(MAX_SEND_BUFFER);
        for( int i=0 ; i<MAX_SEND_BUFFER ; i++ ) {
            result.add(eventLogs.get(i));
        }
        return result;
    }

    /***
     * イベントサイズを取得する
     * @return
     */
    public int getEventSize(){
        return eventLogs.size();
    }

    /***
     * イベント
     * @param eventData
     */
    public void addEventLog( HashMap<String,Object> eventData ){

        boolean check_lock;
        synchronized (lockBuffer) {
            check_lock = isLock();
            // ロック状態のとき
            if( check_lock ) {
                lockBuffer.add(eventData);
            }
        }
        // ロック状態ではないとき
        if( !check_lock ) {
            synchronized (this) {
                int count = 0;
                while (eventLogs.size() >= MAX_STORAGE_BUFFER) {
                    eventLogs.remove(0); // 最も古いのを削除
                    count++;
                }
                if (count > 0) {
                    LogUtil.s(String.format("最大保持件数(%d)をオーバーしたので%d 件削除しました", MAX_STORAGE_BUFFER, count));
                }
                eventLogs.add(eventData);
            }
        }

    }




    /***
     * ロック状態にします
     */
    public void lock(){
        synchronized (lockBuffer) {
            lock = true;
        }
    }


    /***
     * ロックを解除します / 内部データは維持されます
     */
    public void unlock(){
        synchronized (lockBuffer) {
            if( !lock )return;

            eventLogs.addAll(lockBuffer);
            lockBuffer.clear();
            lock = false;
        }
    }

    /***
     * ロックを解除するときにコミットを行います / lock 時の内部データは破棄されます
     */
    public void unlockCommit(){
        synchronized (lockBuffer) {
            if( !lock )return;

            if( eventLogs.size()<=MAX_SEND_BUFFER ) {
                eventLogs.clear();
            } else {
                for( int i=0 ; i<MAX_SEND_BUFFER ; i++ ) {
                    // 先頭から送信しきい値の分だけデータを削除する
                    eventLogs.remove(0);
                }
            }

            eventLogs.addAll(lockBuffer);
            lockBuffer.clear();
            lock = false;
        }
    }

    /***
     * ロック状態かを確認します
     * @return
     */
    public boolean isLock(){
        synchronized (lockBuffer) {
            return lock;
        }
    }


    /***
     * バッファをメモリの読み込みます
     */
    public synchronized void load(){
        try {
            Type eventLogsType = new TypeToken<ArrayList<HashMap<String,Object>>>(){}.getType();

            // イベントバッファを読み込む
            ArrayList<HashMap<String,Object>> results = StorageUtil.deserialize(StorageUtil.FileType.EVENT_LOGS, eventLogsType);
            if( results != null ) {
                eventLogs = results;
            }

            // 一時バッファを読み込む
            ArrayList<HashMap<String, Object>> workResults = StorageUtil.deserialize(StorageUtil.FileType.EVENT_WORK_LOGS, eventLogsType);
            if (workResults != null) {
                eventLogs.addAll(workResults);
                StorageUtil.remove(StorageUtil.FileType.EVENT_WORK_LOGS);

                // 現行のログデータサイズ - 最大件数1000件 = 対象外のログデータ
                trim();
            }

        } catch( Exception e ) {
            // ログデータが破損していた場合など、ｸﾗｯｼｭしないようにする
            e.printStackTrace();
        }
    }

    /***
     *
     */
    public void trim(){
        // 現行のログデータサイズ - 最大件数1000件 = 対象外のログデータ
        int over_size = eventLogs.size() - MAX_STORAGE_BUFFER;
        for( int i=0 ; i<over_size  ; i++ ) {
            // 対象外のログデータの分だけ、古いのから削除していく
            eventLogs.remove(0);
        }
    }

    /***
     * イベントバッファと一時バッファを結合してストレージに保存します
     */
    public synchronized void save(){
        try {
            synchronized (lockBuffer) {
                eventLogs.addAll(lockBuffer);
                lockBuffer.clear();
            }
            trim();
            StorageUtil.serialize(StorageUtil.FileType.EVENT_LOGS, eventLogs);

            saveWorkOnly();
        } catch( Exception e ) {
            // ストレージに書き込めない場合、どうしようもない
            e.printStackTrace();
        }
    }



    /***
     * 一時バッファをストレージに保存します
     */
    public  void saveWorkOnly(){
        synchronized (lockBuffer) {
            if (lockBuffer.size() >= 0) {
                try {
                    StorageUtil.serialize_nowait(StorageUtil.FileType.EVENT_WORK_LOGS, lockBuffer);
                } catch (Exception e) {
                    // ストレージに書き込めない場合、どうしようもない
                }
            } else {
                // ファイルの削除はロード時にする
            }
        }
    }
}
