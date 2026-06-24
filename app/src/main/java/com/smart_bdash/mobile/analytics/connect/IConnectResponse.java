package com.smart_bdash.mobile.analytics.connect;

/***
 * 通信レスポンス用のインターフェイス
 * 
 * @author fujimaru
 */
interface IConnectResponse {
	
	public abstract boolean isAbort();
	
	public abstract void onConnectDropped(final ConnectClient client); 							  /** 捨てられた通信 */
	public abstract void onConnect(final ConnectClient client)throws Exception; 				  /** 成功した通信 */
	public abstract void onPostExecuteImpl(final ConnectClient client, Throwable exception )throws Exception;
}
