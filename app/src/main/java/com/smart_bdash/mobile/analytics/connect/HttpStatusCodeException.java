package com.smart_bdash.mobile.analytics.connect;

/**
 * Httpステータスコード Exception
 *
 * @author fujimaru
 */
public class HttpStatusCodeException extends Exception {

    int statusCode;

    public HttpStatusCodeException(int code ){
        this.statusCode = code;
    }
}
