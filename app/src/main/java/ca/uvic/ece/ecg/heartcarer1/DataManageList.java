package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
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
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This Activity shows history ECG data list returned from Web Service
 */
@SuppressLint("HandlerLeak")
public class DataManageList extends ExpandableListActivity {
	private final String TAG = "DataManageList";
	private ProgressDialog proDialog;
	private String webresult;
	private String temp_testtime_stand;
	private String temp_testtime;
	private boolean temp_ifcs;
	private int tempGroupPosition;
	private int tempChildPosition;
	private ArrayList<HashMap<String, String>> ExGroupArray;
	private ArrayList<ArrayList<HashMap<String, String>>> ExChildArray;
	private MyExpandableListAdapter mExLA;
	private ArrayList<String> starttime=new ArrayList<String>();
	private ArrayList<Integer> length=new ArrayList<Integer>();
	private String starttime_toshow;
	
	
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG,"onCreate()");
	    setContentView(R.layout.datamanage_list);
	    setTitle("History Data List");
	    getActionBar().setIcon(R.drawable.list_128);
	    getActionBar().setDisplayHomeAsUpEnabled(true);
        
	    ExGroupArray = (ArrayList<HashMap<String, String>>) getIntent().
	    		getSerializableExtra("ExGroupArray");
	    ExChildArray = (ArrayList<ArrayList<HashMap<String, String>>>) getIntent().
	    		getSerializableExtra("ExChildArray");
        mExLA = new MyExpandableListAdapter(ExGroupArray, ExChildArray);
        setListAdapter(mExLA);
        registerForContextMenu(getExpandableListView());
        ExpandableListView tempExLV = getExpandableListView();
        tempExLV.setOnChildClickListener(mExChildClickListner);
        tempExLV.setLongClickable(false);
	}
	private final OnChildClickListener mExChildClickListner = new OnChildClickListener(){
		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			tempGroupPosition = groupPosition;
			tempChildPosition = childPosition;
			String tmp = ExChildArray.get(groupPosition).get(childPosition).get("starttime");
			temp_testtime_stand = ExGroupArray.get(groupPosition).get("Date") + " " + 
					tmp.substring(0, 8);
	    	temp_testtime = starttimeConvert(temp_testtime_stand);
			//v.showContextMenu();
	    	DownLoadDataList();
			return true;
		}
	};
	/*@Override
  	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Options");
        menu.add(0, 1, 1, "Show graph (first 60 seconds)");
        menu.add(0, 2, 2, "Delete");
    }
	@Override
  	public boolean onContextItemSelected(MenuItem item) {
  	    switch(item.getItemId()) {
  	    case 1:
  	    	if(findFile(temp_starttime)){
  	    		ShowHistoryGraph();
  	    	}else{
  	    		DownloadData();
  	    	}
  	    	return true;
  	    case 2:
  	    	initDeleteData();
  	    	return true;
  	    }
  	    return false;
  	}*/
  	//Start DataManagePlot Activity to show history ECG signal
	private void ShowHistoryGraph() {
		Intent intent = new Intent(DataManageList.this, DataManagePlot.class);
		intent.putExtra("fileName", downloadFileName(starttime_toshow, temp_ifcs));
		intent.putExtra("ifcs", temp_ifcs);
		intent.putExtra("temp_starttime_stand",starttime_toshow);
		startActivity(intent);
	}
	private void DownLoadDataList(){
    	if(!Global.isNetworkConn(DataManageList.this)){
			Global.toastMakeText(DataManageList.this, getResources().getString(R.string.nointernet));
			return;
		}
    	proDialog = ProgressDialog.show(DataManageList.this, "Downloading...", "", true, false);
    	new Thread(){
	    	public void run(){
    		webresult = getResources().getString(R.string.noresponse);
    		String url = Global.WebServiceUrl + "/getdatalist/";
        	HttpParams hPara = new BasicHttpParams();
    		HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	        HttpConnectionParams.setSoTimeout(hPara, /*Global.socketTimeout*/0);
	        HttpClient hClient = new DefaultHttpClient(hPara);
	        HttpResponse response = null;
	        HttpPost httpPost = new HttpPost(url);
	        try {
	        	JSONObject paraOut = new JSONObject();
            	paraOut.put("Dynamic_id", Global.Dynamic_id);
            	paraOut.put("testtime", temp_testtime);
                StringEntity se = new StringEntity(paraOut.toString());
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setEntity(se);
    			response = hClient.execute(httpPost);
                if(response != null){
                	StringBuilder total = new StringBuilder();
                	BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                	String line;
                	while((line = rd.readLine())!= null){
                        total.append(line);
                    }
                	JSONObject jso = new JSONObject(total.toString());
                	if(jso.getString("result").equals("Success")){
                		starttime.clear();
                		length.clear();
                		int i=0;
                		while(jso.has("starttime"+i) && jso.has("length"+i)){
                				starttime.add(jso.getString("starttime"+i));
                				length.add(jso.getInt("length"+i));
                				i++;
                			}
                		}
                	}
                }	
                
	        catch (Exception e) {
	    		e.printStackTrace();
	        }
	        proDialog.dismiss();
	        ShowData.sendEmptyMessage(0);
            }
	        }.start();
	}
	
	private Handler ShowData = new Handler(){
    	public void handleMessage(Message msg){
    		int i = msg.what;
    		if(i==0){
    		AlertDialog.Builder builder = new AlertDialog.Builder(DataManageList.this);   
	        builder.setTitle("ECG data list");  
	        final String[] mItem = (String[])starttime.toArray(new String[0]);
	        builder.setItems(mItem, new DialogInterface.OnClickListener() {  
	            public void onClick(DialogInterface dialog, int which) {  
	        		AlertDialog.Builder builder = new AlertDialog.Builder(DataManageList.this);   
	    	        builder.setTitle("Options");  
	    	        starttime_toshow=mItem[which];
	    	        final String[] mItems = {"Show graph", "Delete"};
	    	        builder.setItems(mItems, new DialogInterface.OnClickListener() {  
	    	            public void onClick(DialogInterface dialog, int which) {  
	    	            	switch (which){
	    	            	 case 0:
	 	    	      	    	if(findFile(starttime_toshow)){
	 	    	      	    		ShowHistoryGraph();
	 	    	      	    	}else{
	 	    	      	    		DownloadData();
	 	    	      	    	}
	 	    	      	    	break;
	    	            	 case 1:
	    	            		 initDeleteData();
	    	            		}
	    	            	}
	    	        });
	    	        builder.create().show();  
	    			//Global.toastMakeText(DataManageList.this,  mItems[which]);  
	            }  
	        });  
	        builder.create().show();  
    	}
    		if(i==1){
    			ShowHistoryGraph();
    			}
    		}
	};
	
	  
	
	//Download selected history ECG data from Web Server
    private void DownloadData() {
    	if(!Global.isNetworkConn(DataManageList.this)){
			Global.toastMakeText(DataManageList.this, getResources().getString(R.string.nointernet));
			return;
		}
    	proDialog = ProgressDialog.show(DataManageList.this, "Downloading...", "", true, false);
	    new Thread(){
	    	public void run(){
	    		webresult = getResources().getString(R.string.noresponse);
	    		String url = Global.WebServiceUrl + "/downdata_V2/";
	        	HttpParams hPara = new BasicHttpParams();
	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	            HttpConnectionParams.setSoTimeout(hPara,0);
	            HttpClient hClient = new DefaultHttpClient(hPara);
	            HttpResponse response = null;
	            HttpPost httpPost = new HttpPost(url);
	            try {
	            	JSONObject paraOut = new JSONObject();
	            	paraOut.put("Dynamic_id", Global.Dynamic_id);
	            	paraOut.put("starttime", starttime_toshow);
	                StringEntity se = new StringEntity(paraOut.toString());
	                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
	                httpPost.setEntity(se);
	    			response = hClient.execute(httpPost);
	                if(response != null){
	                	StringBuilder total = new StringBuilder();
	                	BufferedReader rd = new BufferedReader
	                			(new InputStreamReader(response.getEntity().getContent()));
	                	String line;
	                	while((line = rd.readLine()) != null){
	                        total.append(line);
	                    }
	                	JSONObject jso = new JSONObject(total.toString());
	                	webresult = jso.getString("result");
	                	if(webresult.equals("Success: Downdata!")){
	                		byte[] jcontent = Base64.decode(jso.getString("content"), Base64.DEFAULT);
	                		OutputStream opstream = new BufferedOutputStream(new FileOutputStream
	                				(downloadFileName(starttime_toshow, temp_ifcs)));
	                		opstream.write(jcontent);
	                		opstream.close();
	                		webresult = "Success: Down and save data!";
	                	}
	                }
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
            	proDialog.dismiss();
            	if(webresult.equals("Success: Down and save data!")){
            		ShowData.sendEmptyMessage(1);

            	}
	    	}
	    }.start();
	}
	
    //Show AlertDialog asking user if want to delete
	private void initDeleteData() {
		Builder builder = new Builder(DataManageList.this);
    	builder.setTitle("Delete data")
    	       .setIcon(R.drawable.ic_action_discard)
    	       .setMessage("Want to delete selected data?")
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
    		public void onClick(DialogInterface dialog,int which){
    			dialog.dismiss();
    			deleteData();
    		}
    	})
    	       .setNegativeButton("No", new DialogInterface.OnClickListener(){
    		public void onClick(DialogInterface dialog,int which){
    			dialog.dismiss();
    		}
    	})
		       .create().show();
	}
	
	//Delete selected ECG data both on Server and locally
    private void deleteData() {
    	if(!Global.isNetworkConn(DataManageList.this)){
			Global.toastMakeText(DataManageList.this, getResources().getString(R.string.nointernet));
			return;
		}
    	proDialog = ProgressDialog.show
    			(DataManageList.this, "Deleting selected item...", "", true, false);
	    new Thread(){
	    	public void run(){
	    		webresult = getResources().getString(R.string.noresponse);
	    		String url = Global.WebServiceUrl + "/deletedata/";
	        	HttpParams hPara = new BasicHttpParams();
	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
	            HttpClient hClient = new DefaultHttpClient(hPara);
	            HttpResponse response = null;
	            HttpPost httppost = new HttpPost(url);
	            try {
	            	JSONObject paraOut = new JSONObject();
	            	paraOut.put("Dynamic_id", Global.Dynamic_id);
	            	paraOut.put("userid", Global.userid);
	            	paraOut.put("starttime",starttime_toshow);
	                StringEntity se = new StringEntity(paraOut.toString());
	                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
	                httppost.setEntity(se);
	    			response = hClient.execute(httppost);
	                if(response != null){
	                	StringBuilder total = new StringBuilder();
	                	BufferedReader rd = new BufferedReader
	                			(new InputStreamReader(response.getEntity().getContent()));
	                	String line;
	                	while((line = rd.readLine()) != null){
	                        total.append(line);
	                    }
	                	webresult = total.toString();
	                }
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
            	proDialog.dismiss();
            	//handler.sendEmptyMessage(0);
            	if(webresult.equals("Success: DeleteData!")){
            		//Refresh list
            		//handler.sendEmptyMessage(1);
            		//Delete local data
            		if(findFile(starttime_toshow)){
            			new File(downloadFileName(starttime_toshow, true)).delete();
            		}
            	}
	    	}
	    }.start();
	}
	
	/*
    private Handler handler = new Handler(){
    	public void handleMessage(Message msg){
    		int i = msg.what;
    		if(i==0){
    			Global.toastMakeText(DataManageList.this,webresult);
    		}else if(i==1){
    			ExChildArray.get(tempGroupPosition).remove(tempChildPosition);
    			if(ExChildArray.get(tempGroupPosition).size()==0){
    				ExChildArray.remove(tempGroupPosition);
    				ExGroupArray.remove(tempGroupPosition);
    			}
    			RefreshList();
    		}else if(i==2){
        		RefreshList();
    			ShowHistoryGraph();
    		}
    	}
    };*/
    //Customized ExpandableListAdapter to show history ECG data
	private class MyExpandableListAdapter extends BaseExpandableListAdapter {
		private ArrayList<HashMap<String, String>> groupList;
		private ArrayList<ArrayList<HashMap<String, String>>> childList;
		private LayoutInflater mInflator;
		
		public MyExpandableListAdapter(ArrayList<HashMap<String, String>> arg0,
				ArrayList<ArrayList<HashMap<String, String>>> arg1){
			groupList = arg0;
			childList = arg1;
			mInflator = getLayoutInflater();
		}
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			GroupViewHolder viewHolder;
			if (convertView == null) {
				convertView = mInflator.inflate(android.R.layout.simple_expandable_list_item_2, null);
				viewHolder = new GroupViewHolder();
				viewHolder.text1 = (TextView)convertView.findViewById(android.R.id.text1);
				viewHolder.text2 = (TextView)convertView.findViewById(android.R.id.text2);
  				convertView.setTag(viewHolder);
			}else{
				viewHolder = (GroupViewHolder)convertView.getTag();
			}
			viewHolder.text1.setText(groupList.get(groupPosition).get("Date"));
			viewHolder.text2.setText(String.valueOf(getChildrenCount(groupPosition))+" items");
			return convertView;
		}
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			ChildViewHolder viewHolder;
			if (convertView==null) {
				convertView = mInflator.inflate(R.layout.datamanagelist_item, null);
				viewHolder = new ChildViewHolder();
				viewHolder.image = (ImageView)convertView.findViewById(R.id.imageView1);
				viewHolder.text1 = (TextView)convertView.findViewById(R.id.textView1);
				viewHolder.text2 = (TextView)convertView.findViewById(R.id.textView2);
  				convertView.setTag(viewHolder);
			}else{
				viewHolder=(ChildViewHolder)convertView.getTag();
			}
			HashMap<String, String> tempMap = childList.get(groupPosition).get(childPosition);
			String tempStarttime = tempMap.get("starttime");
			viewHolder.text1.setText(tempStarttime);
			viewHolder.text2.setText(tempMap.get("data_num")+" items");
			viewHolder.image.setBackgroundResource(R.drawable.document);
			/*if(findFile(starttimeConvert(
					groupList.get(groupPosition).get("Date") + " " + tempStarttime.substring(0, 8))))
				viewHolder.image.setBackgroundResource(R.drawable.downloaddone_64);
  			else
  				viewHolder.image.setBackgroundResource(R.drawable.needdownload_64);*/
			return convertView; 
		}
		private class ChildViewHolder{
  			private ImageView image;
  			private TextView text1;
  			private TextView text2;
  		}
		private class GroupViewHolder{
  			private TextView text1;
  			private TextView text2;
  		}
		public Object getChild(int groupPosition, int childPosition) {
			return childList.get(groupPosition).get(childPosition);
		}
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
		public int getChildrenCount(int groupPosition) {
			return childList.get(groupPosition).size();
		}
		public Object getGroup(int groupPosition) {
			return groupList.get(groupPosition);
		}
		public int getGroupCount() {
			return groupList.size();
		}
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
		public boolean hasStableIds() {
			return true;
		}
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}
	//Refresh history data list
	private void RefreshList(){
		mExLA.notifyDataSetChanged();
	}
	//Return if the file exists locally
	private boolean findFile(final String tempFileName) {
		File folder = new File(Global.downloadPath);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.startsWith(tempFileName + Global.DlFilenameSuffix);
			}
		};
		return folder.listFiles(filter).length==1 ? true : false;
	}
	//Convert starttime from standard format to our format
	private String starttimeConvert(String standardTime) {
		return standardTime.replace(":", "-").replace(" ", "_");
	}
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.datamanagelist, menu);
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.clearcache:
	        	clearCache();
	            return true;
	    }
	    return false;
	}
	//Clear download cache
	private void clearCache() {
		final File tempFile = new File(Global.downloadPath);
		Builder builder=new Builder(DataManageList.this);
    	builder.setTitle("Clear download cache")
    	       .setIcon(R.drawable.ic_action_discard)
    	       .setMessage("Want to clear download cache?\n(" + tempFile.list().length + " files, " + 
    	    		   FileUtils.sizeOfDirectory(tempFile)/1024 + "KB in total)")
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
    	    	   public void onClick(DialogInterface dialog,int which){
    	    		   dialog.dismiss();
    	    		   try {
    	    			   FileUtils.deleteDirectory(tempFile);
    	    			   Global.toastMakeText(DataManageList.this, "Download cache cleared!");
    	    			   tempFile.mkdir();
    	    			   RefreshList();
    	    		   } catch (Exception e) {
    	    			   e.printStackTrace();
    	    			   Global.toastMakeText(DataManageList.this, "Error: Clear download cache!");
    	    		   }
    	    	   }
    	       })
    	       .setNegativeButton("No", new DialogInterface.OnClickListener(){
    	    	   public void onClick(DialogInterface dialog,int which){
    	    		   dialog.dismiss();
    	    	   }
    	       })
    	       .create().show();
	}
	//Generate name of download file
	private String downloadFileName(String temp_starttime, boolean ifcs){
		return Global.downloadPath + "/" + temp_starttime + Global.DlFilenameSuffix + 
				(ifcs ? "_cs" : "") + ".bin";
	}
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