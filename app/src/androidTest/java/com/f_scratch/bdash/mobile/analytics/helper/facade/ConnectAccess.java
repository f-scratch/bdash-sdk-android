package com.f_scratch.bdash.mobile.analytics.helper.facade;

import java.lang.reflect.Field;

/**
 * Created by fscratch on 2017/07/04.
 */

public class ConnectAccess {

    public static String getTokenAPI_URL() throws Exception {
        Class clazz = getConnectConfigClass();

        Field target = clazz.getDeclaredField("API_TOKEN_URL");
        target.setAccessible(true);
        return (String) target.get(clazz);
    }

    private static Class getConnectConfigClass() throws Exception {
        Class clazz = Class.forName("com.f_scratch.bdash.mobile.analytics.connect.ConnectConfig");
        return clazz;
    }

}
