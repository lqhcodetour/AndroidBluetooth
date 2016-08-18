package com.example.liaoqihua.bluetooth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liaoqihua on 16/6/11.
 */
public class Utils {
    private Activity mActivity;
    private static Utils mUitls = null;
    private String LIGHT_NAMES = "lightNames";

    private List<Activity> mActivityList = new ArrayList<Activity>();
    static public Utils getInstance() {
        if (mUitls == null) {
            mUitls = new Utils();
        }
        return mUitls;
    }

    public void setActivity(Activity act) {
        mUitls.mActivity = act;
    }

    public void setStringForKey(String key, String value) {
        SharedPreferences settings = mActivity.getSharedPreferences(LIGHT_NAMES, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public String getStringForKey(String key)
    {
        SharedPreferences settings = mActivity.getSharedPreferences(LIGHT_NAMES, 0);
        try {
            return settings.getString(key,"");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return settings.getAll().get(key).toString();
        }
    }

    public void showLogI(String log)
    {
        Log.i("蓝牙灯",log);
    }

    public void showLogW(String log)
    {
        Log.w("蓝牙灯",log);
    }

    public void showLogE(String log)
    {
        Log.e("蓝牙灯",log);
    }

    public void pushActivity(Activity act)
    {
        if (!mActivityList.contains(act))
        {
            showLogI("增加一个activity");
            mActivityList.add(act);
        }
    }

    public void removeActivity(Activity act)
    {
        if (mActivityList.contains(act))
        {
            showLogI("移除一个activity");
            mActivityList.remove(act);
        }
    }

    public void finishAllActs()
    {
        for (Activity act:mActivityList)
        {
            act.finish();
        }
    }

    public int getActivityCount()
    {
        return mActivityList.size();
    }
}
