package com.smart_bdash.mobile.analytics;

import com.smart_bdash.mobile.analytics.model.config.JsonKey;

import java.util.HashMap;

/**
 * 例外ビューを生成するビルダー
 *
 * @author dataX
 */
public class ExceptionBuilder extends AbstractBuilder {

    public ExceptionBuilder() {
        jsonObj.internalType = JsonKey.TYPE_EXCEPTION_VIEW;
    }

    /**
     *
     * @return
     */
    public HashMap<String,Object> build(){
        try {
            return jsonObj.toHashMap();
        } catch( Exception e ){
        }
        return null;
    }

    /**
     * 例外の名称を設定します
     * @param name 例外名称
     */
    public ExceptionBuilder setName( String name ){
        jsonObj.crashName = name;
        return this;
    }


    /***
     * 例外が致命的かどうかを設定します
     * @param fatal 致命的かどうか
     */
    public ExceptionBuilder setFatal( boolean fatal ){
        jsonObj.crashFatal = Boolean.toString(fatal);
        return this;
    }

    /***
     * 例外の詳細情報を設定します
     * @param description 例外情報
     */
    public ExceptionBuilder setDescription( String description ) {
        jsonObj.crashDescription = description;
        return this;
    }

}
