package com.smart_bdash.mobile.analytics.notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationButton {

    public List<ButtonElement> buttons = new ArrayList<>();

    public static class ButtonElement {
        public int number;
        public String notification_param;
        public String label;

        public ButtonElement(int number, String notification_param, String label) {
            this.number = number;
            this.notification_param = notification_param;
            this.label = label;
        }
    }

    public int      number;
    public String notification_param;
    public String   label;
}