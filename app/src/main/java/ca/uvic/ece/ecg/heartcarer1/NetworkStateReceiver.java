package ca.uvic.ece.ecg.heartcarer1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class NetworkStateReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Toast.makeText(context, "Network " + netInfo.getTypeName() + " connected", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Have Wifi Connection");
            if (!Global.isLogin()) {
                Global.login(() -> {
                    MainActivity.updateAdapter();
                    context.startService(new Intent(context, UpdataService.class));
                }, context);
            }
        } else {
            Toast.makeText(context, "Please connect to Wifi", Toast.LENGTH_SHORT).show();
            Global.logout();
            Log.d(TAG, "Don't have Wifi Connection");
        }
    }
}
