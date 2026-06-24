package com.smart_bdash.mobile.analytics.connect.factory;

import com.smart_bdash.mobile.analytics.connect.ConnectClientController;
import com.smart_bdash.mobile.analytics.connect.IConnectClientController;

public class ConnectClientControllerCreator extends AbstractConnectControllerCreator {

    @Override
    public IConnectClientController create() {
        return new ConnectClientController();
    }
}
