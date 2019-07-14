package ca.uvic.ece.ecg.heartcarer1;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import ca.uvic.ece.ecg.database.FeedReaderContract;
import ca.uvic.ece.ecg.database.FeedReaderContract.FeedEntry;
import ca.uvic.ece.ecg.database.FeedReaderDbHelper;

public class QuickResult extends Activity{
	
	private String filename;
	private List<Integer> MA_Seq;
	private List<Double> New_ECG;
	private ProgressDialog proDialog;
	private Button HR_button, QRS_button, QTC_button, PR_button, ST_button, Next_button, Back_button;
	private TextView Time_textview, HR_textview, QTC_textview, PR_textview, QRS_textview, ST_textview;
	private Cursor c;
	private FeedReaderDbHelper mDbHelper;
	private SQLiteDatabase db;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.quick_test_report);
        setContentView(R.layout.quick_result);
        mDbHelper = new FeedReaderDbHelper(getBaseContext());
		db = mDbHelper.getWritableDatabase();
        findViewsById();
        setListener();
        Intent intent = getIntent();
        if (intent.hasExtra("only_view")) {
    		SQL_query();	
    		c.moveToNext();
    		if (c.getCount() != 0) 
    			updatedata();
    	} else {
        	show_data(getIntent());
    		SQL_query();	
        }
	}
	
	private void SQL_query () {
		c = db.query(FeedEntry.TABLE_NAME,
				new String[]{"HR", "QRS" , "QTC", "PR", "ST", "TestTime"},
				FeedEntry.COLUMN_NAME_UserID  + "= ?",
				new String[]{String.valueOf(Global.userid)},
				null,
				null,
				FeedEntry.COLUMN_NAME_TestTime + " desc");
	}
	
	
	private void setListener() {
		Back_button.setOnClickListener(view_history_Listener);
		Next_button.setOnClickListener(view_history_Listener);
		HR_button.setOnClickListener(Listener);
		QTC_button.setOnClickListener(Listener);
		PR_button.setOnClickListener(Listener);
		ST_button.setOnClickListener(Listener);
		QRS_button.setOnClickListener(Listener);
	}
	
	private OnClickListener view_history_Listener = new OnClickListener(){
		public void onClick(View v){
    		if (v.getId() == R.id.back) {
    			if (c.moveToNext()) {
    				updatedata();
    			} else {
    				Global.toastMakeText(QuickResult.this, "No more data");
    			}
    		} else if (v.getId() == R.id.next){
    			if (c.moveToPrevious()) {
    				updatedata();
    			} else {
    				Global.toastMakeText(QuickResult.this, "No more data");
    			}
    		}
    	}
	};
	private OnClickListener Listener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	private void updatedata() {
		int nameColumnIndex = c.getColumnIndex(FeedEntry.COLUMN_NAME_TestTime); 
		String string_tmp = c.getString(nameColumnIndex); 
		Time_textview.setText(string_tmp);
		nameColumnIndex = c.getColumnIndex(FeedEntry.COLUMN_NAME_HR);
		int int_tmp = c.getInt(nameColumnIndex);
		HR_textview.setText(String.valueOf(int_tmp));
		nameColumnIndex = c.getColumnIndex(FeedEntry.COLUMN_NAME_QTC);
		int_tmp = c.getInt(nameColumnIndex);
		QTC_textview.setText(String.valueOf(int_tmp));
		nameColumnIndex = c.getColumnIndex(FeedEntry.COLUMN_NAME_PR);
		int_tmp = c.getInt(nameColumnIndex);
		PR_textview.setText(String.valueOf(int_tmp));
		nameColumnIndex = c.getColumnIndex(FeedEntry.COLUMN_NAME_QRS);
		int_tmp = c.getInt(nameColumnIndex);
		QRS_textview.setText(String.valueOf(int_tmp));
		nameColumnIndex = c.getColumnIndex(FeedEntry.COLUMN_NAME_ST);
		int_tmp = c.getInt(nameColumnIndex);
		ST_textview.setText(String.valueOf(int_tmp));
	}
	
	private void findViewsById() {
		HR_button = (Button)findViewById(R.id.heart_rate);
		QRS_button = (Button)findViewById(R.id.QRS);
		QTC_button = (Button)findViewById(R.id.QTC);
		PR_button = (Button)findViewById(R.id.PR);
		Time_textview = (TextView)findViewById(R.id.report_time_value);
		HR_textview = (TextView)findViewById(R.id.heart_rate_value);
		QTC_textview = (TextView)findViewById(R.id.QTC_value);
		PR_textview = (TextView)findViewById(R.id.PR_value);
		QRS_textview = (TextView)findViewById(R.id.QRS_value);
		Back_button = (Button)findViewById(R.id.back);
		Next_button = (Button)findViewById(R.id.next);
		ST_textview = (TextView)findViewById(R.id.ST_value);
		ST_button = (Button)findViewById(R.id.ST);
	}
	
	
	protected void show_data(Intent intent) {
		if (intent.getBooleanExtra("isNoise", true)) {
			Dialog alertDialog = new AlertDialog.Builder(this).
				    setTitle("Wrong Data").
				    setMessage("The Data is too noisy to analyse").
				    setIcon(R.drawable.report).
				    setPositiveButton("Return", new DialogInterface.OnClickListener() {  
				    	@Override
				        public void onClick(DialogInterface dialog, int which) {
				         // TODO Auto-generated method stub
				    		QuickResult.this.finish();
				        }
				       }).create();
			 alertDialog.show();
		}
		Time_textview.setText(intent.getStringExtra("time"));
		HR_textview.setText(intent.getStringExtra("HR"));
		QTC_textview.setText(intent.getStringExtra("QTC"));
		PR_textview.setText(intent.getStringExtra("PR"));
		QRS_textview.setText(intent.getStringExtra("QRS"));
		ST_textview.setText(intent.getStringExtra("ST"));
		SQL_update(Integer.valueOf(intent.getStringExtra("HR")), 
				Integer.valueOf(intent.getStringExtra("QTC")),
				Integer.valueOf(intent.getStringExtra("PR")),
				Integer.valueOf(intent.getStringExtra("QRS")),
				Integer.valueOf(intent.getStringExtra("ST")),
				intent.getStringExtra("time"),
				intent.getStringExtra("filename"));
	}
	
	public void SQL_update(int HR, int QTC, int PR, int QRS, int ST, String time, String filename) {
		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put(FeedEntry.COLUMN_NAME_HR, HR);
		values.put(FeedEntry.COLUMN_NAME_QTC, QTC);
		values.put(FeedEntry.COLUMN_NAME_PR, PR);
		values.put(FeedEntry.COLUMN_NAME_QRS, QRS);
		values.put(FeedEntry.COLUMN_NAME_ST, ST);
		values.put(FeedEntry.COLUMN_NAME_TestTime, time);
		values.put(FeedEntry.COLUMN_NAME_data_name, filename);
		values.put(FeedEntry.COLUMN_NAME_UserID, Global.userid);

		// Insert the new row, returning the primary key value of the new row
		long newRowId;
		newRowId = db.insert(
		         FeedEntry.TABLE_NAME,
		         null,
		         values);
	}
	protected void onResume() {
        super.onResume();
	}
	protected void onPause() {
        super.onPause();
    }
	protected void onDestroy() {
        super.onDestroy();
	}
}
