package com.airmap.airmapsdk.ui.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.airmap.airmapsdk.R;

public class ToggleButton extends AppCompatButton implements View.OnClickListener {

    private OnClickListener clickListener;

    @ColorInt
    private int selectedColor;
    @ColorInt
    private int unselectedColor;

    public ToggleButton(Context context) {
        super(context);

        init(context, null);
    }

    public ToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public ToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }


    public void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.ToggleButton,
                    0, 0);

            try {
                selectedColor = a.getColor(R.styleable.ToggleButton_selectedColor, ContextCompat.getColor(context, R.color.colorAccent));
                unselectedColor = a.getColor(R.styleable.ToggleButton_unselectedColor, ContextCompat.getColor(context, R.color.colorPrimaryDark));
            } finally {
                a.recycle();
            }
        } else {
            selectedColor = ContextCompat.getColor(context, R.color.colorAccent);
            unselectedColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        }
    }

    @Override
    public void onClick(View v) {
        // toggle this button
        setSelected(true);

        // toggle its siblings to off
        if (getParent() != null && getParent() instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) getParent()).getChildCount(); i++) {
                View child = (((ViewGroup) getParent()).getChildAt(i));
                if (!v.equals(child)) {
                    child.setSelected(false);
                }
            }
        }

        if (clickListener != null) {
            clickListener.onClick(v);
        }
    }

    @Override
    public void setOnClickListener(OnClickListener clickListener) {
        this.clickListener = clickListener;
        super.setOnClickListener(this);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);

        ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(selected ? selectedColor : unselectedColor));
    }
}
