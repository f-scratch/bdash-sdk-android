package com.smart_bdash.mobile.analytics;

import com.smart_bdash.mobile.analytics.model.config.JsonKey;

import java.util.HashMap;

/**
 * イベントビューを生成するビルダー
 *
 * @author dataX
 */
public class EventBuilder extends AbstractBuilder{


    public EventBuilder(){
        jsonObj.internalType = JsonKey.TYPE_EVENT_VIEW;
    }

    public HashMap<String,Object> build(){
        try {
            return jsonObj.toHashMap();
        } catch( Exception e ){
        }
        return null;
    }

    /**
     * カテゴリー名を設定します
     * @param categoryName
     * @return
     */
    public EventBuilder setCategoryName( String categoryName ) {
        jsonObj.eventCategory = categoryName;
        return this;
    }

    /**
     * アクション名を設定します
     * @param actionName
     * @return
     */
    public EventBuilder setActionName( String actionName ) {
        jsonObj.eventActionName= actionName;
        return this;
    }

    /**
     * イベントバリューを設定します
     * @param eventValue
     * @return
     */
    public EventBuilder setEventValue( String eventValue ) {
        jsonObj.eventValue = eventValue;
        return this;
    }

    /**
     * ラベルを設定します
     * @param label
     * @return
     */
    public EventBuilder setLabel( String label ) {
        jsonObj.eventLabel = label;
        return this;
    }

    /**
     * イベントマップ(複数の任意の key=value)を設定します
     * @param eventMaps
     * @return
     */
    public EventBuilder setEventMaps( HashMap<String,String> eventMaps ) {
        jsonObj.eventMap = eventMaps;
        return this;
    }


}
