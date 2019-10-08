package ca.uvic.ece.ecg.heartcarer1;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import androidx.legacy.app.ActionBarDrawerToggle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.drawerlayout.widget.DrawerLayout;
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

/**
 * This Activity is the main Activity after logging in (or with no account)
 */
@SuppressLint("HandlerLeak")
public class MainActivity extends FragmentActivity implements HrmFragment.sendVoidToSMListener {
    private final String TAG = "MainActivity";
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBar mActionBar;
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mTitles = new String[4];
    private int[] mIconIds = new int[4];
    private Bitmap[] mIcons = new Bitmap[4];
    private int curDrawerPos = -1;
    public static BluetoothAdapter mBluetoothAdapter;
    private Messenger ServiceMessenger;
    private boolean ifBackPressed = false;
    private Menu myMenu;

    private Handler mHandler = new Handler();
    private final Runnable mRunnable = () -> ifBackPressed = false;
    // Local Messenger used to talk to ServiceMessenger, Message received by
    // IncomingHandler
    final Messenger mMessenger = new Messenger(new IncomingHandler());

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

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new MyListAdapter(this));
        mDrawerList.setOnItemClickListener((parent, view, position, id) -> selectItem(position));

        selectItem(0);
        setTitleAndIcon(0);

        bindService(new Intent(MainActivity.this, BleService.class), mConn, Context.BIND_AUTO_CREATE);
        startService(new Intent(MainActivity.this, BleService.class));
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
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
        mIconIds[2] = R.drawable.exit_64;
        mIcons[2] = BitmapFactory.decodeResource(res, mIconIds[2]);

        mTitles[3] = getResources().getString(R.string.main_exit);
        mIconIds[3] = R.drawable.exit_64;
        mIcons[3] = BitmapFactory.decodeResource(res, mIconIds[3]);
    }

    // Connection to BleService using Messenger
    private ServiceConnection mConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.i(TAG, "onServiceConnected()");
            ServiceMessenger = new Messenger(binder);
            try {
                // Register
                Message msg = Message.obtain(null, 0);
                msg.replyTo = mMessenger;
                ServiceMessenger.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Called when a connection to the Service has been lost.
        // This typically happens when the process hosting the service has
        // crashed or been killed.
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "onServiceDisconnected()");
        }
    };

    // Send void message to BleService
    public void sendVoidToSM(int i) {
        try {
            Message msg = Message.obtain(null, i);
            ServiceMessenger.send(msg);
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
        }
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

    private void selectItem(int position) {
        mDrawerLayout.closeDrawer(mDrawerList);

        if (position == 2 || position == 3) {
            Global.exitDialog(MainActivity.this);
        } else {
            if (position != curDrawerPos) {
                curDrawerPos = position;
                Fragment tmp = null;
                if (position == 0)
                    tmp = new HrmFragment();
                else if (position == 1)
                    tmp = new PatientNotesFragment();

                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, tmp, mTitles[position]).commit();
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

    private class MyListAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        MyListAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mTitles.length;
        }

        public Object getItem(int position) {
            return mTitles[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.drawer_list_item, null);
                holder = new ViewHolder();
                holder.ll = (LinearLayout) convertView.findViewById(R.id.linearLayout1);
                holder.icon = (ImageView) convertView.findViewById(R.id.imageView1);
                holder.title = (TextView) convertView.findViewById(R.id.textView1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

//            holder.ll.setBackgroundColor(Global.color_Grey);
            holder.title.setText(mTitles[position]);
            holder.icon.setImageBitmap(mIcons[position]);
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
        if (ifBackPressed) {
            ifBackPressed = false;
            mHandler.removeCallbacks(mRunnable);
            moveTaskToBack(false);
            return;
        }
        ifBackPressed = true;
        Global.toastMakeText(MainActivity.this, getResources().getString(R.string.main_pressback));
        mHandler.postDelayed(mRunnable, Global.backInterval);
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

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