package com.smart_bdash.mobile.analytics.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 
 */
public class TimeUtil {


    /**
     * GMTの取得 / ミリ秒まで取得
     * @return strGmt String
     */
    public static String getGMT() {
        // 日付の取得
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH) ;
        String strGmt = sdf.format(date);
        StringBuffer tmpTimeZone = new StringBuffer();
        tmpTimeZone.append(strGmt.substring(strGmt.length() - 2, strGmt.length()));
        strGmt = strGmt.substring(0, strGmt.length() - 2);
        strGmt = strGmt + tmpTimeZone.toString();
        return strGmt;
    }



}
