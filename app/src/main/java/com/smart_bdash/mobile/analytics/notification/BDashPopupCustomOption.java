package com.smart_bdash.mobile.analytics.notification;

/**
 * 割り込み通知の編集用クラス
 */
public class BDashPopupCustomOption {
    private final static String DEFAULT_BUTTON01_TEXT = "閉じる";
    private final static String DEFAULT_BUTTON02_TEXT = "OK";
    public final static int TWO_BUTTON_LAYOUT = 0;
    public final static int ONE_BUTTON_LAYOUT = 1;

    /**
     * ボタン01のラベル
     */
    public String nameButton01;

    /**
     * ボタン02のラベル
     */
    public String nameButton02;

    /**
     * オーバーレイを使用するか
     */
    public Boolean isOverlay;

    /**
     * ボタンレイアウト
     * 0:2ボタン
     * 1:1ボタン
     */
    public int buttonLayoutType;

    public BDashPopupCustomOption(){
        this.nameButton01 = DEFAULT_BUTTON01_TEXT;
        this.nameButton02 = DEFAULT_BUTTON02_TEXT;
        this.isOverlay = true;
        this.buttonLayoutType = ONE_BUTTON_LAYOUT;
    }

    public BDashPopupCustomOption(Boolean isOverlay){
        this.nameButton01 = DEFAULT_BUTTON01_TEXT;
        this.nameButton02 = DEFAULT_BUTTON02_TEXT;
        this.isOverlay = isOverlay;
        this.buttonLayoutType = ONE_BUTTON_LAYOUT;
    }

    public BDashPopupCustomOption(Boolean isOverlay, int buttonLayoutType){
        this.nameButton01 = DEFAULT_BUTTON01_TEXT;
        this.nameButton02 = DEFAULT_BUTTON02_TEXT;
        this.isOverlay = isOverlay;
        this.buttonLayoutType = buttonLayoutType;
    }

    public BDashPopupCustomOption(String nameButton02, String nameButton01, Boolean isOverlay, int buttonLayoutType) {
        this.nameButton01 = nameButton01;
        this.nameButton02 = nameButton02;
        this.isOverlay = isOverlay;
        this.buttonLayoutType = buttonLayoutType;
    }

    /**
     * オプションの設定
     * @param nameButtonSub
     * @param nameButtonMain
     * @param isOverlay
     * @param buttonLayoutType
     */
    public void setOption(String nameButtonSub, String nameButtonMain, Boolean isOverlay, int buttonLayoutType) {
        this.nameButton02 = nameButtonSub;
        this.nameButton01 = nameButtonMain;
        this.isOverlay = isOverlay;
        this.buttonLayoutType = buttonLayoutType;
    }
}
