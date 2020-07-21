package ca.uvic.ece.ecg.heartcarer1;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This ListActivity allows user to select a Heart rate Sensor
 */
public class BleDevicePicker extends ListActivity {
    private final String TAG = "BleDevicePicker";
    private static final int SCAN_PERIOD = 10000;// 10s
    private static final int AUTO_CONNECT_PERIOD = 2000;// 2s
    private boolean ifScanning = false;
    private Handler mHandler = new Handler();
    private LeDeviceListAdapter mLeDeviceListAdapter;

    private Date requestPermissionTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.bledevicepicker);

        setTitle(getResources().getString(R.string.select) + getResources().getString(R.string.global_sensor));
        Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);

        scanLeDevice();
    }

    private boolean applyBluetoothPermission(Context context) {
        List<String> permissionsList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(Manifest.permission.BLUETOOTH);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(Manifest.permission.BLUETOOTH_ADMIN);

        if (permissionsList.size() > 0) {
            requestPermissionTime = new Date();
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    permissionsList.toArray(new String[0]),
                    1);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Date now = new Date();
        if (now.getTime() - requestPermissionTime.getTime() < 100) {
            Toast.makeText(BleDevicePicker.this, "Please go to phone Settings to enable location permission.", Toast.LENGTH_LONG).show();
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(BleDevicePicker.this, "Please select 'Allow' to enable bluetooth location permission.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        scanLeDevice();
    }

    // Called when an item in the list is selected
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        stopScan();

        BleService.mDevice = mLeDeviceListAdapter.getItem(position);

        setResult(Activity.RESULT_OK);
        finish();
    }

    // Stop scanning BLE devices
    private void stopScan() {
        synchronized (BleDevicePicker.this) {
            if (!ifScanning)
                return;

            ifScanning = false;
            invalidateOptionsMenu();
            MainActivity.mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bledevicepicker, menu);
        if (!ifScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.inprogress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_scan:
            scanLeDevice();
            return true;
        case R.id.menu_stop:
            stopScan();
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

        stopScan();
    }

    // Scan BLE devices
    private void scanLeDevice() {
        if (!applyBluetoothPermission(BleDevicePicker.this))
            return;

        synchronized (BleDevicePicker.this) {
            if (ifScanning)
                return;

            ifScanning = true;
            invalidateOptionsMenu();
            MainActivity.mBluetoothAdapter.startLeScan(mLeScanCallback);

            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();

            // Auto connect to the device when only one valid device name in the list
            mHandler.postDelayed(() -> {
                if (mLeDeviceListAdapter.getCount() == 1){
                    onListItemClick(null, null, 0, 0);
                }
            }, AUTO_CONNECT_PERIOD);

            mHandler.postDelayed(() -> {
                if (mLeDeviceListAdapter.getCount() == 0){
                    Toast.makeText(BleDevicePicker.this, "Please check if the Sensor is on or with power.", Toast.LENGTH_LONG).show();
                }
            }, SCAN_PERIOD);

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(this::stopScan, SCAN_PERIOD);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        // Called when BLE device found during scanning
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            String deviceName = device.getName();
            if (!getResources().getString(R.string.app_name_title).equals(deviceName))
                return;

            runOnUiThread(() -> {
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            });
        }
    };

    // Customized ListAdapter
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device))
                mLeDevices.add(device);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return mLeDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.datamanagelist_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.image = convertView.findViewById(R.id.imageView1);
                viewHolder.deviceName = convertView.findViewById(R.id.textView1);
                viewHolder.deviceAddress = convertView.findViewById(R.id.textView2);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            BluetoothDevice device = mLeDevices.get(position);
            viewHolder.image.setBackgroundResource(R.drawable.main_heart_beat_64);
            viewHolder.deviceName.setText(device.getName());
            viewHolder.deviceAddress.setText(device.getAddress());
            return convertView;
        }

        private class ViewHolder {
            private ImageView image;
            private TextView deviceName;
            private TextView deviceAddress;
        }
    }
}