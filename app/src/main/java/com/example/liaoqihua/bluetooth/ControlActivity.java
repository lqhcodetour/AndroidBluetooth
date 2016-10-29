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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class ControlActivity extends Activity {
    //各种变量
    public float maxVal = 100.0f;
    byte[] mLightVal = {0,0,0};
    String mName = "";
    float beginX = 0,beginY = 0;
    boolean lightBtnClick = false;
    boolean lightOpen = false;
    int []ids = {R.id.imageCold,R.id.imageNormal,R.id.imageWarm,R.id.imageSleep};
    final int []lightOffName = {R.drawable.lengguang_guan,R.drawable.ziranguang_guan,R.drawable.nuanguang_guan,R.drawable.shengdian_guan};
    final int []lightOnName = {R.drawable.lengguang_kai,R.drawable.ziranguang_kai,R.drawable.nuanguang_kai,R.drawable.shengdian_kai};
    final int []blueDotTag = {R.id.blueDot1,R.id.blueDot2,R.id.blueDot3,R.id.blueDot4,R.id.blueDot5};
    final int []greenDotTag = {R.id.greenDot1,R.id.greenDot2,R.id.greenDot3,R.id.greenDot4,R.id.greenDot5};

    //一堆UI组件
    EditText mLightName;
    TextView mProgress1 = null;
    TextView mProgress2 = null;
    SeekBar mSeekBar1 = null;
    SeekBar mSeekBar2 = null;
    ImageButton mBackBtn = null;
    ImageView[]mBlueDot = {null,null,null,null,null};
    ImageView[]mGreenDot = {null,null,null,null,null};

    ImageButton []mLightBtn = {null,null,null,null};
    ImageButton mSwitch = null;

    BluetoothGattService service;
    private static ControlActivity mCtrlAct = null;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_GATT_CHARACTER_DISCOVER);
        intentFilter.addAction(Constant.ACTION_GATT_CHRACTER_READ);
        return intentFilter;
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context,Intent intent)
        {
            final String action = intent.getAction();
            if (action.equals(Constant.ACTION_GATT_CHARACTER_DISCOVER)) {
                Utils.getInstance().showLogI("ControlView 发现属性!");
            }
            else if (action.equals(Constant.ACTION_GATT_CHRACTER_READ)) {
                mLightVal = intent.getByteArrayExtra(Constant.CHARACTERISTIC_DATA);
                Utils.getInstance().showLogI(String.format("读取属性成功: %d %d %d",mLightVal[0],mLightVal[1],mLightVal[2]));
                ControlActivity.this.updateUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_view);
        Utils.getInstance().pushActivity(this);
        mCtrlAct = this;

        Intent it = getIntent();
        CharSequence content = it.getCharSequenceExtra("lightName");
        mLightName = (EditText)findViewById(R.id.lightName);
        mLightName.setText(content);
        mName = content.toString();
        Log.i("灯光名字",mName);

        mSwitch = (ImageButton)findViewById(R.id.switchBtn);
        mSeekBar1 = (SeekBar)findViewById(R.id.seekBar);
        mSeekBar2 = (SeekBar)findViewById(R.id.seekBar2);
        mProgress1 = (TextView)findViewById(R.id.textView);
        mProgress2 = (TextView)findViewById(R.id.textView2);

        //点
        for (int i = 0; i < 5; i++)
        {
            mBlueDot[i] = (ImageView)findViewById(blueDotTag[i]);
            mGreenDot[i] = (ImageView)findViewById(greenDotTag[i]);
        }

        //各种灯光模式
        final int lightVal[][] = {
            {100,0},
            {100,100},
            {0,100},
            {15,15},
        };
        for (int i = 0; i < 4;i++) {
            mLightBtn[i] = (ImageButton)findViewById(ids[i]);
            mLightBtn[i].setTag(Integer.valueOf(i));
            mLightBtn[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!lightOpen)
                    {
                        return;
                    }
                    Integer it = (Integer)v.getTag();
                    int index = it.intValue();
                    Utils.getInstance().showLogI("选中的模式： "+index);
                    int val[] = lightVal[index];
                    mLightVal[0] = (byte)(val[0]/100.0f*maxVal);
                    mLightVal[2] = (byte)(val[1]/100.0f*maxVal);
//                    Utils.getInstance().showLogI(mLightVal[0]+"  "+mLightVal[1]+"  "+mLightVal[2]);
//                    BltService.getInstance().writeCharacteritic(mLightVal);

                    int p1 = (int)(mLightVal[0]/maxVal*100.0f);
                    int p2 = (int)(mLightVal[2]/maxVal*100.0f);
                    mSeekBar1.setProgress(p1);
                    mSeekBar2.setProgress(p2);

                    ((ImageButton)v).setBackgroundResource(lightOnName[index]);
                    lightBtnClick = true;
                    for (int i = 0; i < 4;i++) {
                        if (i != index)
                            mLightBtn[i].setBackgroundResource(lightOffName[i]);
                    }
                }
            });
        }


        AlphaAnimation hideAnimation = new AlphaAnimation(1.0f, 0.0f);
        hideAnimation.setDuration(10);
        hideAnimation.setFillAfter( true );
        mProgress1.startAnimation(hideAnimation);
        mProgress2.startAnimation(hideAnimation);
        initSeekBarListener();

        registerReceiver(mReceiver, makeGattUpdateIntentFilter());
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

        mSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lightOpen = !lightOpen;
                if (lightOpen)
                {
                    Utils.getInstance().showLogI("开启");
                    BltService.getInstance().writeCharacteritic(mLightVal);
                    mSwitch.setBackgroundResource(R.drawable.switch_enabled);
                }
                else
                {
                    Utils.getInstance().showLogI("关闭");
                    mSwitch.setBackgroundResource(R.drawable.switch_disabled);
                    byte[] tmp = {0,0,0};
                    BltService.getInstance().writeCharacteritic(tmp);
                }
                mSeekBar1.setEnabled(lightOpen);
                mSeekBar2.setEnabled(lightOpen);

                for (int i = 0; i < 4;i++) {
                    mLightBtn[i].setBackgroundResource(lightOffName[i]);
                    mLightBtn[i].setEnabled(lightOpen);
                }
            }
        });
        BltService.getInstance().stopBlink(); //去掉提醒
    }

    protected void onStart()
    {
        super.onStart();
        Utils.getInstance().showLogI("onStart");
        BltService.getInstance().readCharacteristic();
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

                    int index = progress/25;
                    //更新蓝点
                    for (int i = 0; i < 5; i++)
                    {
                        mBlueDot[i].setImageResource(i <= index? R.drawable.blue_dot:R.drawable.gray_dot);
                    }
                }
                else
                {
                    Utils.getInstance().showLogI("seekBar2变化" + progress);
                    mLightVal[2] = (byte)(progress/100.0f*maxVal);
                    mProgress2.setText(progress + "%");

                    int index = progress/25;
                    //更新绿点
                    for (int i = 0; i < 5; i++)
                    {
                        mGreenDot[i].setImageResource(i <= index? R.drawable.green_dot:R.drawable.gray_dot);
                    }
                }

                if (lightOpen)
                {
                    BltService.getInstance().writeCharacteritic(mLightVal);
                    if (lightBtnClick)
                    {
                        lightBtnClick = false;
                        for (int i = 0; i < 4;i++) {
                            mLightBtn[i].setBackgroundResource(lightOffName[i]);
                        }
                    }
                }
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

        lightOpen = !(mLightVal[0] <= 0 && mLightVal[0] <= 0 && mLightVal[0] <= 0);
        mSwitch.setBackgroundResource(lightOpen ? R.drawable.switch_enabled:R.drawable.switch_disabled);

        mSeekBar1.setProgress(p1);
        mSeekBar2.setProgress(p2);

        int index1 = p1/25;
        int index2 = p2/25;
        //更新绿点
        for (int i = 0; i < 5; i++)
        {
            mBlueDot[i].setImageResource(i <= index1? R.drawable.blue_dot:R.drawable.gray_dot);
            mGreenDot[i].setImageResource(i <= index2? R.drawable.green_dot:R.drawable.gray_dot);
        }

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
//            editor.putInt(COLD_DATA,(int)mLightVal[0]);
//            editor.putInt(WARM_DATA,(int)mLightVal[2]);
//            editor.putBoolean(LIGHT_SWITCH,lightOpen);
            editor.commit();
            Utils.getInstance().setStringForKey(addr,mLightName.getText().toString());
        }
    }

    //读取数据
    private void retriveData()
    {
        Utils.getInstance().showLogI("取保存的数据");
        String addr = LightListAdapter.getInstance().getConnectedAddress();
        if (addr != null && !addr.equals(""))
        {
            SharedPreferences settings = ControlActivity.this.getSharedPreferences(addr,0);
//            mLightVal[0] = (byte)settings.getInt(COLD_DATA,0);
//            mLightVal[2] = (byte)settings.getInt(WARM_DATA,0);
//            mLightVal[1] = 0;
//            lightOpen =  settings.getBoolean(LIGHT_SWITCH,true);
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
