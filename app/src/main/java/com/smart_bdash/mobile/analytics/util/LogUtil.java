package com.smart_bdash.mobile.analytics.util;


import android.net.Uri;
import android.util.Log;

import com.smart_bdash.mobile.analytics.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * デバッグログ関連のユーティリティ
 */
public class LogUtil {

    // Debug 状態. 配布 AAR の BuildConfig.DEBUG は常に false のため、既定ではログ出力されない
    private static boolean isDebug = BuildConfig.DEBUG;

    /**
     * ログ出力時に値を伏せ字にすべき既定の秘匿キー。
     * （識別子・トークン・個人情報。表記ゆれ（uuId / uuid）も吸収する）
     */
    public static final Set<String> DEFAULT_SENSITIVE_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "appId", "accountId", "customId", "uuId", "uuid",
                    "deviceId", "notificationId", "loginUserId", "dId", "bdId"
            ))
    );


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


    // ===== 秘匿情報のマスキング =====

    /**
     * トークン・識別子等の秘匿文字列をマスクする。
     * 先頭 5 文字のみ残し、以降を {@code ****} に伏せる。
     * 例: {@code "abcdef123456"} -> {@code "abcde****"}、空文字 -> {@code "(empty)"}、{@code null} -> {@code "(nil)"}。
     * 5 文字以下の場合はそのまま返す（伏せる対象が無いため）。
     * @param value マスク対象の文字列。{@code null} も安全に扱える
     * @return マスク済み文字列
     */
    public static String mask(String value) {
        if (value == null) {
            return "(nil)";
        }
        if (value.isEmpty()) {
            return "(empty)";
        }
        if (value.length() <= 5) {
            return value;
        }
        return value.substring(0, 5) + "****";
    }

    /**
     * 本文を出さずにサイズ（文字数）のみで表現する。
     * サーバーレスポンス等、本文に秘匿情報を含みうる文字列のログ用。
     * @param value 対象文字列。{@code null} も安全に扱える
     * @return {@code "(N chars)"} 形式の文字列
     */
    public static String maskData(String value) {
        if (value == null) {
            return "(nil)";
        }
        return "(" + value.length() + " chars)";
    }

    /**
     * 本文を出さずにサイズ（バイト数）のみで表現する。
     * 本文に秘匿情報を含みうるバイト列のログ用。
     * @param data 対象データ。{@code null} も安全に扱える
     * @return {@code "(N bytes)"} 形式の文字列
     */
    public static String maskData(byte[] data) {
        if (data == null) {
            return "(nil)";
        }
        return "(" + data.length + " bytes)";
    }

    /**
     * URL からクエリ・フラグメントを除去し、{@code host + path} のみに丸める。
     * クエリやフラグメントに秘匿パラメータが乗りうるため除外する。
     * @param urlString 対象 URL 文字列
     * @return {@code "host/path"} 形式の文字列。解析不能時は {@code "(invalid url)"}
     */
    public static String maskUrl(String urlString) {
        if (urlString == null) {
            return "(nil)";
        }
        try {
            Uri uri = Uri.parse(urlString);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null && path == null) {
                return "(invalid url)";
            }
            return (host != null ? host : "") + (path != null ? path : "");
        } catch (Exception e) {
            return "(invalid url)";
        }
    }

    /**
     * JSON 文字列から秘匿キーの値を伏せ字に置換した文字列を返す。
     * トラッキング/Web接客のリクエストボディ等、秘匿キーと非秘匿キーが混在する
     * JSON のログ用。秘匿キーの値は {@link #mask(String)}（先頭 5 文字 + {@code ****}）に置換し、
     * キー名と非秘匿キーの値は残す。ネストしたオブジェクト・配列も再帰的に処理する。
     * パース不能な場合は本文を出さず {@code "(masked)"} を返す（安全側へフォールバック）。
     * @param json 対象 JSON 文字列
     * @return マスク済みの JSON 文字列
     */
    public static String maskJson(String json) {
        return maskJson(json, DEFAULT_SENSITIVE_KEYS);
    }

    /**
     * {@link #maskJson(String)} の秘匿キー指定版。
     * @param json 対象 JSON 文字列
     * @param sensitiveKeys 伏せ字にするキー集合
     * @return マスク済みの JSON 文字列
     */
    public static String maskJson(String json, Set<String> sensitiveKeys) {
        if (json == null) {
            return "(nil)";
        }
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                return maskJsonArray(new JSONArray(json), sensitiveKeys).toString();
            }
            return maskJsonObject(new JSONObject(json), sensitiveKeys).toString();
        } catch (Exception e) {
            // パースできない場合は本文を出さない
            return "(masked)";
        }
    }

    private static JSONObject maskJsonObject(JSONObject src, Set<String> sensitiveKeys) {
        JSONObject result = new JSONObject();
        try {
            Iterator<String> keys = src.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = src.get(key);
                result.put(key, maskValue(key, value, sensitiveKeys));
            }
        } catch (Exception e) {
            // 失敗時は本文を残さない
            return new JSONObject();
        }
        return result;
    }

    private static JSONArray maskJsonArray(JSONArray src, Set<String> sensitiveKeys) {
        JSONArray result = new JSONArray();
        try {
            for (int i = 0; i < src.length(); i++) {
                // 配列要素はキーを持たないため非秘匿キー扱い（null）で再帰
                result.put(maskValue(null, src.get(i), sensitiveKeys));
            }
        } catch (Exception e) {
            return new JSONArray();
        }
        return result;
    }

    private static Object maskValue(String key, Object value, Set<String> sensitiveKeys) {
        if (value instanceof JSONObject) {
            return maskJsonObject((JSONObject) value, sensitiveKeys);
        }
        if (value instanceof JSONArray) {
            return maskJsonArray((JSONArray) value, sensitiveKeys);
        }
        if (key != null && sensitiveKeys.contains(key)) {
            if (value instanceof String) {
                return mask((String) value);
            }
            // 文字列以外の秘匿値は完全に伏せる
            return "***";
        }
        return value;
    }
}
