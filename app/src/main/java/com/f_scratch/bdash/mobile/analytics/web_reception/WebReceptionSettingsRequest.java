package com.f_scratch.bdash.mobile.analytics.web_reception;

import com.f_scratch.bdash.mobile.analytics.model.JsonDevice;
import com.f_scratch.bdash.mobile.analytics.model.config.JsonKey;
import com.google.gson.annotations.Expose;

import java.util.HashMap;

/**
 * 「顧客設定取得API」リクエスト用クラス
 */
class WebReceptionSettingsRequest extends JsonDevice {

    @Expose public String[] targets;
    @Expose public String trigger;
    @Expose public String view;
    @Expose public String preView;
    @Expose public String page;
    @Expose public String prePage;
    @Expose public String eventFunc;
    @Expose public HashMap<String,String> customProperty;
    @Expose String accessType;

    public WebReceptionSettingsRequest(){
        JsonKey.initJsonDeviceFields(this);
    }

    public void copy( BDashReport report ){
        this.targets = report.targets;
        this.trigger = report.trigger;
        this.view = report.view;
        this.preView = report.preView;
        this.page = report.page;
        this.prePage = report.prePage;
        this.eventFunc = report.eventFunc;
        this.customProperty = report.customProperty;
        this.accessType = report.accessType;
    }


}
