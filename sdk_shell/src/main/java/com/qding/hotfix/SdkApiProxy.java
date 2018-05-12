package com.qding.hotfix;

import android.content.Context;

import java.io.File;

import dalvik.system.DexClassLoader;

/**
 * Created by kuangbiao on 2018/5/12.
 */

public class SdkApiProxy implements ISdkApi {

    private ISdkApi mSdkApi;

    public void init(Context context, String dexPath, String implCalssName)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        File file = new File(dexPath);
        File dexOutputDir = context.getDir("dex", 0);
        DexClassLoader classLoader = new DexClassLoader(file.getAbsolutePath(),
                dexOutputDir.getAbsolutePath(), null, context.getClassLoader());
        Class sdkClass = classLoader.loadClass(implCalssName);
        mSdkApi = (ISdkApi) sdkClass.newInstance();
    }

    @Override
    public void showToast(Context context) {
        mSdkApi.showToast(context);
    }

}
