package com.f_scratch.bdash.mobile.analytics.connect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.f_scratch.bdash.mobile.analytics.util.LogUtil;

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
     * 「標準リッチ通知」用の画像をダウンロードする
     *
     * @param url
     * @return bitmap image
     */
    public static Bitmap downloadAndConvertToRichImage(String url) throws Exception {
        InputStream in = null;
        Bitmap bmp = null;
        try {
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
