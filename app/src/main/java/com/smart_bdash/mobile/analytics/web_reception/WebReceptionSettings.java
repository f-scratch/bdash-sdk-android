package com.smart_bdash.mobile.analytics.web_reception;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.Gravity;

import com.smart_bdash.mobile.analytics.connect.ConnectType;
import com.google.gson.annotations.Expose;

/**
 * Web接客 「ポップアップ」設定情報クラス
 */
class WebReceptionSettings implements Parcelable{

    @Expose public String target;
    @Expose public String scrollConditions;
    @Expose public String horizontalAlign;
    @Expose public String verticalAlign;
    @Expose public String horizontalMargin;
    @Expose public String verticalMargin;
    @Expose public String width;
    @Expose public String height;
    @Expose public String url;
    @Expose public String allowClick;
    @Expose public String useFilter;
    @Expose public String effect;
    @Expose public String effectDuration;
    @Expose public String closeButtonVerticalAlign;
    @Expose public String closeButtonHorizontalAlign;
    @Expose public String closeButtonHeight;
    @Expose public String closeButtonWidth;
    @Expose public String closeButtonSrc;
    @Expose public String widthPx;
    @Expose public String heightPx;

    // Debug用
    @Expose String forceShow;

    // API 仕様書定義 / align を示す定数
    final private static String CENTER = "CENTER";
    final private static String LEFT   = "LEFT";
    final private static String RIGHT  = "RIGHT";
    final private static String TOP    = "TOP";
    final private static String BOTTOM = "BOTTOM";

    final private static Float DEFAULT_EFFECT_DURATION = 2.0f;

    public final static int ANIM_NONE    = 0;    // アニメーションなし
    public final static int ANIM_FADE_IN = 1;    // フェードイン
    private static class Effect{
        private String name;
        private int value;

        Effect( String name, int value ){
            this.name = name;
            this.value= value;
        }
    }
    // API 仕様書定義 / effect を示す定数
    Effect[] const_Effect = new Effect[]{
        new Effect("fadein", ANIM_FADE_IN)
    };

    public WebReceptionSettings() {
    }

    /**
     * 通常は 0 を返す
     * @return
     */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(target);
        dest.writeString(scrollConditions);
        dest.writeString(horizontalAlign);
        dest.writeString(verticalAlign);
        dest.writeString(horizontalMargin);
        dest.writeString(verticalMargin);
        dest.writeString(width);
        dest.writeString(height);
        dest.writeString(url);
        dest.writeString(allowClick);
        dest.writeString(useFilter);
        dest.writeString(effect);
        dest.writeString(closeButtonVerticalAlign);
        dest.writeString(closeButtonHorizontalAlign);
        dest.writeString(closeButtonHeight);
        dest.writeString(closeButtonWidth);
        dest.writeString(closeButtonSrc);
        dest.writeString(widthPx);
        dest.writeString(heightPx);
    }

    private WebReceptionSettings(Parcel in) {

        // 【注意】
        // 呼び出す順番は書き込み順番と合わせる必要あるので、以下、手動で変更はしないこと

        target = in.readString();
        scrollConditions = in.readString();
        horizontalAlign = in.readString();
        verticalAlign = in.readString();
        horizontalMargin = in.readString();
        verticalMargin = in.readString();
        width = in.readString();
        height = in.readString();
        url = in.readString();
        allowClick = in.readString();
        useFilter = in.readString();
        effect = in.readString();
        closeButtonVerticalAlign = in.readString();
        closeButtonHorizontalAlign = in.readString();
        closeButtonHeight = in.readString();
        closeButtonWidth = in.readString();
        closeButtonSrc = in.readString();
        widthPx = in.readString();
        heightPx = in.readString();
    }

    public static final Creator<WebReceptionSettings> CREATOR
            = new Creator<WebReceptionSettings>() {
        public WebReceptionSettings createFromParcel(Parcel in) {
            return new WebReceptionSettings(in);
        }

        public WebReceptionSettings[] newArray(int size) {
            return new WebReceptionSettings[size];
        }
    };

    /**
     * WebView で表示するURL を返す
     * @return String
     */
    public String getUrl(){
        if( url==null ) {
            return null;
        }
        // memo: デモアプリモードでは http から始まる場合はそのまま返す
        if( BDashReceptionConfig.isDemoAppMode ) {
            if (url.startsWith("http")) {
                return url;
            }
        }
        return ConnectType.getWebViewBaseUrl() + url;
    }

    /**
     * 縦の align を取得
     * @return
     */
    public int getVerticalAlign(){
        if( verticalAlign!=null ) {
            if (verticalAlign.equalsIgnoreCase(TOP)) {
                return Gravity.TOP;
            }
            if (verticalAlign.equalsIgnoreCase(BOTTOM)) {
                return Gravity.BOTTOM;
            }
        }
        return Gravity.CENTER_VERTICAL;
    }

    /**
     * 横の align を取得
     * @return
     */
    public int getHorizontalAlign(){
        if( horizontalAlign!=null ) {
            if (horizontalAlign.equalsIgnoreCase(LEFT)) {
                return Gravity.LEFT;
            }
            if (horizontalAlign.equalsIgnoreCase(RIGHT)) {
                return Gravity.RIGHT;
            }
        }
        return Gravity.CENTER_HORIZONTAL;
    }

    /**
     *
     * @return
     */
    private boolean hasLeftRightAlign(){
        if( horizontalAlign!=null ) {
            if (horizontalAlign.equalsIgnoreCase(LEFT) || horizontalAlign.equalsIgnoreCase(RIGHT)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return
     */
    private boolean hasTopBottomAlign(){
        if( verticalAlign!=null ) {
            if (verticalAlign.equalsIgnoreCase(TOP) || verticalAlign.equalsIgnoreCase(BOTTOM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 幅を取得
     */
    public int getWidth() {
        if( width==null ) {
            return validationWidth(100);
        }
        try {
            int pos = width.indexOf(",");
            if( pos==-1 ){
                return validationWidth(Integer.parseInt(width));
            }
            return validationWidth(Integer.parseInt(width.substring(0, pos)));
        }catch( Exception e ) {
            return validationWidth(100);
        }
    }

    /**
     * @return 高さの係数を取得
     */
    public float getHeight() {
        if( height==null ) {
            return validationHeight(1.0f);
        }
        try {
            return validationHeight(Float.parseFloat(height));
        }catch( Exception e ) {
            return validationHeight(1.0f);
        }
    }

    /**
     * 横方向のマージンを取得
     * @return margin
     */
    public int getHorizontalMargin() {
        if( hasLeftRightAlign() && horizontalMargin!=null ){
            try {
                return Integer.parseInt(horizontalMargin);
            }catch( Exception e ) {
                // memo: 何もしない. 戻り値は 0 とする
            }
        }
        return 0;
    }

    /**
     * 縦方向のマージンを取得
     * @return margin
     */
    public int getVerticalMargin() {
        if( hasTopBottomAlign() && verticalMargin!=null ){
            try {
                return Integer.parseInt(verticalMargin);
            }catch( Exception e ) {
                // memo: 何もしない. 戻り値は 0 とする
            }
        }
        return 0;
    }

    /**
     * エフェクト種別を取得
     * @return effectType
     */
    public int getEffect(){
        if( effect == null )return ANIM_NONE;

        for( Effect target: const_Effect ){
            if( target.name.equalsIgnoreCase(effect) ) {
                return target.value;
            }
        }
        return ANIM_NONE;
    }


    /**
     * エフェクトの演出時間の取得
     * @return
     */
    public int getEffectDuration(){
        if( effectDuration == null ){
            return validationEffectDuration(DEFAULT_EFFECT_DURATION);
        }
        try{
            return validationEffectDuration(Float.parseFloat(effectDuration));
        } catch ( Exception e ) {
            return validationEffectDuration(DEFAULT_EFFECT_DURATION);
        }
    }


    /**
     * フィルター(黒い View がオーバーレイ)が有効か？
     * @return 有効: true  無効: false(default値)
     */
    public boolean hasFilter(){
        if( useFilter == null )return false;
        return useFilter.equalsIgnoreCase("true");
    }


    /**
     * クリックが有効か？
     * @return 有効: true(default値)  無効: false
     */
    public boolean hasAllowClick() {
        if( allowClick == null )return true;
        if( allowClick.equalsIgnoreCase("false") ){
            return false;
        }
        return true;
    }

    /**
     * 閉じるボタンの表示位置(縦)の取得
     * コンテンツの縦幅を100%とした上辺からの距離の割合
     *  - 正の数：コンテンツの上辺に対して下方向へ配置
     *  - 負の数：コンテンツの上辺に対して上方向へ配置
     *  0の場合、閉じるボタンの上辺がコンテンツの上辺と接する
     *  100の場合、閉じるボタンの上辺がコンテンツの下辺と接する
     * @return 上辺からの距離(割合)
     */
    public int getCloseButtonVerticalAlign() {
        if( closeButtonVerticalAlign != null ) {
            try {
                return Integer.parseInt(closeButtonVerticalAlign);
            } catch( Exception e ) {
                // memo: 何もしない. 戻り値は 0 とする
            }
        }
        return 0;
    }

    /**
     * 閉じるボタンの表示位置(横)の取得
     * コンテンツの横幅を100%とした左辺からの距離の割合
     *  - 正の数：コンテンツの左辺に対して右方向へ配置
     *  - 負の数：コンテンツの左辺に対して左方向へ配置
     *  0の場合、閉じるボタンの左辺がコンテンツの左辺と接する
     *  100の場合、閉じるボタンの左辺がコンテンツの右辺と接する
     * @return 左辺からの距離(割合)
     */
    public int getCloseButtonHorizontalAlign() {
        if( closeButtonHorizontalAlign != null ) {
            try {
                return Integer.parseInt(closeButtonHorizontalAlign);
            } catch( Exception e ) {
                // memo: 何もしない. 戻り値は 0 とする
            }
        }
        return 0;
    }

    /**
     * 閉じるボタンの縦幅(係数)の取得
     * 閉じるボタンの横幅に対する縦の長さの割合を係数として取得(default値： 1.0)
     * @return ボタンの縦幅(係数)
     */
    public float getCloseButtonHeight() {
        if (closeButtonHeight == null) {
            return 1.0f;
        }

        try {
            return Float.parseFloat(closeButtonHeight);
        } catch (Exception e) {
            // 値が取得不可もしくはエラー発生の場合、1.0(等倍)を返却する
            return 1.0f;
        }
    }

    /**
     * 閉じるボタンの横幅(割合)の取得
     * コンテンツの横幅を100%とする閉じるボタンの横幅の割合
     * @return 閉じるボタンの横幅(割合)
     */
    public int getCloseButtonWidth() {
        if( closeButtonWidth != null ) {
            try {
                return Integer.parseInt(closeButtonWidth);
            } catch ( Exception e ) {
                // memo: 何もしない. 戻り値は 0 とする
            }
        }
        return 0;
    }

    /**
     * 閉じるボタンの画像URLを取得
     * @return 閉じるボタンの画像URL
     */
    public String getCloseButtonSrc() {
        if (closeButtonSrc == null) {
            return "";
        }
        return closeButtonSrc;
    }

    /**
     * 閉じるボタンが表示可能かどうかを判定
     * @return 表示可能: true(default値)  表示不可: false
     */
    public boolean validateCloseButton() {
        if( closeButtonHeight == null || closeButtonWidth == null ) return false;

        float height = Float.parseFloat(closeButtonHeight);
        int width = Integer.parseInt(closeButtonWidth);

        return height > 0.0f && width > 0;
    }

    /**
     * 横幅の validation を行う
     * @param width
     * @return
     */
    private static int validationWidth( int width ) {
        if( width<10 )width = 10;
        if( width>100 )width = 100;
        return width;
    }

    /**
     * 縦幅 ratio の validation を行う
     */
    private static float validationHeight( float height ) {
        if( height<0.1 )height = 0.1f;
        if( height>3.0 )height = 3.0f;
        return height;
    }

    /**
     * effectDuration の validation を行う
     */
    private static int validationEffectDuration( float duration ) {
        if( duration<0.1 )duration = DEFAULT_EFFECT_DURATION;
        if( duration>3.0 )duration = DEFAULT_EFFECT_DURATION;
        return (int)(duration*1000);
    }

    /**
     * 横幅(単位 px)を取得
     * @return 横幅(単位 px)
     */
    public int getWidthPx() {
        if( widthPx == null || widthPx.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(widthPx);
    }

    /**
     * 高さ(単位px)を取得
     * @return 高さ(単位 px)
     */
    public int getHeightPx() {
        if( heightPx == null || heightPx.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(heightPx);
    }
}
