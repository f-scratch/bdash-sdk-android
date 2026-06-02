package com.f_scratch.bdash.mobile.analytics.web_reception;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

/**
 * 「顧客設定取得API」レスポンス用クラス
 */
class WebReceptionSettingsResponse {

    @Expose public ArrayList<WebReceptionSettings> receptions;

    public WebReceptionSettingsResponse() {
    }

    public WebReceptionSettingsResponse(ArrayList<WebReceptionSettings> reception) {
        this.receptions = reception;
    }

    public WebReceptionSettings getCurrent(){
        if( !existCurrent() ) return null;
        return receptions.get(0);
    }

    public void next(){
        if( existCurrent() ) {
            receptions.remove(0);
        }
    }

    public void clear(){
        receptions.clear();
    }

    public boolean existCurrent(){
        if( receptions==null || receptions.size()==0 ) return false;
        return true;
    }


}
