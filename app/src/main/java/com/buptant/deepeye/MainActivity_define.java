package com.buptant.deepeye;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity_define extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        if (null == savedInstanceState){
            getFragmentManager().beginTransaction()
                    .replace(R.id.activity_main, CameraPreviewFragment_define.newInstance()).commit();
        }

    }
}
