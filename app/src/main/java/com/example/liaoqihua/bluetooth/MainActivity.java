package com.example.liaoqihua.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.liaoqihua.utils.SimpleFooter;
import com.liaoqihua.utils.SimpleHeader;
import com.liaoqihua.utils.ZrcListView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    BluetoothAdapter mBtAdapter = null;
    BluetoothManager mBtManager = null;
    LightListAdapter mListAdapter = null;
    BluetoothAdapter.LeScanCallback mLeScanCb = null;
    BluetoothLeScanner mScanner = null;
    ScanCallback mScanCallback = null;

    ServiceConnection mServiceConnectiion = null;
    private Handler mHandler = null;
    private byte lastLightNum = 0;
    boolean isScaning = false;
    boolean listInited = false;

    ZrcListView mListView = null;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_GATT_CONNECTED);
        intentFilter.addAction(Constant.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context,Intent intent)
        {
            final String action = intent.getAction();
            if (action.equals(Constant.ACTION_GATT_CONNECTED)) {
                Utils.getInstance().showLogI("连接成功,更新数据!");
                mListAdapter.notifyDataSetChanged();
            }
            else if (action.equals(Constant.ACTION_GATT_DISCONNECTED)) {
                Utils.getInstance().showLogI("断开连接成功,更新数据!");
                mListAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.getInstance().showLogI("MainActivity onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.getInstance().pushActivity(this);
        mHandler = new Handler();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this,"sorry 系统不支持蓝牙!", Toast.LENGTH_LONG).show();
        }

        //蓝牙搜索回调(新版)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mScanCallback = new ScanCallback() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    final BluetoothDevice btDevice = result.getDevice();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.getInstance().showLogI("(new API)发现蓝牙设备: "+String.format("%s  %s ",btDevice.getName(),btDevice.getAddress()));
                            mListAdapter.addDevice(btDevice);
                        }
                    });

                }

                public void onScanFailed(int errorCode)
                {
                    Utils.getInstance().showLogI("scan failed! " + errorCode);
                }
            };
        }
        else
        {
            //蓝牙搜索回调(旧版)
            mLeScanCb = new BluetoothAdapter.LeScanCallback()
            {
                public void onLeScan(final BluetoothDevice btDevice, int paramInt, final byte[] arrayOfByte)
                {
                    final int tmp = paramInt;
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Utils.getInstance().showLogI("发现蓝牙设备: "+String.format("%s  %s ",btDevice.getName(),btDevice.getAddress()));
//                        Utils.getInstance().showLogI("发现蓝牙设备: "+ btDevice+"  "+tmp+" "+arrayOfByte);
                            mListAdapter.addDevice(btDevice);
                        }
                    });
                }
            };
        }


        Utils.getInstance().setActivity(this);
//
        checkBleAndGPS();
    }

    protected void onRestart()
    {
        super.onRestart();
        Utils.getInstance().showLogI("MainActivity onRestart");
    }

    protected void onStart()
    {
        super.onStart();
        Utils.getInstance().showLogI("MainActivity onStart");
    }

    protected void onStop()
    {
        Utils.getInstance().showLogI("MainActivity onStop");
        super.onStop();
    }

    protected void onDestroy()
    {
        Utils utils = Utils.getInstance();
        utils.showLogI("MainActivity onDestroy");

        utils.removeActivity(this);
        if (utils.getActivityCount() == 0)
        {
            utils.showLogI("已无activity 终止服务!");
            Intent intent = new Intent(MainActivity.this,BltService.class);
            stopService(intent);
        }
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == 123)
        {
            Utils.getInstance().showLogI("刷新灯光!");
            doNext();
            mListAdapter.clear();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListView.refresh();
                        }
                    });
                }
            }, 800);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Utils.getInstance().showLogI("按下返回键!");
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("是否退出？");
            builder.setTitle("提示");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Utils.getInstance().finishAllActs();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return true;
        }
        return false;
    }

    void doNext()
    {
        if (listInited) return;
        listInited = true;
        //启动服务
        Intent intent = new Intent(this,BltService.class);
        startService(intent);

        //注册广播接收器
        registerReceiver(mReceiver, makeGattUpdateIntentFilter());

        //初始化列表
        initList();
        //开始搜索
        startScan(true);
    }

    private void refreshCallback(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListAdapter.notifyDataSetChanged();
                int num = mListAdapter.getCount();
                if (num == 0)
                {
                    mListView.setRefreshSuccess("没有检测到蓝牙灯"); // 通知加载成功
                }
                else if (num > lastLightNum)
                {
                    mListView.setRefreshSuccess(String.format("新增%d盏灯",num - lastLightNum)); // 通知加载成功
                }
                else
                {
                    mListView.setRefreshSuccess("没有检测到新的蓝牙灯"); // 通知加载成功
                }
                mListView.startLoadMore(); // 开启LoadingMore功能
            }
        }, 2 * 1000);
    }

    void initList()
    {
        mListAdapter = new LightListAdapter(this,R.layout.view_item);

        mListView = (ZrcListView)findViewById(R.id.listView);
        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new ZrcListView.OnItemClickListener() {
            @Override
            public void onItemClick(ZrcListView parent, View view, int position, long id) {
                MainActivity.this.startScan(false);
                Utils.getInstance().showLogI("单击listView");
                BluetoothDevice bltDevice = (BluetoothDevice)mListAdapter.getItem(position);
                if (!BltService.getInstance().isConnected(bltDevice.getAddress()))
                {
                    Toast.makeText(MainActivity.this,"请先连接设备!",Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this,ControlActivity.class);
                LightListAdapter.ViewHolder viewHolder = (LightListAdapter.ViewHolder) view.getTag();
                CharSequence content = (String)viewHolder.lightName.getText();
                intent.putExtra("lightName",content);
                MainActivity.this.startActivityForResult(intent,123);
            }
        });

        float density = getResources().getDisplayMetrics().density;
        mListView.setFirstTopOffset((int) (50 * density));

        // 设置下拉刷新的样式（可选，但如果没有Header则无法下拉刷新）
        SimpleHeader header = new SimpleHeader(this);
        header.setTextColor(0xff0066aa);
        header.setCircleColor(0xff33bbee);
        mListView.setHeadable(header);

        //设置加载更多的样式（可选）
        SimpleFooter footer = new SimpleFooter(this);
        footer.setCircleColor(0xff33bbee);
        mListView.setFootable(footer);

        // 下拉刷新事件回调（可选）
        mListView.setOnRefreshStartListener(new ZrcListView.OnStartListener() {
            @Override
            public void onStart() {
                startScan(true);
                MainActivity.this.refreshCallback();
            }
        });

        // 加载更多事件回调（可选）
        mListView.setOnLoadMoreStartListener(new ZrcListView.OnStartListener() {
            @Override
            public void onStart() {
                startScan(true);
                loadMore();
            }
        });
        mListView.refresh(); // 主动下拉刷新
    }

    private void loadMore(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.setLoadMoreSuccess();
            }
        }, 3 * 1000);
    }

//    public void btnClick(View btn)
//    {
//        Intent intent = new Intent(MainActivity.this,ControlActivity.class);
//        startActivity(intent);
//    }

    //选中菜单Item后触发
    public boolean onContextItemSelected(MenuItem item){
        //关键代码在这里
        AdapterView.AdapterContextMenuInfo menuInfo;
        menuInfo =(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        //输出position
        Toast.makeText(MainActivity.this,String.valueOf(menuInfo.position), Toast.LENGTH_LONG).show();
        TextView tv = (TextView)menuInfo.targetView;
        return super.onContextItemSelected(item);

    }

    public void startScan(boolean isScan)
    {
        if (isScan) {
            if (isScaning) {
                return;
            }
            isScaning = true;
            Utils.getInstance().showLogI("开始搜索蓝牙灯");
//            mListAdapter.clear();
            if (useNewBleAPI())
            {
                mScanner = mBtAdapter.getBluetoothLeScanner();
                mScanner.startScan(mScanCallback);
            }
            else
            {
                mBtAdapter.startLeScan(mLeScanCb);
            }
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    MainActivity.this.startScan(false);
                }
            },2000);
        }
        else
        {
            if (!isScaning)
            {
                return;
            }
            isScaning = false;
            Utils.getInstance().showLogI("停止搜索");
            if (useNewBleAPI())
            {
                mScanner = mBtAdapter.getBluetoothLeScanner();
                mScanner.stopScan(mScanCallback);
            }
            else
            {
                mBtAdapter.stopLeScan(mLeScanCb);
            }
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    void checkBleAndGPS()
    {
        //蓝牙
        mBtManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();

        boolean allReady = true;
        //安卓6.0以后需要gps开启
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                allReady = false;
                //申请定位权限
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},123);
            }

            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
            {
                allReady = false;
                //申请蓝牙权限
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH},456);
            }

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
            {
                allReady = false;
                //申请蓝牙权限
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN},789);
            }

            //gps没有开启  神奇，targetSDK为23时竟然要打开GPS才能用
//            if (!isGPSEnabled())
//            {
//                Toast.makeText(this,"请开启GPS功能",Toast.LENGTH_LONG).show();
//            }
        }
        if (mBtAdapter != null && !mBtAdapter.isEnabled())
        {
            allReady = false;
            Intent intent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,123);
        }

        if (allReady)
        {
            doNext();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123 ||requestCode == 456 || requestCode == 789)
        {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("app需要蓝牙及GPS位置权限");
                builder.setTitle("提示");
                builder.setPositiveButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Utils.getInstance().finishAllActs();
                    }
                });
//                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                });
            }
        }
    }

    public boolean isGPSEnabled()
    {
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return gpsEnabled;
    }

    void openGPS(Context context) {
        Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        startActivityForResult(intent,0);
    }

    public boolean useNewBleAPI()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}

