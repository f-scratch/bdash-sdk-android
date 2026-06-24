package com.smart_bdash.mobile.analytics.connect;

/***
 * 通信リクエストのオプションリクエストを司るクラス
 * 
 * @author fujimaru
 */
public class RequestParam {

	/** HTTP リクエストメソッド */
	public static final String HEAD_METHOD = "HEAD";
	public static final String GET_METHOD  = "GET";
	public static final String POST_METHOD = "POST";
	
	/** HTTP MIME タイプ */
	public static final String MIME_OCTET_STREAM = "application/octet-stream";
	public static final String MIME_JSON         = "application/json"; /** JSON */
	public static final String MIME_IMAGE_JPEG   = "image/jpeg"; /** Image JPEG */
	
	
	int option;
	String method;
	String mimeType;
	int connect_timeout;
	int connect_read_timeout;
	boolean multipart;
	boolean useUserCancel;
	String concat_url;

	public Runnable		param_run_callback;
	public int			param_int;
	public String		param_str;
	public Object		param_obj;



	/** ヘッダー関連 */
	public String if_modified_since;

	public void setMultiPart(boolean enable) {
		multipart = enable;
	}

	public void setRequestMethod(String method) {
		this.method = method;
	}

	public String getRequestMethod() {
		return method;
	}

	public void setConnectTimeOut(int time_out) {
		connect_timeout = time_out;
	}

	public void setConnectReadTimeOut(int time_out) {
		connect_read_timeout = time_out;
	}

	public int getConnectTimeOut() {
		return connect_timeout;
	}

	public int getConnectReadTimeOut() {
		return connect_read_timeout;
	}

	public void setMimeType(String type) {
		this.mimeType = type;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getConcatUrl() {
		return concat_url;
	}

	public void setConcatUrl(String concat_url) {
		this.concat_url = concat_url;
	}
}
