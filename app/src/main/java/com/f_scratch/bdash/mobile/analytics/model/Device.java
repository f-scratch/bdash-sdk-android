package com.f_scratch.bdash.mobile.analytics.model;

/**
 * デバイスモデルクラス
 */
public class Device {

    private static Device instance;

    public String appId;
    public String accountId;
    public String customId;
    public String lang;
    public String carrier;
    public String os;
    public String osVersion;
    public String model;
    public String display;
    public String dataViewIds;

    public String appVersion;   // アプリバージョン
    public String notification_launchActivity;   // APP_BDASH_NOTIFICATION_LAUNCH
    public int notification_icon;             // APP_BDASH_NOTIFICATION_ICON
    public int notification_lollipop_bigIcon; // APP_BDASH_NOTIFICATION_LOLLIPOP_BIG_ICON
    public String notification_icon_accent_color;// APP_BDASH_NOTIFICATION_ICON_ACCENT_COLOR

    public String channel_id;
    public String channel_name;
    public String channel_desc;
    public String channel_imp;
    public boolean channel_badge;


    /***
     * 内部用デバイスクラスのシングルトンを取得する<br>
     * ・複数のスレッドから同時に呼ぶシーンは無い
     * @return Device
     */
    public static Device getInstance() {
        if( instance == null ) {
            instance = new Device();
        }
        return instance;
    }

}
