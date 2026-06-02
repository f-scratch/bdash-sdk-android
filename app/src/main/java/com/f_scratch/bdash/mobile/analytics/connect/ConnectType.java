package com.f_scratch.bdash.mobile.analytics.connect;

public enum ConnectType {

	POST_START
	/////////////////////////////////////////////
	// POST リクエストは以下に定義する
	/////////////////////////////////////////////
	, API_POST
	, API_TOKEN_POST
	, API_WEB_RECEPTION_SETTING
	, API_WEB_RECEPTION_VIEW
    , DEBUG_API
	, _POST_END
	/////////////////////////////////////////////
	// GET リクエストは以下に定義する
	/////////////////////////////////////////////
	, GET_START
	, API_GET
	, OTHER_GET
	/////////////////////////////////////////////
	// ここまで
	/////////////////////////////////////////////
	;

	public static class Option{
		public static final int RESULT_STRING = 0x000000;
		public static final int RESULT_BYTES  = 0x010000;
	}

	public boolean isPostMethod(ConnectType type){
		return (POST_START.ordinal()>=type.ordinal() && type.ordinal()<GET_START.ordinal());
	}

	public boolean isGetMethod(ConnectType type){
		return isPostMethod(type)==false;
	}


	/**
	 * WebView の base url を返す.
	 * @return
	 */
	public static String getWebViewBaseUrl(){
		return ConnectConfig.API_WEB_RECEPTION_VIEW_URL;
	}
}
