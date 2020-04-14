package com.airmap.airmapsdktest;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.airmap.airmapsdk.models.Coordinate;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class Utils extends com.airmap.airmapsdk.util.Utils {

    private static final int[] compassDirections = new int[]{
            com.airmap.airmapsdk.R.string.cardinal_direction_n, com.airmap.airmapsdk.R.string.cardinal_direction_nnne, com.airmap.airmapsdk.R.string.cardinal_direction_ne, com.airmap.airmapsdk.R.string.cardinal_direction_ene,
            com.airmap.airmapsdk.R.string.cardinal_direction_e, com.airmap.airmapsdk.R.string.cardinal_direction_ese, com.airmap.airmapsdk.R.string.cardinal_direction_se, com.airmap.airmapsdk.R.string.cardinal_direction_sse,
            com.airmap.airmapsdk.R.string.cardinal_direction_s, com.airmap.airmapsdk.R.string.cardinal_direction_ssw, com.airmap.airmapsdk.R.string.cardinal_direction_sw, com.airmap.airmapsdk.R.string.cardinal_direction_wsw,
            com.airmap.airmapsdk.R.string.cardinal_direction_w, com.airmap.airmapsdk.R.string.cardinal_direction_wnw, com.airmap.airmapsdk.R.string.cardinal_direction_nw, com.airmap.airmapsdk.R.string.cardinal_direction_nnw};


    public static LatLng getLatLngFromCoordinate(Coordinate coordinate) {
        if (coordinate != null) {
            return new LatLng(coordinate.getLatitude(), coordinate.getLongitude());
        }
        return null;
    }

    public static String directionFromBearing(Context context, double bearing) {
        int index = (int) ((bearing / 22.5) + 0.5) % 16;
        return context.getResources().getString(compassDirections[index]);

    }

    public static String minutesToMinSec(Context context, double minutes) {
        int min = (int) minutes;
        int sec = (int) ((minutes - min) * 60);
        return context.getString(R.string.minutes_seconds, min, sec);
    }

    public static double ktsToKmh(double kts) {
        return kts * 1.852;
    }

    public static double ktsToMph(double kts) {
        return kts * 1.151;
    }

    public static Bitmap getBitmap(Context context, @DrawableRes int drawableResId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableResId);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawableCompat) {
            return getBitmap((VectorDrawableCompat) drawable);
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("Unsupported drawable type");
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap getBitmap(VectorDrawableCompat vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }
}
