package com.smart_bdash.mobile.analytics.connect;


/***
 * ネットワークが見つからない場合に発生する例外
 * 
 * @author fujimaru
 */
@SuppressWarnings("serial")
public class NetworkNotFoundException extends Exception{

	public NetworkNotFoundException(){
		super("network not found exception");
	}

}
