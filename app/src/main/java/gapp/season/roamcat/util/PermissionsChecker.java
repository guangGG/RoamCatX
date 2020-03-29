package gapp.season.roamcat.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import permissions.dispatcher.PermissionUtils;

public final class PermissionsChecker {
    public static boolean hasSelfPermissions(Context context, String... permissions) {
        return PermissionUtils.hasSelfPermissions(context, permissions);
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity, String... permissions) {
        return PermissionUtils.shouldShowRequestPermissionRationale(activity, permissions);
    }

    public static boolean shouldShowRequestPermissionRationale(Fragment fragment, String... permissions) {
        return PermissionUtils.shouldShowRequestPermissionRationale(fragment, permissions);
    }


    public static boolean checkSelfPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }
}
