package com.f_scratch.bdash.mobile.analytics.web_reception;


import java.util.HashMap;

/**
 * BDashSchema schema = BDashSchema.parseSchema(str);
 *
 * BDashSchema schema = new BDashSchema();
 * schema.parse(str);
 *
 *
 * schema.getType();
 * schema.hasCloseOperation()
 *
 */
 class BDashSchema {

    final private static String POPUP_CLOSE = "close";

    // Schema
    final static String _SCHEMA_INTERNAL = "internal://";
    final static String _SCHEMA_WEBVIEW  = "webview://";
    final static String _SCHEMA_POPUP    = "popup://";
    final static String _SCHEMA_EXTERNAL = "external://";
    final static String _SCHEMA_COMMAND  = "command://";

    enum Type{
         SCHEMA_INTERNAL(0)
        ,SCHEMA_WEBVIEW(1)
        ,SCHEMA_POPUP(2)
        ,SCHEMA_EXTERNAL(3)
        ,SCHEMA_COMMAND(4)
        ,SCHEMA_UNKNOWN(-1) // 未定義のスキーマ
        ;

        int value;

        Type(int value){
            this.value = value;
        }

        int getValue(){ return value; };
    }

    Type schemaType;
    String originalParam;
    HashMap<String,String> coordinationParam;


    BDashSchema(){
    }

    /**
     * スキーマデータを解析する
     * @param schema
     * @return
     */
    Type parse( String schema ) {
        String localParam = originalParam = getMessageDetail(schema);

        if (schema.startsWith(_SCHEMA_INTERNAL)) {
            schemaType = Type.SCHEMA_INTERNAL;
            coordinationParam = prepareSchema(localParam);
        } else if (schema.startsWith(_SCHEMA_WEBVIEW)) {
            schemaType = Type.SCHEMA_WEBVIEW;
            coordinationParam = getURLFromWebView(localParam);
        } else if (schema.startsWith(_SCHEMA_POPUP)) {
            schemaType = Type.SCHEMA_POPUP;
        } else if (schema.startsWith(_SCHEMA_EXTERNAL)) {
            schemaType = Type.SCHEMA_EXTERNAL;
        } else if (schema.startsWith(_SCHEMA_COMMAND)) {
            schemaType = Type.SCHEMA_COMMAND;
        } else {
            schemaType = Type.SCHEMA_UNKNOWN;
        }

        return schemaType;
    }

    /**
     *
     * @param value
     * @return
     */
    private HashMap<String,String> prepareSchema(String value) {

        HashMap<String,String> map = new HashMap<>();
        String[] work = value.split("&");

        for( int i=0 ; i<work.length ; i++ ){
            String[] keyVal = work[i].split("=");
            if( keyVal[0].length() == 0 ) continue;

            String key = keyVal[0];
            String val = keyVal.length>1 ? BDashWebReceptionUtil.URLDecode(keyVal[1]): null;

            map.put( key, val );
        }

        return map;
    }

    /**
     * アプリ内のWebView起動時に設定されたURLを取得
     * @param link
     * @return
     */
    private HashMap<String, String> getURLFromWebView(String link) {
        HashMap<String,String> map = new HashMap<>();
        String urlContentKey = "url=";
        String value = BDashWebReceptionUtil.URLDecode(link.replace(urlContentKey, ""));

        map.put("url", value);

        return map;
    }

    /**
     * スキーマタイプを取得
     * @return
     */
    Type getType(){
        return schemaType;
    }

    /**
     * 顧客へ連携する「整形済みのパラメーター」を取得する
     * @return HashMap<String, String>
     */
    HashMap<String, String> getCoordinationParam(){
        return coordinationParam;
    }

    /**
     * 「生のパラメーター」を取得する
     * @return
     */
    String getParam(){
        return originalParam;
    }


    /**
     * メッセージ情報を取得する
     */
    public static String getMessageDetail( String link ) {
        String findChars = "://";
        boolean hasUrlKey = false;
        String urlContentKey = "url=";
        String tmpContentFirst = "";
        String wrongHttpScheme = "http//";
        String wrongHttpsScheme = "https//";
        String detailContent;

        int pos = link.indexOf(findChars);
        if( pos == -1 ){
            return null;
        }

        detailContent = link.substring(pos+findChars.length());

        // http後の":"(コロン)が消失するエラー対応
        int posUrl = detailContent.indexOf(urlContentKey);
        if(posUrl != -1) {
            // "url="キーが存在するか判定(WebView連携時(スキーマ webview:))
            // "url="キー値までの前後の情報をそれぞれ保持
            hasUrlKey = true;
            tmpContentFirst = detailContent.substring(0, posUrl);
            detailContent = detailContent.substring(posUrl+urlContentKey.length());
        }

        // 消失した":"(コロン)を補完
        if(detailContent.startsWith(wrongHttpScheme)) {
            detailContent = detailContent.replaceFirst(wrongHttpScheme, "http://");
        } else if(detailContent.startsWith(wrongHttpsScheme)) {
            detailContent = detailContent.replaceFirst(wrongHttpsScheme, "https://");
        }

        if(hasUrlKey) {
            // "url="キーが存在する場合、保持していた情報を合体
            detailContent = tmpContentFirst + urlContentKey + detailContent;
        }

        return detailContent;
    }

}
