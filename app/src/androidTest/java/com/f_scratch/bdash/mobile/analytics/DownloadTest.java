package com.f_scratch.bdash.mobile.analytics;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.f_scratch.bdash.mobile.analytics.config.TestSettings;
import com.f_scratch.bdash.mobile.analytics.connect.DownloadClient;
import com.f_scratch.bdash.mobile.analytics.helper.util.LogUtil;
import com.f_scratch.bdash.mobile.analytics.helper.util.DebugUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DownloadTest {

    private static Context context;

    static String getMemoryString() {
        return DebugUtil.getMemoryString(context);
    }

    static int getMemoryRatio() {
        return DebugUtil.getMemoryRatio(context);
    }

    /**
     * テストクラスの初期化
     */
    @BeforeClass
    public static void initializeTestClass() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * <pre>
     * シンプルなダウンロードテスト
     *   [テスト観点]
     *    ・jpg画像 をダウンロードし bmp に変換できるか
     * </pre>
     */
    @Test
    public void testCase_richDownload_01() {

        Bitmap bmp = null;

        try {
            bmp = DownloadClient.downloadAndConvertToRichImage("https://grapee.jp/wp-content/uploads/33342_main1.jpg");
        } catch (Exception ignored) {
        }

        assertNotNull("downloadError", bmp);
    }

    /**
     * <pre>
     * 連続ダウンロードテスト & メモリ圧迫率の確認
     *   [テスト観点]
     *     ・様々なサイズの画像形式をダウンロードしメモリが圧迫されずに動作するか
     *       - ファイルサイズが10MB( 1000*1000*10 ) 以内
     *       - 画像サイズが 350x - 1920x などの様々なサイズ
     *
     * </pre>
     */
    @Test
    public void testCase_richDownload_02() {

        String[] images = {
                TestSettings.localBaseUrl + "img/10mb_1000.jpg",

                TestSettings.localBaseUrl + "img/1920x960.gif",
                TestSettings.localBaseUrl + "img/1920x960.png",
                TestSettings.localBaseUrl + "img/1920x960.jpg",

                TestSettings.localBaseUrl + "img/1152x576.gif",
                TestSettings.localBaseUrl + "img/1152x576.png",
                TestSettings.localBaseUrl + "img/1152x576.jpg",

                TestSettings.localBaseUrl + "img/350x350.gif",
                TestSettings.localBaseUrl + "img/350x350.png",
                TestSettings.localBaseUrl + "img/350x350.jpg",

                TestSettings.localBaseUrl + "img/480x480.gif",
                TestSettings.localBaseUrl + "img/480x480.png",
                TestSettings.localBaseUrl + "img/480x480.jpg",
        };

        for (String target : images) {
            {
                Bitmap bmp = null;
                try {
                    bmp = DownloadClient.downloadAndConvertToRichImage(target);
                } catch (Exception ignored) {
                }
                assertNotNull("downloadError: " + target, bmp);

                // ループという連続した処理のため、明示的に戻り値の bmp を開放する
                bmp.recycle();
                bmp = null;
            }
            System.gc();
            Thread.yield();

            String str = getMemoryString();
            int ratio = getMemoryRatio();
            LogUtil.s(str);

            assertFalse("メモリを圧迫しています", ratio >= 30);
        }
    }
}
