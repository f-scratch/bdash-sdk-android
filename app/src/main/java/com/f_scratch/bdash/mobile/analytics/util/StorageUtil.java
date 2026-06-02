package com.f_scratch.bdash.mobile.analytics.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ストレージ制御のユーティリティ
 */
public class StorageUtil {

    private static String ROOT        = "";
    final private static String LOCAL_DIR   = "com.fsbdash.mobile.analytics.ser/";


    public static enum FileType {
        //////////////////////////////////////////////
          USER("001")              // User
        , SETTINGS("002")          // Setting
        , EVENT_LOGS("003")        // Event
        , EVENT_WORK_LOGS("004")   // Event
//        , DEBUG_LOGS("request.log")   // DebugLogs

        //////////////////////////////////////////////
        ;
        private String value;

        FileType( String value ){
            this.value = value;
        }
    }

    /***
     * 初期化 / root ディレクトリを設定する
     * @param context
     */
    public static void initialize( Context context ){
        ROOT = context.getFilesDir().getAbsolutePath() + "/";
    }

    /***
     * ファイル名からディレクトリパス名を生成する
     */
    public static String getBaseDirectory(){
        return ROOT + LOCAL_DIR;
    }

    /***
     * 必要ならディレクトリを作成する
     * @param path
     */
    protected static boolean createDirectoryIfNecessary( String path ){
        boolean result = false;
        File dir = new File(path);
        if( dir.exists() == false ) {
            result = dir.mkdirs();
        }
        return result;
    }


    /**
     * 受け取ったオブジェクトをシリアライズ化して保存する.ストレージ操作は排他制御とする
     * @param type String
     * @param obj Object
     */
    public static synchronized void serialize(FileType type, Object obj) throws Exception {
        FileOutputStream target = null;
        ObjectOutputStream out  = null;
        try {
            String dir = getBaseDirectory();

            // 必要ならディレクトリを作成する
            createDirectoryIfNecessary(dir);

            // ターゲットのファイルを作成する
            File file = new File(dir + type.value);
            target = new FileOutputStream(file);
            out    = new ObjectOutputStream(target);
            out.writeObject(obj);

        } finally {
            if( out!=null ){
                out.close();
            }
            if( target!=null ){
                target.close();
            }
        }
    }

    public static void serialize_nowait(FileType type, Object obj) throws Exception {
        FileOutputStream target = null;
        ObjectOutputStream out  = null;
        try {
            String dir = getBaseDirectory();

            // 必要ならディレクトリを作成する
            createDirectoryIfNecessary(dir);

            // ターゲットのファイルを作成する
            File file = new File(dir + type.value);
            target = new FileOutputStream(file);
            out    = new ObjectOutputStream(target);
            out.writeObject(obj);

        } finally {
            if( out!=null ){
                out.close();
            }
            if( target!=null ){
                target.close();
            }
        }
    }


    /**
     * ファイルからデータを読込み、デシリアライズを行ってオブジェクトを返す.ストレージ操作は排他制御とする
     * @param type String
     * @return obj Object
     */
    public static synchronized Object deserialize(FileType type) throws Exception{
        FileInputStream target = null;
        ObjectInputStream in   = null;
        Object obj = null;
        try {

            String dir = getBaseDirectory();

            // ターゲットのファイルを読み込む
            File file = new File(dir + type.value);
            if( file.exists() ){
                target = new FileInputStream(file);
                in     = new ObjectInputStream(target);
                obj = in.readObject();
            }
        } catch( Exception e ){
            throw e;
        } finally {
            if( in!=null ){
                in.close();
            }
            if( target!=null ){
                target.close();
            }
        }
        return obj;
    }


    /***
     * 指定のファイルを削除する
     * @param type
     * @return
     * @throws Exception
     */
    public static synchronized boolean remove( FileType type ) throws Exception{
        boolean result = false;
        try {

            String dir = getBaseDirectory();

            // ターゲットのファイルを読み込む
            File file = new File(dir + type.value);
            if( file.exists() ){
                result = file.delete();
            }
        } catch( Exception e ){
            throw e;
        }
        return result;
    }

    /***
     * 指定したファイルのサイズを取得する
     * @param type
     * @return
     */
    public static synchronized int getFileSize( FileType type ) {
        String dir = getBaseDirectory();

        // 必要ならディレクトリを作成する
        createDirectoryIfNecessary(dir);

        // ターゲットのファイルを作成する
        File file = new File(dir + type.value);
        return (int)file.length();
    }
}
