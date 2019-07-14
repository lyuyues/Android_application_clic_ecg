package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * This Activity allows user to send feedback to server
 */
@SuppressLint("HandlerLeak")
public class Feedback extends Activity {
	private final String TAG = "Feedback";
	private ProgressDialog proDialog;
	private String webResult;
	private Button button_send;
	private EditText editText_content;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
	    setContentView(R.layout.feedback);
	    
	    setTitle(getResources().getString(R.string.feedback));
	    getActionBar().setIcon(R.drawable.ic_action_chat);
	    getActionBar().setDisplayHomeAsUpEnabled(true);
	    
	    findViewsById();
		setListener();
	}
	private void findViewsById() {
		button_send = (Button)findViewById(R.id.button_send);
		editText_content = (EditText)findViewById(R.id.editText_content);
	}
	private void setListener() {
		button_send.setOnClickListener(sendListener);
	}
	//Send feedback to our server
	private OnClickListener sendListener = new OnClickListener(){
    	public void onClick(View v){
    		if(!Global.isNetworkConn(Feedback.this)){
    			Global.toastMakeText(Feedback.this, getResources().getString(R.string.nointernet));
    			return;
    		}
    		final String content = editText_content.getText().toString();
    		int contentLen = content.length();
    		if(contentLen==0){
    			Global.toastMakeText(Feedback.this, getResources().getString(R.string.feedback_type));
    			return;
    		}else if(contentLen >= 140){
    			Global.toastMakeText(Feedback.this, getResources().getString(R.string.feedback_more));
    			return;
    		}
    		proDialog = ProgressDialog.show
    				(Feedback.this, getResources().getString(R.string.feedback_sending), "", true, false);
    		new Thread(){
    	    	public void run(){
    	    		webResult = getResources().getString(R.string.noresponse);
    	    		String url = Global.WebServiceUrl + "/sendfeedback/";
    	        	HttpParams hPara = new BasicHttpParams();
    	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
    	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
    	            HttpClient hClient = new DefaultHttpClient(hPara);
    	            HttpResponse response = null;
    	            HttpPost httppost = new HttpPost(url);
    	            try {
    	            	JSONObject paraOut = new JSONObject();
    	            	paraOut.put("userid", Global.ifRegUser ? Global.userid : 0);
    	            	paraOut.put("feedback", content);
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
    	                }
    	    		} catch (Exception e) {
    	    			e.printStackTrace();
    	    		}
                	proDialog.dismiss();
	    			handler.sendEmptyMessage(0);
    	    	}
    	    }.start();
    	}
    };
    //Show results from Web Service, and if success return to last screen
    private Handler handler = new Handler(){
    	public void handleMessage(Message msg){
    		Global.toastMakeText(Feedback.this, webResult);
    		if(webResult.equals("Success: Send feedback!")) onBackPressed();
    	}
    };
	protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
	}
	protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }
	protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
	}
}