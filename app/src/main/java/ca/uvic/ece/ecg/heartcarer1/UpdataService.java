package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * This Service updates saved ECG data to Cloud Server
 */
public class UpdataService extends IntentService {
    private final String TAG = "UpdataService";
    private int MinLength = 1250; // 1s

    public UpdataService() {
        super("UpdataService");
    }

    // Handle request - upload saved files
    @Override
    protected void onHandleIntent(Intent arg0) {
        Log.v(TAG, "onHandleIntent(Intent intent)");
        Global.ifUploading = true;
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".bin");
            }
        };

        File[] files = new File(Global.savedPath).listFiles(filter);
        Log.v(TAG, "File number: " + files.length);
        if (files.length != 0) {
            for (File upFile : files) {
                boolean ifcs = (String.valueOf(upFile.getName().charAt(14)).equals("1"));
                String iffirst = String.valueOf(upFile.getName().charAt(29));
                if (upFile.length() < MinLength && !ifcs)
                    continue;
                String result = getResources().getString(R.string.noresponse);
                String url = Global.WebServiceUrl + "/updata/";
                HttpParams hPara = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
                HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
                HttpClient hClient = new DefaultHttpClient(hPara);
                HttpResponse response = null;
                HttpPost httppost = new HttpPost(url);
                try {
                    MultipartEntity outEntity = new MultipartEntity();
                    outEntity.addPart("iffirst", new StringBody(iffirst));
                    // set up datapath according to dataid
                    String filepath = Global.savedPath + "/" + "tmp.zip";
                    FileOutputStream zipin = new FileOutputStream(filepath);
                    ZipOutputStream zipout = new ZipOutputStream(zipin);
                    ZipEntry ze = new ZipEntry("file");
                    zipout.putNextEntry(ze);
                    FileInputStream input = new FileInputStream(upFile);
                    int temp = 0;
                    while ((temp = input.read()) != -1) {
                        zipout.write(temp);
                    }
                    input.close();
                    zipout.closeEntry();
                    zipout.close();

                    File upload = new File(filepath);
                    outEntity.addPart("content", new FileBody(upload));
                    outEntity.addPart("Dynamic_id", new StringBody(String.valueOf(Global.Dynamic_id)));
                    outEntity.addPart("starttime", new StringBody(upFile.getName().substring(0, 14)));
                    outEntity.addPart("testtime", new StringBody(String.valueOf(upFile.getName().substring(15, 29))));
                    outEntity.addPart("length", new StringBody(String.valueOf(ifcs
                            ? 4 * Integer.valueOf(upFile.getName().substring(23, upFile.getName().lastIndexOf(".")))
                            : ((int) upFile.length()) / 1250))); // 1250 = 250Hz * 5 bytes per sample(including 2 channels)
                    outEntity.addPart("ifcs", new StringBody(String.valueOf(ifcs)));
                    httppost.setEntity(outEntity);
                    response = hClient.execute(httppost);
                    if (response != null) {
                        StringBuilder total = new StringBuilder();
                        BufferedReader rd = new BufferedReader(
                                new InputStreamReader(response.getEntity().getContent()));
                        String line;
                        while ((line = rd.readLine()) != null) {
                            total.append(line);
                        }
                        JSONObject jso = new JSONObject(total.toString());
                        result = jso.getString("result");
                    }
                    Log.v(TAG, result);
                    if (result.equals("Success: Updata!") || result.equals("Data already existed in database!")) {
                        boolean flag = upFile.delete() && upload.delete();
                        System.out.print(flag);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");

        Global.ifUploading = false;
    }
}