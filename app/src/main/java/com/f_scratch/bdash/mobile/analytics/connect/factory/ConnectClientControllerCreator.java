package com.f_scratch.bdash.mobile.analytics.connect.factory;

import com.f_scratch.bdash.mobile.analytics.connect.ConnectClientController;
import com.f_scratch.bdash.mobile.analytics.connect.IConnectClientController;

public class ConnectClientControllerCreator extends AbstractConnectControllerCreator {

    @Override
    public IConnectClientController create() {
        return new ConnectClientController();
    }
}
