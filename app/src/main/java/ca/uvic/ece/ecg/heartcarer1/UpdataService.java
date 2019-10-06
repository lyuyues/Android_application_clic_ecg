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
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
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

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);

    public UpdataService() {
        super("UpdataService");
    }

    @Override
    protected void onHandleIntent(Intent arg0) {
        Log.v(TAG, "onHandleIntent()");

        if (Global.token == null || Global.token.isEmpty())
            return;

        if (!Global.isWifiConnected(UpdataService.this))
            return;

        synchronized (LOCK) {
            FilenameFilter filter = (dir, filename) -> filename.endsWith(".bin");
            File[] files = new File(Global.savedPath).listFiles(filter);
            if (null == files)
                return;

            for (File file : files) {
                String fileName = file.getName();
                Log.v(TAG, "File name: " + fileName);

                if (file.length() < MIN_LENGTH)
                    continue;

                try {
                    long startTimeSec = Long.valueOf(fileName.substring(0, fileName.indexOf(".bin")));
                    String startTime = sdf.format(new Date(startTimeSec));
                    String endTime = sdf.format(new Date(file.lastModified()));

                    JSONObject paraOut = new JSONObject();
                    paraOut.put("startTime", startTime)
                            .put("endTime", endTime);

                    MultipartEntity outEntity = new MultipartEntity();
                    outEntity.addPart("newData", new StringBody(paraOut.toString()));
                    outEntity.addPart("file", new FileBody(file));

                    HttpPost httppost = new HttpPost(Global.WebServiceUrl + "patient/ecg-test/ecg-raw-data");
                    httppost.addHeader("verificationcode", Global.token);
                    httppost.setEntity(outEntity);

                    HttpParams hPara = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
                    HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);

                    HttpClient hClient = new DefaultHttpClient(hPara);
                    HttpResponse response = hClient.execute(httppost);

                    if (200 != response.getStatusLine().getStatusCode())
                        continue;

                    StringBuilder total = new StringBuilder();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line;
                    while ((line = rd.readLine()) != null)
                        total.append(line);

                    // check if the response succeed
                    JSONObject jso = new JSONObject(total.toString());
                    if(!"OK.".equals(jso.getString("errorMessage")))
                        continue;

                    Global.updateFrequency(getModel(jso).getInt("frequency"));
                    Global.containData = getModel(jso).getBoolean("containData");

                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    JSONObject getModel(JSONObject jso)
            throws JSONException {
        return jso.getJSONObject("entity").getJSONObject("model");
    }
}