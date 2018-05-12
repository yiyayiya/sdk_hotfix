package com.qding.hotfix;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by kuangbiao on 2018/5/12.
 */

public class SdkImpl implements ISdkApi {
    @Override
    public void showToast(Context context) {
        Toast.makeText(context, "hello word ! 3344", Toast.LENGTH_LONG).show();
    }
}
