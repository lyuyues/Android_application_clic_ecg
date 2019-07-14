package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

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

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DoctorNotes extends ListActivity{
	private ProgressDialog proDialog;
	ArrayList<String> starttime =new ArrayList<String>();
	MyAdapter adapter;
    ArrayList<String> comment_list;
    ArrayList<String> time_list;
    String webresult;
    @SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
    	   super.onCreate(savedInstanceState);
    	   	getActionBar().setDisplayHomeAsUpEnabled(true);
   	    	getActionBar().setIcon(R.drawable.doctor);
   	    	Intent intent = this.getIntent();
    	   	int position = intent.getIntExtra("position", 0);
    	   	comment_list = (ArrayList<String>)intent.getSerializableExtra("comment_list");
    	   	starttime = (ArrayList<String>)intent.getSerializableExtra("starttime");
    	   	time_list = (ArrayList<String>)intent.getSerializableExtra("time_list");
    	    //DownNotes(position);
    	   	//setContentView(R.layout.doctorlist);
    	    setTitle(Global.doctor_name[position]+"'s Comment");
    	    adapter =new MyAdapter(this, comment_list,time_list);
    	    setListAdapter(adapter);
    }  
   
    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		String time=starttime.get(position).substring(0, 19);
		if(findFile(time)){
	    		ShowHistoryGraph(time,position);
	    	}else{
	    		DownloadData(time);
	    	}
	}
	
	private void DownloadData(final String time) {
    	if(!Global.isNetworkConn(DoctorNotes.this)){
			Global.toastMakeText(DoctorNotes.this, getResources().getString(R.string.nointernet));
			return;
		}
    	proDialog = ProgressDialog.show(DoctorNotes.this, "Downloading...", "", true, false);
	    new Thread(){
	    	public void run(){
	    		webresult = getResources().getString(R.string.noresponse);
	    		String url = Global.WebServiceUrl + "/downdata_V2/";
	        	HttpParams hPara = new BasicHttpParams();
	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	            HttpConnectionParams.setSoTimeout(hPara, /*Global.socketTimeout*/0);
	            HttpClient hClient = new DefaultHttpClient(hPara);
	            HttpResponse response = null;
	            HttpPost httpPost = new HttpPost(url);
	            try {
	            	JSONObject paraOut = new JSONObject();
	            	paraOut.put("Dynamic_id", Global.Dynamic_id);
	            	paraOut.put("starttime", time);
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
	                				(downloadFileName(time, false)));
	                		opstream.write(jcontent);
	                		opstream.close();
	                		webresult = "Success: Down and save data!";
	                	}
	                }
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
            	if(webresult.equals("Success: Down and save data!")){
            	}
            	proDialog.dismiss();
	    	}
	    }.start();
	}
	private void ShowHistoryGraph(String time,int position) {
		Intent intent = new Intent(DoctorNotes.this, DoctorNotePlot.class);
		intent.putExtra("fileName", downloadFileName(time, false));
		intent.putExtra("ifcs", false);
	    intent.putExtra("temp_starttime_stand", time);
	    intent.putExtra("comment", comment_list.get(position));
	    intent.putExtra("doctor_name",Global.doctor_name[position]);
		startActivity(intent);
	}
	private String downloadFileName(String temp_starttime, boolean ifcs){
		return Global.downloadPath + "/" + temp_starttime + Global.DlFilenameSuffix + 
				(ifcs ? "_cs" : "") + ".bin";
	}
	private boolean findFile(final String tempFileName) {
		File folder = new File(Global.downloadPath);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.startsWith(tempFileName + Global.DlFilenameSuffix);
			}
		};
		return folder.listFiles(filter).length==1 ? true : false;
	}

	class MyAdapter extends BaseAdapter{
		  private ArrayList<String> comment_list;
		  private ArrayList<String> time_list;
		  private Context context;
		  ViewHolder holder = new ViewHolder();
		  class ViewHolder {
			    TextView comment;
			    TextView time;
			  }
		  
		public MyAdapter (Context context,ArrayList<String> comment_list,ArrayList<String> time_list){
			  this.comment_list = comment_list;
			  this.time_list=time_list;
			  this.context = context;
		}
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return comment_list.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			View view;
			 view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2,
			          null); 
			      String s= comment_list.get(position).toString().substring(0, Math.min(20, comment_list.get(position).toString().length()))+"...";
			      holder.comment = (TextView) view.findViewById(android.R.id.text1);
			      holder.comment.setText(s);
			      holder.time=(TextView) view.findViewById(android.R.id.text2);
			      holder.time.setText(time_list.get(position).toString().substring(0,19));
			return view;
		}
	}
	}