package com.smart_bdash.mobile.analytics.notification;

/**
 * トークンAPI の処理を行うときに処理が割り込めないときに投げられる
 * @see BDashNotification#registerNotification()
 * @see BDashNotification#cancelNotification()
 * @author Created by dataX on 2016/12/21.
 */
public class BDashBusyException extends Exception{

    public BDashBusyException(){
        super("Currently being processed.");
    }
}
