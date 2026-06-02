package com.f_scratch.bdash.mobile.analytics.helper.facade;

import com.f_scratch.bdash.mobile.analytics.Tracker;
import com.f_scratch.bdash.mobile.analytics.connect.ConnectClientController;

import java.lang.reflect.Method;

/**
 * Created by fujimaru on 2016/11/18.
 */
public class TrackerAccess {

    public static ConnectClientController getConnectController( Tracker tracker )throws Exception{
        try {
            Method method = tracker.getClass().getDeclaredMethod("getConnectController");
            method.setAccessible(true);
            return (ConnectClientController )method.invoke(tracker);
        }catch( Exception e ) {
            throw e;
        }
    }
}
