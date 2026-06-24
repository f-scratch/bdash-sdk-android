package com.smart_bdash.mobile.analytics.web_reception;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Web接客内部用のユーティリティクラス
 */
class BDashWebReceptionUtil {

    /**
     * Gsonのインスタンスを取得する
     * @return
     */
    public static Gson getDefaultGson(){
        return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    }

    /**
     * URLエンコードを実施する
     * @param str
     * @return
     * @throws Exception
     */
    public static String URLEncode( String str ){
        try {
            return URLEncoder.encode(str, "UTF-8");
        }catch( Exception e ) {
            return str;
        }
    }

    /**
     * URLデコードを実施する
     * @param str
     * @return
     */
    public static String URLDecode( String str ){
        try {
            return URLDecoder.decode(str, "UTF-8");
        }catch( Exception e ) {
            return str;
        }
    }


    /**
     * SSL エラー発生時のダイアログを作成する
     * @param context
     * @param listener
     * @return AlertDialog
     */
    public static AlertDialog createSslErrorDialog(Context context, final DialogInterface.OnClickListener listener ){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Webサイトのセキュリティ証明書に問題があります。");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if( listener!=null ) {
                    try {
                        listener.onClick(dialog, which);
                    }catch( Exception e) {
                    }
                }
            }
        });
        return builder.create();
    }
}
