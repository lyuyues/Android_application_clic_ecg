package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.TimeZone;

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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.TimePicker;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.content.Context;
import android.app.Dialog;
import java.text.SimpleDateFormat;

/**
 * This Fragment allows user to change notification settings
 */
@SuppressLint("PatientNotesFragment")
    public class PatientNotesFragment extends Fragment {
    private final String TAG = "PatientNotesFragment";
    private View view;
    private Button button_send;
    private EditText edittext_message, note_message;
    private DatePicker dp_date;
    private TimePicker tp_time;
    private String webResult;
    private ProgressDialog proDialog;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.i(TAG, "onCreateView()");

        view = inflater.inflate(R.layout.patientnotes, container, false);
        findViewsById();
        setListener();
        note_message.setText(Global.notesmes);

        return view;

    }


    private void findViewsById() {
        dp_date = (DatePicker)view.findViewById(R.id.note_datePiker);
        tp_time = (TimePicker)view.findViewById(R.id.note_timePicker);
        button_send = (Button)view.findViewById(R.id.button_send);
        note_message = (EditText)view.findViewById(R.id.notes);
    }

    private void setListener() {
        //button_send.setOnClickListener(testListener);
    }

    //Save notes in cloud and locally
    private OnClickListener saveListener = new OnClickListener(){
        public void onClick(View v){
            Global.emergencymes = edittext_message.getText().toString();
            Global.notesmes = note_message.getText().toString();
            if(Global.emergencymes.length() > 100){
                Global.toastMakeText(getActivity(), "Length of Message should be 0-100!");
                return;
            }
            if(Global.notesmes.length() > 500){
                Global.toastMakeText(getActivity(), "Length of Message should be 0-500!");
                return;
            }
            if(!Global.isNetworkConn(getActivity())){
                Global.toastMakeText(getActivity(), getResources().getString(R.string.nointernet) + "\nSaved in local!");
                return;
            }
            proDialog = ProgressDialog.show(getActivity(), "Updating...", "", true, false);
            new Thread(){
                public void run(){
                    webResult = getResources().getString(R.string.noresponse);
                    String url = Global.WebServiceUrl + "/upsms/";
                    HttpParams hPara = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
                    HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
                    HttpClient hClient = new DefaultHttpClient(hPara);
                    HttpResponse response = null;
                    HttpPost httppost = new HttpPost(url);
                    try {
                        JSONObject paraOut = new JSONObject();
                        paraOut.put("userid", Global.userid);
                        paraOut.put("emergencynum", Global.emergencynum);
                        paraOut.put("emergencymes", Global.emergencymes);
                        paraOut.put("notesmes", Global.notesmes);
                        StringEntity se = new StringEntity(paraOut.toString());
                        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                        httppost.setEntity(se);
                        response = hClient.execute(httppost);
                        if(response != null){
                            StringBuilder total = new StringBuilder();
                            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                            String line;
                            while((line = rd.readLine()) != null){
                                total.append(line);
                            }
                            webResult = total.toString();
                            if(webResult.equals("Success: UpSms!")){
                                handler.sendEmptyMessage(1);
                            }else{
                                handler.sendEmptyMessage(0);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        handler.sendEmptyMessage(0);
                    }
                    proDialog.dismiss();
                }
            }.start();
        }
    };
    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            int i = msg.what;
            if(i==0){
                Global.toastMakeText(getActivity(), "Saved locally but not in cloud!");
            }else if(i==1){
                Global.toastMakeText(getActivity(), "Saved both in local and cloud!");
            }else if(i==2){
                Builder builderDevInfo = new Builder(getActivity());
                builderDevInfo.setTitle(getResources().getString(R.string.noti_mycurloc))
                        .setIcon(R.drawable.location_64);
                builderDevInfo.create().show();
            }else if(i==3){
                Global.toastMakeText(getActivity(), getResources().getString(R.string.noti_messent));
            }
        }
    };
    public void onResume (){
        super.onResume();
        Log.i(TAG, "onResume()");
    }
    public void onPause (){
        super.onPause();
        Log.i(TAG, "onPause()");
    }
    public void onDestroy (){
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }

}