package ca.uvic.ece.ecg.heartcarer1;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import ca.uvic.ece.ecg.ECG.AveCC;
import ca.uvic.ece.ecg.ECG.HR_FFT;
import ca.uvic.ece.ecg.ECG.HR_detect;
import ca.uvic.ece.ecg.ECG.MADetect1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.util.Log;

import com.google.common.collect.EvictingQueue;

public class BleService extends Service {
    // 0-Not Connected; 1-Connected; 2-Connecting
    public static int ConState;
    public static final int ConState_NotConnected = 0;
    public static final int ConState_Connected = 1;
    public static final int ConState_Connecting = 2;

    public static final int STATE_REGISTERED = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_DISCONNECTING = 4;
    public static final int STATE_DISCONNECTED = 5;
    public static final int STATE_START_SAVING = 6;
    public static final int STATE_STOP_SAVING = 7;
    public static final int STATE_UPDATE_BPM = 8;
    public static final int STATE_UPDATE_VTVF = 9;
    public static final int STATE_MULTI_VAL = 10;

    public static boolean enableNoti;
    public static BluetoothDevice mDevice;
    private HR_FFT hr = new HR_FFT();
    private HR_detect hrd = new HR_detect();
    private final String TAG = "BleService";
    private Messenger mMainActivityMessenger;
    private BluetoothGatt mBluetoothGatt;
    private final int buf_length = 1024 * 1024;
    private byte[] buffer = new byte[buf_length];
    private final int buf_hr_length = 3750;
    private double[] buffer_HR = new double[buf_hr_length];
    private EvictingQueue<Byte> gqrsHRQueue = EvictingQueue.create(buf_hr_length * 5 * 4);
    private byte[] bufferHRGqrsInThread = new byte[0];
    private List<Double> buffer_HR_list = new ArrayList<Double>();
    private List<Double> buffer_HR_list_thread = new ArrayList<Double>();
    private int pointer_HR = 0;
    private int pointerBuf = 0;
    private OutputStream output;
    private String saveFileName;
    private Timer timerSavingFile = null;
    private boolean ifHbNormal = true;
    private Handler mHandler = new Handler();
    private Vibrator vibrator;
    private final int VibrateLength = 300; // unit: ms
    private NotificationManager mNotiManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");

        initStatic();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mNotiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    // Initiate static variables
    private void initStatic() {
        ConState = ConState_NotConnected;
        enableNoti = true;
        Global.ifSaving = false;
        mDevice = null;
    }

    // Called by the system every time a client starts the service by calling
    // "startService"
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand(), startId = " + String.valueOf(startId));

        if (startId == 1)
            StartForeground();
        return super.onStartCommand(intent, flags, startId);
    }

    // Show foreground notification
    private void StartForeground() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            builder = new Notification.Builder(BleService.this, Global.CHANNEL_ID);
        } else {
            builder = new Notification.Builder(BleService.this);
        }
        Notification noti = builder
                .setContentIntent(PendingIntent.getActivity(BleService.this, 0, Global.defaultIntent(BleService.this), 0))
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_name) + getResources().getString(R.string.ble_run))
                .setSmallIcon(R.drawable.main_heart_beat_64).build();
        startForeground(1, noti);
    }

    // Return the communication channel to the service
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");

        // Update widget
        sendBroadcast(new Intent(Global.WidgetAction).putExtra("bpm", 0));
        mNotiManager.cancelAll();

        disconnect();
        if (Global.ifSaving) {
            stopSavingFinal();
            Global.ifSaving = false;
        }

        super.onDestroy();
    }

    private synchronized void connect() {
        Log.i(TAG, "connect, ConState = " + ConState);
        if (ConState == ConState_Connected)
            return;

        if (mDevice != null) {
            ConState = ConState_Connecting;
            mBluetoothGatt = mDevice.connectGatt(BleService.this, false, mGattCallback);
        }
    }

    private synchronized void reconnect() {
        Log.i(TAG, "reconnect, ConState = " + ConState);
        if (ConState == ConState_Connected)
            return;

        ConState = ConState_Connecting;
        mBluetoothGatt = mDevice.connectGatt(BleService.this, true, mGattCallback);
    }

    // Disconnect BLE connection
    private synchronized void disconnect() {
        Log.i(TAG, "disconnect, ConState = " + ConState);
        if (ConState == ConState_NotConnected)
            return;

        enableNoti = true;
        ConState = ConState_NotConnected;
        if (null != mBluetoothGatt) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
        mBluetoothGatt = null;

        sendVoidToAM(STATE_DISCONNECTED, null);
        vibrator.vibrate(VibrateLength);
    }

    // Handler which handles incoming Message
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                // Register
                mMainActivityMessenger = msg.replyTo;
                sendVoidToAM(STATE_REGISTERED, null);
            } else if (i == 1) {
                // Connect
                connect();
            } else if (i == 2) {
                // Disconnect
                disconnect();

                if (Global.ifSaving)
                    stopSavingFinal();
            } else if (i == 4) {
                // Button - SaveToFile
                if (ConState != ConState_Connected || enableNoti) {
                    toastMakeText("Please start " + getResources().getString(R.string.global_sensor) + "!");
                    return;
                }

                if (Global.ifSaving) {
                    stopSavingFinal();
                } else {
                    startSaving();
                    startTimerSavingFile();
                    toastMakeText("Start saving!");
                }
            } else if (i == 5) {
                // Quick_check
                if (ConState != ConState_Connected || enableNoti) {
                    toastMakeText("Please start " + getResources().getString(R.string.global_sensor) + "!");
                    return;
                }

                if (Global.ifSaving) {
                    stopSavingFinal();
                } else {
                    startSaving();
                    toastMakeText("Start saving!");
                }
            }
        }
    }

    // Local Messenger used to talk to ActivityMessenger, Message received by
    // IncomingHandler
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private void sendVoidToAM(int what, Serializable obj) {
        if (!Global.ifHrmFragmentAlive)
            return;

        try {
            Message message = new Message();
            message.what = what;
            if (null != obj) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("data", obj);
                message.setData(bundle);
            }
            mMainActivityMessenger.send(message);
        } catch (Exception ignore) {
        }
    }

    // Start saving ECG data to file
    private void startSaving() {
        Log.v(TAG, "startSaving()");

        saveFileName = System.currentTimeMillis() + ".bin";
        try {
            output = new BufferedOutputStream(new FileOutputStream(Global.cachePath + "/" + saveFileName));
        } catch (Exception ignore) {
        }

        sendVoidToAM(STATE_START_SAVING, saveFileName);
        Global.ifSaving = true;
    }

    // Stop saving ECG data to file
    private void stopSaving() {
        Log.v(TAG, "stopSaving()");

        byte[] tempByte;
        synchronized (BleService.this) {
            tempByte = new byte[pointerBuf];
            System.arraycopy(buffer, 0, tempByte, 0, pointerBuf);
            pointerBuf = 0;
        }

        try {
            output.write(tempByte);
            output.close();
            String oldPath = Global.cachePath + "/" + saveFileName;
            String newPath = Global.savedPath + "/" + saveFileName;
            //noinspection ResultOfMethodCallIgnored
            new File(oldPath).renameTo(new File(newPath));
        } catch (Exception e) {
            e.printStackTrace();
            toastMakeText("Error: Stop saving!");
        }

        startService(new Intent(BleService.this, UpdataService.class));

        System.gc();
    }

    // Stop saving to file thoroughly
    private final void stopSavingFinal() {
        cancelTimerSavingFile();
        stopSaving();

        sendVoidToAM(STATE_STOP_SAVING, null);
        Global.ifSaving = false;
    }

    // BLE Callback
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        // Called when connection state changes
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange(), status = " + status + ", newState = " + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnect();
                if (Global.ifSaving) {
                    stopSavingFinal();

                    reconnect();
                }
            }
        }

        // Called when BLE services are discovered
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered()");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                initNoti();

                ConState = ConState_Connected;
                if (Global.ifSaving) {
                    startSaving();
                    startTimerSavingFile();
                }

                sendVoidToAM(STATE_CONNECTED, null);
                vibrator.vibrate(VibrateLength);
            } else {
                toastMakeText("Error: Discover Services!");
            }
        }

        // Called when notification received
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] notiValue = characteristic.getValue();

            synchronized (BleService.this) {
                if (Global.ifSaving) {
                    System.arraycopy(notiValue, 0, buffer, pointerBuf, notiValue.length);
                    pointerBuf += notiValue.length;
                }
            }

            if (!Global.ifHrmFragmentAlive)
                return;

            int[] multiValue = new int[8];
            try {
                for (int i = 0; i < 8; i = i + 2) {
                    int index = i * 5 / 2;
                    index++;
                    multiValue[i] = notiValue[index++];
                    multiValue[i] = multiValue[i] << 8;
                    multiValue[i] += (notiValue[index++] & 0xFF);

                    multiValue[i + 1] = (notiValue[index++] & 0x0F);
                    multiValue[i + 1] = (multiValue[i + 1] << 8);
                    multiValue[i + 1] += (notiValue[index++] & 0xFF);

                    buffer_HR[pointer_HR] = multiValue[i + 1];

                    int j = i * 5 / 2;
                    gqrsHRQueue.offer(notiValue[j]);
                    gqrsHRQueue.offer(notiValue[j + 1]);
                    gqrsHRQueue.offer(notiValue[j + 2]);
                    gqrsHRQueue.offer(notiValue[j + 3]);
                    gqrsHRQueue.offer(notiValue[j + 4]);

                    buffer_HR_list.add(multiValue[i + 1] + 0.0);
                    pointer_HR++;
                    if (pointer_HR == buf_hr_length - 1) {
                        showHeartBeat();
//                        showVTVF();

                        buffer_HR_list.clear();
                        pointer_HR = 0;
                    }
                }

                sendVoidToAM(STATE_MULTI_VAL, multiValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Calculate Heart Beat
     *
     */
    private void showVTVF() {
        int bpm = hr.getHR(buffer_HR);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MADetect1 template = new MADetect1();
                    List<Double> Sample_Seq = new ArrayList<Double>();
                    List<Double> data = new ArrayList<Double>(); // first 8
                                                                 // second data
                    int i = 0;
                    for (double tmp : buffer_HR) {
                        if (i == 2000) {
                            break;
                        }
                        data.add(tmp);
                        i++;
                    }
                    boolean flagTemp = template.run(data);
                    boolean VTVF = false;
                    if (flagTemp) {
                        Sample_Seq = template.getSampleResult();
                        AveCC avecc = new AveCC();
                        avecc.run(Sample_Seq, data);
                        VTVF = avecc.getResult();
                    }
                    int msg = VTVF ? 1 : -1;
                    handler.sendEmptyMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        // bpm = -1;
        // updateBeat(bpm);
        if (bpm < Global.lowBpm && ifHbNormal) {
            showNotification();
            ifHbNormal = false;
        }
        if (bpm >= 60)
            ifHbNormal = true;
    }

    private void showHeartBeat() {
        buffer_HR_list_thread.addAll(buffer_HR_list);

        if (bufferHRGqrsInThread.length != gqrsHRQueue.size())
            bufferHRGqrsInThread = new byte[gqrsHRQueue.size()];
        int i = 0;
        for (Byte b : gqrsHRQueue)
            bufferHRGqrsInThread[i++] = b;

        AsyncTask.execute(() -> {
            int bpm = -1;
            if (hrd.begin(buffer_HR_list_thread)) {
                bpm = (int) hrd.getHR();
            }
            buffer_HR_list_thread.clear();
            hrd.reset();

            int bpmGqrs = -1;
            try {
                bpmGqrs = GqrsProcess.gqrsProcess(bufferHRGqrsInThread);
            } catch (Exception ignore) {
            }

            updateBeat(bpm, bpmGqrs);
            ifHbNormal = !ifHbNormal || bpmGqrs >= Global.lowBpm;
        });
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            upVTVF(msg.what);
        }
    };

    /**
     * Show Heart Beat on screen and widget
     *
     * @param bpm:
     *            Heart Beat to show in integer
     */
    private void updateBeat(int bpm, int bpmGqrs) {
        sendVoidToAM(STATE_UPDATE_BPM, new int[]{bpm, bpmGqrs});

        // Update widget
        sendBroadcast(new Intent(Global.WidgetAction).putExtra("bpm", bpm));
    }

    private void upVTVF(int VTVF) {
        sendVoidToAM(STATE_UPDATE_VTVF, VTVF);
    }

    // Show notification when low Heart Beat, and send SMS
    private void showNotification() {
        Notification noti = new Notification.Builder(BleService.this)
                .setContentIntent(
                        PendingIntent.getActivity(BleService.this, 0, Global.defaultIntent(BleService.this), 0))
                .setContentTitle(getResources().getString(R.string.app_name)).setContentText("Warning: Low heart beat!")
                .setSmallIcon(R.drawable.warning_64).setAutoCancel(true).setLights(Global.color_Red, 2000, 1000)
                .build();
        mNotiManager.notify(0, noti);
        vibrator.vibrate(3000);
    }

    // Initiate for Notification receiving
    private void initNoti() {
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"))
                .getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"));
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        mBluetoothGatt.setCharacteristicNotification(characteristic, enableNoti);
        mBluetoothGatt.writeDescriptor(descriptor);
        enableNoti = !enableNoti;
    }

    private void startTimerSavingFile() {
        if (timerSavingFile == null)
            timerSavingFile = new Timer();

        timerSavingFile.schedule(new SaveFileTimerTask(), Global.savingLength);
    }

    private void cancelTimerSavingFile() {
        if (timerSavingFile != null) {
            timerSavingFile.cancel();
            timerSavingFile = null;
        }
    }

    private class SaveFileTimerTask extends TimerTask {
        @Override
        public void run() {
            handlerTimer.sendEmptyMessage(0);

            if (null != timerSavingFile) {
                try {
                    timerSavingFile.schedule(new SaveFileTimerTask(), Global.savingLength);
                } catch (IllegalStateException ignore) {
                }
            }
        }
    }

    private Handler handlerTimer = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            stopSaving();
            startSaving();
        }
    };

    private void toastMakeText(final String string) {
        mHandler.post(() -> Global.toastMakeText(BleService.this, string));
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(Global.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert(null != notificationManager);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
