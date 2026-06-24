package com.smart_bdash.mobile.analytics;

import com.smart_bdash.mobile.analytics.model.User;
import com.smart_bdash.mobile.analytics.model.config.JsonKey;

import java.util.HashMap;

public class TrackerDebug {

    private boolean connectState;
    @SuppressWarnings("FieldCanBeLocal")
    private Runnable connectListener;

    public boolean _isConnectSuccess() {
        return connectState;
    }

    public void _setConnectListener(Runnable listener) {
        connectListener = listener;
    }

    public void _addTestData(Tracker instance, HashMap<String, Object> build) {
        // パラメーターの設定
        build.put(JsonKey.KEY_SCREEN_NAME, instance.screenName);
        build.put(JsonKey.KEY_LOGIN_USERID, instance.loginUser);
        build.put(JsonKey.KEY_RELATIONAL, instance.relationalKey);
        build.put(JsonKey.KEY_RELATIONAL_VALUE, instance.relationalValue);
        build.put(JsonKey.KEY_BOOT_TYPE, instance.bootType);
        build.put(JsonKey.KEY_BOOT_VALUE, instance.bootValue);
        build.put(JsonKey.KEY_USER_MAP, instance.userMap);

        // リセット
        instance.screenName = null;
        instance.loginUser = null;
        instance.relationalKey = null;
        instance.relationalValue = null;
        instance.bootType = null;
        instance.bootValue = null;
        instance.userMap = null;

        EventLogManager.getInstance().addEventLog(build);
    }

    public int _getBufferSize() {
        return EventLogManager.getInstance().getEventSize();
    }

    public int _getMaxSendBufferSize() {
        return EventLogManager.THRESHOLD_SEND_BUFFER;
    }

    public int _getMaxRangeBufferSize() {
        return EventLogManager.MAX_SEND_BUFFER;
    }

    public int _getMaxStorageBufferSize() {
        return EventLogManager.MAX_STORAGE_BUFFER;
    }

    public void _bufferClear() {
        while (EventLogManager.getInstance().getEventSize() > 0) {
            EventLogManager.getInstance().lock();
            EventLogManager.getInstance().unlockCommit();
        }
        EventLogManager.getInstance().save();
    }

    public void _save() {
        try {
            EventLogManager.getInstance().save();
        } catch (Exception ignored) {
        }
    }

    public String _getInternalLogs() {
        StringBuffer sb = new StringBuffer();
        sb.append("adId: " + User.getInstance().getAdId());
        sb.append("\n");
        sb.append("uuId: " + User.getInstance().getUniqueId());
        sb.append("\n");
        return sb.toString();
    }
}
