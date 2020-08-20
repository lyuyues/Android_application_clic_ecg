package ca.uvic.ece.ecg.heartcarer1;

import android.util.Log;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SendToServer {
    private static final String TAG = "SendToServer";
    static final String WebServiceUrl = "http://ecg.uvic.ca:8080/v1/test/";
    static final int connectionTimeout = 3000;
    static final int socketTimeout = 3000;


    interface FuncInterface {
        void callbackAfterSuccess(Object obj);

        void callbackAfterFail(Object obj);

        default void handleException(Exception e) {
            Log.i(TAG, "handleException, " + e.toString());
        }
    }

//    /**
//     * Verify user with the server and get a token if verified.
//     *
//     * @param
//     * @return
//     */
//    public void login(Context context) {
//        // if no network connection notify user and return
//        if (!Global.isWifiOrCellularConnected(context)) {
//            return;
//        }
//        JSONObject paraOut = new JSONObject();
//        new Thread() {
//            public void run() {
//                try {
//                    paraOut.put("deviceMacAddress", BleService.mDevice.getAddress());
//                    StringEntity entity = new StringEntity(paraOut.toString());
//                    entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
//
//                    HttpPost httppost = new HttpPost(Global.WebServiceUrl + "phones");
//                    httppost.setEntity(entity);
//
//                    sendToServer("login", httppost, new FuncInterface() {
//                        @Override
//                        public void callbackAfterSuccess(Object obj) {
//                            JSONObject jso = (JSONObject) obj;
//                            try {
//                                Global.token = jso.getJSONObject("entity").getJSONObject("model").getString("message");
//                            } catch (Exception e) {
//
//                            }
//                        }
//
//                        @Override
//                        public void callbackAfterFail(Object obj) {
//                        }
//
//                        @Override
//                        public void handleException(Exception e) {
//
//                        }
//                    });
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }.start();
//    }

    /**
     * Post to server with url "http://ecg.uvic.ca:8080/v1/test/phones/comment"
     * Header: "verificationcode" - token
     * Body: "comment" - body
     *
     * @param body a string includes start time, end time and notes contents
     */
    public static void sendPatientNotes(String body, FuncInterface func) {
        JSONObject paraOut = new JSONObject();
        new Thread() {
            public void run() {
                try {
                    Log.i(TAG, body);
                    paraOut.put("comment", body);
                    StringEntity entity = new StringEntity(paraOut.toString());
                    entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

                    HttpPost httppost = new HttpPost(Global.WebServiceUrl + "phones/comment");
                    httppost.addHeader("verificationcode", Global.token);
                    httppost.setEntity(entity);

                    sendToServer("sendPatientNotes", httppost, func);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * For user unregister the phone when ready to return the phone
     * @param func: all callback functions implement interface FuncInterface
     */
    public static void returnDevice(FuncInterface func) {
        JSONObject paraOut = new JSONObject();
        new Thread() {
            public void run() {
                try {
                    //Wrap JSON
                    paraOut.put("deviceMacAddress", "testMacAddress")
                            .put("returnDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH).format(new Date()));

                    StringEntity entity = new StringEntity(paraOut.toString());
                    entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

                    HttpPost httppost = new HttpPost(Global.WebServiceUrl + "phones/return-status");
                    httppost.addHeader("verificationcode", Global.token);
                    httppost.setEntity(entity);

                    sendToServer("returnDevice", httppost, func);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

//    public void sendDataToServer() {
//        JSONObject paraOut = new JSONObject();
//        new Thread() {
//            public void run() {
//                try {
//                    paraOut.put("startTime", null)
//                            .put("endTime", null)
//                            .put("phoneStatus", Global.phoneStatus);
//                    StringEntity entity = new StringEntity(paraOut.toString());
//                    entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
//                    HttpPost httppost = new HttpPost(WebServiceUrl + "patient/ecg-/ecg-raw-data");
//                    httppost.addHeader("verificationcode", Global.token);
//                    httppost.setEntity(entity);
//
//                    sendToServer("sendDataToServer", httppost, new FuncInterface() {
//                        @Override
//                        public void callbackAfterSuccess(Object obj) {
//                        }
//
//                        @Override
//                        public void callbackAfterFail(Object obj) {
//                        }
//
//                        @Override
//                        public void handleException(Exception e) {
//
//                        }
//                    });
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }.start();
//    }

    /**
     * Sent the package to server and get the response
     * @param tag : the process call this function
     * @param httppost:
     * @param func : callback functions
     */
    private static synchronized void sendToServer(String tag, HttpPost httppost, FuncInterface func) {
        new Thread() {
            public void run() {
                try {
                    HttpParams hPara = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(hPara, connectionTimeout);
                    HttpConnectionParams.setSoTimeout(hPara, socketTimeout);

                    HttpClient hClient = new DefaultHttpClient(hPara);
                    HttpResponse response = hClient.execute(httppost);

                    // Get the response string
                    StringBuilder total = new StringBuilder();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line;
                    while ((line = rd.readLine()) != null)
                        total.append(line);

                    if (200 != response.getStatusLine().getStatusCode()) {
                        Log.i(tag, "Response non-200");
                        Log.i(tag, "" + response.getStatusLine().getStatusCode() + total.toString());
                        func.callbackAfterFail(null);
                        return;
                    }

                    // Double check if the response succeed
                    JSONObject jso = new JSONObject(total.toString());
                    if (!"OK.".equals(jso.getString("errorMessage"))) {
                        Log.v(tag, jso.getString("errorMessage"));
                        func.callbackAfterFail(null);
                        return;
                    }

                    // If succeed, do the job
                    Log.i(tag, "successfully connect to server");
                    func.callbackAfterSuccess(null);

                } catch (Exception e) {
                    func.handleException(e);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    JSONObject getModel(JSONObject jso)
            throws JSONException {
        return jso.getJSONObject("entity").getJSONObject("model");
    }
}
