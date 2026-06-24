package com.smart_bdash.mobile.analytics.notification;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.smart_bdash.mobile.analytics.R;
import com.smart_bdash.mobile.analytics.util.LogUtil;

/**
 * <pre>
 * b->dash が提供する「割り込み通知」の設定画面用の Activity
 *  </pre>
 */

public class BdashNotificationPopupSettings extends AppCompatActivity {

    private final int FIRST_INDEX = 0;
    private final int FAILED_GETTING_INTENT_PARAM = -1;

    /** <pre>
     * Activity 生成時にテーマオプションを設定する際の Intent.putExtra 用のキー
     *  R.style.BDashSDK_Theme: 標準テーマ
     *  R.style.BDashSDK_Theme: グリーンテーマ
     *  R.style.BDashSDK_Theme_gray: グレイテーマ
     * </pre>
     * */
    public static final String EXTRA_THEME = "theme";

    LinearLayout toolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //テーマの設定
        int theme = getIntent().getIntExtra(EXTRA_THEME, FAILED_GETTING_INTENT_PARAM);
        if( theme != FAILED_GETTING_INTENT_PARAM ) {
            try {
                setTheme(theme);
            } catch( Throwable e ) {
                // 何もしない
            }
        }
        setContentView(R.layout.com_smart_bdash_mobile_popup_notification_settings);

        toolBar = (LinearLayout) findViewById(R.id.toolbar);
        TypedArray array = getTheme().obtainStyledAttributes(new int[]{androidx.appcompat.R.attr.colorPrimary});
        if (array != null) {
            LogUtil.s(">>has array");
            toolBar.setBackground(array.getDrawable(FIRST_INDEX));
        } else {
            LogUtil.s(">>null array");
        }

        int statusBarHeight = getResources().getDimensionPixelSize(
                getResources().getIdentifier("status_bar_height", "dimen", "android")
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            LogUtil.s(">> set padding (Android 15〜)");
            toolBar.setPadding(0, statusBarHeight, 0, 35);
        } else {
            LogUtil.s(">> set padding (〜 Android 14)");
            toolBar.setPadding(0, 0, 0, 0);
        }

        toolBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        //割り込み通知音の設定
        ((CheckBox) findViewById(R.id.com_smart_bdash_mobile_check_sound)).setChecked(BDashNotification.getInstance(getApplicationContext()).isEnablePopupSound());
        ((CheckBox) findViewById(R.id.com_smart_bdash_mobile_check_sound)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                boolean isChecked = ((CheckBox)view).isChecked();
                try {
                    BDashNotification.getInstance(getApplicationContext()).setEnablePopupSound(isChecked);
                } catch( Exception e ) {
                }
            }
        });

        //割り込み通知のバイブレーション設定
        ((CheckBox) findViewById(R.id.com_smart_bdash_mobile_check_vib)).setChecked(BDashNotification.getInstance(getApplicationContext()).isEnablePopupVibration());
        ((CheckBox) findViewById(R.id.com_smart_bdash_mobile_check_vib)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                boolean isChecked = ((CheckBox)view).isChecked();
                try {
                    BDashNotification.getInstance(getApplicationContext()).setEnablePopupVibration(isChecked);
                } catch( Exception e ) {
                }
            }
        });

    }
}