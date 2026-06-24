package com.smart_bdash.mobile.analytics.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowInsets;
import android.view.WindowManager;

/**
 * デバイス情報のユーティリティ
 */
public class DeviceUtil {

    private final static float SMALLEST_WIDTH_THRESHOLD = 600f;

    /***
     * アプリケーションインフォを取得する
     * @param c
     * @return
     */
    public static ApplicationInfo getApplicationInfo( Context c ) {
        ApplicationInfo app = null;
        try {
            app = c.getPackageManager().getApplicationInfo(c.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {}

        return app;
    }


    /***
     * バージョン名を取得する
     *
     * @return
     */
    public static String getVersionName( Context context ){
        PackageManager pm = context.getPackageManager();
        String versionName = "";
        try{
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return versionName;
    }

    /***
     * バージョンコードを取得する
     *
     * @param context
     * @return
     */
    public static int getVersionCode(Context context){
        PackageManager pm = context.getPackageManager();
        int versionCode = 0;
        try{
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 端末の画面サイズを取得する
     * @param context
     * @return
     */
    public static Point getRealSize(Context context) {
        WindowManager wm = (WindowManager)context.getSystemService(Activity.WINDOW_SERVICE);
        Point point = new Point(0, 0);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            Rect rect = wm.getCurrentWindowMetrics().getBounds();
            point.x = rect.width();
            point.y = rect.height();
        }
        else{
            // Android 4.2~
            Display display = wm.getDefaultDisplay();
            display.getRealSize(point);
        }
        return point;
    }

    /**
     * クリップボードに文字列をコピーする
     * @param context
     * @param label
     * @param text
     * @return
     */
    public static ClipData copyToClipboard(Context context, String label, String text) {
        // copy to clipboard
        ClipboardManager clipboardManager =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (null == clipboardManager) {
            return null;
        }
        ClipData result = ClipData.newPlainText(label, text);
        clipboardManager.setPrimaryClip(result);
        return result;
    }

    /**
     * 利用端末がスマホかタブレットか判定する
     * 端末の最小幅がsw600dpを超えているかで判定
     * @return
     */
    public static boolean isTablet(Activity context) {
        float swDp = getSmallestWidthDp(context);

        LogUtil.s(">>>  smallest width is " + swDp + "dp");
        return swDp >= SMALLEST_WIDTH_THRESHOLD ;
    }

    /**
     * 端末の最小幅の長さ(単位:dp)を取得
     * @param context Activityのコンテキスト
     * @return
     */
    private static float getSmallestWidthDp(Activity context) {
        float widthDp;
        float heightDp;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float density = metrics.density;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30以上
            WindowInsets windowInsets = wm.getCurrentWindowMetrics().getWindowInsets();
            Rect bounds = wm.getCurrentWindowMetrics().getBounds();

            int widthPx = bounds.width();
            int heightPx = bounds.height();

            // システムバー、カットアウトを除外
            Insets insets = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
            );
            int usableWidthPx = widthPx - insets.left - insets.right;
            int usableHeightPx = heightPx - insets.top - insets.bottom;
            LogUtil.s(">> usableWidthPx: " + usableWidthPx + "px, usableHeightPx: " + usableHeightPx + "px");

            widthDp = usableWidthPx / density;
            heightDp = usableHeightPx / density;
        } else {
            Display display = wm.getDefaultDisplay();
            display.getRealMetrics(metrics);

            int widthPx = metrics.widthPixels;
            int heightPx = metrics.heightPixels;
            LogUtil.s(">> widthPx: " + widthPx + "px, heightPx: " + heightPx + "px");

            widthDp = widthPx / density;
            heightDp = heightPx / density;
        }

        LogUtil.s(">> width: " + widthDp + "dp, height: " + heightDp +"dp");

        return Math.min(widthDp, heightDp);
    }

    /**
     * 画面に表示するpx数を算出
     * APIレスポンスで返却されるpx値はdpi/ptで算出されたものとして扱う
     * @param px
     * @param metrics
     * @return
     */
    public static float getDisplayPxSize(int px ,DisplayMetrics metrics) {
        float density;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14(SDK 34)以降、scaledDensity が非推奨のため
            density = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, metrics);
        } else {
            density = metrics.density;
        }

        return px * density;
    }
}
