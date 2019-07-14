package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/**
 * This class is the main Activity that will launch when app starts, in which user can log in 
 * in three ways, or sign up, or continue without an account
 */
@SuppressLint("HandlerLeak")
public class Login extends Activity {
	private final String TAG = "Login";
	private EditText edittext_username, edittext_password;
	private Button button_login;
	private TextView textview_start, textview_signup;
	private CheckBox checkbox_remember;
	private String webResult;
	private ProgressDialog proDialog;
	private SharedPreferences share;
	private boolean ifBackPressed = false;
	private Handler mHandler = new Handler();
	private final Runnable mRunnable = new Runnable(){
		@Override
		public void run() {
			ifBackPressed = false;
		}
	};
	
	/**
	 * This method will be call first
	 */
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login);
        System.gc();        
        Global.initiate();
        findViewsById();
        setListener();
        //Initiate Username and Password
        share = getSharedPreferences(getResources().getString(R.string.app_name), 0);
        edittext_username.setText(share.getString(Global.shareUsername, ""));
        String tmpPassword = share.getString(Global.sharePassword, "");
        if(!tmpPassword.equals("")){
        	edittext_password.setText(tmpPassword);
        	checkbox_remember.setChecked(true);
        }

        Global.setFocus(button_login);
        //Create a shortcut of app if hasn't done it before
        if(share.getBoolean("ifshortcut", true)){
        	share.edit().putBoolean("ifshortcut", false).commit();
            createShortcut();
        }
        //Get current app version and check for update if Wifi is connected
        Global.curVer = getResources().getString(R.string.app_versionName);
        if(Global.isWifiConn(Login.this)) checkForUpdate();
	}
    
    
    //Check for any updated version of app
	private void checkForUpdate() {
		proDialog = ProgressDialog.show
				(Login.this, getResources().getString(R.string.login_check), "", true, false);
		new Thread(){
	    	public void run(){
	    		webResult = getResources().getString(R.string.noresponse);
	    		String url = Global.WebServiceUrl + "/getcurversion/";
	        	HttpParams hPara = new BasicHttpParams();
	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
	            HttpClient hClient = new DefaultHttpClient(hPara);
	            HttpResponse response = null;
	            HttpGet httpGet = new HttpGet(url);
	            try {
	    			response = hClient.execute(httpGet);
	                if(response != null){
	                	BufferedReader rd = new BufferedReader(new InputStreamReader
	                			(response.getEntity().getContent()));
	                	webResult = rd.readLine();
	                }
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
            	proDialog.dismiss();
            	handler.sendEmptyMessage(1);
	    	}
	    }.start();
	}
	private void findViewsById() {
		edittext_username = (EditText)findViewById(R.id.editTextUsername);
		edittext_password = (EditText)findViewById(R.id.editTextPassword);
		button_login = (Button)findViewById(R.id.buttonLogin);
		textview_signup = (TextView)findViewById(R.id.textViewsignUp);
		textview_start = (TextView)findViewById(R.id.textView3);
		checkbox_remember = (CheckBox)findViewById(R.id.checkBox1);
	}
	private void setListener() {
		button_login.setOnClickListener(loginListener);
		textview_signup.setOnClickListener(signupListener);
		textview_start.setOnClickListener(startListener);
	}
	//Log in using Username and Password
	private OnClickListener loginListener = new OnClickListener(){
    	public void onClick(View v){
    		final String userName = edittext_username.getText().toString();
    		Global.username=userName;
    		final String passWord = edittext_password.getText().toString();
    		share.edit().putString(Global.shareUsername, userName).commit();
    		share.edit().putString(Global.sharePassword, 
    				checkbox_remember.isChecked() ? passWord : "").commit();
    		if(!Global.isNetworkConn(Login.this)){
    			Global.toastMakeText(Login.this, getResources().getString(R.string.nointernet));
    			return;
    		}
    		if(userName.length()==0 || userName.length()>15){
    			Global.toastMakeText(Login.this, getResources().getString(R.string.login_length_un) + "1-15 !");
    			return;
    		}
    		if(passWord.length()==0 || passWord.length()>15){
    			Global.toastMakeText(Login.this, getResources().getString(R.string.login_length_pw) + "1-20 !");
    			return;
    		}
    		proDialog = ProgressDialog.show(Login.this, getResources().getString(R.string.login_logging), "", true, false);
    	    new Thread(){
    	    	public void run(){
    	    		webResult = getResources().getString(R.string.noresponse);
    	    		String url = Global.WebServiceUrl + "/login/";
    	        	HttpParams hPara = new BasicHttpParams();
    	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
    	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
    	            HttpClient hClient = new DefaultHttpClient(hPara);
    	            HttpResponse response = null;
    	            HttpPost httppost = new HttpPost(url);
    	            try {
    	            	JSONObject paraOut = new JSONObject();
    	            	paraOut.put("username", userName);
    	                paraOut.put("password", passWord);
    	                paraOut = Global.loginGeneral(paraOut, Login.this);
    	                StringEntity se = new StringEntity(paraOut.toString());
    	                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
    	                httppost.setEntity(se);
    	    			response = hClient.execute(httppost);
    	                if(response != null){
    	                	StringBuilder total = new StringBuilder();
    	                	BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
    	                	 String line;
    	                	while((line = rd.readLine())!= null){
    	                        total.append(line);
    	                    }
    	                	JSONObject jso = new JSONObject(total.toString());
    	                	webResult = jso.getString("result");
    	                	if(webResult.equals("Success: Login!")){
    	                		Global.Dynamic_id=jso.getString("Dynamic_id");
    	                		String url1 = Global.WebServiceUrl + "/get_doclist/";
    	    			    	HttpParams hPara1 = new BasicHttpParams();
    	    			        HttpConnectionParams.setConnectionTimeout(hPara1, Global.connectionTimeout);
    	    			        HttpConnectionParams.setSoTimeout(hPara1, Global.socketTimeout);
    	    			        HttpClient hClient1 = new DefaultHttpClient(hPara1);
    	    			        HttpResponse response1 = null;
    	    			        HttpPost httppost1 = new HttpPost(url1);
    	    			        try {
    	    			        	JSONObject paraOut1 = new JSONObject();
    	    			        	paraOut1.put("Dynamic_id", Global.Dynamic_id);
    	    			        	paraOut1.put("WHORU", 1);
    	    			            StringEntity se1 = new StringEntity(paraOut1.toString());
    	    			            se1.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
    	    			            httppost1.setEntity(se1);
    	    						response1 = hClient1.execute(httppost1);
    	    			            if(response1 != null){
    	    			            	StringBuilder total1 = new StringBuilder();
    	    			            	BufferedReader rd1 = new BufferedReader(new InputStreamReader(response1.getEntity().getContent()));
    	    			            	String line1;
    	    			            	while((line1 = rd1.readLine()) != null){
    	    			                    total1.append(line1);
    	    			                }
    	    			            	JSONObject jso1 = new JSONObject(total1.toString());
    	    			            	int doctor_num = jso1.getInt("doctor_num");
    	    			            	int access_request = jso1.getInt("access_request");
    	    			                Global.doctor_num=doctor_num;
    	    			                Global.access_request_doc=access_request;
    	    			            	for (int n=1;n<=doctor_num;n++){
    	    			        			Global.doctor_name[n]=jso1.getString("doctor"+n);
    	    			        		}
    	    			            	for (int n=1;n<=access_request;n++){
    	    			            		Global.access_request_name[n]=jso1.getString("doctor_request"+n);
    	    			        		}
    	    				    }}
    	    			            catch (Exception e) {
    	    			    			e.printStackTrace();
    	    			    		}
    	                		Global.initRegUser(jso);
    	    	    			Intent intent= new Intent(Login.this, MainActivity.class);
    	    	    			intent.putExtra("userName", userName);
    	    	        		startActivity(intent);
    	    	        		proDialog.dismiss();
    	    	        		finish();
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
    //Handler to handle incoming message
    private Handler handler = new Handler(){
    	public void handleMessage(Message msg){
    		int i = msg.what;
    		//Fail to log in using Facebook and Google+
    		if(i==0){
    			Global.toastMakeText(Login.this, webResult);
    		}else{
    			//If found an updated app version, ask user to download it
    			try{
    				if((!webResult.equals(getResources().getString(R.string.noresponse))) && 
        					((Float.parseFloat(Global.curVer) < 
        							Float.parseFloat(webResult.replace("_", "."))))){
        				Builder builder = new Builder(Login.this);
            	    	builder.setTitle(getResources().getString(R.string.login_update))
            	    	       .setIcon(R.drawable.update_64)
            	    	       .setMessage(getResources().getString(R.string.login_latest) + "\"" + 
            	    	    		   getResources().getString(R.string.app_name) + " " + 
            	    	    		   webResult.replace("_", ".") + "\"" + 
            	    	    		   getResources().getString(R.string.login_available))
            	    	       .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener(){
            	    	    	   public void onClick(DialogInterface dialog, int which){
            	    	    		   dialog.dismiss();
            	    	    		   startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
            	    	    				   (Global.WebServiceUrl + "/getapk")));
            	    	    	   }
            	    	       })
            	    	       .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener(){
            	    	    	   public void onClick(DialogInterface dialog, int which){
            	    	    		   dialog.dismiss();
            	    	    	   }
            	    	       })
            			       .create().show();
            		}
    			}catch(Exception e){
    				e.printStackTrace();
    			}
    		}
    	}
    };
    //Start Signup Activity
    private OnClickListener signupListener = new OnClickListener(){
    	public void onClick(View v){
    		startActivity(new Intent(Login.this, Signup.class));
    	}
    };
    //Continue without an account
    private OnClickListener startListener = new OnClickListener(){
    	public void onClick(View v){
    		Intent intent = new Intent(Login.this, MainActivity.class);
    		intent.putExtra("userName", "");
    		startActivity(intent);
    		finish();
    	}
    };
    //Create shortcut for app
	private void createShortcut() {
		Intent shortCutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		shortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
		shortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(Login.this, R.drawable.main_heart_beat_128));
		shortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, Global.defaultIntent(Login.this));
		sendBroadcast(shortCutIntent);
	}
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    super.onActivityResult(requestCode, resultCode, intent);
	    Log.v(TAG,"onActivityResult()");
	}
	@Override
	protected void onSaveInstanceState (Bundle outState){
		super.onSaveInstanceState(outState);
	}
	/**
	 * When back key is pressed twice in 2 seconds, finish app
	 */
	@Override
    public void onBackPressed() {
    	if(ifBackPressed){
    		mHandler.removeCallbacks(mRunnable);
    		super.onBackPressed();
    		return;
    	}
    	ifBackPressed = true;
    	Global.toastMakeText(Login.this, getResources().getString(R.string.login_pressback));
    	mHandler.postDelayed(mRunnable, Global.backInterval);
    }
	/*protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");
    }*/
	protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
	}
	protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }
	/*protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }*/
	protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
	}
	
}