package com.buptant.deepeye;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SelectActivity extends AppCompatActivity{

    private Button Button_320, Button_640, Button_800, Button_1280, Button_1280_two ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_select);
        getSupportActionBar().hide();
        Button_320 = (Button) findViewById(R.id.Button_320);
        Button_640 = (Button) findViewById(R.id.Button_640);
        Button_800 = (Button) findViewById(R.id.Button_800);
        Button_1280 = (Button) findViewById(R.id.Button_1280);
        Button_1280_two = (Button) findViewById(R.id.Button_1280_two);
        Button_320.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_320();
            }
        });
        Button_640.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_640();
            }
        });
        Button_800.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_800();
            }
        });
        Button_1280.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_1280();
            }
        });
        Button_1280_two.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_1280_two();
            }
        });
    }
    /** 上次点击返回键的时间 */
    private long lastBackPressed;
    /** 两次点击的间隔时间 */
    private static final int QUIT_INTERVAL = 2000;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode== KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0) {
            long backPressed = System.currentTimeMillis();
            if (backPressed - lastBackPressed > QUIT_INTERVAL) {
                lastBackPressed = backPressed;
                Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void dialog_320(){
        Intent intent = new Intent(SelectActivity.this, MainActivity_define.class);
        Bundle bd1 = new Bundle();
        bd1.putInt("width",320);
        bd1.putInt("height",240);
        bd1.putInt("text",7);
        bd1.putInt("model",Constants.Large);
        bd1.putString("toast","320*240");
        intent.putExtras(bd1);
        startActivity(intent);
    }
    private void dialog_640(){
        Intent intent = new Intent(SelectActivity.this, MainActivity_define.class);
        Bundle bd1 = new Bundle();
        bd1.putInt("width",640);
        bd1.putInt("height",480);
        bd1.putInt("text",13);
        bd1.putInt("model",Constants.Large);
        bd1.putString("toast","640*480");
        intent.putExtras(bd1);
        startActivity(intent);
    }
    private void dialog_800(){
        Intent intent = new Intent(SelectActivity.this, MainActivity_define.class);
        Bundle bd1 = new Bundle();
        bd1.putInt("width",800);
        bd1.putInt("height",480);
        bd1.putInt("text",15);
        bd1.putInt("model",Constants.Large);
        bd1.putString("toast","800*480");
        intent.putExtras(bd1);
        startActivity(intent);
    }
    private void dialog_1280(){
        Intent intent = new Intent(SelectActivity.this, MainActivity_define.class);
        Bundle bd1 = new Bundle();
        bd1.putInt("width",1280);
        bd1.putInt("height",720);
        bd1.putInt("text",23);
        bd1.putInt("model",Constants.Small);
        bd1.putString("toast","1280*720");
        intent.putExtras(bd1);
        startActivity(intent);
    }
    private void dialog_1280_two(){
        Intent intent = new Intent(SelectActivity.this, MainActivity_define.class);
        Bundle bd1 = new Bundle();
        bd1.putInt("width",1280);
        bd1.putInt("height",960);
        bd1.putInt("text",27);
        bd1.putInt("model",Constants.Small);
        bd1.putString("toast","1280*960");
        intent.putExtras(bd1);
        startActivity(intent);
    }

}
