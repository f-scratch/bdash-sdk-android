package com.smart_bdash.mobile.analytics.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * ストレージ制御のユーティリティ
 */
public class StorageUtil {

    private static final String APP_PREFERENCE_NAME = "com.smart_bdash.mobile.analytics.storage";
    private static String ROOT        = "";
    private static final String LOCAL_DIR   = "com.smart_bdash.mobile.analytics.ser/";

    /**
     * 保存・復元に使用する Gson インスタンス.<br>
     *  ・{@code HashMap<String,Object>} の値を復元する際、既定では整数も Double 化(123 → 123.0)するため、<br>
     *    ToNumberPolicy.LONG_OR_DOUBLE を指定して整数を Long で復元し、送信 JSON の桁化けを防ぐ
     */
    private static final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();


    public static enum FileType {
        //////////////////////////////////////////////
          USER("001.json")              // User
        , SETTINGS("002.json")          // Setting
        , EVENT_LOGS("003.json")        // Event
        , EVENT_WORK_LOGS("004.json")   // Event
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

    /**
     * アプリケーション用のプリファレンスを取得する
     * @param context プリファレンス取得に使用するコンテキスト
     * @return アプリケーション用の SharedPreferences
     */
    public static SharedPreferences getApplicationPreferences(Context context) {
        return context.getSharedPreferences(APP_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
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
     * 受け取ったオブジェクトを JSON 化して保存する.ストレージ操作は排他制御とする
     * @param type 保存先のファイル種別
     * @param obj 保存するオブジェクト
     */
    public static synchronized <T> void serialize(FileType type, T obj) throws Exception {
        FileOutputStream target = null;
        Writer out  = null;
        try {
            String dir = getBaseDirectory();

            // 必要ならディレクトリを作成する
            createDirectoryIfNecessary(dir);

            // ターゲットのファイルを作成する
            File file = new File(dir + type.value);
            target = new FileOutputStream(file);
            out    = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            gson.toJson(obj, out);

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
     * 受け取ったオブジェクトを JSON 化して保存する.
     * 呼び出し元で同一ファイルタイプへの並行アクセスが発生しないことが保証される場合にのみ使用すること
     * 並行アクセスの可能性がある場合は serialize() を使用すること
     * @param type
     * @param obj
     * @param <T>
     * @throws Exception
     */
    public static <T> void serialize_nowait(FileType type, T obj) throws Exception {
        FileOutputStream target = null;
        Writer out  = null;
        try {
            String dir = getBaseDirectory();

            // 必要ならディレクトリを作成する
            createDirectoryIfNecessary(dir);

            // ターゲットのファイルを作成する
            File file = new File(dir + type.value);
            target = new FileOutputStream(file);
            out    = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            gson.toJson(obj, out);

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
     * ファイルから JSON を読込み、指定クラスのオブジェクトに復元して返す.ストレージ操作は排他制御とする
     * @param type 読み込み対象のファイル種別
     * @param clazz 復元するクラス
     * @return 復元したオブジェクト.ファイルが存在しない場合は null
     */
    public static synchronized <T> T deserialize(FileType type, Class<T> clazz) throws Exception{
        return deserialize(type, (Type) clazz);
    }

    /**
     * ファイルから JSON を読込み、指定の型に復元して返す.ストレージ操作は排他制御とする<br>
     *  ・{@code ArrayList<HashMap<String,Object>>} のようなジェネリック型は {@code TypeToken} で型を渡す
     * @param type 読み込み対象のファイル種別
     * @param typeOfT 復元する型
     * @return 復元したオブジェクト.ファイルが存在しない場合は null
     */
    public static synchronized <T> T deserialize(FileType type, Type typeOfT) throws Exception{
        FileInputStream target = null;
        Reader in   = null;
        T obj = null;
        try {

            String dir = getBaseDirectory();

            // ターゲットのファイルを読み込む
            File file = new File(dir + type.value);
            if( file.exists() ){
                target = new FileInputStream(file);
                in     = new InputStreamReader(target, StandardCharsets.UTF_8);
                obj = gson.fromJson(in, typeOfT);
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

    /**
     * 指定したファイルのサイズを取得する
     * @param type 取得対象のファイル種別
     * @return ファイルサイズ（バイト数）
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
