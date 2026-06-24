package com.smart_bdash.mobile.analytics.connect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import com.smart_bdash.mobile.analytics.util.LogUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadClient {


    private static int MAX_DOWNLOAD_SIZE = 1000 * 1000 * 10; // 10MB

    private static int READ_TIMEOUT = 1000 * 30;       // ReadTimeout
    private static int CONNECT_TIMEOUT = 1000 * 30;    // ConnectTimeout


    static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // 画像の元サイズ
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = 1 + Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = 1 + Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    /**
     * 指定の URL が https スキームであるかを判定する.<br>
     *  ・http: / file: / content: / intent: などのスキームを弾くために使用する<br>
     *  ・null・空文字・パース不能な場合は false を返す
     * @param url 判定対象の URL
     * @return https の場合のみ true
     */
    static boolean isAllowedHttpsUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        try {
            String scheme = Uri.parse(url).getScheme();
            if (scheme == null) {
                return false;
            }
            return "https".equals(scheme.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 「標準リッチ通知」用の画像をダウンロードする.<br>
     *  ・https スキームの URL のみ許可し、それ以外は null を返す（BDA-004）
     *
     * @param url ダウンロード対象の画像 URL（https のみ）
     * @return bitmap image。url が https でない場合や空・不正な場合は null
     */
    public static Bitmap downloadAndConvertToRichImage(String url) throws Exception {
        InputStream in = null;
        Bitmap bmp = null;
        try {
            // BDA-004: https 以外のスキーム（http/file/content/intent 等）は許可しない
            if (!isAllowedHttpsUrl(url)) {
                return null;
            }
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setReadTimeout(READ_TIMEOUT);
            con.setConnectTimeout(CONNECT_TIMEOUT);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            con.connect();
            in = con.getInputStream();

            int length = con.getContentLength();
            if (length > MAX_DOWNLOAD_SIZE) {
                throw new Exception("file size over.");
            }

            byte[] work = new byte[1024 * 10];
            int rd;
            while ((rd = in.read(work)) >= 0) {
                if (rd == 0) continue;
                out.write(work, 0, rd);

                if (out.size() > MAX_DOWNLOAD_SIZE) {
                    throw new Exception("file size over.");
                }
            }
            byte[] images = out.toByteArray();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(images, 0, images.length, options);

            // 1350px を超えるのは memory 保護のためスケーリングしてから読み込む
            //  450 * xxhdpi(3) = 1350px
            //
            //  通知領域の画像サイズそのものは 450x192
            options.inSampleSize = calculateInSampleSize(options, 1350, 1350);

            options.inJustDecodeBounds = false;
            bmp = BitmapFactory.decodeByteArray(images, 0, images.length, options);

        } catch (Exception e) {
            throw e;
        } finally {
            if (in != null) in.close();
        }
        return bmp;
    }
}
