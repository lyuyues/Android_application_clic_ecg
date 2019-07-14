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
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;

/**
 * This Fragment allows user to change general settings
 */
@SuppressLint("HandlerLeak")
public class GeneralSettingFragment extends Fragment {
	private final String TAG = "GeneralSettingFragment";
	private View view;
	private Switch switch_cs, switch_rotate, switch_bt, switch_wifi;
	private Button button_location, button_wifi, button_lowBpm, button_highBpm,
				   button_saveInCloud;
	private EditText editText_lowBpm, editText_highBpm;
	private CheckBox checkBox_turnOffBt;
	private WifiManager wifiManager;
	private ProgressDialog proDialog;
	private String webResult;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		Log.i(TAG, "onCreateView()");
		
		view = inflater.inflate(R.layout.general_setting, container, false);
		findViewsById();
		//checkBox_AutoSave.setClickable(!Global.ifSaving);
		setListener();
		switch_bt.setChecked(Settings.System.getInt
				(getActivity().getContentResolver(), Settings.Global.BLUETOOTH_ON, 0)
				==1 ? true : false);
		wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        switch_cs.setChecked(Global.ifCsMode);
        checkBox_turnOffBt.setChecked(Global.ifTurnOffBt);
        //checkBox_AutoSave.setChecked(Global.ifAutoSave);
        if(!Global.ifRegUser) button_saveInCloud.setVisibility(View.GONE);
        
        IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
        
		return view;
	}
	private void findViewsById() {
		switch_cs = (Switch)view.findViewById(R.id.switch_cs);
		switch_rotate = (Switch)view.findViewById(R.id.switch_rotate);
		switch_wifi = (Switch)view.findViewById(R.id.switch_wifi);
		switch_bt = (Switch)view.findViewById(R.id.switch_bt);
		button_location = (Button)view.findViewById(R.id.button_location);
		button_wifi = (Button)view.findViewById(R.id.button_wifi);
		checkBox_turnOffBt = (CheckBox)view.findViewById(R.id.checkBox_CloseBt);
		//checkBox_AutoSave = (CheckBox)view.findViewById(R.id.checkBox_AutoSave);
		//editText_saveLen = (EditText)view.findViewById(R.id.editText_saveLen);
		//button_saveLen = (Button)view.findViewById(R.id.button_saveLen);
		editText_lowBpm = (EditText)view.findViewById(R.id.editText_lowBpm);
		editText_highBpm = (EditText)view.findViewById(R.id.editText_highBpm);
		button_lowBpm = (Button)view.findViewById(R.id.button_lowBpm);
		button_highBpm = (Button)view.findViewById(R.id.button_highBpm);
		button_saveInCloud = (Button)view.findViewById(R.id.button_saveInCloud);
	}
	private void setListener() {
		switch_cs.setOnClickListener(csListener);
		switch_rotate.setOnClickListener(rotateListener);
		switch_wifi.setOnClickListener(wifiListener);
		switch_bt.setOnClickListener(btListener);
		button_location.setOnClickListener(locationListener);
		button_wifi.setOnClickListener(wifiButtonListener);
		checkBox_turnOffBt.setOnClickListener(turnOffBtListener);
		//checkBox_AutoSave.setOnClickListener(autoSaveListener);
		//button_saveLen.setOnClickListener(saveLenListener);
		button_lowBpm.setOnClickListener(lowBpmListener);
		button_highBpm.setOnClickListener(highBpmListener);
		button_saveInCloud.setOnClickListener(saveInCloudListener);
	}
	//Change system mode, Compressed Sensing or Normal
	private OnClickListener csListener = new OnClickListener(){
    	public void onClick(View v){
    		if(BleService.ConState != BleService.ConState_Connected){
    			Builder builder=new Builder(getActivity());
            	builder.setTitle("System Mode")
            	       .setIcon(R.drawable.system_64)
            	       .setMessage("Want to change system mode?\n(Make sure sensor supports)")
            	       .setCancelable(false)
            	       .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener(){
            	    	   	public void onClick(DialogInterface dialog, int which){
            	    	   		dialog.dismiss();
            	    			Global.ifCsMode = switch_cs.isChecked();
            	    			BleService.ifDraw = !Global.ifCsMode;
            	    			Global.toastMakeText(getActivity(), "System mode changed!");
            	    	   	}
            	       })
            	       .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener(){
            	    	   public void onClick(DialogInterface dialog, int which){
            	    		   dialog.dismiss();
            	    		   switch_cs.setChecked(!switch_cs.isChecked());
            	    	   }
            	       })
        		       .create().show();
    		}
    		else{
    			switch_cs.setChecked(!switch_cs.isChecked());
    			Global.toastMakeText(getActivity(), "Mode can't be changed when connected!");
    		}
    	}
    };
    //Change screen rotation setting
    private OnClickListener rotateListener = new OnClickListener(){
    	public void onClick(View v){
    		int flag = switch_rotate.isChecked() ? 1 : 0;
    		Settings.System.putInt
    			(getActivity().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, flag);
    	}
    };
    //Switch Wifi
    private OnClickListener wifiListener = new OnClickListener(){
    	public void onClick(View v){
    		wifiManager.setWifiEnabled(switch_wifi.isChecked());
    	}
    };
    //Switch Bluetooth
    private OnClickListener btListener = new OnClickListener(){
    	public void onClick(View v){
    		if(!switch_bt.isChecked()){
    			if(!MainActivity.mBluetoothAdapter.disable())
    				Global.toastMakeText(getActivity(), "Failure: Turn off bluetooth!");
    		}else{
    			if(!MainActivity.mBluetoothAdapter.enable())
    				Global.toastMakeText(getActivity(), "Failure: Turn on bluetooth!");
    		}
    	}
    };
    //Open location setting
    private OnClickListener locationListener = new OnClickListener(){
    	public void onClick(View v){
    		startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    	}
    };
    //Open Wifi setting
    private OnClickListener wifiButtonListener = new OnClickListener(){
    	public void onClick(View v){
    		startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    	}
    };
    //Set if turn off Bluetooth after leaving app
    private OnClickListener turnOffBtListener = new OnClickListener(){
    	public void onClick(View v){
    		Global.ifTurnOffBt = checkBox_turnOffBt.isChecked();
    	}
    };
    /*private OnClickListener autoSaveListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Global.ifAutoSave=checkBox_AutoSave.isChecked();
		}
    
    };*/
    //Set saving file length
   /* private OnClickListener saveLenListener = new OnClickListener(){
    	public void onClick(View v){
    		int length = Integer.parseInt(editText_saveLen.getText().toString());
    		if(length>60 || length<1){
    			Global.toastMakeText(getActivity(), "Save length should be 1-60 min!");
    			return;
    		}
    		Global.savingLength = 1000*60*length;
    		Global.toastMakeText(getActivity(), "Save length changed!");
    	}
    };*/
    //Set low Bpm value
    private OnClickListener lowBpmListener = new OnClickListener(){
    	public void onClick(View v){
    		int tmp = Integer.parseInt(editText_lowBpm.getText().toString());
    		if(tmp>100 || tmp<20){
    			Global.toastMakeText(getActivity(), "Low bpm should be 20-100!");
    			return;
    		}
    		Global.lowBpm = tmp;
    		Global.toastMakeText(getActivity(), "Low bpm changed!");
    	}
    };
    //Set high Bpm value
    private OnClickListener highBpmListener = new OnClickListener(){
    	public void onClick(View v){
    		int tmp = Integer.parseInt(editText_highBpm.getText().toString());
    		if(tmp>100 || tmp<20){
    			Global.toastMakeText(getActivity(), "Low bpm should be 20-100!");
    			return;
    		}
    		Global.highBpm = tmp;
    		Global.toastMakeText(getActivity(), "High bpm changed!");
    	}
    };
    //Save setting in cloud
    private OnClickListener saveInCloudListener = new OnClickListener(){
    	public void onClick(View v){
    		if(!Global.isNetworkConn(getActivity())){
    			Global.toastMakeText(getActivity(), getResources().getString(R.string.nointernet));
    			return;
    		}
    		proDialog = ProgressDialog.show(getActivity(), "Uploading settings...", "", true, false);
    	    new Thread(){
    	    	public void run(){
    	    		webResult = getResources().getString(R.string.noresponse);
    	    		String url = Global.WebServiceUrl + "/upgeneralsetting/";
    	        	HttpParams hPara = new BasicHttpParams();
    	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
    	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
    	            HttpClient hClient = new DefaultHttpClient(hPara);
    	            HttpResponse response = null;
    	            HttpPost httpPost = new HttpPost(url);
    	            try {
    	            	JSONObject paraOut = new JSONObject();
    	            	paraOut.put("Dynamic_id", Global.Dynamic_id);
    	            	paraOut.put("ifcsmode", Global.ifCsMode);
    	                paraOut.put("ifturnoffbt", Global.ifTurnOffBt);
    	                paraOut.put("savelength", Global.savingLength);
    	                paraOut.put("lowbpm", Global.lowBpm);
    	                paraOut.put("highbpm", Global.highBpm);
    	                StringEntity se = new StringEntity(paraOut.toString());
    	                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
    	                httpPost.setEntity(se);
    	    			response = hClient.execute(httpPost);
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
    private Handler handler = new Handler(){
    	public void handleMessage(Message msg){
    		Global.toastMakeText(getActivity(), webResult);
    	}
    };
    //Receive Wifi and Bluetooth status changing Broadcast
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
		public void onReceive(Context arg0, Intent intent) {
			if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
				int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 23);
				if(wifiState==WifiManager.WIFI_STATE_ENABLED) switch_wifi.setChecked(true);
				if(wifiState==WifiManager.WIFI_STATE_DISABLED) switch_wifi.setChecked(false);
			}else{
				int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				if(btState==BluetoothAdapter.STATE_ON) switch_bt.setChecked(true);
				if(btState==BluetoothAdapter.STATE_OFF) switch_bt.setChecked(false);
			}
		}
	};
    public void onResume (){
    	super.onResume();
    	Log.i(TAG, "onResume()");
    	
    	int flag = Settings.System.getInt
				(getActivity().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
		switch_rotate.setChecked(flag==1 ? true : false);
		//editText_saveLen.setText(String.valueOf(Global.savingLength/1000/60));
		editText_lowBpm.setText(String.valueOf(Global.lowBpm));
		editText_highBpm.setText(String.valueOf(Global.highBpm));
		//checkBox_AutoSave.setClickable(!Global.ifSaving);
		
    }
    public void onPause (){
    	super.onPause();
    	Log.i(TAG,"onPause()");
    }
    public void onDestroy (){
		super.onDestroy();
		Log.i(TAG, "onDestroy()");
		
		getActivity().unregisterReceiver(broadcastReceiver);
	}
}