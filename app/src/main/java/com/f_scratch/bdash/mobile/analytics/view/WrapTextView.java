package com.f_scratch.bdash.mobile.analytics.view;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.InputFilter;
import android.util.AttributeSet;

public class WrapTextView  extends AppCompatTextView{

    private CharSequence mOrgText = "";
    private BufferType mOrgBufferType = BufferType.NORMAL;

    public WrapTextView(Context context) {
        super(context);
        init();
    }

    public WrapTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WrapTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init (){
        setFilters(new InputFilter[] { new WrapTextViewFilter(this) });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        if (isInEditMode()) {
            // 編集モードだったら処理終了
            return ;
        }
        setText(mOrgText, mOrgBufferType);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        mOrgText = text;
        mOrgBufferType = type;
        super.setText(text, type);
    }

    @Override
    public CharSequence getText() {
        return mOrgText;
    }

    @Override
    public int length() {
        return mOrgText.length();
    }


}




