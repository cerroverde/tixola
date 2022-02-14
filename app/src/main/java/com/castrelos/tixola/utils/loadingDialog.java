package com.castrelos.tixola.utils;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.castrelos.tixola.R;

public class loadingDialog {
    private Activity mActivity;
    private AlertDialog mAlertDialog;
    private String mMessage;

    public loadingDialog(Activity activity, String message) {
        this.mActivity = activity;
        this.mMessage = message;
    }

    public void startLoadingDialog(){
        androidx.appcompat.app.AlertDialog.Builder
                builder = new androidx.appcompat.app.AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.custom_loading, null));
        builder.setCancelable(false);

        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    public void dismissDialog(){
        mAlertDialog.dismiss();
    }
}
