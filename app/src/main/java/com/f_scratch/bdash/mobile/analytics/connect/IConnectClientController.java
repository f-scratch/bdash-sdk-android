package com.f_scratch.bdash.mobile.analytics.connect;

public abstract class IConnectClientController {

    public abstract ConnectClient connect( IConnectAsyncResponse response, ConnectType type, String send );
    public abstract ConnectClient connect( IConnectAsyncResponse response, ConnectType type, String send, RequestParam param) ;
    public abstract void waitConnect();

}
