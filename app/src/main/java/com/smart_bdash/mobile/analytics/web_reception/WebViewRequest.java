package com.smart_bdash.mobile.analytics.web_reception;

import com.smart_bdash.mobile.analytics.model.Device;
import com.smart_bdash.mobile.analytics.model.User;
import com.google.gson.annotations.Expose;

import java.util.HashMap;

/**
 * {@link BDashReport} から「WebView リクエスト」のプロパティ値を有効化したクラス
 */
class WebViewRequest extends BDashReport {

    @Expose public String uuid;
    @Expose public String appId;

    public WebViewRequest(){
        this.uuid = User.getInstance().getUniqueId();
        this.appId = Device.getInstance().appId;
    }

    public void copy( BDashReport report ) {
        // 対象プロパティは新規に生成しデータをコピーする
        this.trigger = newString(report.trigger);
        this.page    = newString(report.page);
        this.prePage = newString(report.prePage);
        this.view    = newString(report.view);
        this.preView = newString(report.preView);
        this.eventFunc = newString(report.eventFunc);
        this.customProperty= newHashMap(report.customProperty);

        // 対象外のプロパティは null を設定する
        this.accessType = null;
        this.targets  = null;
    }


    private String newString( String str ) {
        if( str!=null ) return new String(str);
        return null;
    }

    private HashMap<String,String> newHashMap( HashMap<String, String> str ) {
        if( str!=null ) return new HashMap<String,String>(str);
        return null;
    }

}
