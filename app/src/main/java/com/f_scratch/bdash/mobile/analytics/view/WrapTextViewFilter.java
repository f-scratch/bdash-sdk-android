package com.f_scratch.bdash.mobile.analytics.view;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 *
 */
public class WrapTextViewFilter implements InputFilter {

    private final String REGEX = "(https?)://[-\\w+&@#/%?=~|!:,.;]*[-\\w+&@#/%=~|]";
    //    private final String REGEX = "(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private final TextView view;

    public WrapTextViewFilter(TextView view) {
        this.view = view;
    }

    //@Override
    public CharSequence filter(CharSequence source, int start, int end,	Spanned dest, int dstart, int dend) {
        TextPaint paint = view.getPaint();
        int w = view.getWidth();
        if( w==0 )return source;
        int wpl = view.getCompoundPaddingLeft();
        int wpr = view.getCompoundPaddingRight();
        int width = w - wpl - wpr;

        SpannableStringBuilder result = new SpannableStringBuilder();
        for (int index = start; index < end; index++) {

            if (Layout.getDesiredWidth(source, start, index + 1, paint) > width) {
                result.append(source.subSequence(start, index));
                result.append("\n");

                start = index;

            } else if (source.charAt(index) == '\n') {
                result.append(source.subSequence(start, index));
                start = index;
            }
        }

        if (start < end) {
            result.append(source.subSequence(start, end));
        }

        setLinkMovement(result);

        return result;
    }

    private void setLinkMovement( SpannableStringBuilder builder ){
        String work = builder.toString();
        int pos = -1;
        ArrayList<Integer> list = new ArrayList<>();
        while( (pos=work.indexOf("\n",pos+1)) != -1 ) {
            list.add(pos);
        }
        work = work.replaceAll("\n", "");

        Matcher urlMatcher = Pattern.compile(REGEX).matcher(work);
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            int _start = urlMatcher.start();
            int _end = urlMatcher.end();

            for( Integer ln_position : list ){
                if( ln_position < _start ) {
                    _start++;
                    _end++;
                }
            }
            for( Integer ln_position : list ){
                if( ln_position>_start && ln_position<_end) {
                    _end++;
                }
            }

            builder.setSpan(new URLSpan(url), _start, _end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * TextViewのテキスト内のリンクを表現するクラス.
     */
    private static class URLSpan extends ClickableSpan {

        private final String url;

        public URLSpan (@NonNull String url) {
            this.url = url;
        }
        public void onClick(View view) {
            // ブラウザを起動
            Uri webPage = Uri.parse(url)  ;
            Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
            view.getContext().startActivity(intent);
        }
    }

}