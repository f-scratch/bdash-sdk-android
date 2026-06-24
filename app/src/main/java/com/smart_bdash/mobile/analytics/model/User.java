package com.smart_bdash.mobile.analytics.model;

import com.smart_bdash.mobile.analytics.util.StorageUtil;

/***
 * ユーザークラス<br>
 *  ・ユーザー属性を管理します<br>
 */
public class User {

    /** シングルトン用インスタンス **/
    private static User instance;

    /** 共通フィールド */
    private String uniqueId;
    private String adId;

    /** 状態を保存しない. default==false */
    transient private boolean can_use_adId;

    /***
     *
     */
    private User(){
    }

    /***
     * インスタンスを取得<br>
     *  ・この段階で排他制御していないのは、わざと(明示的に)してる<br>
     *  ・複数のスレッドから同時に呼び出されることは無い
     * @return User
     */
    public static User getInstance() {
        if( instance == null ) {
            instance = loadInstance();
            if( instance == null ) {
                instance = new User();
            }
        }
        return instance;
    }



    public boolean hasUniqueId() {
        return uniqueId!=null && uniqueId.length()>0;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public boolean hasAdId() {
        return adId!=null && !adId.isEmpty();
    }

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public boolean isCanUseAdId() {
        return can_use_adId;
    }

    public void setCanUseAdId(boolean canUseAdId) {
        this.can_use_adId = canUseAdId;
    }

    /***
     * 保存する
     */
    public void save() throws Exception{
        StorageUtil.serialize(StorageUtil.FileType.USER, this);
    }

    /***
     *
     */
    private static User loadInstance() {
        User user = null;
        try {
            user = StorageUtil.deserialize(StorageUtil.FileType.USER, User.class);
        } catch( Exception e ){
            // ファイルが壊れている場合は、仕方ないので null インスタンスを返すフローになる
        }
        return user;
    }

}
