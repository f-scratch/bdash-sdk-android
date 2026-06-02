package com.f_scratch.bdash.mobile.analytics.model.config;

import com.f_scratch.bdash.mobile.analytics.model.Device;
import com.f_scratch.bdash.mobile.analytics.model.JsonDevice;
import com.f_scratch.bdash.mobile.analytics.model.JsonToken;
import com.f_scratch.bdash.mobile.analytics.model.User;

/**
 */
public class JsonKey {


    public static final String TYPE_SCREEN_VIEW    = "screenview";
    public static final String TYPE_EVENT_VIEW     = "event";
    public static final String TYPE_EXCEPTION_VIEW = "exception";

    public static final String KEY_SCREEN_NAME      = "screenName";
    public static final String KEY_LOGIN_USERID     = "loginUserId";
    public static final String KEY_RELATIONAL       = "relationalKey";
    public static final String KEY_RELATIONAL_VALUE = "relationalValue"; // BDash 2.0 2017/12 re;ease向け
    public static final String KEY_VISITOR_ID       = "visitorId";
    public static final String KEY_BOOT_TYPE        = "bootType";
    public static final String KEY_BOOT_VALUE       = "bootValue";
    public static final String KEY_USER_MAP         = "userMap";

    public static final String KEY_TRACKINGS     = "trackings"; // 本来の単語として、スペルが最後 s いらない

    public static final String KEY_RECEPTION_BOOT   = "boot";
    public static final String KEY_RECEPTION_UPDATE = "update";


    /***
     *
     * @return
     */
    public static JsonDevice createJsonDeviceFields(){
        JsonDevice result= new JsonDevice();
        initJsonDeviceFields(result);
        return result;
    }

    public static void initJsonDeviceFields( JsonDevice result ){
        result.appVersion= Device.getInstance().appVersion;
        result.accountId = Device.getInstance().accountId;
        result.deviceId  = User.getInstance().adId;
        result.uuId      = User.getInstance().uniqueId;
        result.appId     = Device.getInstance().appId;
        result.model     = Device.getInstance().model;
        result.display   = Device.getInstance().display;
        result.carrier   = Device.getInstance().carrier;
        result.lang      = Device.getInstance().lang;
        result.os        = Device.getInstance().os;
        result.osVersion = Device.getInstance().osVersion;
        result.customId  = Device.getInstance().customId;
        result.dataViewIds  = Device.getInstance().dataViewIds;
    }


    /**
     *
     * @return
     */
    public static JsonToken createJsonTokenFields(){
        JsonToken result = new JsonToken();
        initJsonDeviceFields(result);
        return result;
    }

}

