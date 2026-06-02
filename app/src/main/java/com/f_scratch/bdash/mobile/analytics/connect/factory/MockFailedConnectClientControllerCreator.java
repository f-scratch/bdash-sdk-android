package com.f_scratch.bdash.mobile.analytics.connect.factory;

import com.f_scratch.bdash.mobile.analytics.connect.IConnectClientController;
import com.f_scratch.bdash.mobile.analytics.connect.mock.MockFailedConnectClientController;

public class MockFailedConnectClientControllerCreator extends AbstractConnectControllerCreator {

    @Override
    public IConnectClientController create() {
        return new MockFailedConnectClientController();
    }
}
