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

public class refresh_doclist {
	public void a(){
		System.out.println("123");
	}
		public void refresh() { 
			String url = Global.WebServiceUrl + "/get_doclist/";
	    	HttpParams hPara = new BasicHttpParams();
	        HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
	        HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
	        HttpClient hClient = new DefaultHttpClient(hPara);
	        HttpResponse response = null;
	        HttpPost httppost = new HttpPost(url);
	        try {
	        	JSONObject paraOut = new JSONObject();
	        	paraOut.put("username", Global.username);
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
	            	
	            	for (int n=1;n<=access_request;n++){
	    				String[] access_request_name = null;
						access_request_name[n]="doctor_request"+n;
	    				access_request_name[n]=jso.getString(access_request_name[n]);
					      Global.access_request_name[n]=access_request_name[n];
	        		}
	            	for (int n=1;n<=doctor_num;n++){
	        			String[] doctor_name = null;
						doctor_name[n]=jso.getString("doctor"+n);
					      Global.doctor_name[n]=doctor_name[n];
	        		}
		    }}
	            catch (Exception e) {
	    			e.printStackTrace();
	    		}}

		}	

