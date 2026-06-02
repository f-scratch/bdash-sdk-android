package com.f_scratch.bdash.mobile.analytics.notification;

import com.f_scratch.bdash.mobile.analytics.util.LogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;

/**
 * Push通知メッセージクラス
 */
public class NotificationMessage {

    public String title;
    public String body;
    public String custom_payload;
    public String dId;
    public String bdId;
    public String jp_co_fscratch;
    public String fcm_api;  // FCMがレガシーかHTTPv1かを判別する値(レガシー:legacy,HTTPv1:v1)
    public String image;
    public String notification_type_android = "toast";

    // FCM legacy
    public int message_id;
    public String   param;
    public String notification_param;
    public int      id;
    public ArrayList<NotificationButton> buttons = new ArrayList<>();

    public static NotificationMessage decode( Map<String,String> data ) {
        NotificationMessage result = new NotificationMessage();
        Field fieldList[] = NotificationMessage.class.getDeclaredFields();
        for (Field field : fieldList) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    !Modifier.isStatic(field.getModifiers())) {
                try {
                    String name = field.getName();
                    LogUtil.s( "target: " + name );
                    if( !data.containsKey(name) )continue;
                    String value= data.get(name);
                    if( value == null )continue;

                    // 専用機構のため定数を使用
                    final String keyButtons = "buttons";

                    LogUtil.s( String.format(">>set(%s): %s=%s",field.getType(),name,value) );
                    if( field.getType() == Integer.class || field.getType() == int.class){
                        field.set(result, Integer.parseInt(value));
                    } else if ( field.getType() == String.class ) {
                        field.set(result, value);
                    } else if( name.equals(keyButtons) ){
                        field.set(result, decodeButtons(value));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }


    public static NotificationMessage decode( String jsonText ) throws Exception{
        JSONObject json = new JSONObject(jsonText);
        NotificationMessage result = new NotificationMessage();


        Field fieldList[] = NotificationMessage.class.getDeclaredFields();//getFields();
        for (Field field : fieldList) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    !Modifier.isStatic(field.getModifiers())) {
                try {
                    //
                    String name = field.getName();
                    if( json.isNull(name) )continue;
                    String value= json.getString(name);

                    LogUtil.s( String.format(">>set(%s): %s=%s",field.getType(),name,value) );
                    if( field.getType() == Integer.class || field.getType() == int.class){
                        field.set(result, Integer.parseInt(value));
                    } else if ( field.getType() == String.class ) {
                        field.set(result, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public static <T> T decodeEx(String jsonText, Class<? extends T> cls) throws Exception {
        Object result = ((Class)cls).newInstance();
        JSONObject json = new JSONObject(jsonText);

        Field fieldList[] = NotificationMessage.class.getDeclaredFields();//getFields();
        for (Field field : fieldList) {
            if (Modifier.isPublic(field.getModifiers()) &&
                    !Modifier.isStatic(field.getModifiers())) {
                try {
                    //
                    String name = field.getName();
                    if( json.isNull(name) )continue;
                    String value= json.getString(name);

                    LogUtil.s( String.format(">>set(%s): %s=%s",field.getType(),name,value) );
                    if( field.getType() == Integer.class || field.getType() == int.class){
                        field.set(result, Integer.parseInt(value));
                    } else if ( field.getType() == String.class ) {
                        field.set(result, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return (T)result;
    }

    private static ArrayList<NotificationButton> decodeButtons( String stringButtons ) {
        JsonArray jsonArray = new Gson().fromJson(stringButtons, JsonArray.class);
        ArrayList<NotificationButton> buttons = new ArrayList<>();
        Field fieldList[] = NotificationButton.class.getFields();
        for(int i=0; i<jsonArray.size(); i++){
            NotificationButton button = new NotificationButton();
            JsonObject obj = jsonArray.get(i).getAsJsonObject();
            for (Field field : fieldList) {
                try {
                    String value = obj.get(field.getName()).getAsString();
                    if (field.getType() == int.class || field.getType() == Integer.class) {
                        field.set(button, Integer.parseInt(value));
                    } else if (field.getType() == String.class) {
                        field.set(button, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            buttons.add(button);
        }

        return buttons;
    }
}
