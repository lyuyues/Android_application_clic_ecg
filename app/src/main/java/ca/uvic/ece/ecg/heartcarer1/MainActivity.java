package ca.uvic.ece.ecg.heartcarer1;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.legacy.app.ActionBarDrawerToggle;

/**
 * This Activity is the main Activity after logging in (or with no account)
 */
@SuppressLint("HandlerLeak")
public class MainActivity extends FragmentActivity implements HrmFragment.sendVoidToSMListener {
    private final String TAG = "MainActivity";
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private static BaseAdapter mListAdapter;
    private ActionBar mActionBar;
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mTitles = new String[4];
    private int[] mIconIds = new int[4];
    private Bitmap[] mIcons = new Bitmap[4];
    private int[] menuIds = new int[4];
    private int[] menuIdsNotLogin = new int[2];
    private int curDrawerPos = -1;
    public static BluetoothAdapter mBluetoothAdapter;
    private Messenger mBleServiceMessenger;
    private boolean ifBackPressed = false;
    private Menu myMenu;
    private long exitTime;

    private Handler mHandler = new Handler();
    private final Runnable mRunnable = () -> ifBackPressed = false;
    // Local Messenger used to talk to ServiceMessenger, Message received by
    // IncomingHandler
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private NetworkStateReceiver networkStateReceiver;
    private BleReceiver bleReceiver;

    private static final int RETURN_PERIOD =  2000;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        Global.initiate(MainActivity.this);

        setContentView(R.layout.main_activity);
        initTitlesAndIcons();

        mActionBar = getActionBar();
        assert (null != mActionBar);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, mDrawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View drawerView) {
                setTitleAndIcon(curDrawerPos);
                for (int i = 0; i < myMenu.size(); i++)
                    myMenu.getItem(i).setVisible(true);
            }

            public void onDrawerOpened(View drawerView) {
                setTitleAndIcon(-1);
                for (int i = 0; i < myMenu.size(); i++)
                    myMenu.getItem(i).setVisible(false);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerList = findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(mListAdapter = new MyListAdapter(this));
        mDrawerList.setOnItemClickListener((parent, view, position, id) -> selectItem(position));

        selectItem(0);
        setTitleAndIcon(0);

        bindService(new Intent(MainActivity.this, BleService.class), mConn, Context.BIND_AUTO_CREATE);
        startService(new Intent(MainActivity.this, BleService.class));
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        networkStateReceiver = new NetworkStateReceiver();
        bleReceiver = new BleReceiver(MainActivity.this);

        IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        networkFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(networkStateReceiver, networkFilter);

        IntentFilter bleFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        bleFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(bleReceiver, bleFilter);
    }

    private void initTitlesAndIcons() {
        Resources res = MainActivity.this.getResources();

        mTitles[0] = getResources().getString(R.string.main_hrm);
        mIconIds[0] = R.drawable.hrmonitor_64;
        mIcons[0] = BitmapFactory.decodeResource(res, mIconIds[0]);

        mTitles[1] = getResources().getString(R.string.main_notes);
        mIconIds[1] = R.drawable.report;
        mIcons[1] = BitmapFactory.decodeResource(res, mIconIds[1]);

        mTitles[2] = getResources().getString(R.string.main_return_device);
        mIconIds[2] = R.drawable.return_back;
        mIcons[2] = BitmapFactory.decodeResource(res, mIconIds[2]);

        mTitles[3] = getResources().getString(R.string.main_exit);
        mIconIds[3] = R.drawable.exit_64;
        mIcons[3] = BitmapFactory.decodeResource(res, mIconIds[3]);

        menuIds[0] = 0;
        menuIds[1] = 1;
        menuIds[2] = 2;
        menuIds[3] = 3;

        menuIdsNotLogin[0] = 0;
        menuIdsNotLogin[1] = 3;
    }

    // Connection to BleService using Messenger
    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.i(TAG, "onServiceConnected()");
            mBleServiceMessenger = new Messenger(binder);
            try {
                // Register
                Message msg = Message.obtain(null, 0);
                msg.replyTo = mMessenger;
                mBleServiceMessenger.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Called when a connection to the Service has been lost.
        // This typically happens when the process hosting the service has
        // crashed or been killed.
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "onServiceDisconnected()");
        }
    };

    // Send void message to BleService
    public void sendVoidToSM(int i) {
        try {
            Message msg = Message.obtain(null, i);
            mBleServiceMessenger.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Send void message to HrmFragment
    public void sendMessageToHrmFragment(int i) {
        try {
            getFragmentManager().executePendingTransactions();
            HrmFragment hrmFrag = (HrmFragment) getSupportFragmentManager()
                    .findFragmentByTag(getResources().getString(R.string.main_hrm));
            if (hrmFrag != null)
                hrmFrag.handleMainActivityMes(Message.obtain(null, i));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Forward received Message from BleService to HrmFragment
    private class IncomingHandler extends Handler {
        public void handleMessage(Message msg) {
            getFragmentManager().executePendingTransactions();
            HrmFragment hrmFrag = (HrmFragment) getSupportFragmentManager()
                    .findFragmentByTag(getResources().getString(R.string.main_hrm));
            if (hrmFrag != null)
                hrmFrag.handleMainActivityMes(msg);

            int i = msg.what;
            if (i == BleService.STATE_CONNECTED) {
                Global.login(MainActivity.this);
            }
        }
    }

    public static void updateAdapter() {
        mListAdapter.notifyDataSetChanged();
    }
    // Set the title and icon of ActionBar
    private void setTitleAndIcon(int index) {
        if (-1 == index) {
            mActionBar.setTitle(getResources().getString(R.string.app_name_title));
            mActionBar.setIcon(R.drawable.main_heart_beat_64);
        } else {
            mActionBar.setTitle(mTitles[index]);
            mActionBar.setIcon(mIconIds[index]);
        }
    }

    /**
     * Choose a item from the menu list
     * Logined: 0 : monitor, 1 : exit
     * non-login: 0 :monitor  1: patient note 2: return device 3: exit
     * @param position the order number of the item
     */
    private void selectItem(int position) {
        mDrawerLayout.closeDrawer(mDrawerList);

        if (!Global.isLogin()) {
            if (position == 1) {
                Global.exitDialog(MainActivity.this);
            } else if (position != curDrawerPos) {
                curDrawerPos = position;
                Fragment tmp = new HrmFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, tmp, mTitles[getPosition(position)]).commit();
            }
        } else {
            if (position == 2) {
                Global.returnDeviceDialog(MainActivity.this);
            } else if (position == 3) {
                Global.exitDialog(MainActivity.this);
            } else {
                if (position != curDrawerPos) {
                    curDrawerPos = position;
                    Fragment tmp = null;
                    if (position == 0)
                        tmp = new HrmFragment();
                    else if (position == 1)
                        tmp = new PatientNotesFragment();

                    getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, tmp, mTitles[getPosition(position)]).commit();
                }
            }
        }

        mDrawerList.setItemChecked(curDrawerPos, true);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        myMenu = menu;
        return super.onPrepareOptionsMenu(menu);
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getPosition(int position) {
        return !Global.isLogin() ? menuIdsNotLogin[position] : menuIds[position];
    }

    private class MyListAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        MyListAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return !Global.isLogin() ? 2 : 4;
        }

        public Object getItem(int position) {
            return mTitles[getPosition(position)];
        }

        public long getItemId(int position) {
            return getPosition(position);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.drawer_list_item, null);
                holder = new ViewHolder();
                holder.ll = convertView.findViewById(R.id.linearLayout1);
                holder.icon = convertView.findViewById(R.id.imageView1);
                holder.title = convertView.findViewById(R.id.textView1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

//            holder.ll.setBackgroundColor(Global.color_Grey);
            holder.title.setText(mTitles[getPosition(position)]);
            holder.icon.setImageBitmap(mIcons[getPosition(position)]);
            return convertView;
        }

        private class ViewHolder {
            private LinearLayout ll;
            private ImageView icon;
            private TextView title;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList);
            return;
        }

        if (System.currentTimeMillis() - exitTime > RETURN_PERIOD) {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (current != getSupportFragmentManager().findFragmentByTag(mTitles[0])) {
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new HrmFragment()).commit();
                selectItem(0);
                setTitleAndIcon(0);
            }
            Toast.makeText(this, getResources().getString(R.string.main_pressback), Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

        this.unregisterReceiver(bleReceiver);
        this.unregisterReceiver(networkStateReceiver);

        Global.toastMakeText(MainActivity.this, getResources().getString(R.string.main_thank)
                + getResources().getString(R.string.app_name) + getResources().getString(R.string.excla));
        unbindService(mConn);
        stopService(new Intent(MainActivity.this, BleService.class));
        if (Global.ifTurnOffBt)
            mBluetoothAdapter.disable();
        if (ifBackPressed) {
            mHandler.removeCallbacks(mRunnable);
        }
    }
}