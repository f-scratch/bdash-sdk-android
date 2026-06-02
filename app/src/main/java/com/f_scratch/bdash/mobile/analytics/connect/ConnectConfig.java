package com.f_scratch.bdash.mobile.analytics.connect;

/**
 * 通信関連のコンフィグ
 *
 * @author fujimaru
 */
class ConnectConfig {

    /** 共通デバッグ用 */
    public static final String DEBUG_EMPTY_URL = "";  // 接続先の無いURL

    /** Tracking API 接続先 */
    public static final String API_URL = "https://trackersdk.smart-bdash.com/v2/tracking";

    /** プッシュ通知 API 接続先 */
   public static final String API_TOKEN_URL = "https://mobile.smart-bdash.com/v2/notification";

    /** Web接客 API 接続先 */
    // WebView リクエスト先
    public static final String API_WEB_RECEPTION_VIEW_URL = "https://receptions.smart-bdash.com/v2/";
    // 顧客設定情報 リクエスト先
    public static final String API_WEB_RECEPTION_SETTING_URL = "https://receptions.smart-bdash.com/mobile/v2/receptions";


    public static String getUrl( ConnectType type ) {
        switch(type) {
        // トラッキングAPI
        case API_GET:
        case API_POST:
            return API_URL;

        // プッシュ通知・トークンAPI
        case API_TOKEN_POST:
            return API_TOKEN_URL;

        // WEB接客 設定情報API
        case API_WEB_RECEPTION_SETTING:
            return API_WEB_RECEPTION_SETTING_URL;

        // WEB接客 WebView API
        case API_WEB_RECEPTION_VIEW:
            return API_WEB_RECEPTION_VIEW_URL;

        // デバッグ用
        case DEBUG_API:
            return DEBUG_EMPTY_URL;
        }
        return null;
    }
}
