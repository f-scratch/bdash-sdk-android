package com.smart_bdash.mobile.analytics.notification;

import android.content.Context;

interface EventListener {
    boolean onRegistered(Context context, String registrationId);
}
