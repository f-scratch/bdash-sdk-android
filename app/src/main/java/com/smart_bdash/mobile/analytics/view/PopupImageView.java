package com.smart_bdash.mobile.analytics.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.smart_bdash.mobile.analytics.R;

public class PopupImageView extends AppCompatImageView {

    Paint mMaskedPaint;
    Paint mCopyPaint;
    Drawable mMaskDrawable;

    public PopupImageView(@NonNull Context context) {
        super(context);
        mask();
    }

    public PopupImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mask();
    }

    public PopupImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mask();
    }

    private void mask(){
        mMaskedPaint = new Paint();
        mMaskedPaint.setXfermode(new PorterDuffXfermode(
                PorterDuff.Mode.SRC_ATOP));

        mCopyPaint = new Paint();
        mMaskDrawable = getResources().getDrawable(R.drawable.com_smart_bdash_mobile_notification_draw_image_view_mask);
    }

    Rect mBounds;
    RectF mBoundsF;

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mBounds = new Rect(0, 0, w, h);
        mBoundsF = new RectF(mBounds);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int sc = canvas.saveLayer(mBoundsF, mCopyPaint);

        mMaskDrawable.setBounds(mBounds);
        mMaskDrawable.draw(canvas);

        canvas.saveLayer(mBoundsF, mMaskedPaint);

        super.onDraw(canvas);

        canvas.restoreToCount(sc);
    }
}
