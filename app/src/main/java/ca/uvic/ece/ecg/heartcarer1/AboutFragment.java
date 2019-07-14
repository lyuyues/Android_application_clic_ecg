package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.TextView;

/**
 * This Fragment shows information about this app
 */
@SuppressLint("HandlerLeak")
public class AboutFragment extends Fragment {
    private final String TAG = "AboutFragment";
    private View view;
    private TextView textview_app;
    private Button button_update;
    private ProgressDialog proDialog;
    private String webResult;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        setHasOptionsMenu(true);

        view = inflater.inflate(R.layout.about_fragment, container, false);
        findViewsById();
        setListener();
        textview_app.setText(getResources().getString(R.string.app_name) + " " + Global.curVer);

        return view;
    }

    private void findViewsById() {
        button_update = (Button) view.findViewById(R.id.button_update);
        textview_app = (TextView) view.findViewById(R.id.textView1);
    }

    private void setListener() {
        button_update.setOnClickListener(updateListener);
    }

    // Check for any updated version of app
    private OnClickListener updateListener = new OnClickListener() {
        public void onClick(View v) {
            if (!Global.isNetworkConn(getActivity())) {
                Global.toastMakeText(getActivity(), getResources().getString(R.string.nointernet));
                return;
            }
            proDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.login_check), "", true,
                    false);
            new Thread() {
                public void run() {
                    webResult = getResources().getString(R.string.noresponse);
                    String url = Global.WebServiceUrl + "/getcurversion/";
                    HttpParams hPara = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
                    HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);
                    HttpClient hClient = new DefaultHttpClient(hPara);
                    HttpResponse response = null;
                    HttpGet httpGet = new HttpGet(url);
                    try {
                        response = hClient.execute(httpGet);
                        if (response != null) {
                            BufferedReader rd = new BufferedReader(
                                    new InputStreamReader(response.getEntity().getContent()));
                            webResult = rd.readLine();
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
    // Handler which handles result from Web Service
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (webResult.equals(getResources().getString(R.string.noresponse))) {
                Global.toastMakeText(getActivity(), webResult);
                return;
            }
            try {
                if (Float.parseFloat(Global.curVer) >= Float.parseFloat(webResult.replace("_", "."))) {
                    Global.toastMakeText(getActivity(), getResources().getString(R.string.about_uptodate));
                } else {
                    Builder builder = new Builder(getActivity());
                    builder.setTitle("Update").setIcon(R.drawable.update_64)
                            .setMessage("The latest version \"" + getResources().getString(R.string.app_name) + " "
                                    + webResult.replace("_", ".") + "\" is available, download now?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    startIntentGetApk();
                                }
                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
            } catch (NumberFormatException e) {
                Global.toastMakeText(getActivity(), "Error: Check for update!");
            }
        }
    };

    // Start intent to download latest Apk from Web Service
    private void startIntentGetApk() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Global.WebServiceUrl + "/getapk")));
    }

    // Create menu
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.about, menu);
    }

    // Handle menu options selected event
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.homepage:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_homepage))));
            return true;
        case R.id.downloadapk:
            startIntentGetApk();
            return true;
        case R.id.feedback:
            startActivity(new Intent(getActivity(), Feedback.class));
            return true;
        }
        return false;
    }
}