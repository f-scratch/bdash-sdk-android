package com.smart_bdash.mobile.analytics.notification;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smart_bdash.mobile.analytics.R;
import com.smart_bdash.mobile.analytics.util.LogUtil;

import java.util.ArrayList;

import static com.smart_bdash.mobile.analytics.R.style.BDashSDK_PopupTheme_white;

//  AppCompactActivity は OS 4.x系 でうまく動かない
/**
 * 独自のリッチ通知用の Activity
 *
 * @author fujimaru on 2016/09/26
 */
public class BDashPopupActivity extends Activity {
    /** 強制適応されるテーマ */
    public static final String INTENT_FORCE_THEME = "Theme";
    /** オーバーレイ */
    public static final String INTENT_OVERLAY = "Overlay";
    /** 一つ目のボタンのラベル */
    public static final String INTENT_NAME_BUTTON_SUB = "NameButtonSub";
    /** 二つ目のボタンのラベル */
    public static final String INTENT_NAME_BUTTON_MAIN = "NameButtonMain";
    /** 割り込み通知のボタンレイアウト */
    public static final String INTENT_BUTTON_LAYOUT = "ButtonLayout";

    public static final String META_DATA_POPUP_THEME = "com.smart_bdash.mobile.push.popup.theme"; // manifest の meta-data

    /** Cache ディレクトリに作成するキャッシュファイル名. 他のアプリと衝突しない名前にする*/
    private final static String CACHE_NAME = "com.smart_bdash.mobile.analytics.cache";

    private final static String PREFERENCE_NAME        = "com.smart_bdash.mobile.analytics.notification";
    private final static String POPUP_BITMAP = "popupBitmap";

    private final static int MESSAGE_MAX_LINES_WITH_IMAGE = 15;

    /** スワイプ関連*/
    private static final int SWIPE_MIN_DISTANCE = 50;
    private static final int SWIPE_THRESHOLD_VELOCITY = SWIPE_MIN_DISTANCE;
    private static final int OUT_DISPLAY_RANGE = 2000;

    private Handler handler = new Handler();

    private boolean isDestroy;
    private ViewGroup imageArea;
    private ImageView imageView;
    private ProgressBar progressBar;
    private TextView notificationAppMessage;
    private ArrayList<Bitmap> managedBitmap = new ArrayList<>();

    private Intent intent;

    private static Vibrator vib;

    /** アニメーション処理 */
    private TranslateAnimation transAnimation, centerAnimation;
    private ViewGroup rootView;
    private GestureDetector mGestureDetector;
    private float startX;

    private final GestureDetector.SimpleOnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {

            return true;
        }
        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                                float distanceY) {
            if( transAnimation!=null ) return true;

            View v = rootView;

            float moveX = event2.getX() - event1.getX();
            float moveY = 0;
            float velocityX = moveX;

            if (event1.getX() - event2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // 開始位置から終了位置の移動距離が指定値より大きい
                // X軸の移動速度が指定値より大きい
                LogUtil.s( "left fling: " + (event1.getX() - event2.getX()) );
                int trans = 0;
                if( v.getX()>0 )trans = (int)-v.getX();
                trans -= OUT_DISPLAY_RANGE;
                if( trans < -OUT_DISPLAY_RANGE )trans = -OUT_DISPLAY_RANGE;

                runTransSideAnimation( v, (int)v.getX() - OUT_DISPLAY_RANGE );
                // 左に移動
                return true;

            } else if (event2.getX() - event1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // 終了位置から開始位置の移動距離が指定値より大きい
                // X軸の移動速度が指定値より大きい
                LogUtil.s( "right fling: " + (event2.getX() - event1.getX()) );
                runTransSideAnimation( v, (int)v.getX() + OUT_DISPLAY_RANGE );
                return true;
            } else {
                int posX = (int) (v.getX() + moveX); // Android4.2 系のために計算し終わったのをベースにすること
                int posY = (int) (v.getY() + moveY);
                v.layout( posX, posY, posX+v.getWidth(), posY+v.getHeight() );
            }


            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return super.onDown(e);
        }


     };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_smart_bdash_mobile_popup_android);
        overridePendingTransition(0,0);

        applyMetaTheme();

        // 割り込み通知が出る際に設定に応じて音とバイブを鳴らす
        soundAndVib();

        initialize();
    }

    private void applyMetaTheme(){
        try {
            ApplicationInfo ai = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            int meta =  ai.metaData.getInt(META_DATA_POPUP_THEME); // manifest の meta-data
            {
                meta = getIntent().getIntExtra(INTENT_FORCE_THEME, meta);
                if( meta == 0 ){
                    // 定義されていない時のデフォルトテーマ
                    meta = BDashSDK_PopupTheme_white;
                }

                TypedArray ta = obtainStyledAttributes(meta, R.styleable.BDashButton);

                if( ta != null ){
                    String nameButton01 = (String) getIntent().getExtras().get(INTENT_NAME_BUTTON_SUB);
                    if (nameButton01 != null) {
                        ((Button)findViewById(R.id.notification_01_btn)).setText(nameButton01);
                    }
                    String nameButton02 = (String) getIntent().getExtras().get(INTENT_NAME_BUTTON_MAIN);
                    if (nameButton02 != null) {
                        ((Button)findViewById(R.id.notification_02_btn)).setText(nameButton02);
                    }
                    int buttonLayout = (int) getIntent().getIntExtra(INTENT_BUTTON_LAYOUT, BDashPopupCustomOption.TWO_BUTTON_LAYOUT);
                    Button Button02 = (Button)findViewById(R.id.notification_02_btn);
                    switch (buttonLayout){
                        case BDashPopupCustomOption.TWO_BUTTON_LAYOUT:
                            Button02.setVisibility(View.VISIBLE);
                            break;
                        case BDashPopupCustomOption.ONE_BUTTON_LAYOUT:
                            Button02.setVisibility(View.GONE);
                            break;
                        default:
                            LogUtil.s("割り込み通知のレイアウトに失敗したため、二ボタンで設定します。");
                            Button02.setVisibility(View.VISIBLE);
                            break;
                    }

                    // recycle
                    ta.recycle();
                }

                Boolean isOverlay = (Boolean) getIntent().getExtras().get(INTENT_OVERLAY);
                if (isOverlay) {
                    getWindow().setDimAmount(0.5f);
                }
            }

        } catch( Exception e ){
            e.printStackTrace();;
        }
    }

    /***
     * 初期化処理
     */
    void initialize(){
        mGestureDetector = new GestureDetector(this, mOnGestureListener);
        rootView = (ViewGroup) findViewById(R.id.root);
        rootView.setOnTouchListener(onTouchListener);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                startX = rootView.getX();

                LogUtil.s( "startX: " + startX );
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        imageArea = (ViewGroup) findViewById((R.id.notification_image_area));
        imageView = (ImageView) findViewById(R.id.notification_image_01);
        progressBar = (ProgressBar) findViewById(R.id.notification_prog_01);
        notificationAppMessage = (TextView) findViewById(R.id.notification_app_message);
        isDestroy = false;

        applyMessage();
    }

    public void applyMessage(){
        intent = getIntent();
        String subjectString = (String) intent.getStringExtra(BDashNotification.LAUNCH_TITLE);
        String messageString = (String) intent.getStringExtra(BDashNotification.LAUNCH_MESSAGE);
        final String imageUrl = (String) intent.getStringExtra(BDashNotification.LAUNCH_IMAGE);

        ((TextView)findViewById(R.id.notification_app_message)).setText(messageString);
        ((TextView)findViewById(R.id.notification_app_subject)).setText(subjectString);

        BDashNotification notification = BDashNotification.getInstance(getApplicationContext());
        // ボタンイベント
        (findViewById(R.id.notification_02_btn)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // manifest で設定された Activity を起動する
                try {
                    notification.call_onClickPopup02(intent);
                } catch ( Exception e ) {
                    e.printStackTrace();;
                }
                finish();
            }
        });

        (findViewById(R.id.notification_01_btn)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    LogUtil.s("cast success");
                    LogUtil.s( String.format(" >>%s", BDashPopupActivity.this.getApplicationContext().getClass().getSimpleName() ));
                    notification.call_onClickPopup01(intent);
                } catch( Exception e ){
                    e.printStackTrace();
                }
                finish();
            }
        });

        if(!TextUtils.isEmpty(imageUrl)) {
            imageArea.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            notificationAppMessage.setMaxLines(MESSAGE_MAX_LINES_WITH_IMAGE);

            // image があるのでダウンロード
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if( isDestroy )return;
                        Bitmap work = null;
                        synchronized (managedBitmap) {
                            work = readBitmapFromSharedPreference();
                            if( work != null ) {
                                if( isDestroy ){
                                    work.recycle();
                                    return;
                                }
                                managedBitmap.add(work);
                            }
                        }
                        if( isDestroy )return;

                        final Bitmap readImage = work;
                        BDashPopupActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if( isDestroy )return;

                                if( readImage != null ) {
                                    if (readImage.getHeight() < readImage.getWidth()) {
                                        FrameLayout.LayoutParams layoutParams =
                                                new FrameLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
                                        imageView.setLayoutParams(layoutParams);
                                    }
                                    imageView.setImageBitmap(readImage);
                                } else {
                                    imageArea.setVisibility(View.GONE);
                                }
                                progressBar.setVisibility(View.GONE);
                            }
                        });

                    }catch ( Exception e ) {
                        // read error
                        BDashPopupActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if( isDestroy )return;
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }

                }
            });
            thread.start();

        } else {
            imageArea.setVisibility(View.GONE);
        }
    }

    /**
     * サウンドとバイブを鳴らす
     */
    private void soundAndVib() {
        BDashNotification notification = BDashNotification.getInstance(getApplicationContext());
        // サウンド
        if( notification.isEnablePopupSound() ) {
            runNotificationSound(getApplicationContext());
        }

        // バイブ
        if( notification.isEnablePopupVibration() ) {
            runVibration(getApplicationContext());
        }
    }

    /***
     * 端末の通知音を鳴らす
     * @param context
     */
    private static void runNotificationSound( Context context ){
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone mRingtone = RingtoneManager.getRingtone(context, uri);
            mRingtone.play();
        } catch ( Exception e ){
            // この例外は「ログを出力」が正しい
            LogUtil.s(e);
        }
    }

    /***
     * バイブレーションを行う
     * @param context
     */
    @SuppressLint("MissingPermission")
    private static void runVibration( Context context ) {
        try {
            if (vib == null) {
                vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            vib.vibrate(getVibrationPattern(), -1);
        } catch( Exception e ){
            // クライアントがパーミッションを与えていない場合はここに来る
            // この例外は「何もしない」が正しい
        }
    }

    /**
     * バイブレーションのパターンを取得する
     * @return
     */
    private static long[] getVibrationPattern(){
        return new long[]{100, 300, 500, 200};
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogUtil.s( ">>onNewIntent: " + isFinishing());
        setIntent(intent);

        applyMessage();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isDestroy = true;
        imageView.setImageDrawable(null);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 解放スレッド
        // onDestroy() のタイミングで解放すると描画に使用されて FatalError でアプリが落ちることが有るので
        // タイミングをずらして解放する
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            synchronized (managedBitmap) {
                if (managedBitmap.size() > 0) {
                    for (Bitmap bmp : managedBitmap) {
                        if (!bmp.isRecycled()) {
                            bmp.recycle();
                        }
                    }
                    managedBitmap.clear();
                    LogUtil.s( "bitmap recycle");
                }
            }
            }
        }, 3000);
    }

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            if( event.getAction() == MotionEvent.ACTION_UP ) {
                if( transAnimation == null && centerAnimation == null ) {
                    runTransCenterAnimation(rootView, rootView.getX());
                    LogUtil.s( ">>action up" );
                }
            }
            return true;
        }
    };

    private void runTransSideAnimation(final View view, float x ) {
        transAnimation = createTranslateAnimation( view, x, 500, new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setAnimation(null);
                view.setVisibility(View.INVISIBLE);
                finish();
                LogUtil.s( "end");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(transAnimation);
    }

    private void runTransCenterAnimation(final View view, float x ) {
        centerAnimation = createTranslateAnimation( view, startX - x, 200, new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                centerAnimation= null;
                view.setAnimation(null);
                View v = view;
                v.layout((int) (startX), (int) (v.getY()),
                        (int) (v.getWidth() + startX), (int) (v.getY()
                                + v.getHeight()));
                LogUtil.s( ">>center end");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(centerAnimation);
    }

    private static TranslateAnimation createTranslateAnimation(View view, float x, int duration, Animation.AnimationListener listener ){
        TranslateAnimation anim = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0,
                TranslateAnimation.ABSOLUTE, x,
                TranslateAnimation.RELATIVE_TO_SELF, 0,
                TranslateAnimation.RELATIVE_TO_SELF, 0
        );
        anim.setAnimationListener(listener);
        anim.setDuration(duration);
        return anim;
    }

    private SharedPreferences getPreferences() {
        return getApplicationContext().getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
    }

    private Bitmap readBitmapFromSharedPreference() {
        final SharedPreferences prefs = getPreferences();
        String bitmapString = prefs.getString(POPUP_BITMAP, "");
        if (!bitmapString.equals("")) {
            byte[] b = Base64.decode(bitmapString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length).copy(Bitmap.Config.ARGB_8888, true);
            prefs.edit().remove(POPUP_BITMAP).commit();
            return bitmap;
        }
        return null;
    }
}
