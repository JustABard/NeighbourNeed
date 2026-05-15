package com.example.neighbourneed;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.neighbourneed.data.SessionManager;

class UiPreferences {

    private static final int TAG_ORIGINAL_SIZE = 1001001;
    private static final int TAG_ORIGINAL_STYLE = 1001002;

    static void apply(View root, SessionManager sessionManager) {
        applyToView(root, sessionManager.isBoldTextEnabled());
    }

    private static void applyToView(View view, boolean boldEnabled) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (textView.getTag(TAG_ORIGINAL_SIZE) == null) {
                textView.setTag(TAG_ORIGINAL_SIZE, textView.getTextSize());
                Typeface typeface = textView.getTypeface();
                textView.setTag(TAG_ORIGINAL_STYLE, typeface == null ? Typeface.NORMAL : typeface.getStyle());
            }

            float originalSize = (float) textView.getTag(TAG_ORIGINAL_SIZE);
            int originalStyle = (int) textView.getTag(TAG_ORIGINAL_STYLE);
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
