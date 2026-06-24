package com.smart_bdash.mobile.analytics.util;

import android.content.Context;

import java.util.Locale;

/**
 * ロケールのユーティリティ
 */
public class LocaleUtil {

    /**
     * 言語を取得する
     * @return locale String
     */
    public static String getLocale(Context context) {
        String locale = context.getResources().getConfiguration().locale.getLanguage();
        return locale;
    }

    /***
     * 国を取得する
     * @param context
     * @return
     */
    public static String getCountry(Context context) {
        String locale = context.getResources().getConfiguration().locale.getCountry();
        return locale;
    }

    /***
     * ja_JP など言語と国を組み合わせて返す
     * @return
     */
    public static String getLocaleAndCountry(Context context) {
        String locale = getLocale(context) + "_" + getCountry(context);
        return locale;
    }

}
