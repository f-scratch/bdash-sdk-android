package com.smart_bdash.mobile.analytics.model;


import com.smart_bdash.mobile.analytics.util.LogicUtil;
import com.smart_bdash.mobile.analytics.util.TimeUtil;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 現在テスト用 / リファクタリングして本採用する可能性アリ
 */
public class JsonEventModel implements Serializable{

    private static final long serialVersionUID = 5064018217141345993L;

    public String screenName;
    public String crashName;
    public String crashDescription;
    public String crashFatal;
    public String eventCategory;
    public String eventActionName;
    public String eventValue;
    public String eventLabel;
    public String eventDateTime;
    public HashMap<String, String> eventMap;
    public String internalType;

    public JsonEventModel(){
        this.eventDateTime = TimeUtil.getGMT();
    }



    /***
     * 自身を HashMap 型に変換を行う
     * @return
     * @throws Exception
     */
    public HashMap<String,Object> toHashMap() throws Exception {
        return LogicUtil.class2HashMapWithCreateInstance(getClass(), this);
    }



}
