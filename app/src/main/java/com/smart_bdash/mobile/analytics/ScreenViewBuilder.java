package com.smart_bdash.mobile.analytics;

import com.smart_bdash.mobile.analytics.model.config.JsonKey;

import java.util.HashMap;

/**
 * 画面ビューを生成するビルダー
 *
 * @author FromScratch
 */
public class ScreenViewBuilder extends AbstractBuilder {

    public ScreenViewBuilder(){
        jsonObj.internalType = JsonKey.TYPE_SCREEN_VIEW;
    }

    public HashMap<String,Object> build(){
        try {
            return jsonObj.toHashMap();
        } catch( Exception e ){
        }
        return null;
    }

}
