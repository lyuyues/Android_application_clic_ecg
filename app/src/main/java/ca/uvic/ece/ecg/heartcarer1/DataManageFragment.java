package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

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
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * This Fragment allows user to search history ECG data by type and date
 */
@SuppressLint("HandlerLeak")
public class DataManageFragment extends Fragment {
	private final String TAG = "DataManageFragment";
	private View view;
	private Spinner spinner_dataMode;
	private TextView textview_start, textview_end;
	private Button button_start, button_end, button_proceed, button_searchAll;
	private ProgressDialog proDialog;
	private String webResult;
	private ArrayList<HashMap<String, String>> ExGroupArray;
	private ArrayList<ArrayList<HashMap<String, String>>> ExChildArray;
	private ArrayList<Integer> ExGroupItemNum;
	
	public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState){
		Log.i(TAG,"onCreateView()");
		setHasOptionsMenu(true);
		
		view=inflater.inflate(R.layout.datamanage_select, container, false);
		findViewsById();
		setListener();
		initDate();
		initSpinner();
        Global.setFocus(button_proceed);
        
		return view;
	}
	private void findViewsById() {
		spinner_dataMode = (Spinner)view.findViewById(R.id.spinner1);
		textview_start = (TextView)view.findViewById(R.id.textView3);
		textview_end = (TextView)view.findViewById(R.id.textView5);
		button_start = (Button)view.findViewById(R.id.button1);
		button_end = (Button)view.findViewById(R.id.button2);
		button_proceed = (Button)view.findViewById(R.id.button3);
		button_searchAll = (Button)view.findViewById(R.id.button4);
	}
	//Initiate Start Date and End Date as today
	private void initDate(){
		Calendar c = Calendar.getInstance();
		String curDate = dateConvert(c.get(Calendar.YEAR), 
				c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		textview_start.setText(curDate);
		textview_end.setText(curDate);
	}
	//Initiate Spinner according to whether it's Compressed Sensing mode
	private void initSpinner() {
		ArrayAdapter<String> arrayAdapterSpinner = new ArrayAdapter<String>(getActivity(), 
				android.R.layout.simple_spinner_item, new ArrayList<String>());
		arrayAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		if(Global.ifCsMode){
			arrayAdapterSpinner.add("Compressed Sensing");
			arrayAdapterSpinner.add("Normal");
		}else{
			arrayAdapterSpinner.add("Normal");
			arrayAdapterSpinner.add("Compressed Sensing");
		}
		arrayAdapterSpinner.add("Both");
		spinner_dataMode.setAdapter(arrayAdapterSpinner);
	}
	private void setListener() {
		button_start.setOnClickListener(startDateListener);
		button_end.setOnClickListener(endDateListener);
		button_proceed.setOnClickListener(proceedListener);
		button_searchAll.setOnClickListener(searchAllListener);
	}
	//Show DatePickerFragment to allow user to select start date
	private OnClickListener startDateListener = new OnClickListener(){
		public void onClick(View v) {
			DatePickerFragment temp = new DatePickerFragment();
			temp.setCancelable(false);
			temp.show(getFragmentManager(), "startDatePicker");
		}
	};
	//Show DatePickerFragment to allow user to select end date
	private OnClickListener endDateListener = new OnClickListener(){
		public void onClick(View v) {
			DatePickerFragment temp = new DatePickerFragment();
			temp.setCancelable(false);
			temp.show(getFragmentManager(), "endDatePicker");
		}
	};
	//Call getDataList method using selected Start Date and End Date
	private OnClickListener proceedListener = new OnClickListener(){
	   	public void onClick(View v){
	   		getDataList(false);
	   	}
	};
	//Call getDataList method for all days
	private OnClickListener searchAllListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			startActivity(new Intent(getActivity(), Data_statistic.class));
		}
		/*public void onClick(View v) {
			getDataList(true);
		}*/
		
	};
	//Connect Web Service to get history ECG data list
	private void getDataList(final boolean ifSearchAll){
		if(!Global.isNetworkConn(getActivity())){
			Global.toastMakeText(getActivity(), getResources().getString(R.string.nointernet));
			return;
		}
   		final String temp1 = textview_start.getText().toString(),
   				temp2 = textview_end.getText().toString();
   		if(temp1.compareTo(temp2) > 0){
   			Global.toastMakeText(getActivity(), "Start date should before end date!");
			return;
   		}
   		final String startdate = temp1 + "_00-00-00", enddate = temp2 + "_23-59-59";
   		proDialog = ProgressDialog.show(getActivity(), "Getting history data list...", "", true,false);
	    new Thread(){
	    	public void run(){
	    		webResult = getResources().getString(R.string.noresponse);
	    		String url = Global.WebServiceUrl + "/gettestlist/";
	    		JSONArray jsa = null;
	        	HttpParams hPara = new BasicHttpParams();
	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
	            HttpClient hClient = new DefaultHttpClient(hPara);
	            HttpResponse response = null;
	            HttpPost httppost = new HttpPost(url);
	            try {
	            	JSONObject paraOut = new JSONObject();
	            	paraOut.put("Dynamic_id", Global.Dynamic_id);
	            	paraOut.put("startdate", startdate);
	                paraOut.put("enddate", enddate);
	                paraOut.put("ifSearchAll", ifSearchAll);
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
	                	webResult = total.toString();
	                	jsa = new JSONArray(webResult);
	                	webResult=jsa.getString(0);
	                }
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
            	proDialog.dismiss();
            	handler.sendEmptyMessage(0);
            	if(webResult.equals(getResources().getString(R.string.nointernet)) || jsa==null){
            		return;
            	}
            	if(jsa.length() > 1){
            		int len = (jsa.length()-1)/2;
                    ExGroupArray = new ArrayList<HashMap<String, String>>();
                    ExGroupItemNum = new ArrayList<Integer>();
                    ExChildArray = new ArrayList<ArrayList<HashMap<String, String>>>();
            		for(int i=0; i<len; i++){
            			try {
            				int index;
            				String tempStartTime = jsa.getString(i*2+1);
            				String tempDate = tempStartTime.substring(0, 10);
            				String temptime = tempStartTime.substring(11);
							HashMap<String, String> temp1 = new HashMap<String, String>();
							temp1.put("Date", tempDate);
							HashMap<String, String> temp2 = new HashMap<String, String>();
							temp2.put("starttime", temptime);
							temp2.put("data_num", String.valueOf(jsa.getInt(i*2+2)));
							if(ExGroupArray.contains(temp1)){
								index = ExGroupArray.indexOf(temp1);
								ExGroupItemNum.set(index, ExGroupItemNum.get(index)+1);
								ExChildArray.get(index).add(temp2);
							}else{
								ExGroupArray.add(temp1);
								ExGroupItemNum.add(1);
								index = ExGroupItemNum.size()-1;
								ArrayList<HashMap<String, String>> temp3 = 
										new ArrayList<HashMap<String, String>>();
								temp3.add(temp2);
								ExChildArray.add(temp3);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
            		}
            		handler.sendEmptyMessage(1);
            	}
	    	}
	    }.start();
	}
	private Handler handler = new Handler(){
    	public void handleMessage(Message msg){
    		int i = msg.what;
    		if(i==0){
    			Global.toastMakeText(getActivity(), webResult);
    		}else if(i==1){
    			Intent intent= new Intent(getActivity(), DataManageList.class);
    			intent.putExtra("ExGroupArray", ExGroupArray);
    			intent.putExtra("ExChildArray", ExChildArray);
        		startActivity(intent);
    		}
    	}
    };
    //Convert history data length in Integer to String
	private String convertLength(int length) {
		String result;
		if(length < 60){
			result = String.valueOf(length) + "s";
		}else if(length < 3600){
			int s, m;
			m = length/60;
			s = length - m*60;
			result = String.valueOf(m) + "m " + String.valueOf(s) + "s";
		}else{
			int s, m, h;
			h = length / 3600;
			m = (length-h*3600)/60;
			s = length-h*3600-m*60;
			result = String.valueOf(h)+"h "+String.valueOf(m)+"m "+String.valueOf(s)+"s";
		}
		return result;
	}
	//Convert Year, Month and Day to String format
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
	//Parse Year, Month and Day from String
	private int parseDate(String temp, int what){
		if(what==1){
			return Integer.valueOf(temp.substring(0, 4));
		}else if(what==2){
			return Integer.valueOf(temp.substring(5, 7));
		}else{
			return Integer.valueOf(temp.substring(8, 10));
		}
	}
	@SuppressLint("ValidFragment")
	private class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener{
		//Called when creating DatePickerDialog
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
        	String temp;
        	if(getTag().equals("startDatePicker")){
        		temp = textview_start.getText().toString();
        	}else{
        		temp = textview_end.getText().toString();
        	}
        	DatePickerDialog dpd = new DatePickerDialog(getActivity(), this, parseDate(temp, 1),
        			parseDate(temp, 2) - 1, parseDate(temp, 3));
        	if(getTag().equals("startDatePicker")){
        		dpd.setTitle("Start Date");
        	}else{
        		dpd.setTitle("End Date");
        	}
			return dpd;
		}
		//Called when date is selected by user
        public void onDateSet(DatePicker view, int year, int month, int day) {
        	String temp = dateConvert(year, month, day);
        	if(getTag().equals("startDatePicker")){
        		textview_start.setText(temp);
        	}else{
        		textview_end.setText(temp);
        	}
		}
    }
	public void onCreateOptionsMenu (Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.upload, menu);
	}
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.upload:
	        	menuUpload();
	            return true;
	    }
	    return false;
	}
	//Manually upload saved ECG data
	private void menuUpload() {
		if(Global.isWifiConn(getActivity())){
			if((!Global.ifUploading)){
				Intent serintent = new Intent(getActivity(), UpdataService.class);
				getActivity().startService(serintent);
				Global.toastMakeText(getActivity(), "Start uploading!");
			}else{
				Global.toastMakeText(getActivity(), "Upload already in progress!");
			}
		}else{
			Global.toastMakeText(getActivity(), "Wifi is not available!");
		}
	}
	public void onResume (){
    	super.onResume();
    	Log.i(TAG,"onResume()");
    }
    public void onPause (){
    	super.onPause();
    	Log.i(TAG,"onPause()");
    }
    public void onDestroy (){
		super.onDestroy();
		Log.i(TAG,"onDestroy()");
	}
}