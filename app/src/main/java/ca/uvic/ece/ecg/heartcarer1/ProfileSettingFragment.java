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
import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * This Fragment allows user to change profile settings
 */
@SuppressLint("HandlerLeak")
public class ProfileSettingFragment extends Fragment {
	private final String TAG = "ProfileSettingFragment";
	private View view;
	private EditText edittext_email, edittext_phone, edittext_firstname,
    	edittext_lastname,edittext_username, editTextaddress, editTextmpn;
    private Button button_update;
    private ProgressDialog proDialog;
    private String webresult;
    private String eMail, phone, firstname, lastname, username, address, mpn;
	private int gender;
    private TextView gender_text;
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		Log.i(TAG, "onCreateView()");
		setHasOptionsMenu(false);
		
		view = inflater.inflate(R.layout.profile_setting, container, false);
		findViewsById();
		setListener();
		initProfile();
        Global.setFocus(button_update);
        
		return view;
	}
	private void findViewsById() {
		edittext_email = (EditText)view.findViewById(R.id.editTextEmailAddress);
		edittext_phone = (EditText)view.findViewById(R.id.editTextPhone);
		edittext_firstname = (EditText)view.findViewById(R.id.editTextFirstName);
		edittext_lastname = (EditText)view.findViewById(R.id.editTextLastName);
		edittext_username=(EditText)view.findViewById(R.id.edittext_username);
		button_update = (Button)view.findViewById(R.id.btnUpdate);
		gender_text = (TextView)view.findViewById(R.id.gender);
		editTextaddress = (EditText)view.findViewById(R.id.editTextaddress);
		editTextmpn = (EditText)view.findViewById(R.id.editTextmpn);
	}
	private void setListener() {
		button_update.setOnClickListener(updateListener);
	}
	//Update profile settings using Web Service
    private OnClickListener updateListener = new OnClickListener(){
    	public void onClick(View v){
    		if(!Global.isNetworkConn(getActivity())){
    			Global.toastMakeText(getActivity(), getResources().getString(R.string.nointernet));
    			return;
    		}
    		eMail = edittext_email.getText().toString();
    		phone = edittext_phone.getText().toString();
    		firstname = edittext_firstname.getText().toString();
    		lastname = edittext_lastname.getText().toString();
    		username=edittext_username.getText().toString();
    		address = editTextaddress.getText().toString();
    		mpn = editTextmpn.getText().toString();
    		if(eMail.length() > 40){
    			Global.toastMakeText(getActivity(), "Length of Email should be 0-40!");
    			return;
    		}else if(eMail.length()>0 && (!Global.isEmailValid(eMail))){
    			Global.toastMakeText(getActivity(), "Email is not valid!");
    			return;
    		}
    		if(phone.length() > 20){
    			Global.toastMakeText(getActivity(), "Length of Phone should be 0-20!");
    			return;
    		}
    		if(firstname.length() > 20){
    			Global.toastMakeText(getActivity(), "Length of First name should be 0-20!");
    			return;
    		}
    		if(lastname.length() > 20){
    			Global.toastMakeText(getActivity(), "Length of Last name should be 0-20!");
    			return;
    		}
    		if(username.length() > 20){
    			Global.toastMakeText(getActivity(), "Length of Username should be 0-20!");
    			return;
    		}
    		
    		
    		proDialog = ProgressDialog.show(getActivity(), "Updating profile...", "", true, false);
    	    new Thread(){
    	    	public void run(){
    	    		webresult = getResources().getString(R.string.noresponse);
    	    		String url = Global.WebServiceUrl + "/upuserinfo/";
    	        	HttpParams hPara = new BasicHttpParams();
    	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
    	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
    	            HttpClient hClient = new DefaultHttpClient(hPara);
    	            HttpResponse response = null;
    	            HttpPost httppost = new HttpPost(url);
    	            try {
    	            	JSONObject paraOut = new JSONObject();
    	            	paraOut.put("Dynamic_id", String.valueOf(Global.Dynamic_id));
    	            	paraOut.put("userid", Global.userid);
    	            	paraOut.put("email", eMail);
    	                paraOut.put("phone", phone);
    	                paraOut.put("firstname", firstname);
    	                paraOut.put("lastname", lastname);
    	                paraOut.put("username", username);
    	                paraOut.put("address", address);
    	                paraOut.put("address", address);
    	                paraOut.put("medical_plan_number", mpn);
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
    		            	JSONObject jso = new JSONObject(total.toString());
    		            	String result = jso.getString("result");
    		            	webresult = result;
    	                	Log.v(TAG, result);
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
    private Handler handler = new Handler(){
    	public void handleMessage(Message msg){
    		int i = msg.what;
    		if (webresult.indexOf("Success")!=-1){
    		if(i==1){
    			edittext_email.setText(eMail);
    			edittext_phone.setText(phone);
    			edittext_firstname.setText(firstname);
                edittext_lastname.setText(lastname);
                edittext_username.setText(username);
        		String gender_t = gender == 0 ? "Gender : Male" : "Gender : Female";
        		gender_text.setText(gender_t);
        		editTextaddress.setText(address);
        		editTextmpn.setText(mpn);
    		}}
    		else  Global.toastMakeText(getActivity(), webresult);
    	}
    };
    //Initialize profile from Server
	private void initProfile() {
		if(!Global.isNetworkConn(getActivity())){
			Global.toastMakeText(getActivity(), getResources().getString(R.string.nointernet));
			return;
		}
		proDialog = ProgressDialog.show(getActivity(), "Downloading profile...", "", true, false);
	    Thread downProfileThread = new Thread(){
	    	public void run(){
	    		webresult = getResources().getString(R.string.noresponse);
	    		String url = Global.WebServiceUrl + "/downuserinfo/";
	        	HttpParams hPara = new BasicHttpParams();
	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
	            HttpClient hClient = new DefaultHttpClient(hPara);
	            HttpResponse response = null;
	            HttpPost httppost = new HttpPost(url);
	            try {
	            	JSONObject paraOut=new JSONObject();
	            	paraOut.put("Dynamic_id", String.valueOf(Global.Dynamic_id));
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
	                	JSONObject jso = new JSONObject(total.toString());
	                	webresult = jso.getString("result");
	                	eMail = jso.getString("email");
	                	firstname = jso.getString("firstname");
	                	lastname = jso.getString("lastname");
	                	phone = jso.getString("phone");
	                	username=jso.getString("username");
	                	gender = jso.getInt("sex");
	                	address = jso.getString("address");
	                	mpn = jso.getString("medical_plan_number");
	                }
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
            	proDialog.dismiss();
            	handler.sendEmptyMessage(1);
	    	}
	    };
	    downProfileThread.start();
	}
	public void onCreateOptionsMenu (Menu menu, MenuInflater inflater){
		Log.v(TAG, "onCreateOptionsMenu");
		inflater.inflate(R.menu.profile_setting, menu);
	}
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    	case R.id.userid:
	    		Global.infoDialog(getActivity(), getResources().getString(R.string.userid), 
	    				android.R.attr.alertDialogIcon, String.valueOf(Global.userid));
	    		return true;
	        case R.id.refresh:
	        	initProfile();
	            return true;
	    }
	    return false;
	}
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