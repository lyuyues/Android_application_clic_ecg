package ca.uvic.ece.ecg.heartcarer1;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class stores global variables and methods which are used by all the
 * other classes.
 *
 * @author yizhou
 */
public final class Global {
    public static String gqrsTempPath;

    static final String WebServiceUrl = "http://ecg.uvic.ca:8080/v1/test/";
    static final int connectionTimeout = 10000;
    static final int socketTimeout = 10000;

    static final String WidgetAction = "com.android.mywidgetaction";

    static final String CHANNEL_ID = "Default";

    public static final int PHONE_STATUS_INIT = 0;
    public static final int PHONE_STATUS_BLE_CONNECTED_DATA_RECEIVED = 1;
    public static final int PHONE_STATUS_BLE_DISCONNECTED = 2;
    public static final int PHONE_STATUS_BLE_CONNECTED_DATA_LOST = 3;

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
    static String folder;
    static boolean ifHrmFragmentAlive;
    static boolean ifSaving = false;
    static int phoneStatus = PHONE_STATUS_INIT;

    static boolean isLogin() {
        return !TextUtils.isEmpty(token);
    }

    static void toastMakeText(Context mContext, String string) {
        Toast.makeText(mContext, string, Toast.LENGTH_SHORT).show();
    }

    static Intent defaultIntent(Context mContext) {
        return new Intent(mContext, MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    }

    static void returnDeviceDialog(final Activity mActivity) {
        new Builder(mActivity)
                .setIcon(R.drawable.exit_64)
                .setTitle(mActivity.getResources().getString(R.string.main_return_device))
                .setMessage(mActivity.getResources().getString(R.string.global_return_device))
                .setPositiveButton(mActivity.getResources().getString(R.string.yes), (dialog, which) -> {
                    ProgressDialog proDialog = ProgressDialog.show(
                            mActivity,
                            mActivity.getResources().getString(R.string.main_returning_device),
                            "",
                            true,
                            false);

                    SendToServer.returnDevice(new SendToServer.FuncInterface() {
                        @Override
                        public void callbackAfterSuccess(Object obj) {
                            Global.token = "";
                            proDialog.dismiss();
                            dialog.dismiss();
                            mActivity.finish();

                            // Empty all stored files when user successfully return the device
                            FilenameFilter filter = (dir, filename) -> filename.endsWith(".bin");
                            File[] files = new File(Global.savedPath).listFiles(filter);
                            for (File file : files) {
                                file.delete();
                            }
                            files = new File(Global.savedPath).listFiles(filter);
                            if (0 == files.length) {
                                Log.i(TAG, "Empty files successfully");
                            }
                        }

                        @Override
                        public void callbackAfterFail(Object obj) {
                        }

                        @Override
                        public void handleException(Exception e) {
                            Global.toastMakeText(mActivity, e.getMessage());
                            proDialog.dismiss();
                            dialog.dismiss();
                        }
                    });
                })
                .setNegativeButton(mActivity.getResources().getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    static void exitDialog(final Activity mActivity) {
        new Builder(mActivity).setIcon(R.drawable.exit_64)
                .setTitle(mActivity.getResources().getString(R.string.global_exit))
                .setMessage(mActivity.getResources().getString(R.string.global_wtexit) + mActivity.getResources().getString(R.string.app_name) + "?")
                .setPositiveButton(mActivity.getResources().getString(R.string.yes), (dialog, which) -> {
                    token = "";
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
    static void initiate(Context context) {
        ifTurnOffBt = true;
        savingLength = frequency * 1000;
        lowBpm = 40;
        highBpm = 100;

        final String RootPath = context.getApplicationInfo().dataDir;
        cachePath = RootPath + "/Cache";
        savedPath = RootPath + "/Saved for upload";
        gqrsTempPath = RootPath + "/Temp";
        folder = RootPath;

        new File(RootPath).mkdir();
        new File(cachePath).mkdir();
        new File(savedPath).mkdir();
        new File(gqrsTempPath).mkdir();
    }

    static void updateFrequency(int frequency) {
        Global.frequency = frequency;
        Global.savingLength = frequency * 1000;
    }

    interface FuncInterface
    {
        void callback();

        default void handleException(Exception e) {
            Log.i(TAG, "handleException, " + e.toString());
        }
    }

    private static final String TAG = "Global";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);

    private static final String MessageKey = "message";
    private static final Handler mHandler = new Handler();

    static boolean isNetworkConnected(Context mContext, int networkType) {
        Object service = mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!(service instanceof ConnectivityManager))
            return false;

        NetworkInfo networkInfo = ((ConnectivityManager) service).getNetworkInfo(networkType);
        return null != networkInfo && networkInfo.isConnected();
    }

    static boolean isWifiConnected(Context mContext) {
        return isNetworkConnected(mContext, ConnectivityManager.TYPE_WIFI);
    }

    static boolean isCellularConnected(Context mContext) {
        return isNetworkConnected(mContext, ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean isWifiOrCellularConnected(Context context) {
        return Global.isWifiConnected(context) || Global.isCellularConnected(context);
    }

    public static void login(FuncInterface func, Context context) {
        Log.i(TAG, "login");
        if (BleService.mDevice == null) {
            return;
        }

        // if no network connection notify user and return
        if (!isWifiOrCellularConnected(context)) {
            return;
        }

        new Thread() {
            public void run() {
                try {
                    JSONObject paraOut = new JSONObject();
                    paraOut.put("deviceMacAddress", BleService.mDevice.getAddress());

                    StringEntity se = new StringEntity(paraOut.toString());
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

                    HttpPost httppost = new HttpPost(Global.WebServiceUrl + "phones");
                    httppost.setEntity(se);

                    HttpParams hPara = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
                    HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);

                    HttpClient hClient = new DefaultHttpClient(hPara);
                    HttpResponse response = hClient.execute(httppost);

                    if (200 != response.getStatusLine().getStatusCode()) {
                        Log.i(TAG, "Server respond non 200");
                        func.handleException(new Exception());
                        return;
                    }

                    // get the response string
                    StringBuilder total = new StringBuilder();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        total.append(line);
                    }

                    // check if the response succeed
                    JSONObject jso = new JSONObject(total.toString());
                    String errorMessage = jso.getString("errorMessage");
                    if (!"OK.".equals(errorMessage)) {
                        Log.i(TAG, errorMessage);
                        mHandler.post(() -> Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show());
                        func.handleException(new Exception(errorMessage));
                        return;
                    }

                    Global.token = jso.getJSONObject("entity").getJSONObject("model").getString("message");

                    mHandler.post(func::callback);
                } catch (Exception e) {
                    func.handleException(e);
                }
            }
        }.start();
    }

    public static void logout() {
        token = "";
        MainActivity.updateAdapter();
    }
}