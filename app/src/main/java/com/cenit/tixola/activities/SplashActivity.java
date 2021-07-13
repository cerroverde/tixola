package com.cenit.tixola.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cenit.tixola.R;

public class SplashActivity extends AppCompatActivity {
    private int mProgressBarStatus = 0;


    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.mProgressBar);
        Handler mHandler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mProgressBarStatus < 100){
                    mProgressBarStatus++;
                    android.os.SystemClock.sleep(50);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(mProgressBarStatus);
                        }
                    });
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(
                                SplashActivity.this, MainActivity.class));
                        finish();
                    }
                });
            }
        });

    }
}
