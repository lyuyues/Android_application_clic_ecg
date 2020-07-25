package ca.uvic.ece.ecg.heartcarer1;

import android.util.*;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.bluetooth.*;
import android.widget.Toast;

public class BleReceiver extends BroadcastReceiver {
    private static final String TAG = "BleReceiver";

    private final MainActivity mMainActivity;

    public BleReceiver(MainActivity mainActivity) {
        super();

        this.mMainActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            switch (blueState) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, "BLE_STATE_TURNING_ON");
                    break;
                case BluetoothAdapter.STATE_ON:
                    mMainActivity.sendMessageToHrmFragment(BleService.STATE_CONNECTING);
                    Log.d(TAG, "BLE_STATE_ON");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(TAG, "BLE_STATE_TURNING_OFF");
                    break;
                case BluetoothAdapter.STATE_OFF:
                    mMainActivity.sendMessageToHrmFragment(BleService.STATE_DISCONNECTING);
                    Toast.makeText(context, "Bluetooth is off, please turn it on", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "BLE_STATE_OFF");
                    break;
            }
        }
    }
}
