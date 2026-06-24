package com.smart_bdash.mobile.analytics.web_reception;

import com.smart_bdash.mobile.analytics.model.annotation.JSONIgnore;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Web 接客の「表示条件/成果」となるプロパティ値を管理するクラス
 */
public class BDashReport implements Serializable {

    private static final long serialVersionUID = 2786677166598738405L;

    // トリガー
    /** 起動トリガー */
    public final static String TRIGGER_BOOT   = "boot";
    /** View トリガー */
    public final static String TRIGGER_VIEW   = "view";
    /** Default トリガー */
    public final static String TRIGGER_DEFAULT= "default";
    /** タッチトリガー */
    public final static String TRIGGER_TOUCH  = "touch";
    /** スクロールトリガー */
    public final static String TRIGGER_SCROLL = "scroll";

    final static String ACCESS_TYPE_BOOT   = "boot";
    final static String ACCESS_TYPE_UPDATE = "update";
    final static String ACCESS_TYPE_TRACKING = "tracking";

    /* customProperty 予約語一覧*/
    public final static String CUSTOM_LOGIN_USER = "__loginUserId";



    /** ターゲット */
    @Expose public String[] targets;
    /** トリガー
     * @see BDashReport#TRIGGER_BOOT
     * @see BDashReport#TRIGGER_VIEW
     * @see BDashReport#TRIGGER_DEFAULT
     * @see BDashReport#TRIGGER_TOUCH
     * @see BDashReport#TRIGGER_SCROLL
     */
    @Expose public String trigger;
    /** 現在のスクリーン名 */
    @Expose public String view;
    /** 一つ前のスクリーン名 */
    @Expose public String preView;
    /** 現在のページ名 */
    @Expose public String page;
    /** 一つ前のページ名 */
    @Expose public String prePage;
    /** イベント関数名 */
    @Expose public String eventFunc;
    /** カスタム領域 */
    @Expose public HashMap<String,String> customProperty;
    /** アクセス種別 */
    @Expose String accessType;

    // debug 用
    @JSONIgnore
    String debugConnectUrl;


    /**
     * BDashReport の public データをすべて初期化する
     */
    public void reset(){
        targets = null;
        trigger= null;
        view = null;
        page = null;
        preView = null;
        prePage = null;
        eventFunc = null;
        customProperty = null;
    }

    void setAccessType( String type ) {
        accessType = type;
    }


}
