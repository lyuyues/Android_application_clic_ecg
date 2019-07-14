package ca.uvic.ece.ecg.heartcarer1;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
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
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This Activity is the main Activity after logging in (or with no account)
 */
@SuppressLint("HandlerLeak")
public class MainActivity extends Activity implements HrmFragment.sendVoidToSMListener {
    private final String TAG = "MainActivity";
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mTitles = new String[6];
    private int curDrawerPos = 0;
    public static BluetoothAdapter mBluetoothAdapter;
    private Messenger ServiceMessenger;
    private boolean ifBackPressed = false;
    private Menu myMenu;

    private ArrayList<Integer> menuItemList = new ArrayList<Integer>();
    private boolean isNewFragment = false;
    private Handler mHandler = new Handler();
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            ifBackPressed = false;
        }
    };
    // Local Messenger used to talk to ServiceMessenger, Message received by
    // IncomingHandler
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    // Location Services
    public static boolean ifLCConnected = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.main_activity);
        final String userName = getIntent().getStringExtra("userName");
        initTitles();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new MyListAdapter(this, mTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, mDrawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View drawerView) {
                setTitle(mTitles[curDrawerPos]);
                if (!isNewFragment) {
                    for (int i : menuItemList) {
                        myMenu.getItem(i).setVisible(true);
                    }
                } else {
                    isNewFragment = false;
                }
            }

            public void onDrawerOpened(View drawerView) {
                setTitle(getResources().getString(R.string.app_name_title)
                        + (userName.equals("") ? "" : (" - " + userName)));
                menuItemList.clear();
                for (int i = 0; i < myMenu.size(); i++) {
                    if (myMenu.getItem(i).isVisible()) {
                        myMenu.getItem(i).setVisible(false);
                        menuItemList.add(i);
                    }
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if (savedInstanceState == null) {
            selectItem(0, true);
        }
        bindService(new Intent(MainActivity.this, BleService.class), mConn, Context.BIND_AUTO_CREATE);
        startService(new Intent(MainActivity.this, BleService.class));
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        Global.toastMakeText(MainActivity.this,
                getResources().getString(R.string.main_hi) + (userName.equals("") ? "" : (" " + userName))
                        + getResources().getString(R.string.main_welcome) + getResources().getString(R.string.app_name)
                        + "!");
        Memory_Managemnt_Util memory = new Memory_Managemnt_Util();
        memory.Memory_management();
    }

    private void initTitles() {
        mTitles[0] = getResources().getString(R.string.main_hrm);
        mTitles[1] = getResources().getString(R.string.main_data);
        mTitles[2] = getResources().getString(R.string.main_settings);
        mTitles[3] = getResources().getString(R.string.main_about);
        mTitles[4] = getResources().getString(R.string.main_mydoctors);
        mTitles[5] = getResources().getString(R.string.main_exit);

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
            HrmFragment hrmFrag = (HrmFragment) getFragmentManager()
                    .findFragmentByTag(getResources().getString(R.string.main_hrm));
            if (hrmFrag != null)
                hrmFrag.handleMainActivityMes(msg);
        }
    }

    // Set the title and icon of ActionBar
    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
        int tmp = R.drawable.main_heart_beat_64;
        if (title.equals(mTitles[0]))
            tmp = R.drawable.hrmonitor_64;
        else if (title.equals(mTitles[1]))
            tmp = R.drawable.clouddata_128;
        else if (title.equals(mTitles[2]))
            tmp = R.drawable.setting_128;
        else if (title.equals(mTitles[3]))
            tmp = R.drawable.about_64;
        else if (title.equals(mTitles[4]))
            tmp = R.drawable.doctor;
        getActionBar().setIcon(tmp);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position, false);
        }
    }

    /**
     * Handle drawer item clicked event
     * 
     * @param position:
     *            Position selected
     * @param ifFirst:
     *            If it's first to launch HrmFragment
     */
    private void selectItem(int position, boolean ifFirst) {
        if (ifFirst) {
            setTitle(mTitles[position]);
            getFragmentManager().beginTransaction().replace(R.id.content_frame, new HrmFragment(), mTitles[position])
                    .commit();
        } else {
            if (position == 5) {
                mDrawerLayout.closeDrawer(mDrawerList);
                mDrawerList.setItemChecked(curDrawerPos, true);
                Global.exitDialog(MainActivity.this);
                return;
            }
            if ((!Global.ifRegUser) && (position == 1 || position == 2 || position == 4)) {
                Global.toastMakeText(MainActivity.this, getResources().getString(R.string.avail));
                mDrawerList.setItemChecked(curDrawerPos, true);
                return;
            }
            if (position != curDrawerPos) {
                curDrawerPos = position;
                isNewFragment = true;
                Fragment tmp = null;
                if (position == 0)
                    tmp = new HrmFragment();
                else if (position == 1)
                    tmp = new DataManageFragment();
                else if (position == 2)
                    tmp = new SettingFragment();
                else if (position == 3)
                    tmp = new AboutFragment();
                else if (position == 4)
                    tmp = new DoctorList();
                getFragmentManager().beginTransaction().replace(R.id.content_frame, tmp, mTitles[position]).commit();
            }
            mDrawerLayout.closeDrawer(mDrawerList);
        }
        mDrawerList.setItemChecked(position, true);
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

    // Customized ListAdapter for Drawer
    private class MyListAdapter extends BaseAdapter {
        private String[] mList;
        private LayoutInflater inflater;
        private Bitmap BM_hrm, BM_settings, BM_data, BM_exit, BM_about, BM_DocList;

        public MyListAdapter(Context context, String[] mL) {
            mList = mL;
            inflater = LayoutInflater.from(context);
            Resources res = context.getResources();
            BM_hrm = BitmapFactory.decodeResource(res, R.drawable.hrmonitor_64);
            BM_settings = BitmapFactory.decodeResource(res, R.drawable.setting_64);
            BM_data = BitmapFactory.decodeResource(res, R.drawable.clouddata_64);
            // BM_profile =
            // BitmapFactory.decodeResource(res,R.drawable.personal_64);
            // BM_noti =
            // BitmapFactory.decodeResource(res,R.drawable.notification_64);
            BM_exit = BitmapFactory.decodeResource(res, R.drawable.exit_64);
            BM_about = BitmapFactory.decodeResource(res, R.drawable.about_64);
            BM_DocList = BitmapFactory.decodeResource(res, R.drawable.doctor);
        }

        public int getCount() {
            return mList.length;
        }

        public Object getItem(int position) {
            return mList[position];
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
            if ((!Global.ifRegUser) && (position == 1 || position == 2 || position == 4)) {
                holder.ll.setBackgroundColor(Global.color_Grey);
            }
            holder.title.setText(mList[position]);
            if (position == 0)
                holder.icon.setImageBitmap(BM_hrm);
            else if (position == 1)
                holder.icon.setImageBitmap(BM_data);
            else if (position == 2)
                holder.icon.setImageBitmap(BM_settings);
            // else if(position==3) holder.icon.setImageBitmap(BM_noti);
            // else if(position==4) holder.icon.setImageBitmap(BM_general);
            else if (position == 3)
                holder.icon.setImageBitmap(BM_about);
            else if (position == 4)
                holder.icon.setImageBitmap(BM_DocList);
            else
                holder.icon.setImageBitmap(BM_exit);
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

    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
    }

    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
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