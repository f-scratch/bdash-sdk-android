package com.f_scratch.bdash.mobile.analytics.connect.mock;

import com.f_scratch.bdash.mobile.analytics.connect.ConnectClient;
import com.f_scratch.bdash.mobile.analytics.connect.ConnectType;
import com.f_scratch.bdash.mobile.analytics.connect.IConnectAsyncResponse;
import com.f_scratch.bdash.mobile.analytics.connect.IConnectClientController;
import com.f_scratch.bdash.mobile.analytics.connect.RequestParam;

public class MockFailedConnectClientController extends IConnectClientController {

    @Override
    public ConnectClient connect(IConnectAsyncResponse response, ConnectType type, String send) {
        return connect(response, type, send, null);
    }

    @Override
    public ConnectClient connect(IConnectAsyncResponse response, ConnectType type, String send, RequestParam param) {
        FailedConnectClient client = new FailedConnectClient();
        try {
            response.onPostExecuteImpl(client, new Exception("MockFailedException"));
        }catch( Exception e ) {
        }
        return client;
    }

    @Override
    public void waitConnect() {

    }
}
