package ca.uvic.ece.ecg.heartcarer1;

import java.io.File;
import java.util.regex.Pattern;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Toast;

import ca.uvic.ece.ecg.ECG.Filter_Queue;

/**
 * This class stores global variables and methods which are used by all the
 * other classes.
 * @author yizhou
 *
 */
@SuppressLint("SdCardPath")
public final class Global {
    // Laptop address
    // public static final String WebServiceUrl =
    // "http://134.87.151.50:8080/RestWebService/rest/MOBILE";
    // Lab server address: Nginx
    public static final String WebServiceUrl = "http://ecg.ece.uvic.ca/rest/MOBILE";
    // "http://ecg.ece.uvic.ca/rest";*/
    // Lab server address: Django

    /*
     * public static final String WebServiceUrl =
     * "http://142.104.77.209:3380/ecgcloud";
     */
    public static final int connectionTimeout = 3000;// waiting to connect
    public static final int socketTimeout = 3000;// waiting to data
    // public static final String WebResultDefault = "Response is NULL";
    // public static final String NoInternet = "";
    // public static final String AvailText = "";
    public static boolean quick_testing = false;
    public static final String DlFilenameSuffix = "_dl";
    public static final String WidgetAction = "com.android.mywidgetaction";
    public static final int backInterval = 2000;// 2 seconds
    public static final int color_Red = 0xFFFF0000;
    public static final int color_Black = 0xFF000000;
    public static final int color_White = 0xFFFFFFFF;
    public static final int color_Pink = 0xFFFACDCD;
    public static final int color_Blue = 0xFF4169E1;
    public static final int color_Grey = 0xFFDCDCDC;
    // Totally show 30 seconds signal
    public static final int xAxis_Max = 300 * 3;
    public static final int xAxis_Num = 20;
    public static final int xAxis_Total_Max = xAxis_Max * xAxis_Num;
    public static final double yAxis_Min_Channel1 = 4d;
    public static final double yAxis_Max_Channel1 = 7.5d;
    public static final double yAxis_Min_Channel2 = 4d;
    public static final double yAxis_Max_Channel2 = 7.5d;
    public static final String RootPath = "/sdcard/Heart Carer Data";
    public static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public static final String shareUsername = "username";
    public static String username;
    public static final String sharePassword = "password";
    public static final int access_request = 0; // number of access request
    public static final int fft_num = 8192;
    public static final int block_num = 1024;
    public static boolean ifUploading;
    public static boolean ifRegUser;
    public static boolean ifCsMode;
    public static boolean ifTurnOffBt;
    // public static boolean ifAutoSave;
    public static int savingLength;
    public static int lowBpm;
    public static int highBpm;
    public static boolean ifFbUser;
    public static boolean ifGpUser;
    // No need to initiate
    public static int userid;
    public static String doctor_name[] = new String[20];;// vision
    public static int doctor_id[] = new int[20];;// vision
    public static String access_request_name[] = new String[20];// vision
    public static int access_request_id[] = new int[20];;// vision
    public static String curVer;
    public static String cachePath;
    public static String savedPath;
    public static String downloadPath;
    public static String gqrsTempPath;
    public static String quickcheckpath;
    public static String folder;
    public static String emergencynum;
    public static String emergencymes;
    public static boolean ifSendSms;
    public static boolean ifAppendLoc;
    public static boolean ifHrmFragmentAlive;
    public static int access_request_doc;
    public static int doctor_num;
    public static String Dynamic_id = null;
    public static int Channel_selection;
    public static boolean ifSaving = false;
    private static Toast mToast;
    public static String testtime;
    public static Filter_Queue Q = new Filter_Queue();
    public static int max_memory;

    /**
     * Check if network is available
     *
     * @param mContext:
     *            Current Context
     * @return True if network is connected
     */
    public static final boolean isNetworkConn(Context mContext) {
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.isConnected();
            }
        }
        return false;
    }

    /**
     * Check if wifi is available
     *
     * @param mContext:
     *            Current Context
     * @return True if wifi is connected
     */
    public static final boolean isWifiConn(Context mContext) {
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null)
                return networkInfo.isConnected();
        }
        return false;
    }

    /**
     * Make toast text on the screen with short disappear time
     *
     * @param mContext:
     *            Current Context
     * @param string:
     *            String to show
     */
    public static final void toastMakeText(Context mContext, String string) {
        if (mToast == null) {
            mToast = Toast.makeText(mContext, string, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(string);
        }
        mToast.show();
    }

    /**
     * Get default intent
     *
     * @param mContext:
     *            Current Context
     * @return Default intent
     */
    public static final Intent defaultIntent(Context mContext) {
        return new Intent(mContext, Login.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    }

    /**
     * Show exit dialog
     *
     * @param mContext:
     *            Current Context
     */
    public static final void exitDialog(final Activity mActivity) {
        Builder builder = new Builder(mActivity);
        builder.setTitle(mActivity.getResources().getString(R.string.global_exit)).setIcon(R.drawable.exit_64)
        .setMessage(mActivity.getResources().getString(R.string.global_wtexit)
                + mActivity.getResources().getString(R.string.app_name) + "?")
        .setPositiveButton(mActivity.getResources().getString(R.string.yes),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mActivity.finish();
            }
        })
        .setNegativeButton(mActivity.getResources().getString(R.string.no),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
        .create().show();
    }

    /**
     * Build info dialog
     *
     * @param mActivity:
     *            Current Context
     * @param title:
     *            Title of dialog
     * @param icon:
     *            Icon of dialog
     * @param message:
     *            Message of dialog
     */
    public static final void infoDialog(final Activity mActivity, final String title, final int icon,
            final String message) {
        AlertDialog.Builder builderDevInfo = new Builder(mActivity);
        builderDevInfo.setTitle(title).setIcon(icon).setMessage(message)
        .setNeutralButton(mActivity.getResources().getString(R.string.back),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
        .create().show();
    }

    /**
     * Check if Email is valid using Regular Expression
     *
     * @param email:
     *            Email address
     * @return True if email is valid
     */
    public static final boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(email).matches();
    }

    /**
     * Check if screen is landscape
     *
     * @param mContext:
     *            Current Context
     * @return True if screen is landscape
     */
    public static final boolean ifLandscape(Context mContext) {
        return (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    /**
     * Set the view focused (cause the "requestFocus" in layout often doesn't
     * work)
     *
     * @param view:
     *            View to be set focused
     */
    public static final void setFocus(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
    }

    /**
     * Send SMS
     *
     * @param num:
     *            Phone number to send to
     * @param mes:
     *            Message to send
     */
    public static final void sendSMS(String num, String mes) {
        SmsManager.getDefault().sendTextMessage(num, null, mes, null, null);
    }

    /**
     * Add device info, current version and imei to the JsonObject sent to
     * server when logging in
     *
     * @param paraOut:
     *            JsonObject to send
     * @param mContext:
     *            Current Context
     * @return Modified JsonObject
     */
    public static final JSONObject loginGeneral(JSONObject paraOut, Context mContext) {
        try {
            paraOut.put("lastdevice", Build.MANUFACTURER + " " + Build.MODEL);
            paraOut.put("version", curVer);
            String tmp = ((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            paraOut.put("imei", tmp == null ? "" : tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paraOut;
    }

    /**
     * Initiate some global variables when app starts
     */
    public static final void initiate() {
        ifRegUser = false;
        ifUploading = false;
        mToast = null;
        // General Settings
        ifCsMode = false;
        ifTurnOffBt = true;
        savingLength = 1 * 60 * 1000;// 1min
        lowBpm = 40;
        highBpm = 100;
        ifFbUser = false;
        ifGpUser = false;
        max_memory = 500;
    }

    /**
     * Initiate global variables for registered user and create file path
     *
     * @param jso:
     *            JsonObject containing user preference
     */
    public static final void initRegUser(JSONObject jso) {
        try {
            userid = jso.getInt("userid");
            emergencynum = jso.getString("emergencynum");
            emergencymes = jso.getString("emergencymes");
            ifSendSms = jso.getBoolean("ifsendsms");
            ifAppendLoc = jso.getBoolean("ifappendloc");
            ifCsMode = jso.getBoolean("ifcsmode");
            ifTurnOffBt = jso.getBoolean("ifturnoffbt");
            savingLength = jso.getInt("savelength");
            lowBpm = jso.getInt("lowbpm");
            highBpm = jso.getInt("highbpm");
            /*
             * access_request_doc=jso.getInt("access_request"); doctor_num
             * =jso.getInt("doctor_num"); for (int n=1;n<=doctor_num;n++){
             * doctor_name[n]=jso.getString("doctor"+n); } for (int
             * n=1;n<=access_request_doc;n++){
             * access_request_name[n]="doctor_request"+n;
             * access_request_name[n]=jso.getString(access_request_name[n]); }
             */
        } catch (Exception e) {
            e.printStackTrace();
        }

        ifRegUser = true;

        String FilePath = RootPath + "/" + String.valueOf(userid);
        cachePath = FilePath + "/Cache";
        savedPath = FilePath + "/Saved for upload";
        downloadPath = FilePath + "/Download";
        gqrsTempPath = RootPath + "/Temp";
        quickcheckpath = FilePath + "/Quick check";
        folder = FilePath;
        new File(RootPath).mkdir();
        new File(FilePath).mkdir();
        new File(cachePath).mkdir();
        new File(savedPath).mkdir();
        new File(downloadPath).mkdir();
        new File(gqrsTempPath).mkdir();
        new File(quickcheckpath).mkdir();
    }

    public static double getYAxisMin() {
        return 0 == Global.Channel_selection ? yAxis_Min_Channel1 : yAxis_Min_Channel2;
    }

    public static double getYAxisMax() {
        return 0 == Global.Channel_selection ? yAxis_Max_Channel1 : yAxis_Max_Channel2;
    }
}