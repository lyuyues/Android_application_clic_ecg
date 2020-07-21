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
import org.apache.http.entity.mime.FormBodyPartBuilder;
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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
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
            BluetoothAdapter bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

            JSONObject paraOut = new JSONObject();

            try {
                if (null == files) {
                    if (bluetoothAdapter.enable()) {
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
                            .addTextBody("newData", paraOut.toString())
                            .addPart(FormBodyPartBuilder.create("file", null).build());

                    sendToServer(outEntity);
                } else {

                    for (File file : files) {
                        if (file == null)
                            continue;
                        String fileName = file.getName();
                        Log.v(TAG, "File name: " + fileName);

                    if (file.length() < MIN_LENGTH)
                        continue;

                        long startTimeSec = Long.parseLong(fileName.substring(0, fileName.indexOf(".bin")));
                        String startTime = sdf.format(new Date(startTimeSec));
                        String endTime = sdf.format(new Date(file.lastModified()));
                        Log.i(TAG, "startTime: " + startTime);
                        Log.i(TAG, "endTime: " + endTime);

                        // update phone status
                        Global.phoneStatus = Global.PHONE_STATUS_BLE_CONNECTED_DATA_RECEIVED;
                        Log.i(TAG, "phoneStatus: " + Global.phoneStatus);

                        paraOut.put("startTime", startTime)
                                .put("endTime", endTime)
                                .put("phoneStatus", Global.phoneStatus);

                        MultipartEntityBuilder outEntity = MultipartEntityBuilder.create();
                        outEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                                .addTextBody("newData", paraOut.toString())
                                .addPart("file", new FileBody(file));

                        if (CONNECT_TO_SERVER_SUCCESSFULLY == sendToServer(outEntity)) {
                            Log.i(TAG, " successfully send to server ");
                            boolean res = file.delete();
                            if (!res){
                                Log.i(TAG, fileName +  " deleting failed.");
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

            if (200 != response.getStatusLine().getStatusCode()) {
                Log.i(TAG, " failed connect to server");
                return CONNECT_TO_SERVER_FAILED;
            }

            StringBuilder total = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line;
            while ((line = rd.readLine()) != null)
                total.append(line);

            // check if the response succeed
            JSONObject jso = new JSONObject(total.toString());
            if (!"OK.".equals(jso.getString("errorMessage"))) {
                Log.i(TAG, jso.getString("errorMessage"));
                return CONNECT_TO_SERVER_ERROR;
            }

            Global.updateFrequency(getModel(jso).getInt("frequency"));
            Global.containData = getModel(jso).getBoolean("containData");

            Log.v(TAG, total.toString());
            Log.i(TAG, "successfully connect to server");
            return CONNECT_TO_SERVER_SUCCESSFULLY;

        }catch (Exception e) {
            e.printStackTrace();
        }
        return CONNECT_TO_SERVER_DEFAULT;
    }

    JSONObject getModel(JSONObject jso)
            throws JSONException {
        return jso.getJSONObject("entity").getJSONObject("model");
    }
}