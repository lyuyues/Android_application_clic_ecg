package ca.uvic.ece.ecg.heartcarer1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
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
import android.widget.Toast;

import ca.uvic.ece.ecg.ECG.ECGDetect;
import ca.uvic.ece.ecg.ECG.MADetect;

/**
 * This Fragment behaves as a Heart Rate Monitor
 */
public class HrmFragment extends Fragment {
    private final String TAG = "HrmFragment";
    private static final int QUICK_TEST_TIME_IN_SEC = 30;

    private static HrmFragment instance = null;

    private View view;
    private Button buttonConState, buttonStartTest;
    private TextView textviewHeartRate, textviewVtvf;
    // private ToggleButton tButton_sensor, tButton_plot, tButton_save;
    private TableRow tableRow1;

    private sendVoidToSMListener mSendToSMListener;

    private Menu menu;
    // AChartEngine
    private TimeCount time;
    private String filename;
    private ProgressDialog proDlg;

    private int bpm = 0, bpmGqrs = 0, vtvf = -1;

    private static final int REQUEST_CODE_BLE_FOR_DEVICE_PICKER = 0;
    private static final int REQUEST_CODE_BLE_FOR_CONNECT = 1;
    private static final int REQUEST_CODE_FOR_DEVICE_PICKER = 2;

    // This interface is implemented by MainActivity, and HrmFragment can send
    // Message to BleService
    public interface sendVoidToSMListener {
        public void sendVoidToSM(int num);
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
        instance = this;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");

        Global.ifHrmFragmentAlive = false;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        instance = null;
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
        case R.id.selectchannel:
            selectChannel();
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
            if (!Global.ifRegUser) {
                Toast.makeText(getActivity(), "Only for registered User", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Global.ifSaving) {
                buttonStartTest.setText("Start Test");

                mSendToSMListener.sendVoidToSM(4);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final String[] selection = new String[2];
            selection[0] = QUICK_TEST_TIME_IN_SEC + " Seconds Quick Test";
            selection[1] = "Long Term Monitor";
            builder.setItems(selection, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        buttonStartTest.setClickable(false);
                        Global.quick_testing = true;

                        time = new TimeCount(QUICK_TEST_TIME_IN_SEC * 1000, 1000);
                        time.start();

                        mSendToSMListener.sendVoidToSM(5);
                    } else {
                        buttonStartTest.setText("Stop Long Term Monitor");

                        mSendToSMListener.sendVoidToSM(4);
                    }
                }
            });
            builder.show();
        }
    };

    private OnClickListener connectListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Global.quick_testing) {
                Global.toastMakeText(getActivity(), "Quick testing");
                return;
            }

            if (BleService.ConState == BleService.ConState_Connected) {
                disconncetBle();
                return;
            }

            if (!MainActivity.mBluetoothAdapter.isEnabled())
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE_BLE_FOR_CONNECT);
            else
                connectBle();
        }
    };

    private void connectBle() {
        buttonConState.setText(getResources().getString(R.string.hrm_connecting));
        buttonConState.setClickable(false);

        mSendToSMListener.sendVoidToSM(1);
    }

    private void disconncetBle() {
        buttonConState.setText(getResources().getString(R.string.hrm_disconnecting));
        buttonConState.setClickable(false);

        mSendToSMListener.sendVoidToSM(2);
    }

    // Initiate chart
    private void initChart() {
        Global.Channel_selection = 0;
        SingleChartFragment SingleChartFragment = new SingleChartFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.chart, SingleChartFragment, getResources().getString(R.string.Single_Chart_Fragment))
                .commit();
    }


    // Show sensor information
    private void sensorInfo() {
        String deviceName = (BleService.mDevice.getName() == null) ? "Unknown Device" : BleService.mDevice.getName();
        Global.infoDialog(getActivity(),
                getResources().getString(R.string.global_sensor) + getResources().getString(R.string.hrm_info),
                R.drawable.bluetooth_64,
                "Device Name: " + deviceName + "\nMac Address: " + BleService.mDevice.getAddress());
    }

    private void selectChannel() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select a Channel");
        final String[] channel = new String[]{"Channel 1", "Channel 2", "Both Channel"};
        builder.setItems(channel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), channel[which], Toast.LENGTH_SHORT).show();

                if (Global.Channel_selection == which)
                    return;

                if (which == 2) {
                    Global.Channel_selection = 2;
                    getFragmentManager().beginTransaction().replace(R.id.chart, new DoubleChartFragment(),
                            getResources().getString(R.string.Double_Chart_Fragment)).commit();
                } else if (Global.Channel_selection == 2) {
                    Global.Channel_selection = which;
                    getFragmentManager().beginTransaction().replace(R.id.chart, new SingleChartFragment(),
                            getResources().getString(R.string.Single_Chart_Fragment)).commit();
                } else {
                    Global.Channel_selection = which;

                    handleMsgByChart(Message.obtain(null, BleService.STATE_DISCONNECTED));
                }
            }
        });
        builder.show();
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

    private void showResult() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataResult result = dataAnalysis(filename);
                if (result.isNoise) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Wrong Data")
                                    .setMessage("The Data is too noisy to analyse" + filename)
                                    .setIcon(R.drawable.report)
                                    .setPositiveButton("Return", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    }).create().show();
                        }
                    });
                } else {
                    Intent intent = new Intent(getActivity(), QuickResult.class);
                    intent.putExtra("isNoise", result.isNoise);
                    intent.putExtra("HR", String.valueOf((int) result.HR));
                    intent.putExtra("QRS", String.valueOf((int) result.QRS));
                    intent.putExtra("QTC", String.valueOf((int) result.QTc));
                    intent.putExtra("PR", String.valueOf((int) result.PR));
                    intent.putExtra("ST", String.valueOf((int) result.ST));
                    intent.putExtra("time", result.time);
                    intent.putExtra("filename", filename);
                    startActivity(intent);
                }
                dismissProDlgHandler.sendEmptyMessage(0);
            }
        }).start();
    }

    Handler dismissProDlgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (null != proDlg)
                proDlg.dismiss();
        }
    };

    // Data structure for data analyst
    class DataResult {
        boolean isNoise;
        double HR;
        double QRS;
        double QTc;
        double PR;
        double ST;
        String time;

        public DataResult(boolean isNoise, double HR, double QRS, double QTc, double PR, double ST) {
            this.isNoise = isNoise;
            this.HR = HR;
            this.QRS = QRS;
            this.QTc = QTc;
            this.PR = PR;
            this.ST = ST;
            this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }
    }

    /**
     *
     * @param filename
     *            : quick check's file name
     * @return analysis result
     */
    protected DataResult dataAnalysis(String filename) {
        MADetect ecg = new MADetect();
        ecg.setFS(250);
        // ecg.setPath(Global.quickcheckpath + "/" +
        // "201603171356070201603171356071.bin");
        ecg.setPath(Global.quickcheckpath + "/" + filename);
        ecg.run();
        List<Integer> MA_Seq = new ArrayList<Integer>();
        List<Double> New_ECG = new ArrayList<Double>();
        MA_Seq = ecg.getMASeq();
        New_ECG = ecg.getNewECG();
        for (int index = 0; index < MA_Seq.size(); ++index) {
            System.out.println(MA_Seq.get(index));
        }
        double HR = ecg.getHR();
        ECGDetect ecg2 = new ECGDetect();
        ecg2.setFS(250);
        if (!ecg2.run(New_ECG)) {
            return new DataResult(true, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        double QRS = ecg2.getQRS_duration();
        double QTc = ecg2.getQTc();
        double PR = ecg2.getPR_interval();
        double ST_avg = ecg2.getSTavg();
        return new DataResult(false, HR, QRS, QTc, PR, ST_avg);
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

            if (null != time) {
                time.cancel();
                time = null;
            }
        } else if (i == BleService.STATE_START_SAVING) {
            filename = msg.getData().getString("data");

            if (!Global.quick_testing)
                buttonStartTest.setText("Stop Long Term Monitor");
        } else if (i == BleService.STATE_STOP_SAVING) {
            if (Global.quick_testing) {
                showResult();
                Global.quick_testing = false;
            }
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
        if (Global.Channel_selection != 2) {
            SingleChartFragment SingleChartFragment = (SingleChartFragment) getFragmentManager()
                    .findFragmentByTag(getResources().getString(R.string.Single_Chart_Fragment));
            if (SingleChartFragment != null)
                SingleChartFragment.handleHrmFragmentMes(msg);
        } else {
            DoubleChartFragment DoubleChartFragment = (DoubleChartFragment) getFragmentManager()
                    .findFragmentByTag(getResources().getString(R.string.Double_Chart_Fragment));
            if (DoubleChartFragment != null)
                DoubleChartFragment.handleHrmFragmentMes(msg);
        }
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
            if (Global.quick_testing) {
                buttonStartTest.setClickable(false);
            } else if (Global.ifSaving) {
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

    private class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            if (null == instance)
                return;
            instance.mSendToSMListener.sendVoidToSM(5);

            instance.buttonStartTest.setText("Start Test");
            instance.buttonStartTest.setClickable(true);

            instance.proDlg = ProgressDialog.show(instance.getActivity(), "Analysing", "", true, false);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (null == instance)
                return;
            instance.buttonStartTest.setText(millisUntilFinished / 1000 + " seconds");
        }
    }
}
