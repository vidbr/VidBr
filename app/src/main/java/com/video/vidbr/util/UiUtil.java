package com.video.vidbr.util;

import android.content.Context;
import android.widget.Toast;

public class UiUtil {

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
