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
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
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
import android.widget.ImageButton;

/**
 * This Fragment allows user to change notification settings
 */
@SuppressLint("HandlerLeak")
public class NotificationSettingFragment extends Fragment {
	private final String TAG = "NotificationSettingFragment";
	private View view;
	private Button button_test, button_save, button_curLoc;
	private EditText edittext_number, edittext_message;
	private ImageButton imageButton_contact;
	private CheckBox checkbox_Sms, checkbox_Loc;
	private String webResult;
	private ProgressDialog proDialog;
	private String curLocation;
	private final String errorLocSer = "Error: Location Services not available!";
	private final String noLocation = "Sorry, can not get your current location...";
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		Log.i(TAG, "onCreateView()");
		
		view = inflater.inflate(R.layout.notification_setting, container, false);
		findViewsById();
        setListener();
        edittext_number.setText(Global.emergencynum);
        edittext_message.setText(Global.emergencymes);
        checkbox_Sms.setChecked(Global.ifSendSms);
        checkbox_Loc.setChecked(Global.ifAppendLoc);
        Global.setFocus(button_save);
        
		return view;
	}
	private void findViewsById() {
		button_test = (Button)view.findViewById(R.id.cancel);
		button_save = (Button)view.findViewById(R.id.save);
		button_curLoc = (Button)view.findViewById(R.id.buttonCurLoc);
		edittext_number = (EditText)view.findViewById(R.id.emergencyNumber);
		edittext_message = (EditText)view.findViewById(R.id.emergencyMsg);
		checkbox_Sms = (CheckBox)view.findViewById(R.id.checkSendMessage);
		checkbox_Loc = (CheckBox)view.findViewById(R.id.checkAppendLocation);
		imageButton_contact = (ImageButton)view.findViewById(R.id.imageButton_contact);
	}
	private void setListener() {
		button_test.setOnClickListener(testListener);
		button_save.setOnClickListener(saveListener);
		button_curLoc.setOnClickListener(curLocListener);
		imageButton_contact.setOnClickListener(contactListener);
	}
	//Select phone number from contact list
	private OnClickListener contactListener = new OnClickListener(){
		public void onClick(View v){
			try{
				startActivityForResult(new Intent(Intent.ACTION_PICK, 
		   				ContactsContract.Contacts.CONTENT_URI), 0);
			}catch(Exception e){
				e.printStackTrace();
				Global.toastMakeText(getActivity(), "Contact App not installed!");
			}
		}
	};
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==0 && resultCode==Activity.RESULT_OK){
			Cursor cursor = getActivity().getContentResolver().query(
					data.getData(), 
					new String[] {ContactsContract.Contacts._ID,
						          ContactsContract.Contacts.HAS_PHONE_NUMBER},
					null, null, null);
			if(cursor.getCount()==1 && cursor.moveToFirst()){
				if(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
						.equalsIgnoreCase("1")){
					Cursor cursorPhone = getActivity().getContentResolver().query
							(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
							 ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + 
									cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)),
							 null, null);
					cursorPhone.moveToFirst();
					String num = cursorPhone.getString
							(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					edittext_number.setText(num.replace(" ", ""));
					cursorPhone.close();
				}else{
					Global.toastMakeText(getActivity(), "No phone number!");
				}
			}else{
				Global.toastMakeText(getActivity(), "Error: Get phone number from contacts!");
			}
			cursor.close();
		}
	}
	//Get current location
	private OnClickListener curLocListener = new OnClickListener(){
		public void onClick(View v){
			if(!MainActivity.ifLCConnected){
				Global.toastMakeText(getActivity(), errorLocSer);
				return;
			}
			curLocation = "";
			proDialog = ProgressDialog.show(getActivity(), "Getting current location...", "", true,false);
			new Thread(){
				public void run(){
					curLocation = noLocation;
					proDialog.dismiss();
					handler.sendEmptyMessage(2);
				}
			}.start();
		}
	};
	//Send testing SMS
	private OnClickListener testListener = new OnClickListener(){
    	public void onClick(View v){
    		final String num = edittext_number.getText().toString(),
    			         mes = edittext_message.getText().toString();
    		if(num.length() > 15 || num.length() < 10){
    			Global.toastMakeText(getActivity(), "Length of Emergency Number should be 10-15!");
    			return;
    		}
    		if(mes.length() > 100){
    			Global.toastMakeText(getActivity(), "Length of Message should be 0-100!");
    			return;
    		}
    		if(Global.ifAppendLoc && !MainActivity.ifLCConnected){
    			Global.toastMakeText(getActivity(), errorLocSer);
    			return;
    		}
    		Builder builder = new Builder(getActivity());
        	builder.setTitle(getResources().getString(R.string.noti_sendmestest))
        	       .setIcon(R.drawable.sms_64)
        	       .setMessage(getResources().getString(R.string.noti_wanttosend))
        	       .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener(){
        	    	   public void onClick(DialogInterface dialog, int which){
        	    		   dialog.dismiss();
        	    		   if(!Global.ifAppendLoc){
        	    			   Global.sendSMS(num, mes);
        	    			   handler.sendEmptyMessage(3);
        	    		   }else{
        	    			   curLocation = "";
        	    			   proDialog = ProgressDialog.show(getActivity(), "Getting current location...", "", true,false);
        	    			   new Thread(){
        	    				   public void run(){
        	    					   curLocation = noLocation;
        	    					   proDialog.dismiss();
        	    					   Global.sendSMS(num, mes + " My current location: " + curLocation);
        	    					   handler.sendEmptyMessage(3);
        	    				   }
        	    			   }.start();
        	    		   }
        	    	   }
        	       })
        	       .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener(){
        	    	   public void onClick(DialogInterface dialog, int which){
        	    		   dialog.dismiss();
        	    	   }
        	       })
    		       .create().show();
    	}
    };
    //Save notification settings in cloud and locally
    private OnClickListener saveListener = new OnClickListener(){
    	public void onClick(View v){
    		Global.emergencynum = edittext_number.getText().toString();
    		Global.emergencymes = edittext_message.getText().toString();
    		Global.ifSendSms = checkbox_Sms.isChecked();
    		Global.ifAppendLoc = checkbox_Loc.isChecked();
    		if(Global.ifSendSms){
    			if(Global.emergencynum.length()>15 || Global.emergencynum.length()<10){
        			Global.toastMakeText(getActivity(), "Length of Emergency Number should be 10-15!");
        			return;
        		}
        		if(Global.emergencymes.length() > 100){
        			Global.toastMakeText(getActivity(), "Length of Message should be 0-100!");
        			return;
        		}
    		}
    		if(Global.ifAppendLoc && !MainActivity.ifLCConnected){
    			Global.toastMakeText(getActivity(), "Unable to append your location:\nLocation Services not available!");
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
    	                paraOut.put("ifSendSms", Global.ifSendSms);
    	                paraOut.put("ifAppendLoc", Global.ifAppendLoc);
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
    						  .setIcon(R.drawable.location_64)
    						  .setMessage(curLocation);
    			if(curLocation.equals(noLocation)){
    				builderDevInfo.setNeutralButton(getResources().getString(R.string.back), new DialogInterface.OnClickListener(){
    							      public void onClick(DialogInterface dialog, int which) {
    							    	  dialog.dismiss();
    							      }
    							  });
    			}else{
    				builderDevInfo.setPositiveButton(getResources().getString(R.string.noti_showinmap), new DialogInterface.OnClickListener(){
    							      public void onClick(DialogInterface dialog, int which) {
    							    	  dialog.dismiss();
    							  		  startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
    							  		      ("geo:" + curLocation.substring(curLocation.lastIndexOf("(") + 1, curLocation.length()-1))));
    							  	  }
    							  })
    							  .setNegativeButton(getResources().getString(R.string.back), new DialogInterface.OnClickListener(){
    								  public void onClick(DialogInterface dialog, int which) {
    									  dialog.dismiss();
    								  }
    							  });
    			}
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