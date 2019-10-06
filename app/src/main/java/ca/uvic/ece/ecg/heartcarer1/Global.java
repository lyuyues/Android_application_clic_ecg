package ca.uvic.ece.ecg.heartcarer1;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;

/**
 * This class stores global variables and methods which are used by all the
 * other classes.
 *
 * @author yizhou
 */
public final class Global {
    public static String gqrsTempPath;

    static final String WebServiceUrl = "http://ecg.uvic.ca:8080/v1/test/";
    static final int connectionTimeout = 3000;
    static final int socketTimeout = 3000;

    static final String WidgetAction = "com.android.mywidgetaction";

    static final int backInterval = 2000;
    static final int color_Red = 0xFFFF0000;
    static final int color_Black = 0xFF000000;
    static final int color_Pink = 0xFFFACDCD;
    static final int color_Grey = 0xFFDCDCDC;
    static final int xAxis_Max = 300 * 3;
    static final int xAxis_Total_Max = xAxis_Max * 20;
    static final double yAxis_Min_Channel1 = 4d;
    static final double yAxis_Max_Channel1 = 7.5d;
    static final double yAxis_Min_Channel2 = 4d;
    static final double yAxis_Max_Channel2 = 7.5d;
    static String token = "";
    static int frequency = 5;
    static boolean containData = false;
    static boolean ifTurnOffBt;
    static int savingLength;
    static int lowBpm;
    static int highBpm;
    static String cachePath;
    static String savedPath;
    static String downloadPath;
    static String quickcheckpath;
    static String folder;
    static boolean ifHrmFragmentAlive;
    static int Channel_selection;
    static boolean ifSaving = false;
    static int max_memory;

    static boolean isWifiConnected(Context mContext) {
        Object service = mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!(service instanceof ConnectivityManager))
            return false;

        NetworkInfo networkInfo = ((ConnectivityManager) service).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return null != networkInfo && networkInfo.isConnected();
    }

    static void toastMakeText(Context mContext, String string) {
        Toast.makeText(mContext, string, Toast.LENGTH_SHORT).show();
    }

    static Intent defaultIntent(Context mContext) {
        return new Intent(mContext, MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    }

    static void exitDialog(final Activity mActivity) {
        new Builder(mActivity).setIcon(R.drawable.exit_64)
                .setTitle(mActivity.getResources().getString(R.string.global_exit))
                .setMessage(mActivity.getResources().getString(R.string.global_wtexit) + mActivity.getResources().getString(R.string.app_name) + "?")
                .setPositiveButton(mActivity.getResources().getString(R.string.yes), (dialog, which) -> {
                    dialog.dismiss();
                    mActivity.finish();
                })
                .setNegativeButton(mActivity.getResources().getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    static void infoDialog(final Activity mActivity, final String title, final String message) {
        new Builder(mActivity).setIcon(R.drawable.bluetooth_64)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(mActivity.getResources().getString(R.string.back), (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    static boolean ifLandscape(Context mContext) {
        return (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void initiate() {
        ifTurnOffBt = true;
        savingLength = frequency * 1000;
        lowBpm = 40;
        highBpm = 100;
        max_memory = 500;

        final String RootPath = Environment.getExternalStorageDirectory().getPath() + "/Heart Carer Data";
        cachePath = RootPath + "/Cache";
        savedPath = RootPath + "/Saved for upload";
        downloadPath = RootPath + "/Download";
        gqrsTempPath = RootPath + "/Temp";
        quickcheckpath = RootPath + "/Quick check";
        folder = RootPath;

        new File(RootPath).mkdir();
        new File(cachePath).mkdir();
        new File(savedPath).mkdir();
        new File(downloadPath).mkdir();
        new File(gqrsTempPath).mkdir();
        new File(quickcheckpath).mkdir();
    }

    static void updateFrequency(int frequency) {
        Global.frequency = frequency;
        Global.savingLength = frequency * 1000;
    }
}