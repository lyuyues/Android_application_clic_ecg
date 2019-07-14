package ca.uvic.ece.ecg.heartcarer1;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

import ca.uvic.ece.ecg.database.FeedReaderContract;
import ca.uvic.ece.ecg.database.FeedReaderContract.FeedEntry;
import ca.uvic.ece.ecg.database.FeedReaderDbHelper;

public class Data_statistic extends Activity {

	private Cursor c;
	private SQLiteDatabase db;
	private FeedReaderDbHelper mDbHelper;
	private XYMultipleSeriesRenderer mRenderer;
	private XYSeries mCurrentSeries;
	private XYMultipleSeriesDataset mDataset;
	private GraphicalView mChartView = null;
	private Button HR_button, QRS_button, QTC_button, PR_button;




	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.data_statistic);
        getActionBar().setTitle(R.string.quick_test_report);
		SQL_query();
		findViewById();
		draw(0);

	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.data_statistic, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.View_Detail:
	        	Intent intent = new Intent(Data_statistic.this, QuickResult.class);
	        	intent.putExtra("only_view", true);
        		startActivity(intent);
                return true;
	    }
	    return false;
	}
	
	
	private void findViewById() {
		HR_button = (Button)findViewById(R.id.HR);
		QRS_button = (Button)findViewById(R.id.QRS);
		QTC_button = (Button)findViewById(R.id.QTC);
		PR_button = (Button)findViewById(R.id.PR);
		HR_button.setOnClickListener(Listener);
		QRS_button.setOnClickListener(Listener);
		QTC_button.setOnClickListener(Listener);
		PR_button.setOnClickListener(Listener);
	}
	
	private OnClickListener Listener = new OnClickListener(){
		public void onClick(View v){
    		if (v.getId() == R.id.HR) {
    			draw(0);
    		} else if (v.getId() == R.id.QRS) {
    			draw(1);
    		} else if (v.getId() == R.id.QTC) {
    			draw(2);
    		} else if (v.getId() == R.id.PR) {
    			draw(3);
    		}
    	}
	};

	
	protected XYMultipleSeriesDataset buildDateDataset(String[] titles, List<List<Date>> xValues,
            List<List<Integer>> yValues) {
          XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
          int length = titles.length;
          for (int i = 0; i < length; i++) {
            TimeSeries series = new TimeSeries(titles[i]);
            List<Date> xV = xValues.get(i);      
            List<Integer> yV = yValues.get(i);
            int seriesLength = xV.size();
            for (int k = 0; k < seriesLength; k++) {
              series.add(xV.get(k), yV.get(k));
            }
            dataset.addSeries(series);
          }
          return dataset;
        }
	
	protected XYMultipleSeriesRenderer buildRenderer(int[] colors, PointStyle[] styles) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setMargins(new int[] {0, 20, 0, 0});//up,left,down,right
        renderer.setPointSize(1);
        renderer.setShowGrid(true);
        //renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(Color.WHITE);
        renderer.setMarginsColor(Color.WHITE);
        renderer.setGridColor(Color.LTGRAY);
        renderer.setAxesColor(Color.LTGRAY);
        renderer.setLabelsColor(Color.BLACK);
        renderer.setXLabelsColor(Color.BLACK);
        renderer.setYLabelsColor(0, Color.BLACK);
        int length = colors.length;
        for (int i = 0; i < length; i++) {
          XYSeriesRenderer r = new XYSeriesRenderer();
          r.setColor(colors[i]);
          r.setPointStyle(styles[i]);
          renderer.addSeriesRenderer(r);
        }
        return renderer;
    }
	@SuppressLint("SimpleDateFormat")
	private void draw(int i) {
    	List<List<Date>> List_date = new ArrayList<List<Date>>();
   		List<List<Integer>> List_value = new ArrayList<List<Integer>>();
 		List<Date> date = new ArrayList<Date>();
  		List<Integer> Value_list = new ArrayList<Integer>();
		String title = "";
		String str = "";
		switch (i) {
			case 0 :
				str = "avg(HR)";
				title = "HR";
				break;
			case 1:
				str = "avg(QRS)";
				title = "QRS";
				break;
			case 2:
				str = "avg(QTC)";
				title = "QTC";
				break;
			case 3:
				str = "avg(PR)";
				title = "PR";
				break;
		}
		c.moveToFirst();
		c.moveToPrevious();
		int max = 0;
		int min = Integer.MAX_VALUE;
    	while(c.moveToNext()){
    		int nameColumnIndex = c.getColumnIndex(FeedEntry.COLUMN_NAME_TestTime);
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  
    		Date D = null;
			try {
				D = sdf.parse(c.getString(nameColumnIndex).substring(0, 10));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
    		date.add(D); 
    		nameColumnIndex = c.getColumnIndex(str);
    		int int_tmp = c.getInt(nameColumnIndex);
    		Value_list.add(int_tmp);
    		min = Math.min(min, int_tmp);
    		max = Math.max(max, int_tmp);
    		}
    	List_date.add(date);
    	List_value.add(Value_list);
		mDataset = buildDateDataset(new String[]{title}
					, List_date, List_value);    
		mRenderer = buildRenderer(new int[]{Color.RED}, 
				new PointStyle[]{PointStyle.CIRCLE});
		mRenderer.setYAxisMin(min * 0.98);
	    mRenderer.setYAxisMax(max * 1.02);
		mChartView = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "M/d");
		mChartView.repaint();
		LinearLayout layout=(LinearLayout)findViewById(R.id.chart);
        layout.removeAllViews();
		layout.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT,
				         LayoutParams.MATCH_PARENT));
	}
	
	/*private void initChart() {
        XYSeries series = new XYSeries(getResources().getString(R.string.ecgsignal1));
        mDataset.addSeries(series);
        mCurrentSeries = series;
        mChartView = ChartFactory.getLineChartView(Data_statistic.this, mDataset, mRenderer);
        mChartView.setOnClickListener(new View.OnClickListener(){
			public void onClick(View arg0) {
				@SuppressWarnings("unused")
				SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
			}
        });
        LinearLayout layout=(LinearLayout)findViewById(R.id.chart);
        layout.addView(mChartView, new LayoutParams
        		(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mChartView.repaint();
	}*/
	
	private void SQL_query () {
		mDbHelper = new FeedReaderDbHelper(getBaseContext());
		db = mDbHelper.getWritableDatabase();
		c = db.query(FeedEntry.TABLE_NAME,
				new String[]{"avg(HR)", "avg(QRS)", "avg(QTC)", "avg(PR)", FeedEntry.COLUMN_NAME_TestTime},
				FeedEntry.COLUMN_NAME_UserID  + "= ?",
				new String[]{String.valueOf(Global.userid)},
				"substr(" + FeedEntry.COLUMN_NAME_TestTime + ", 1, 10)",
				null,
				null
				);
	}

}
