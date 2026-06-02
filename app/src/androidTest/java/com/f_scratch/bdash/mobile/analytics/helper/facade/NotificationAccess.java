package com.f_scratch.bdash.mobile.analytics.helper.facade;

import android.content.Context;

import com.f_scratch.bdash.mobile.analytics.connect.IConnectAsyncResponse;
import com.f_scratch.bdash.mobile.analytics.connect.factory.AbstractConnectControllerCreator;
import com.f_scratch.bdash.mobile.analytics.notification.BDashNotification;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by fujimaru on 2016/12/12.
 */
public class NotificationAccess {


    public static boolean requestTokenAPI(BDashNotification instance, String notificationId) throws Exception {
        try {
            Method method = instance.getClass().getDeclaredMethod("requestTokenAPI", String.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(instance, notificationId);
        } catch (Exception e) {
            throw e;
        }
    }

    public static void requestTokenRefresh(BDashNotification instance, Context context) throws Exception {
        try {
            Method method = instance.getClass().getDeclaredMethod("requestTokenRefresh", Context.class);
            method.setAccessible(true);
            method.invoke(instance, context);
        } catch (Exception e) {
            throw e;
        }
    }

    public static void requestTokenRefreshByService(BDashNotification instance, Context context, String newToken) throws Exception {
        try {
            Method method = instance.getClass().getDeclaredMethod("requestTokenRefreshByService", Context.class, String.class);
            method.setAccessible(true);
            method.invoke(instance, context, newToken);
        } catch (Exception e) {
            throw e;
        }
    }

    public static void setHookConnectResponse(BDashNotification instance, IConnectAsyncResponse response) throws Exception {
        Field target = instance.getClass().getDeclaredField("hookResponse");
        target.setAccessible(true);
        target.set(instance, response);
    }

    public static AbstractConnectControllerCreator setConnectControllerCreator(BDashNotification instance, AbstractConnectControllerCreator creator) throws Exception {
        Field target = instance.getClass().getDeclaredField("connectControllerCreator");
        target.setAccessible(true);
        AbstractConnectControllerCreator obj = (AbstractConnectControllerCreator) target.get(instance);
        target.set(instance, creator);
        return obj;
    }

    public static void instanceReset(BDashNotification instance) throws Exception {
        Field target = instance.getClass().getDeclaredField("instance");
        target.setAccessible(true);
        target.set(null, null);
    }

    /**
     * call_onNewToken( newToken ) を呼び出す
     *
     * @param instance
     * @param newToken
     * @throws Exception
     */
    public static void call_onNewToken(BDashNotification instance, String newToken) throws Exception {
        try {
            Method method = instance.getClass().getDeclaredMethod("call_onNewToken", String.class);
            method.setAccessible(true);
            method.invoke(instance, newToken);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * call_onUnregistered( oldToken ) を呼び出す
     *
     * @param instance
     * @param newToken
     * @throws Exception
     */
    public static void call_onUnregistered(BDashNotification instance, String newToken) throws Exception {
        try {
            Method method = instance.getClass().getDeclaredMethod("call_onUnregistered", String.class);
            method.setAccessible(true);
            method.invoke(instance, newToken);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * call_onDeletedMessages() を呼び出す
     *
     * @param instance
     * @throws Exception
     */
    public static void call_onDeletedMessages(BDashNotification instance) throws Exception {
        try {
            Method method = instance.getClass().getDeclaredMethod("call_onDeletedMessages");
            method.setAccessible(true);
            method.invoke(instance);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * setEnableNotification() を呼び出す
     *
     * @param instance
     * @param enable
     * @throws Exception
     */
    public static void setEnableNotification(BDashNotification instance, boolean enable) throws Exception {
        try {
            Method method = instance.getClass().getDeclaredMethod("setEnableNotification", boolean.class);
            method.setAccessible(true);
            method.invoke(instance, enable);
        } catch (Exception e) {
            throw e;
        }
    }

}
