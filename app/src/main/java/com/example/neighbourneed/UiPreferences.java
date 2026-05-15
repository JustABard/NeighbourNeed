package com.example.neighbourneed;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.neighbourneed.data.SessionManager;

class UiPreferences {

    static void apply(View root, SessionManager sessionManager) {
        applyToView(root, sessionManager.isBoldTextEnabled());
    }

    private static void applyToView(View view, boolean boldEnabled) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (textView.getTag(R.id.tag_original_text_size) == null) {
                textView.setTag(R.id.tag_original_text_size, textView.getTextSize());
                Typeface typeface = textView.getTypeface();
                textView.setTag(R.id.tag_original_text_style, typeface == null ? Typeface.NORMAL : typeface.getStyle());
            }

            float originalSize = (float) textView.getTag(R.id.tag_original_text_size);
            int originalStyle = (int) textView.getTag(R.id.tag_original_text_style);
            if (boldEnabled) {
                textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        originalSize + (2 * textView.getResources().getDisplayMetrics().scaledDensity));
            } else {
                textView.setTypeface(Typeface.DEFAULT, originalStyle);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSize);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToView(group.getChildAt(i), boldEnabled);
            }
        }
    }
}
