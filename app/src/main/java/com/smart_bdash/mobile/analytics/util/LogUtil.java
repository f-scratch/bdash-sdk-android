package com.smart_bdash.mobile.analytics.util;


import android.util.Log;

import com.smart_bdash.mobile.analytics.BuildConfig;


/**
 * デバッグログ関連のユーティリティ
 */
public class LogUtil {

    // Debug 状態. 配布 AAR の BuildConfig.DEBUG は常に false のため、既定ではログ出力されない
    private static boolean isDebug = BuildConfig.DEBUG;


    /***
     * デバッグモードの管理フラグを設定する
     * @param use
     */
    public static void setDebuggable( boolean use ) {
        isDebug = use;
    }

    /***
     * デバッグモードの管理フラグ
     * @return
     */
    public static boolean isDebuggable(){
        return isDebug;
    }


    /**
     * ログ出力
     * @param msg String
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
     * @param msg String
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
