package com.smart_bdash.mobile.analytics.model.config;

import com.smart_bdash.mobile.analytics.BuildConfig;

/**
 * SDK コンフィグクラス
 */
public class SDKConfig {

    /** SDK バージョン */
    public static final String SDK_VERSION = BuildConfig.SDK_VERSION;

    /** マニフェスト用のコンフィグ */
    public static final String APP_BDASH_APP_ID     = "APP_BDASH_APP_ID";
    public static final String APP_BDASH_ACCOUNT_ID = "APP_BDASH_ACCOUNT_ID";
    public static final String APP_BDASH_DATA_VIEW  = "APP_BDASH_DATA_VIEW";
    public static final String APP_BDASH_NOTIFICATION_LAUNCH    = "com.smart_bdash.mobile.push.launch";
    public static final String APP_BDASH_NOTIFICATION_ICON      = "com.smart_bdash.mobile.push.icon";
    public static final String APP_BDASH_NOTIFICATION_BIG_ICON  = "com.smart_bdash.mobile.push.bigIcon";
    public static final String APP_BDASH_NOTIFICATION_LOLLIPOP_BIG_ICON  = "com.smart_bdash.mobile.push.lollipop.bigIcon";
    public static final String APP_BDASH_NOTIFICATION_ICON_ACCENT_COLOR  = "com.smart_bdash.mobile.push.accentColor";

    // 2018/11 Google-Play API26 仕様に対応 / SDK v2.1.0
    public static final String APP_BDASH_NOTIFICATION_CHANNEL_ID    = "com.smart_bdash.mobile.push.channel.id";
    public static final String APP_BDASH_NOTIFICATION_CHANNEL_NAME  = "com.smart_bdash.mobile.push.channel.name";
    public static final String APP_BDASH_NOTIFICATION_CHANNEL_DESC  = "com.smart_bdash.mobile.push.channel.desc";
    public static final String APP_BDASH_NOTIFICATION_CHANNEL_BADGE = "com.smart_bdash.mobile.push.channel.badge";
    public static final String APP_BDASH_NOTIFICATION_CHANNEL_IMP   = "com.smart_bdash.mobile.push.channel.importance";

    public static final String APP_BDASH_FCM_PAYLOAD_KEY_TITLE = "gcm.notification.title";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_BODY = "gcm.notification.body";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_CUSTOM_PAYLOAD = "custom_payload";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_DID = "dId";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_BDID = "bdId";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_JP_CO_DATAX = "jp_co_fscratch";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_FCM_API = "fcm_api";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_IMAGE = "gcm.notification.image";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_NOTIFICATION_TYPE_ANDROID = "notification_type_android";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_MESSAGE_ID = "messageId";

    // BundleクラスからNotificationMessageクラスへのペイロード情報の変換に必要な定義
    public static final String APP_BDASH_NOTIFICATION_TITLE = "title";
    public static final String APP_BDASH_NOTIFICATION_BODY = "body";
    public static final String APP_BDASH_NOTIFICATION_IMAGE = "image";

    // FCM legacy
    public static final String APP_BDASH_FCM_API_V1 = "v1";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_TITLE_LEGACY = "title";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_MESSAGE = "message";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_PARAM = "param";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_NOTIFICATION_PARAM = "notification_param";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_ID = "id";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_MEDIA_URL = "mediaUrl";
    public static final String APP_BDASH_FCM_PAYLOAD_KEY_BUTTONS = "buttons";
}
