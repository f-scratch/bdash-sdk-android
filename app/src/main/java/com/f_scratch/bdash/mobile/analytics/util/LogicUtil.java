package com.f_scratch.bdash.mobile.analytics.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Point;
import android.os.Build;

import com.f_scratch.bdash.mobile.analytics.model.Device;
import com.f_scratch.bdash.mobile.analytics.model.JsonDevice;
import com.f_scratch.bdash.mobile.analytics.model.annotation.JSONIgnore;
import com.f_scratch.bdash.mobile.analytics.model.annotation.JSONName;
import com.f_scratch.bdash.mobile.analytics.model.config.JsonKey;
import com.f_scratch.bdash.mobile.analytics.model.config.SDKConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * SDK のロジック処理を集約
 */
public class LogicUtil {

    private final static String TARGET_OS = "Android";

    /***
     * デバイス情報の初期化
     */
    public static void initDeviceInfo(Device device, Context context, String customId) {
        // アプリバージョン
        device.appVersion = DeviceUtil.getVersionName(context);

        // カスタムID
        device.customId = customId;

        // 言語
        device.lang = LocaleUtil.getLocaleAndCountry(context);

        // OS
        device.os = TARGET_OS;

        // OS バージョン
        device.osVersion = Build.VERSION.RELEASE; //OSバージョン

        // キャリア( ブランド )
        device.carrier = Build.BRAND;

        // モデル
        device.model = Build.MODEL;

        // 画面解像度
        {
            Point point = DeviceUtil.getRealSize(context);
            device.display = String.format("%dx%d", point.x, point.y);
        }

        ApplicationInfo info = DeviceUtil.getApplicationInfo(context);
        if (info != null) {
            // アプリID
            device.appId = info.metaData.getString(SDKConfig.APP_BDASH_APP_ID);
            // アカウントID
            device.accountId = getStringOrInt(info, SDKConfig.APP_BDASH_ACCOUNT_ID);
            // データView
            device.dataViewIds = getStringOrInt(info, SDKConfig.APP_BDASH_DATA_VIEW);
            // 起動アクティビティ
            device.notification_launchActivity = info.metaData.getString(SDKConfig.APP_BDASH_NOTIFICATION_LAUNCH);
            // アイコン
            device.notification_icon = info.metaData.getInt(SDKConfig.APP_BDASH_NOTIFICATION_ICON, 0);
            // ロリポップ(Android5)以降で使用するビッグアイコン
            // 元々はSDKがAndroid4以前も対応していたので、お客さん側のアプリ実装状況を考えて、この命名を使い続ける
            device.notification_lollipop_bigIcon = info.metaData.getInt(SDKConfig.APP_BDASH_NOTIFICATION_LOLLIPOP_BIG_ICON);
            // アイコンアクセントカラー
            if ( info.metaData.containsKey(SDKConfig.APP_BDASH_NOTIFICATION_ICON_ACCENT_COLOR) ) {
                device.notification_icon_accent_color = Integer.toString(info.metaData.getInt(SDKConfig.APP_BDASH_NOTIFICATION_ICON_ACCENT_COLOR));
            }

            // チャンネル関連の初期化
            // resource/value共に値を許容したいため、resourceで取得 -> 取得できなければvalueで取得とする
            //　そのため、resourceでの取得でExceptionが発生してもそのまま動作を続ける
            LogUtil.s(">>>>>>　通知チャンネル設定用の値初期化 >>>>>>");
            try{
                device.channel_id = context.getResources().getString(info.metaData.getInt(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_ID));
                LogUtil.s("resourceからチャンネルIDを取得しました : 結果 => " + device.channel_id);
            }catch (Exception e){
                device.channel_id = info.metaData.getString(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_ID);
                LogUtil.s("valueからチャンネルIDを取得しました : 結果 => " + device.channel_id);
            }

            try {
                device.channel_name = context.getResources().getString(info.metaData.getInt(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_NAME));
                LogUtil.s("resourceからチャンネル名を取得しました : 結果 => " + device.channel_name);
            }catch( Exception e ) {
                device.channel_name = info.metaData.getString(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_NAME);
                LogUtil.s("valueからチャンネル名を取得しました : 結果 => " + device.channel_name);
            }

            try {
                device.channel_desc = context.getResources().getString(info.metaData.getInt(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_DESC));
                LogUtil.s("resourceからチャンネル説明を取得しました : 結果 => " + device.channel_desc);
            }catch( Exception e ) {
                device.channel_desc = info.metaData.getString(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_DESC);
                LogUtil.s("valueからチャンネル説明を取得しました : 結果 => " + device.channel_desc);
            }

            try {
                device.channel_badge = context.getResources().getBoolean(info.metaData.getInt(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_BADGE));
                LogUtil.s("resourceからバッチを表示するかを取得しました : 結果 => " + device.channel_badge);
            }catch (Exception e){
                device.channel_badge = info.metaData.getBoolean(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_BADGE, true);
                LogUtil.s("valueからバッチを表示するかを取得しました : 結果 => " + device.channel_badge);
            }
            
            try{
                device.channel_imp = context.getResources().getString(info.metaData.getInt(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_IMP));
                LogUtil.s("resourceから通知の優先度を取得しました : 結果 => " + device.channel_imp);
            }catch (Exception e){
                device.channel_imp = info.metaData.getString(SDKConfig.APP_BDASH_NOTIFICATION_CHANNEL_IMP);
                LogUtil.s("valueから通知の優先度を取得しました : 結果 => " + device.channel_imp);
            }
            LogUtil.s(">>>>>>　通知チャンネル設定用の値初期化 完了>>>>>>");
        }
    }


    /***
     * meta データから String/int データを String 型として取得する
     * @param info
     * @param key
     * @return
     */
    private static String getStringOrInt( ApplicationInfo info, String key ){
        String result = null;
        try {
            Object obj = info.metaData.get(key);
            if( obj instanceof Integer ) {
                result = Integer.toString((Integer)obj);
            } else if( obj instanceof String ) {
                result = (String)obj;
            }
        } catch( Throwable e ) {
        }
        return result;
    }



    /***
     * @param clazz
     * @param fieldName
     * @return
     */
    public static String getJsonFieldName(Class clazz, String fieldName) {
        Field fieldList[] = clazz.getDeclaredFields();//getFields();
        for (Field field : fieldList) {
            if (field.getName().equals(fieldName)) {
                for (Annotation annot : field.getAnnotations()) {
                    if (annot instanceof JSONName) {
                        JSONName a = (JSONName) annot;
                        return a.value();
                    }
                }
            }
        }
        return fieldName;
    }


    /**
     *
     * @param clazz
     * @param fieldName
     * @return
     */
    public static boolean isJsonTarget( Class clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            return _isJsonTarget(field);
        } catch( Exception e ) {
        }
        return true;
    }

    private static boolean _isJsonTarget( Field field ) {
        try {
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation instanceof JSONIgnore) {
                    return false;
                }
            }
        } catch( Exception e ) {
        }
        return true;
    }

    /***
     * クラスをハッシュマップに変換
     *
     * @param clazz
     * @return
     * @throws Exception
     */
    public static HashMap<String, Object> class2HashMapWithCreateInstance(Class clazz, Object obj) throws Exception {
        HashMap<String, Object> result = new HashMap<String, Object>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    !Modifier.isStatic(field.getModifiers())) {
                if( _isJsonTarget(field) ) {
                    try {
                        result.put(getJsonFieldName(clazz, field.getName()), field.get(obj));
                    } catch (Exception e) {
                    }
                }
            }
        }
        return result;
    }

    public static void class2HashMap(HashMap<String, Object> result, Class clazz, Object obj) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    !Modifier.isStatic(field.getModifiers())) {
                if( _isJsonTarget(field) ) {
                    try {
                        result.put(getJsonFieldName(clazz, field.getName()), field.get(obj));
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
    public static void class2JsonObj(JSONObject result, Object obj) throws Exception {
        Class clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if ( !Modifier.isStatic(field.getModifiers())) {
                if( _isJsonTarget(field) ) {
                    field.setAccessible(true);
                    try {
                        // 1次元配列のみ対応
                        Object value = field.get(obj);
                        if( field.getType().isArray() ){
                            JSONArray array = new JSONArray();
                            int length = Array.getLength(field.get(obj));
                            for( int i=0 ; i<length; i++ ) {
                                array.put( i, Array.get(field.get(obj), i));
                            }
                            result.put(getJsonFieldName(clazz, field.getName()), array);
                        } else if( field.get(obj) instanceof HashMap ) {
                            result.put(getJsonFieldName(clazz, field.getName()), _createJsonRequest((HashMap)value));
                        } else {
                            result.put(getJsonFieldName(clazz, field.getName()), field.get(obj));
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /***
     *
     * @param device
     * @param maps
     * @return
     * @throws Exception
     */
    public static String createJsonRequest(JsonDevice device, ArrayList<HashMap<String, Object>> maps) throws Exception {
        JSONArray resultArray = new JSONArray();
        JSONObject resultObj  = new JSONObject();

        // device 領域
        _createJsonRequest(resultObj, class2HashMapWithCreateInstance(device.getClass(), device));

        // 配列領域
        if( maps!= null ) {
            for (HashMap<String, Object> target : maps) {
                resultArray.put(_createJsonRequest(target));
            }
            resultObj.put(JsonKey.KEY_TRACKINGS, resultArray);
        }

        return resultObj.toString();
    }

    private static JSONObject _createJsonRequest(HashMap<String, Object> target) throws Exception {
        JSONObject result = new JSONObject();
        _createJsonRequest(result, target);
        return result;
    }

    private static void _createJsonRequest( JSONObject result, HashMap<String, Object> target) throws Exception {
        Iterator ite = target.entrySet().iterator();
        for (; ite.hasNext(); ) {
            Map.Entry<String,Object> entry =(Map.Entry)ite.next();

            String key = entry.getKey();
            Object val = entry.getValue();
            try {
                if (val instanceof HashMap) {
                    HashMap<String, String> local_maps = (HashMap<String, String>) val;
                    JSONObject local = new JSONObject();

                    Iterator local_ite = local_maps.entrySet().iterator();
                    for (; local_ite.hasNext(); ) {
                        Map.Entry<String,String> local_entry =(Map.Entry)local_ite.next();
                        String local_key = local_entry.getKey();
                        if( local_key!=null && local_key.length()>0 ) {
                            String local_val = local_entry.getValue();
                            local.put(local_key, local_val);
                        }
                    }
                    result.put(key, local);
/*
                } else if (val instanceof JsonTracker) {
                    JSONObject temp = _createJsonRequest( class2HashMap(val.getClass(), val) );
                    result.put(key, temp);
*/

                } else {
                    result.put(key, val);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     *
     * @param clazz
     * @return
     * @throws Exception
     */
    public static String createJsonRequestWithCommonParameter( Object clazz ) throws Exception{
        JSONObject resultObj  = new JSONObject();
        JsonDevice device = JsonKey.createJsonDeviceFields();

        // device 領域
        class2JsonObj(resultObj, device);
        class2JsonObj(resultObj, clazz);

        return resultObj.toString(4);
    }

    /***
     * JsonToken モデルから JSON リクエストを生成する
     * @param clazz
     * @return
     * @throws Exception
     */
    public static String createJsonRequestByJsonToken( Object clazz ) throws Exception{
        JSONObject resultObj = new JSONObject();
        HashMap<String, Object> result = new HashMap<String, Object>();
        // JsonToken モデル
        class2HashMap(result, clazz.getClass(), clazz);
        try {
            // JsonDevice モデル
            class2HashMap(result, clazz.getClass().getSuperclass(), clazz);
        }catch( Exception e ) {
            // 何もしない
        }
        _createJsonRequest(resultObj, result);
        return resultObj.toString(4);
    }

    /**
     * モデルから JSON リクエストを作成する. 全モデル対応
     * @param clazz
     * @return
     * @throws Exception
     */
    public static String createJsonRequest( Object clazz ) throws Exception{
        JSONObject resultObj = new JSONObject();
        HashMap<String, Object> result = new HashMap<String, Object>();

        // current モデル
        class2HashMap(result, clazz.getClass(), clazz);
        Class superClazz = clazz.getClass().getSuperclass();
        while( superClazz!=null ) {

            class2HashMap(result, superClazz, clazz);
            superClazz = superClazz.getSuperclass();
        }

        _createJsonRequest(resultObj, result);
        return resultObj.toString(4);
    }

    /**
     * モデルから HashMap を作成する
     * @param clazz
     * @return
     * @throws Exception
     */
    public static HashMap<String,Object> createMap( Object clazz ) throws Exception {
        HashMap<String, Object> result = new HashMap<String, Object>();

        class2HashMap(result, clazz.getClass(), clazz);
        Class superClazz = clazz.getClass().getSuperclass();
        while( superClazz!=null ) {

            class2HashMap(result, superClazz, clazz);
            superClazz = superClazz.getSuperclass();
        }
        return result;
    }

    /**
     *
     * @param map
     * @return
     * @throws Exception
     */
    public static String createURLEncodeRequestByMap( HashMap<String, Object> map ) throws Exception{
        StringBuffer sb = new StringBuffer();
        Iterator< Map.Entry<String,Object> > ite = map.entrySet().iterator();
        boolean insert = false;
        for( ; ite.hasNext() ; ) {
            Map.Entry<String,Object> entry = ite.next();
            if( entry.getValue()!=null ) {
                if( insert ) {
                    sb.append("&");
                }
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(URLEncoder.encode((String) entry.getValue(), "UTF-8"));
                insert = true;
            }
        }
        return sb.toString();
    }

    /**
     *
     * @param clazz
     * @return
     * @throws Exception
     */
    public static String createURLEncodeRequestByClass( Object clazz ) throws Exception{
        return createURLEncodeRequestByMap(createMap(clazz));
    }


    /***
     * UUID を生成する
     * @return
     */
    public static String generateUUID(){
        return UUID.randomUUID().toString();
    }

}