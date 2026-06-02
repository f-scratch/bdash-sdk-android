package com.f_scratch.bdash.mobile.analytics.connect.mock;

import com.f_scratch.bdash.mobile.analytics.connect.ConnectClient;

public class FailedConnectClient extends ConnectClient {

    public FailedConnectClient(){
        super(null,null,null,null);
    }

    @Override
    public int getResponseCode() {
        return 500;
    }

    @Override
    public String getResponse() {
        return null;
    }


}
