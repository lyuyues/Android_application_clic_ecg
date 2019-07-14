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
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * This Activity allows user to sign up and log in using the newly created account
 */
@SuppressLint("HandlerLeak")
public class Signup extends Activity {
	private final String TAG = "Signup";
	private EditText edittext_username, edittext_email, edittext_phone,
	                 edittext_firstname, edittext_lastname, edittext_password,
	                 edittext_cfpassword;
	private Button button_signup,button_birthday;
	private TextView textview_username, textview_password, textview_gender,
						textview_cfpassword,editTextaddress,editTextmpn, textview_birthday;
	private ProgressDialog proDialog;
	private String webresult;
	private Spinner spinner;
	private int sex = 0;
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.signup);
        
        findViewsById();
        setListener();
        setRedWord(textview_birthday);
        setRedWord(textview_username);
        setRedWord(textview_password);
        setRedWord(textview_cfpassword);
        setRedWord(textview_gender);
        Global.setFocus(button_signup);
        initUserInfoAsk();
	}
	//Set red star
	private final void setRedWord(TextView tv) {
        SpannableString ss = new SpannableString(tv.getText().toString() + "*");
        ss.setSpan(new ForegroundColorSpan(Color.RED), ss.length() - 1, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(ss);
	}
	//Ask user if wants to use default information to fill
	private final void initUserInfoAsk() {
		Builder builder = new Builder(Signup.this);
    	builder.setTitle(getResources().getString(R.string.signup_autofill))
    	       .setMessage(getResources().getString(R.string.signup_default))
    	       .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener(){
    	    	   public void onClick(DialogInterface dialog,int which){
    	    		   dialog.dismiss();
    	    		   initUserInfo();
    	    	   }
    	       })
    	       .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener(){
    	    	   public void onClick(DialogInterface dialog, int which){
    	    		   dialog.dismiss();
    	    	   }
    	       })
		       .create().show();
	}
	//Initiate user information using default information
	private final void initUserInfo() {
		String tmpPhone = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
        if(tmpPhone != null) edittext_phone.setText(tmpPhone);
        //Name
        Cursor cursor = getContentResolver().query(
				Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, 
						ContactsContract.Contacts.Data.CONTENT_DIRECTORY), 
				new String[] {ContactsContract.Profile.DISPLAY_NAME}, 
				null, null, null);
        if(cursor!=null && cursor.getCount()>=1 && cursor.moveToFirst()){
        	String tmpName = cursor.getString(cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
        	if(!tmpName.contains(" ")){
				edittext_firstname.setText(tmpName);
			}else{
				String[] tmpString = tmpName.split(" ");
				edittext_firstname.setText(tmpString[0]);
				edittext_lastname.setText(tmpString[tmpString.length-1]);
			}
        	cursor.close();
        }
        //Phone
        cursor = getContentResolver().query(
				Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, 
						ContactsContract.Contacts.Data.CONTENT_DIRECTORY), null, 
				ContactsContract.Contacts.Data.MIMETYPE + " = ?", 
				new String[]{ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE}, null);
        if(tmpPhone==null && cursor!=null && cursor.getCount()>=1 && cursor.moveToFirst()){
        	tmpPhone = cursor.getString(cursor.getColumnIndex
        			(ContactsContract.CommonDataKinds.Phone.NUMBER));
        	edittext_phone.setText(tmpPhone.replace(" ", ""));
        	cursor.close();
        }
        //Email
        cursor = getContentResolver().query(
				Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, 
						ContactsContract.Contacts.Data.CONTENT_DIRECTORY), null, 
				ContactsContract.Contacts.Data.MIMETYPE + " = ?", 
				new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE}, null);
        if(cursor!=null && cursor.getCount()>=1 && cursor.moveToFirst()){
        	String tmp = cursor.getString(cursor.getColumnIndex
        			(ContactsContract.CommonDataKinds.Email.ADDRESS));
        	edittext_email.setText(tmp);
        	cursor.close();
        }
	}
	private void findViewsById() {
		edittext_username = (EditText)findViewById(R.id.editTextUsername);
		edittext_email = (EditText)findViewById(R.id.editTextEmailAddress);
		edittext_phone = (EditText)findViewById(R.id.editTextPhone);
		edittext_firstname = (EditText)findViewById(R.id.editTextFirstName);
		edittext_lastname = (EditText)findViewById(R.id.editTextLastName);
		edittext_password = (EditText)findViewById(R.id.editTextPassword);
		edittext_cfpassword = (EditText)findViewById(R.id.editTextConfirmPassword);
		button_signup = (Button)findViewById(R.id.btnSignUp);
		textview_username = (TextView)findViewById(R.id.textView1);
		textview_password = (TextView)findViewById(R.id.textView9);
		textview_cfpassword = (TextView)findViewById(R.id.textView8);
		textview_gender = (TextView)findViewById(R.id.gender);
		button_birthday=(Button)findViewById(R.id.button_birthday);
		editTextaddress=(EditText)findViewById(R.id.editTextaddress);
		editTextmpn=(EditText)findViewById(R.id.editTextmpn);
		spinner=(Spinner)findViewById(R.id.spinner1);
		textview_birthday = (TextView)findViewById(R.id.textview_birthday);
	}
	private void setListener() {
		button_signup.setOnClickListener(signupListener);
		button_birthday.setOnClickListener(birthdayListener);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, 
                    int pos, long id) {
            
                sex=pos;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
	}
	
	private OnClickListener birthdayListener=new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			DatePickerFragment temp = new DatePickerFragment();
			temp.setCancelable(false);
			temp.show(getFragmentManager(), "startDatePicker");
		}
		
	};
	
	//Sign up using filled information
	private OnClickListener signupListener = new OnClickListener(){
    	public void onClick(View v){
    		if(!Global.isNetworkConn(Signup.this)){
    			Global.toastMakeText(Signup.this, getResources().getString(R.string.nointernet));
    			return;
    		}
    		final String userName = edittext_username.getText().toString();
    		if(userName.length()==0 || userName.length()>15){
    			Global.toastMakeText(Signup.this, "Length of Username should be 1-15!");
    			return;
    		}
    		final String eMail = edittext_email.getText().toString();
    		if(eMail.length() > 40){
    			Global.toastMakeText(Signup.this, "Length of Email should be 0-40!");
    			return;
    		}else if((eMail.length()>0) && (!Global.isEmailValid(eMail))){
    			Global.toastMakeText(Signup.this, "Email is not valid!");
    			return;
    		}
    		final String phone = edittext_phone.getText().toString();
    		if(phone.length() > 20){
    			Global.toastMakeText(Signup.this, "Length of Phone should be 0-20!");
    			return;
    		}
    		final String firstname = edittext_firstname.getText().toString();
    		if(/*firstname.length()==0||*/firstname.length()>20){
    			Global.toastMakeText(Signup.this, "Length of First name should be 0-20!");
    			return;
    		}
    		final String address=editTextaddress.getText().toString();
    		if(/*firstname.length()==0||*/address.length()>100){
    			Global.toastMakeText(Signup.this, "Length of address should be 0-100!");
    			return;
    		}
    		final String mpn = editTextmpn.getText().toString();
    		final String lastname = edittext_lastname.getText().toString();
    		if(/*lastname.length()==0||*/lastname.length()>20){
    			Global.toastMakeText(Signup.this, "Length of Last name should be 0-20!");
    			return;
    		}
    		final String passWord = edittext_password.getText().toString();
    		if(passWord.length()==0 || passWord.length()>20){
    			Global.toastMakeText(Signup.this, "Length of Password should be 1-20!");
    			return;
    		}
    		final String cfpassWord=edittext_cfpassword.getText().toString();
    		if(!passWord.equals(cfpassWord)){
    			Global.toastMakeText(Signup.this, "Confirm password not match!");
    			return;
    		}
    		final String birthday = button_birthday.getText().toString();
    		proDialog = ProgressDialog.show(Signup.this, getResources().getString(R.string.signup_signing), "", true, false);
    	    new Thread(){
    	    	public void run(){
    	    		webresult = getResources().getString(R.string.noresponse);
    	    		String url = Global.WebServiceUrl + "/signup/";
    	    		JSONObject jso = null;
    	        	HttpParams hPara = new BasicHttpParams();
    	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
    	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
    	            HttpClient hClient = new DefaultHttpClient(hPara);
    	            HttpResponse response = null;
    	            HttpPost httppost = new HttpPost(url);
    	            try {
    	            	JSONObject paraOut = new JSONObject();
    	            	paraOut.put("username", userName);
    	            	paraOut.put("email", eMail);
    	                paraOut.put("phone", phone);
    	                paraOut.put("firstname", firstname);
    	                paraOut.put("lastname", lastname);
    	                paraOut.put("password", passWord);
    	                paraOut.put("address", address);
    	                if (!mpn.isEmpty())
    	                	paraOut.put("medical_plan_number",mpn);
    	                paraOut.put("birthday", birthday);
    	                paraOut.put("gender", sex);
    	                paraOut = Global.loginGeneral(paraOut, Signup.this);
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
    	                	jso = new JSONObject(total.toString());
    	                	webresult = jso.getString("result");
    	                	Global.Dynamic_id = jso.getString("Dynamic_id");
    	                }
    	    		} catch (Exception e) {
    	    			e.printStackTrace();
    	    		}
                	proDialog.dismiss();
    	    		if(webresult.equals("Success: Signup!")){
    	    			Global.initRegUser(jso);
    	    			Intent intent= new Intent(Signup.this, MainActivity.class);
    	    			intent.putExtra("userName", userName);
    	    			startActivity(intent);
    	    			finishAffinity();
    	    		}else{
    	    			handler.sendEmptyMessage(0);
    	    		}
    	    	}
    	    }.start();
    	}
    };
    //Handler to show result from web service
    private Handler handler = new Handler(){
    	public void handleMessage(Message msg){
    		Global.toastMakeText(Signup.this, webresult);
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
	private class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener{
		//Called when creating DatePickerDialog
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
        	String temp;
        		temp = button_birthday.getText().toString();
        	DatePickerDialog dpd = new DatePickerDialog(getActivity(), this, parseDate(temp, 1),
        			parseDate(temp, 2) - 1, parseDate(temp, 3));
        		dpd.setTitle("Birthday");
        	
			return dpd;
		}

     private int parseDate(String temp, int what){
		if(what==1){
			return Integer.valueOf(temp.substring(0, 4));
		}else if(what==2){
			return Integer.valueOf(temp.substring(5, 7));
		}else{
			return Integer.valueOf(temp.substring(8, 10));
		}
	}

		@Override
		public void onDateSet(DatePicker view, int year, int month, int day) {
			// TODO Auto-generated method stub
			String temp = dateConvert(year, month, day);
        	
        		button_birthday.setText(temp);
        	
		}

		private String dateConvert(int Year, int Month, int Day) {
		String result = null;
		if(Month < 9){
			if(Day < 10){
				result = String.valueOf(Year) + "-0" + String.valueOf(Month + 1) + "-0" + String.valueOf(Day);
			}else{
				result = String.valueOf(Year) + "-0" + String.valueOf(Month + 1) + "-" + String.valueOf(Day);
			}
		}else{
			if(Day < 10){
				result = String.valueOf(Year) + "-" + String.valueOf(Month + 1) + "-0" + String.valueOf(Day);
			}else{
				result = String.valueOf(Year) + "-" + String.valueOf(Month + 1) + "-" + String.valueOf(Day);
			}
		}
		return result;
	}
}}