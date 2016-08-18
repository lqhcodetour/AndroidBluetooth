package com.example.liaoqihua.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Contacts;
import android.util.Log;

import java.util.List;

/**
 * Created by liaoqihua on 16/6/20.
 */
public class BltService extends Service{

    public static String ACTION_GATT_CONNECTED = "gatt_connected";
    public static String ACTION_GATT_DISCONNECTED = "gatt_disconnected";
    public static String ACTION_GATT_CHARACTER_DISCOVER = "gatt_charac_discover";
    public static String ACTION_GATT_CHRACTER_READ = "gatt_charac_read";
    public static String CHARACTERISTIC_DATA = "characteristic_data";

    public static  BltService mService = null;
    private BluetoothManager mBtManager = null;
    private BluetoothAdapter mAdapter = null;
    private BluetoothGatt mBtGatt = null;
    private BluetoothGattCallback mGattCallback = null;
    private BluetoothGattCharacteristic mCharacter = null;

    private String mLastAddress = null;
    private boolean isBusy = false;
    BluetoothGattService b;

    private int curConnectState = BluetoothGatt.STATE_DISCONNECTED;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent,int flags,int startId)
    {
        Utils.getInstance().showLogE("服务启动");
        return START_NOT_STICKY;
    }

    public static BltService getInstance()
    {
        return mService;
    }

    public void onCreate()
    {
        Utils.getInstance().showLogE("创建服务");
        mService = this;
        mBtManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (mBtManager == null) {
            Utils.getInstance().showLogE("获取蓝牙服务失败!");
            stopSelf();
            return;
        }
        mAdapter = mBtManager.getAdapter();

        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    if (newState == BluetoothGatt.STATE_CONNECTED)
                    {
                        curConnectState = BluetoothGatt.STATE_CONNECTED;
                        Utils.getInstance().showLogI("连接成功!");
                        LightListAdapter.getInstance().setConnectedAddress(mLastAddress);
                        updateBroadCast(ACTION_GATT_CONNECTED);
                        mBtGatt.discoverServices();
                    }
                    else if (newState == BluetoothGatt.STATE_DISCONNECTED)
                    {
                        curConnectState = BluetoothGatt.STATE_DISCONNECTED;
                        Utils.getInstance().showLogI("断开成功!");
                        LightListAdapter.getInstance().setConnectedAddress("");
                        updateBroadCast(ACTION_GATT_DISCONNECTED);
                    }
                }
            }

            public void onCharacteristicRead(BluetoothGatt paramBluetoothGatt, BluetoothGattCharacteristic paramBluetoothGattCharacteristic, int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    Utils.getInstance().showLogI("读数据成功!");
                    byte[]data = paramBluetoothGattCharacteristic.getValue();
//                    updateBroadCast(ACTION_GATT_CHRACTER_READ,data);

                    Utils.getInstance().showLogI(data[0] + "  "+data[1]+"  "+data[2]);
                }
            }

            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    Utils.getInstance().showLogI("写数据成功!");
                }
            }

            public void onServicesDiscovered(BluetoothGatt paramBluetoothGatt, int paramInt)
            {
                Utils.getInstance().showLogI("发现服务");
                boolean getCharacterSuccess = false;
                List<BluetoothGattService> services = mBtGatt.getServices();
                for (BluetoothGattService s:services)
                {
                    String uuidStr = s.getUuid().toString();
                    int pos = uuidStr.indexOf("-");
                    if(pos > 0 )
                    {
                        String subStr = uuidStr.substring(0,pos);
                        if (subStr.equals("0000fff0"))
                        {
                            Utils.getInstance().showLogI("获取属性成功!");
                            List<BluetoothGattCharacteristic> chars = s.getCharacteristics();
                            mCharacter = chars.get(0);

//                            updateBroadCast(ACTION_GATT_CHARACTER_DISCOVER);
                            readCharacteristic();
                            getCharacterSuccess = true;
                            break;
                        }
                    }
                }

                if (!getCharacterSuccess)
                {
                    Utils.getInstance().showLogI("获取属性失败! 2");
                }
            }
        };
    }

    private void updateBroadCast(String action)
    {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void updateBroadCast(String action,byte[] extraData)
    {
        Intent intent = new Intent(action);
        intent.putExtra(CHARACTERISTIC_DATA,extraData);
        sendBroadcast(intent);
    }

    public boolean connect(String address)
    {
        if (isBusy)
        {
            Utils.getInstance().showLogE("繁忙中...");
            return false;
        }

        if (mLastAddress != null && mLastAddress.equals(address) && mBtGatt != null )
        {
            if (mBtGatt.connect())
            {
                curConnectState = BluetoothGatt.STATE_CONNECTED;
                return true;
            }
            else
            {
                curConnectState = BluetoothGatt.STATE_DISCONNECTED;
                return false;
            }
        }

        BluetoothDevice device = mAdapter.getRemoteDevice(address);

        int i = mBtManager.getConnectionState(device,BluetoothProfile.GATT);
        boolean cangoon = false;
        switch(i)
        {
            case BluetoothProfile.STATE_CONNECTED:
                Utils.getInstance().showLogE("已连接");
                cangoon = false;
                isBusy = false;
                break;
            case BluetoothProfile.STATE_CONNECTING:
                Utils.getInstance().showLogE("正在连接");
                isBusy = true;
                cangoon = false;
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                Utils.getInstance().showLogE("正在断开");
                isBusy = true;
                cangoon = false;
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Utils.getInstance().showLogI("已断开");
                isBusy = false;
                cangoon = true;
                break;
            default:
        }

        if (!cangoon)
        {
            return false;
        }
        mLastAddress = address;
        mBtGatt = device.connectGatt(BltService.this,false,mGattCallback);
        curConnectState = BluetoothGatt.STATE_CONNECTED;
        return true;
    }

    public void disconnect()
    {
        if (curConnectState == BluetoothGatt.STATE_CONNECTED)
        {
            mBtGatt.disconnect();
        }
    }

    public boolean isConnected(String addr)
    {
        return mLastAddress!= null && mLastAddress.equals(addr) && curConnectState == BluetoothGatt.STATE_CONNECTED;
    }

    public void readCharacteristic()
    {
        mBtGatt.readCharacteristic(mCharacter);
    }

    public void writeCharacteritic(byte[] data)
    {
        if (mCharacter != null)
        {
            mCharacter.setValue(data);
            mBtGatt.writeCharacteristic(mCharacter);
        }
    }

    public void onDestroy()
    {
        Utils.getInstance().showLogE("销毁服务");
        if (mBtGatt != null) {
            mBtGatt.disconnect();
            mBtGatt.close();
        }

        mService = null;
        mAdapter = null;
        mLastAddress = null;
        mBtGatt = null;
    }
}
