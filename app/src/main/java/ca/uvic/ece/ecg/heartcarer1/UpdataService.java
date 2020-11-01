package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * This Service updates saved ECG data to Cloud Server
 */
public class UpdataService extends IntentService {
    private static final String TAG = "UpdataService";
    private static final Object LOCK = new Object();
    private static final int MIN_LENGTH = 1250; // 1s
    private static final int CONNECT_TO_SERVER_DEFAULT = 0;
    private static final int CONNECT_TO_SERVER_FAILED = 1;
    private static final int CONNECT_TO_SERVER_ERROR = 2;
    private static final int CONNECT_TO_SERVER_SUCCESSFULLY = 3;
    private static final int CONNECT_TO_SERVER_EXPIRED_DATA = 4;
    private static final int CONNECT_TO_SERVER_EXPIRED_VERIFICATION_CODE = 5;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);

    public UpdataService() {
        super("UpdataService");
    }

    @Override
    protected void onHandleIntent(Intent arg0) {
        Log.v(TAG, "onHandleIntent()");

        if (Global.token == null || Global.token.isEmpty())
            return;

        if (!Global.isWifiOrCellularConnected(UpdataService.this))
            return;

        synchronized (LOCK) {
            FilenameFilter filter = (dir, filename) -> filename.endsWith(".bin");
            File[] files = new File(Global.savedPath).listFiles(filter);

            if (files != null) {
                for (File file : files) {
                    if (file.length() < MIN_LENGTH) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                }

                files = new File(Global.savedPath).listFiles(filter);
            }

            JSONObject paraOut = new JSONObject();

            try {
                if (null == files) {
                    if (BleService.ConState == BleService.ConState_Connected) {
                        Global.phoneStatus = Global.PHONE_STATUS_BLE_CONNECTED_DATA_LOST;
                    } else {
                        Global.phoneStatus = Global.PHONE_STATUS_BLE_DISCONNECTED;
                    }
                    Log.i(TAG, "phoneStatus: " + Global.phoneStatus);

                    paraOut.put("startTime", null)
                            .put("endTime", null)
                            .put("phoneStatus", Global.phoneStatus);

                    MultipartEntityBuilder outEntity = MultipartEntityBuilder.create();
                    outEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                            .addTextBody("newData", paraOut.toString());

                    sendToServer(outEntity);
                } else {
                    if (BleService.ConState == BleService.ConState_Connected) {
                        Global.phoneStatus = Global.PHONE_STATUS_BLE_CONNECTED_DATA_RECEIVED;
                    } else {
                        Global.phoneStatus = Global.PHONE_STATUS_BLE_DISCONNECTED;
                    }
                    Log.i(TAG, "phoneStatus: " + Global.phoneStatus);

                    for (File file : files) {
                        if (file == null)
                            continue;
                        String fileName = file.getName();
                        Log.v(TAG, "File name: " + fileName);

                        long startTimeSec = Long.parseLong(fileName.substring(0, fileName.indexOf(".bin")));
                        String startTime = sdf.format(new Date(startTimeSec));
                        String endTime = sdf.format(new Date(file.lastModified()));
                        Log.i(TAG, "startTime: " + startTime);
                        Log.i(TAG, "endTime: " + endTime);

                        paraOut.put("startTime", startTime)
                                .put("endTime", endTime)
                                .put("phoneStatus", Global.phoneStatus);

                        MultipartEntityBuilder outEntity = MultipartEntityBuilder.create();
                        outEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                                .addTextBody("newData", paraOut.toString())
                                .addPart("file", new FileBody(file));

                        int status = sendToServer(outEntity);
                        if (CONNECT_TO_SERVER_SUCCESSFULLY == status || CONNECT_TO_SERVER_EXPIRED_DATA == status) {
                            if (file.delete()) {
                                Log.i(TAG, fileName + " deleting successfully.");
                            } else {
                                Log.i(TAG, fileName + " deleting failed.");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // Sent the package to server and get the response
    private synchronized int sendToServer(MultipartEntityBuilder outEntity) {

        try {
            HttpPost httppost = new HttpPost(Global.WebServiceUrl + "patient/ecg-test/ecg-raw-data");
            httppost.addHeader("verificationcode", Global.token);
            httppost.setEntity(outEntity.build());

            HttpParams hPara = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);

            HttpClient hClient = new DefaultHttpClient(hPara);
            HttpResponse response = hClient.execute(httppost);

            // Get status code
            int statusCode = response.getStatusLine().getStatusCode();
            Log.i(TAG, "statusCode: " + statusCode);
            
            // Read respond information
            StringBuilder total = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line;
            while ((line = rd.readLine()) != null)
                total.append(line);
            rd.close();
            Log.i(TAG, "Server Return: " + statusCode + ", " + total.toString());

            // check if the response succeed
            JSONObject jso = new JSONObject(total.toString());
            String errorMess = jso.getString("errorMessage");

            if (200 != statusCode && 400 != statusCode && 401 != statusCode) {
                Log.i(TAG, "Failed connect to server");
                Log.i(TAG,  errorMess);
                return CONNECT_TO_SERVER_FAILED;
            }

            // when the verification code is expired, re-login to get an new one.
            if (401 == response.getStatusLine().getStatusCode()) {
                Log.i(TAG, "The verification code is expired, please Re-login");
                Global.login(MyApplication.getContext());
                return CONNECT_TO_SERVER_EXPIRED_VERIFICATION_CODE;
            }

            int errorCode =  jso.getInt("errorCode");
            if (400 == statusCode && 1031 == errorCode) {
                Log.i(TAG, "The file contains expired data, please delete it.");
                Log.i(TAG, errorMess);
                return CONNECT_TO_SERVER_EXPIRED_DATA;
            }

            // check if the response succeed
            if (!"OK.".equals(errorMess)) {
                Log.i(TAG, errorMess);
                return CONNECT_TO_SERVER_ERROR;
            }

            Global.updateFrequency(getModel(jso).getInt("frequency"));
            Global.containData = getModel(jso).getBoolean("containData");

            Log.i(TAG, "successfully connect to server");
            return CONNECT_TO_SERVER_SUCCESSFULLY;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return CONNECT_TO_SERVER_DEFAULT;
    }

    JSONObject getModel(JSONObject jso)
            throws JSONException {
        return jso.getJSONObject("entity").getJSONObject("model");
    }
}