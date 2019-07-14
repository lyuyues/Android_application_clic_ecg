package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

import android.R.integer;
import android.view.View.OnClickListener;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DoctorList extends ListFragment {
	private MyAdapter adapter; 
	private ProgressDialog proDialog;
	private ArrayList<String> list = new ArrayList<String>();
	private ArrayList<String> listHospital = new ArrayList<String>();

	private ArrayList<integer> list_id = new ArrayList<integer>();
	final String[] mItems = {"Doctor Profile","Doctor Comment"}; 
	ArrayList<String> starttime =new ArrayList<String>();
	ArrayList<String> comment_list = new ArrayList<String>();
	ArrayList<String> time_list = new ArrayList<String>();
	 
       
	 @Override 
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    refresh();
	    adapter =new MyAdapter(getActivity(), list, listHospital);
	 }
	 

	  @Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup container,
	      Bundle savedInstanceState) {
			setHasOptionsMenu(true);
	    View view = inflater.inflate(R.layout.doctorlist, null);
	    setListAdapter(adapter);
	    return view;
	 }
	  public void onCreateOptionsMenu (Menu menu, MenuInflater inflater){
			inflater.inflate(R.menu.doctlist_refresh, menu);
		}
	  public boolean onOptionsItemSelected(MenuItem item) {
		        	refresh();
		            return true;
		    }
	  public void showInfo(final int position){
	      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());   
	      builder.setItems(mItems, new DialogInterface.OnClickListener() {  
	            public void onClick(DialogInterface dialog, int which) {  
	            	if(which==0){
	            	Toast.makeText(getActivity(), list.get(position), Toast.LENGTH_SHORT).show();}
	            	else{
	            		
	                    DownNotes(position);
	                    
	            	
	            	}
	            }  
	        });  
	        builder.create().show();
	          
	    }
	  
	  private void DownNotes(final int position) {
			// TODO Auto-generated method stub
		  if(!Global.isNetworkConn(getActivity())){
				Global.toastMakeText(getActivity(), getResources().getString(R.string.nointernet));
				return;
			}
			else{
				proDialog = ProgressDialog.show(getActivity(), "Downloading Doctor comments...", "", true, false);
				Thread thread = new Thread(){
			    	public void run(){
			    		String url = Global.WebServiceUrl + "/Read_doccomment/";
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
				        	paraOut.put("doctor_id",Global.doctor_id[position]);
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

				            	JSONArray jso = new JSONArray(total.toString());
				            	String webresult=jso.getString(0);
								comment_list.clear();
						        time_list.clear();
						        starttime.clear();
				            	for(int i=1;i<jso.length();i=i+3){
				            		comment_list.add(jso.getString(i));
				            		time_list.add(jso.getString(i+1));
				            		starttime.add(jso.getString(i+2));
				            	}
				            	Intent mIntent = new Intent(getActivity(),DoctorNotes.class);     
			                    mIntent.putExtra("position",position);  
				            	mIntent.putExtra("comment_list", comment_list);
			                    mIntent.putExtra("time_list", time_list);  
			                    mIntent.putExtra("starttime", starttime);
			                    startActivity(mIntent);  
				            }
				        }
				        catch (Exception e) {
				    		e.printStackTrace();
				    	}
		        proDialog.dismiss();
			}
		};
		thread.start();
	}}
	@Override
	  public void onListItemClick(ListView l, View v, int position, long id) {
	    super.onListItemClick(l, v, position, id);
	    //Toast.makeText(getActivity(), list.get(position), Toast.LENGTH_SHORT).show();
	    showInfo(position);
	  }
	
	private void refresh(){
		if(!Global.isNetworkConn(getActivity())){
			Global.toastMakeText(getActivity(), getResources().getString(R.string.nointernet));
			return;
		}
		proDialog = ProgressDialog.show(getActivity(), "Downloading list...", "", true, false);
	    Thread refresh = new Thread(){
	    	public void run(){
	    		String url = Global.WebServiceUrl + "/get_doclist/";
		    	HttpParams hPara = new BasicHttpParams();
		        HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
		        HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
		        HttpClient hClient = new DefaultHttpClient(hPara);
		        HttpResponse response = null;
		        HttpPost httppost = new HttpPost(url);
		        try {
		        	JSONObject paraOut = new JSONObject();
		            paraOut.put("WHORU", 1);
		            paraOut.put("Dynamic_id", Global.Dynamic_id);
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
		            	int doctor_num = jso.getInt("doctor_num");
		            	int access_request = jso.getInt("access_request");
		                Global.doctor_num=doctor_num;
		                Global.access_request_doc=access_request;
		            	list.clear();
		            	listHospital.clear();
		            	for (int n=0;n<doctor_num;n++){
		        			Global.doctor_name[n]=jso.getString("doctorname"+n);
		        			Global.doctor_id[n]=jso.getInt("doctor_id"+n);
		      			    list.add(Global.doctor_name[n]);
		      			    listHospital.add(jso.getString("hospital"+n));
		        		}
		            	for (int n=0;n<access_request;n++){
		            		Global.access_request_name[n]=jso.getString("doctor_request"+n);
		        			Global.access_request_id[n]=jso.getInt("doctor_request_id"+n);
		            		list.add( Global.access_request_name[n]);
		      			    listHospital.add(jso.getString("hospital"+n));
		        		}		            	
		            }
		        }
		        catch (Exception e) {
		    		e.printStackTrace();
		    	}
		        proDialog.dismiss();
            	handler.sendEmptyMessage(1);
	    	}
	};
	refresh.start();
	}

	 private Handler handler = new Handler(){
	    	public void handleMessage(Message msg){
	    		int i = msg.what;
	    		if(i==1){
	    			adapter.notifyDataSetChanged();
	    		}
	    	}
	    };
	}

class MyAdapter extends BaseAdapter{
	  private ArrayList<String> list;
	  private ArrayList<String> listHospital;
	  private Context context;
	  int n;
		private String webResult;
	    ViewHolder holder = new ViewHolder();
		private Map<Integer,View> rowViews = new HashMap<Integer,View>();


	  
	  public MyAdapter(Context context,  ArrayList<String>  list, ArrayList<String> listHospital) {
	    this.list = list;
	    this.listHospital = listHospital;
	    this.context = context;
	  }

	  @Override
	  public int getCount() {
	    return list.size();
	  }

	  @Override
	  public Object getItem(int position) {
	    return list.get(position);
	  }

	  @Override
	  public long getItemId(int position) {
	    return position;
	  }

	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = rowViews.get(position);
	    View convertView1,convertView2;
     
	  if(position<Global.doctor_num) {
		  convertView2 = rowView; 
		    if (convertView2 == null) {
		      convertView2 = LayoutInflater.from(context).inflate(R.layout.item,
		          null); 
		      holder.textView = (TextView) convertView2
		          .findViewById(R.id.textView);
		      holder.textView.setText(list.get(position).toString());
		      
		      holder.textView = (TextView) convertView2
			          .findViewById(R.id.hospital);
			  holder.textView.setText(listHospital.get(position).toString());
			      
		      rowView=convertView2;
				rowViews.put(position, rowView);
		    } 
		    }
	  else   {
       	convertView1 = rowView;
 		  if (convertView1 == null) {
 		      convertView1 = LayoutInflater.from(context).inflate(R.layout.item2,
 		          null); 
 		      holder.textView = (TextView) convertView1.findViewById(R.id.textView);
 		      holder.button_decline=(Button) convertView1.findViewById(R.id.button_decline);
 		      holder.button_accept=(Button) convertView1.findViewById(R.id.button_accept);
 		      holder.button_decline.setOnClickListener(new decline_listener(position,holder.button_decline,holder.button_accept));
 		      holder.button_accept.setOnClickListener(new decline_listener(position,holder.button_accept,holder.button_decline));
              convertView1.setTag(holder);
              holder.textView.setText(list.get(position).toString());
 		      holder.textView = (TextView) convertView1.findViewById(R.id.hospital);
			  holder.textView.setText(listHospital.get(position).toString());

  			rowViews.put(position, rowView);
              rowView=convertView1;}
 		 
 		    
	  }
        return rowView;
	  }

	  class ViewHolder {
	    TextView textView;
	    Button button_accept;
	    Button button_decline;
	  }

	 class decline_listener implements OnClickListener{
		 private int position;
		 private Button button1;
		 private Button button2;

		 decline_listener(int pos,Button button_first,Button button_second) {
	            position = pos;
	            button1=button_first;
	            button2=button_second;
	        }
		
		 @Override
		 public void onClick(View v) {
			// TODO Auto-generated method stub
			int decision;
		    decision=v.getId()==R.id.button_decline ? 0:1;
		    ThreadRunnable myThread=new ThreadRunnable();
		    myThread.getDecision(decision);
		    Thread thread = new Thread(myThread);
		    thread.start();
		    if (decision==0){
		    	button1.setText("refused");
		        button2.setVisibility(View.INVISIBLE);
		        button1.setClickable(false);
		    } else {
		        button1.setText("added");
		        button2.setVisibility(View.INVISIBLE);
		        button1.setClickable(false);
		    }		            
		}
		
		public class ThreadRunnable implements Runnable {  
			private int decision;
		    public void getDecision(int decision) {
		        this.decision = decision;
		    }
		    public void run() {
		    	String url = Global.WebServiceUrl + "/confirm_request/";
	        	HttpParams hPara = new BasicHttpParams();
	            HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	            HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
	            HttpClient hClient = new DefaultHttpClient(hPara);
	            HttpResponse response = null;
	            HttpPost httppost = new HttpPost(url);
	            try {
	            	JSONObject paraOut = new JSONObject();
	            	paraOut.put("Dynamic_id", Global.Dynamic_id);
	            	paraOut.put("doctor_id", Global.access_request_id[position-Global.doctor_num]);
	            	paraOut.put("decision", decision);
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
	                	webResult = jso.getString("result");
	                	System.out.println(webResult);
	                	list.clear();	
	                }
	            }
	            catch (Exception e) {
	    			e.printStackTrace();
	    		}
	            refresh_doclist refresh=new refresh_doclist();
        	    refresh.refresh();
	    	}
		};
	 }
}
