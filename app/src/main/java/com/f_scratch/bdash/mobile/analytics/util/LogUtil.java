package com.f_scratch.bdash.mobile.analytics.util;


import android.util.Log;


/**
 * デバッグログ関連のユーティリティ
 */
public class LogUtil {

    // Debug 状態
    private static boolean isDebug = false;


    /***
     * デバッグモードの管理フラグを設定する
     */
    public static void setDebuggable( boolean use ) {
        isDebug = use;
    }

    /***
     * デバッグモードの管理フラグ
     */
    public static boolean isDebuggable(){
        return isDebug;
    }


    /**
     * ログ出力
     */
    public static final void out(String msg) {
        if (isDebuggable()) {
            StackTraceElement trace = Thread.currentThread().getStackTrace()[3];
            Log.e(trace.getClassName() + "[" + trace.getMethodName() + "]", msg);
        }
    }
    public static final void out(Throwable e) {
        if (isDebuggable()) {
            StackTraceElement trace = Thread.currentThread().getStackTrace()[3];
            Log.e(trace.getClassName() + "[" + trace.getMethodName() + "]", e.toString());
        }
    }

    /**
     * ログ出力( System.out 形式 )
     */
    public static final void s(String msg) {
        if (isDebuggable()) {
            System.out.println(msg);
        }
    }
    public static final void s(Throwable e) {
        s(e.toString());
    }
}
