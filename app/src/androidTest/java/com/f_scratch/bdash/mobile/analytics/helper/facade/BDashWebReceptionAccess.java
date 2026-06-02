package com.f_scratch.bdash.mobile.analytics.helper.facade;

import com.f_scratch.bdash.mobile.analytics.web_reception.BDashReport;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BDashWebReceptionAccess {

    public static String get_BDashReport_AccessTypeUpdate(BDashReport instance) {
        Class clazz = instance.getClass();
        while (clazz != null) {
            try {
                return _getStringProperty(instance, clazz, "ACCESS_TYPE_UPDATE");
            } catch (Exception e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public static void set_BDashReport_setAccessType(BDashReport instance, String value) throws Exception {
        Class clazz = instance.getClass();
        while (clazz != null) {
            try {
                _setStringProperty(instance, clazz, "accessType", value);
                return;
            } catch (Exception e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new Exception("");
    }

    public static void _setStringProperty(Object instance, Class clazz, String propertyName, String value) throws Exception {
        Field target = clazz.getDeclaredField(propertyName);
        target.setAccessible(true);
        target.set(instance, value);
    }

    public static String _getStringProperty(Object instance, Class clazz, String propertyName) throws Exception {
        try {
            Field target = clazz.getDeclaredField(propertyName);
            target.setAccessible(true);
            return (String) target.get(instance);
        } catch (Exception e) {
            throw e;
        }
    }

}