package ca.uvic.ece.ecg.heartcarer1;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * This AppWidgetProvider manages Widget of app
 */
public class MyAppWidgetProvider extends AppWidgetProvider {
	private final String TAG = "MyAppWidgetProvider";

	//Initialize Widget
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		//Log.v(TAG,"onUpdate()");
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        views.setOnClickPendingIntent(R.id.linearLayout1,
        		PendingIntent.getActivity(context, 0, Global.defaultIntent(context), 0));
        appWidgetManager.updateAppWidget(appWidgetIds, views);
	}
	//Update Widget using Intent extra value
	public void onReceive (Context context, Intent intent){
		super.onReceive(context, intent);
		if(intent.getAction().equals(Global.WidgetAction)){
			Log.v(TAG, "Global.WidgetAction");
			
			String bpm = String.valueOf(intent.getIntExtra("bpm", 0));
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
			views.setTextViewText(R.id.textView1, bpm);
			AppWidgetManager.getInstance(context).updateAppWidget(
					new ComponentName(context,MyAppWidgetProvider.class), views);
		}
	}
}