package com.example.liaoqihua.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class ControlActivity extends Activity {
    public static final String COLD_DATA = "coldData";
    public static final String WARM_DATA = "warmData";
    public static final String LIGHT_NAME = "light_name";

    public float maxVal = 100.0f;
    byte[] mLightVal = {0,0,0};
    String mName = "";
    float beginX = 0,beginY = 0;
    boolean lightOpen = false;

    //一堆UI组件
    EditText mLightName;
    TextView mProgress1 = null;
    TextView mProgress2 = null;
    SeekBar mSeekBar1 = null;
    SeekBar mSeekBar2 = null;
    ImageButton mBackBtn = null;
    Switch mSwitch = null;

    BluetoothGattService service;
    private static ControlActivity mCtrlAct = null;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BltService.ACTION_GATT_CHARACTER_DISCOVER);
        intentFilter.addAction(BltService.ACTION_GATT_CHRACTER_READ);
        return intentFilter;
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context,Intent intent)
        {
            final String action = intent.getAction();
            if (action.equals(BltService.ACTION_GATT_CHARACTER_DISCOVER)) {
//                Utils.getInstance().showLogI("发现属性!,开始读取");
//                BltService.getInstance().readCharacteristic();
            }
            else if (action.equals(BltService.ACTION_GATT_CHRACTER_READ)) {
                Utils.getInstance().showLogI("读取属性成功");
//                mLightVal = intent.getByteArrayExtra(BltService.CHARACTERISTIC_DATA);
//                ControlActivity.this.updateUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_view);
        Utils.getInstance().pushActivity(this);
        mCtrlAct = this;
//        Log.i("蓝牙2",this.toString());

        Intent it = getIntent();
        CharSequence content = it.getCharSequenceExtra("lightName");
        mLightName = (EditText)findViewById(R.id.lightName);
        mLightName.setText(content);
        mName = content.toString();
        Log.i("灯光名字",mName);

        mSwitch = (Switch)findViewById(R.id.switch1);
        mSeekBar1 = (SeekBar)findViewById(R.id.seekBar);
        mSeekBar2 = (SeekBar)findViewById(R.id.seekBar2);
        mProgress1 = (TextView)findViewById(R.id.textView);
        mProgress2 = (TextView)findViewById(R.id.textView2);

        AlphaAnimation hideAnimation = new AlphaAnimation(1.0f, 0.0f);
        hideAnimation.setDuration(10);
        hideAnimation.setFillAfter( true );
        mProgress1.startAnimation(hideAnimation);
        mProgress2.startAnimation(hideAnimation);
        initSeekBarListener();

        retriveData();
        mBackBtn = (ImageButton)findViewById(R.id.back_btn);
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
                ControlActivity.this.finish();
                overridePendingTransition(0,R.anim.base_slide_right_out);
            }
        });

        //开关改变
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                {
                    Utils.getInstance().showLogI("开启");
                    BltService.getInstance().writeCharacteritic(mLightVal);
                }
                else
                {
                    Utils.getInstance().showLogI("关闭");
                    byte[] tmp = {0,0,0};
                    BltService.getInstance().writeCharacteritic(tmp);
                }
                lightOpen = isChecked;
            }
        });
        mSwitch.setChecked(true);

        updateUI();
        registerReceiver(mReceiver, makeGattUpdateIntentFilter());
    }

    protected void onStart()
    {
        super.onStart();
        Utils.getInstance().showLogI("onStart");
        Intent it = getIntent();
        CharSequence content = it.getCharSequenceExtra("lightName");
        if (content != null)
            Utils.getInstance().showLogI(content.toString());
    }

    protected void onRestart()
    {
        super.onRestart();
        Utils.getInstance().showLogI("onRestart");
        Intent it = getIntent();
        CharSequence content = it.getCharSequenceExtra("lightName");
        if (content != null)
            Utils.getInstance().showLogI(content.toString());
    }

    private void initSeekBarListener()
    {
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar == mSeekBar1)
                {
                    Utils.getInstance().showLogI("seekBar1变化" + progress);
                    mLightVal[0] = (byte)(progress/100.0f*maxVal);
                    mProgress1.setText(progress + "%");
                }
                else
                {
                    Utils.getInstance().showLogI("seekBar2变化" + progress);
                    mLightVal[2] = (byte)(progress/100.0f*maxVal);
                    mProgress2.setText(progress + "%");
                }
                Utils.getInstance().showLogI(mLightVal[0]+"  "+mLightVal[1]+"  "+mLightVal[2]);
                if (lightOpen)
                    BltService.getInstance().writeCharacteritic(mLightVal);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                AlphaAnimation showAnimation = new AlphaAnimation(0.0f, 1.0f);
                showAnimation.setDuration(800);
                showAnimation.setFillAfter( true );
                if (seekBar == mSeekBar1)
                {
                    Utils.getInstance().showLogI("seekBar1 onStartTrackingTouch");
                    mProgress1.startAnimation(showAnimation);
                }
                else
                {
                    Utils.getInstance().showLogI("seekBar2 onStartTrackingTouch");
                    mProgress2.startAnimation(showAnimation);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                AlphaAnimation hideAnimation = new AlphaAnimation(1.0f, 0.0f);
                hideAnimation.setDuration(800);
                hideAnimation.setFillAfter( true );
                if (seekBar == mSeekBar1)
                {
                    Utils.getInstance().showLogI("seekBar1 onStopTrackingTouch");
                    mProgress1.startAnimation(hideAnimation);
                }
                else
                {
                    Utils.getInstance().showLogI("seekBar2 onStopTrackingTouch");
                    mProgress2.startAnimation(hideAnimation);
                }
            }
        };

        mSeekBar1.setOnSeekBarChangeListener(listener);
        mSeekBar2.setOnSeekBarChangeListener(listener);
    }

    private void updateUI()
    {
        mLightName.setText(mName);
        int p1 = (int)(mLightVal[0]/maxVal*100.0f);
        int p2 = (int)(mLightVal[2]/maxVal*100.0f);
        Utils.getInstance().showLogI("更新UI: "+p1+"  " +p2);

        mSeekBar1.setProgress(p1);
        mSeekBar2.setProgress(p2);
    }

    //保存数据
    private void saveData()
    {
        Utils.getInstance().showLogI("保存数据");
        String addr = LightListAdapter.getInstance().getConnectedAddress();
        Utils.getInstance().showLogI("连接的灯光地址: "+addr);
        if (addr != null && !addr.equals(""))
        {
            SharedPreferences settings = ControlActivity.this.getSharedPreferences(addr,0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(COLD_DATA,(int)mLightVal[0]);
            editor.putInt(WARM_DATA,(int)mLightVal[2]);
            editor.commit();
            Utils.getInstance().setStringForKey(addr,mLightName.getText().toString());
        }
    }

    //读取数据
    private void retriveData()
    {
        Utils.getInstance().showLogI("取数据");
        String addr = LightListAdapter.getInstance().getConnectedAddress();
        if (addr != null && !addr.equals(""))
        {
            SharedPreferences settings = ControlActivity.this.getSharedPreferences(addr,0);
            mLightVal[0] = (byte)settings.getInt(COLD_DATA,0);
            mLightVal[2] = (byte)settings.getInt(WARM_DATA,0);
            mLightVal[1] = 0;
            mName = Utils.getInstance().getStringForKey(addr);
        }
    }

    protected void onStop()
    {
        Utils.getInstance().showLogI("ControlActivity onStop");
        super.onStop();
    }

    protected void onDestroy()
    {
        Utils utils = Utils.getInstance();
        utils.showLogI("ControlActivity onDestroy");
        BltService.getInstance().disconnect();
        utils.removeActivity(this);
        if (utils.getActivityCount() == 0)
        {
            utils.showLogI("已无activity 终止服务!");
            Intent intent = new Intent(ControlActivity.this,BltService.class);
            stopService(intent);
        }
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
