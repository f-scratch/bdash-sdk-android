package com.f_scratch.bdash.mobile.analytics.connect;


/***
 * 非同期通信を受け取るレスポンス
 * 
 * @author fujimaru
 */
public abstract class IConnectAsyncResponse implements IConnectResponse{
	
	public boolean isAbort(){
		return false;
	}
	
	public void onConnectDropped(final ConnectClient client){}

}
