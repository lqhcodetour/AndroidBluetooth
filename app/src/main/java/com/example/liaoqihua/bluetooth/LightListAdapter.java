package com.example.liaoqihua.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by liaoqihua on 16/6/11.
 */
public class LightListAdapter extends BaseAdapter {
    private int mResourceId = 0;
    Context mContext = null;
    List<BluetoothDevice> mBTDevice;
    String connectedAddress = "";
    private static LightListAdapter mLightListAdapter = null;

    LightListAdapter(Context ctx,int resourceId){
        mContext = ctx;
        mResourceId = resourceId;
        mBTDevice = new ArrayList<BluetoothDevice>();
        mLightListAdapter = this;
    }

    public static LightListAdapter getInstance()
    {
        return mLightListAdapter;
    }

    public void addDevice(BluetoothDevice device)
    {
       if (!mBTDevice.contains(device))
       {
            mBTDevice.add(device);
       }
    }

    public void setConnectedAddress(String address)
    {
        connectedAddress = address;
    }

    public String getConnectedAddress()
    {
        return connectedAddress;
    }

    @Override
    public int getCount() {
        return mBTDevice.size();
    }

    public void clear(){mBTDevice.clear();}

    @Override
    public Object getItem(int position) {
        return mBTDevice.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder vieHolder;
        BluetoothDevice device = mBTDevice.get(position);
        String devName = device.getName();
        final String devAddress = device.getAddress();
        String name = Utils.getInstance().getStringForKey(devAddress);
        String finalName = name.equals("") ? devName:name;

        TextView linkState = null;
        TextView lightName = null;
        Button linkBtn = null;

//        Utils.getInstance().showLogI("发现蓝牙灯: "+devAddress);
        if (convertView == null)
        {
            vieHolder = new ViewHolder();
            view = LayoutInflater.from(mContext).inflate(mResourceId,null);
            lightName = (TextView)view.findViewById(R.id.lightName);
            lightName.setText(finalName);
            linkState = (TextView)view.findViewById(R.id.linkState);
            linkBtn = (Button)view.findViewById(R.id.linkButton);

            vieHolder.lightName = lightName;
            vieHolder.linkState = linkState;
            vieHolder.linkBtn = linkBtn;
            view.setTag(vieHolder);
        }
        else
        {
            view = convertView;
            vieHolder = (ViewHolder)view.getTag();

            linkState = vieHolder.linkState;
            lightName = vieHolder.lightName;
            linkBtn = vieHolder.linkBtn;
            lightName.setText(finalName);
        }

        if (linkState != null && linkBtn != null)
        {
            linkBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity)mContext).startScan(false);
                    Utils.getInstance().showLogI("网卡地址: "+devAddress);
                    BltService.getInstance().connect(devAddress);
                }
            });

            if(!devAddress.equals(connectedAddress))
            {
                linkState.setVisibility(View.INVISIBLE);
                linkState.setText("未连接");

                linkBtn.setVisibility(View.VISIBLE);
                linkBtn.setText("连接");
            }
            else
            {
                linkState.setVisibility(View.VISIBLE);
                linkState.setText("已连接");

                linkBtn.setVisibility(View.INVISIBLE);
                linkBtn.setText("连接");
            }
        }
        return view;
    }

    static class ViewHolder {
        TextView lightName;
        TextView linkState;
        Button linkBtn;
    }
}
