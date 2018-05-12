package com.qding.hotfix;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "kbtest";
    private HotFixProxy mHotFixProxy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHotFixProxy = new HotFixProxy();
        boolean initSdkSuccess = false;
        try {
            mHotFixProxy.init(getApplicationContext(), "/sdcard/sdk_dex.jar", "com.qding.hotfix.HotFixImpl");
            initSdkSuccess = true;
        } catch (ClassNotFoundException e) {
            //类找不到异常
            e.printStackTrace();
            showException(e);
        } catch (IllegalAccessException e) {
            //创建实例权限异常,一般来说，是由于java在反射时调用了private方法所导致的
            e.printStackTrace();
            showException(e);
        } catch (InstantiationException e) {
            //创建实例异常,当试图通过newInstance()方法创建某个类的实例,而该类是一个抽象类或接口时,抛出该异常
            e.printStackTrace();
            showException(e);
        } finally {
            if (!initSdkSuccess) {
                mHotFixProxy = null;
            }
        }
        findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHotFixProxy == null) {
                    showException(new RuntimeException("sdk 加载失败"));
                    return;
                }
                mHotFixProxy.showToast(getBaseContext());
            }
        });
    }

    private void showException(Exception e) {
        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        Log.e(TAG, e.getMessage(), e);
    }
}
