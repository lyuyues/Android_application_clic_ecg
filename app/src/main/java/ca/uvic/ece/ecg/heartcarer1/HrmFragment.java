package ca.uvic.ece.ecg.heartcarer1;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

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

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This Fragment behaves as a Heart Rate Monitor
 */
public class HrmFragment extends Fragment {
    private static final String TAG = "HrmFragment";

    private View view;
    private Button buttonConState, buttonStartTest;
    private TextView textviewHeartRate, textviewVtvf;
    // private ToggleButton tButton_sensor, tButton_plot, tButton_save;
    private TableRow tableRow1;

    private sendVoidToSMListener mSendToSMListener;

    private Menu menu;

    private int bpm = 0, bpmGqrs = 0, vtvf = -1;

    private static final int REQUEST_CODE_BLE_FOR_DEVICE_PICKER = 0;
    private static final int REQUEST_CODE_BLE_FOR_CONNECT = 1;
    private static final int REQUEST_CODE_FOR_DEVICE_PICKER = 2;

    // This interface is implemented by MainActivity, and HrmFragment can send
    // Message to BleService
    public interface sendVoidToSMListener {
        void sendVoidToSM(int num);
    }

    // Called when a fragment is first attached to its activity
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i(TAG, "onAttach()");

        mSendToSMListener = (sendVoidToSMListener) activity;
    }

    // Called to have the fragment instantiate its user interface view
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");

        view = inflater.inflate(R.layout.hrm_fragment, container, false);
        setHasOptionsMenu(true);
        findViewsById();
        setListener();
        initChart();
        if (Global.ifLandscape(getActivity()))
            adaptScreen(0);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        Global.ifHrmFragmentAlive = true;
        refreshViews();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");

        Global.ifHrmFragmentAlive = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Called by the system when the device configuration changes
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            adaptScreen(0);
        else
            adaptScreen(1);
    }

    // Hide views in table row when in landscape orientation
    private void adaptScreen(int i) {
        tableRow1.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
    }

    // Create menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.hrmfragment, menu);

        this.menu = menu;
        setMenuSensorInfoEnabled(BleService.ConState == BleService.ConState_Connected);
    }

    // Handle menu item selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.selectsensor:
                selectSensor();
                return true;
            case R.id.sensorinfo:
                sensorInfo();
                return true;
        }
        return false;
    }

    private void setMenuSensorInfoEnabled(boolean enabled) {
        menu.getItem(1).setEnabled(enabled);
    }

    // Handle result from enable-bluetooth Intent
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BLE_FOR_DEVICE_PICKER) {
            if (resultCode == Activity.RESULT_OK)
                startDevicePicker();
            else
                Global.toastMakeText(getActivity(), getResources().getString(R.string.hrm_turnon));
        } else if (requestCode == REQUEST_CODE_BLE_FOR_CONNECT) {
            if (resultCode == Activity.RESULT_OK)
                connectBle();
            else
                Global.toastMakeText(getActivity(), getResources().getString(R.string.hrm_turnon));
        } else if (requestCode == REQUEST_CODE_FOR_DEVICE_PICKER) {
            if (resultCode == Activity.RESULT_OK)
                connectBle();
        }
    }

    private void findViewsById() {
        buttonConState = (Button) view.findViewById(R.id.button1);
        textviewHeartRate = (TextView) view.findViewById(R.id.textView_HR);
        textviewVtvf = (TextView) view.findViewById(R.id.textView_vtvf);
        tableRow1 = (TableRow) view.findViewById(R.id.tableRow1);
        buttonStartTest = (Button) view.findViewById(R.id.start_test);
    }

    private void setListener() {
        buttonConState.setOnClickListener(connectListener);
        buttonStartTest.setOnClickListener(start_test_Listener);
    }

    private OnClickListener start_test_Listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Global.ifSaving) {
                buttonStartTest.setText("Start Test");

                mSendToSMListener.sendVoidToSM(4);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final String[] selection = new String[1];
            selection[0] = "Long Term Monitor";
            builder.setItems(selection, (dialog, which) -> {
                buttonStartTest.setText("Stop Long Term Monitor");
                mSendToSMListener.sendVoidToSM(4);
            });
            builder.show();
        }
    };

    private OnClickListener connectListener = v -> {
        if (BleService.ConState == BleService.ConState_Connected) {
            disconncetBle();
            return;
        }

        if (!MainActivity.mBluetoothAdapter.isEnabled())
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE_BLE_FOR_CONNECT);
        else
            connectBle();
    };

    private void connectBle() {
        buttonConState.setText(getResources().getString(R.string.hrm_connecting));
        buttonConState.setClickable(false);

        mSendToSMListener.sendVoidToSM(1);

        login();
    }

    private void disconncetBle() {
        buttonConState.setText(getResources().getString(R.string.hrm_disconnecting));
        buttonConState.setClickable(false);

        mSendToSMListener.sendVoidToSM(2);
    }

    private void login() {
        new Thread() {
            public void run() {
                try {
                    JSONObject paraOut = new JSONObject();
                    //paraOut.put("deviceMacAddress", BleService.mDevice.getAddress());
                    paraOut.put("deviceMacAddress", "testMacAddress");

                    StringEntity se = new StringEntity(paraOut.toString());
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

                    HttpPost httppost = new HttpPost(Global.WebServiceUrl + "phones");
                    httppost.setEntity(se);

                    HttpParams hPara = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(hPara, Global.connectionTimeout);
                    HttpConnectionParams.setSoTimeout(hPara, Global.socketTimeout);

                    HttpClient hClient = new DefaultHttpClient(hPara);
                    HttpResponse response = hClient.execute(httppost);

                    if (200 != response.getStatusLine().getStatusCode())
                        return;

                    // get the response string
                    StringBuilder total = new StringBuilder();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        total.append(line);
                    }

                    // check if the response succeed
                    JSONObject jso = new JSONObject(total.toString());
                    if (!"OK.".equals(jso.getString("errorMessage")))
                        return;

                    Global.token = jso.getJSONObject("entity").getJSONObject("model").getString("message");

                    getActivity().startService(new Intent(getActivity(), UpdataService.class));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // Initiate chart
    private void initChart() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.chart, new DoubleChartFragment(), getResources().getString(R.string.Double_Chart_Fragment))
                .commit();
    }


    // Show sensor information
    private void sensorInfo() {
        String deviceName = (BleService.mDevice.getName() == null) ? "Unknown Device" : BleService.mDevice.getName();
        Global.infoDialog(getActivity(),
                getResources().getString(R.string.global_sensor) + getResources().getString(R.string.hrm_info),
                "Device Name: " + deviceName + "\nMac Address: " + BleService.mDevice.getAddress());
    }

    private void selectSensor() {
        if (BleService.ConState != BleService.ConState_NotConnected) {
            Global.toastMakeText(getActivity(), getResources().getString(R.string.hrm_disfirst));
            return;
        }

        if (!MainActivity.mBluetoothAdapter.isEnabled())
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE_BLE_FOR_DEVICE_PICKER);
        else
            startDevicePicker();
    }

    private void startDevicePicker() {
        startActivityForResult(new Intent(getActivity(), BleDevicePicker.class), REQUEST_CODE_FOR_DEVICE_PICKER);
    }

    public void handleMainActivityMes(Message msg) {
        int i = msg.what;
        if (i == BleService.STATE_MULTI_VAL) {
            handleMsgByChart(msg);
        } else if (i == BleService.STATE_CONNECTED) {
            setMenuSensorInfoEnabled(true);
            Global.toastMakeText(getActivity(), getResources().getString(R.string.global_sensor) + " connected!");
        } else if (i == BleService.STATE_DISCONNECTED) {
            setMenuSensorInfoEnabled(false);
            Global.toastMakeText(getActivity(), "Disconnected!");
            handleMsgByChart(msg);
        } else if (i == BleService.STATE_START_SAVING) {
            buttonStartTest.setText("Stop Long Term Monitor");
        } else if (i == BleService.STATE_STOP_SAVING) {
        } else if (i == BleService.STATE_UPDATE_BPM) {
            int[] data = msg.getData().getIntArray("data");
            bpm = data[0];
            bpmGqrs = data[1];
        } else if (i == BleService.STATE_UPDATE_VTVF) {
            vtvf = msg.getData().getInt("data");
        }
        if (i != BleService.STATE_MULTI_VAL)
            refreshViews();
    }

    private void handleMsgByChart(Message msg) {
        DoubleChartFragment DoubleChartFragment = (DoubleChartFragment) getFragmentManager()
                .findFragmentByTag(getResources().getString(R.string.Double_Chart_Fragment));
        if (DoubleChartFragment != null)
            DoubleChartFragment.handleHrmFragmentMes(msg);
    }

    private void refreshViews() {
        if (BleService.ConState == BleService.ConState_NotConnected)
            buttonConState.setText(getResources().getString(R.string.hrm_notconnected));
        else if (BleService.ConState == BleService.ConState_Connected)
            buttonConState.setText(getResources().getString(R.string.hrm_connected));
        else
            buttonConState.setText(getResources().getString(R.string.hrm_connecting));
        buttonConState.setEnabled(null != BleService.mDevice);
        buttonConState.setClickable(BleService.ConState == BleService.ConState_Connected
                || BleService.ConState == BleService.ConState_NotConnected);

        buttonStartTest.setEnabled(null != BleService.mDevice && BleService.ConState == BleService.ConState_Connected);
        if (BleService.ConState == BleService.ConState_NotConnected || BleService.ConState == BleService.ConState_Connecting) {
            buttonStartTest.setText("Start Test");
            buttonStartTest.setClickable(true);
        } else {
            if (Global.ifSaving) {
                buttonStartTest.setText("Stop Long Term Monitor");
                buttonStartTest.setClickable(true);
            } else {
                buttonStartTest.setText("Start Test");
                buttonStartTest.setClickable(true);
            }
        }

        textviewHeartRate.setText(bpm + "/" + bpmGqrs + " bpm");
        textviewVtvf.setText(vtvf == -1 ? "" : "VT/VF");
    }
}
