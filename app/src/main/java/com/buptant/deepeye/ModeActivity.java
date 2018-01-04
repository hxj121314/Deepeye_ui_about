package com.buptant.deepeye;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class ModeActivity extends AppCompatActivity implements View.OnClickListener{

    private Button button_adaption, button_define;
    private ImageButton button_about;
//    public static ModeMsg modeMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);
        getSupportActionBar().hide();
        //       modeMsg=new ModeMsg();
        button_adaption = (Button) findViewById(R.id.button_adaption);
        button_define = (Button) findViewById(R.id.button_define);
        button_about = (ImageButton) findViewById(R.id.about);
        button_adaption.setOnClickListener(this);
        button_define.setOnClickListener(this);
        button_about.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_adaption:
                dialog_adaption();
//                adaption();

                break;
            case R.id.button_define:
//                dialog_userdefined();
                define();
                break;

            case R.id.about:
                about_us();
                break;

        }
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


    private void dialog_adaption(){
        //先new出一个监听器，设置好监听
        DialogInterface.OnClickListener dialogOnclicListener=new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case Dialog.BUTTON_POSITIVE:

                        Intent intent = new Intent(ModeActivity.this, MainActivity.class);
                        Bundle bd1 = new Bundle();
                        bd1.putInt("width",320);
                        bd1.putInt("height",240);
                        bd1.putInt("text",5);
                        bd1.putString("toast","320*240");
                        intent.putExtras(bd1);
                        startActivity(intent);
//                        modeMsg.setMode("市区模式");
//                        modeMsg.setModeNum(1);
                        break;
                    case Dialog.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
        //dialog参数设置
        AlertDialog.Builder builder=new AlertDialog.Builder(this);  //先得到构造器
        builder.setTitle("自适应模式"); //设置标题
        builder.setMessage("系统将根据当前网速为您选择相应的图像采集分辨率"); //设置内容
        builder.setPositiveButton("开始旅途",dialogOnclicListener);
        builder.setNegativeButton("重新选择", dialogOnclicListener);
        builder.create().show();
    }
    private void define(){
        Intent intent = new Intent(ModeActivity.this, SelectActivity.class);
        startActivity(intent);
    }

    private void about_us(){
        //先new出一个监听器，设置好监听
        DialogInterface.OnClickListener dialogOnclicListener=new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case Dialog.BUTTON_POSITIVE:
                        break;
                }
            }
        };
        //dialog参数设置
        AlertDialog.Builder builder=new AlertDialog.Builder(this);  //先得到构造器
        builder.setTitle("关于我们"); //设置标题
        builder.setMessage("DeepEye是一款提供辅助驾驶功能的应用软件，结合人工智能领域中的计算机视觉技术，" +
                "对汽车前方道路景象中的车辆、行人、自行车和交通信号灯、标识牌等进行识别。" +
                "DeepEye用户只需将手机安置于车辆前挡风玻璃下方，软件便可通过手机摄像头监控车辆前方物体，" +
                "且将识别效果展现出来，对驾驶员进行辅助驾驶。" +
                "DeepEye共分为自适应模式和自定义模式。其中自适应模式，可根据手机所处网络环境状况自行选择合适的分辨率进行图像采集；" +
                "自定义模式中，用户可根据当前车辆所处交通场景复杂程度，自行选择图像采集分辨率，这里我们提供五种不同分辨率可用选择。" +
                "当前版本主要功能为：车辆检测；行人检测；自行车、摩托车检测；交通信号灯、标识牌检测。"); //设置内容
        builder.setPositiveButton("我了解了",dialogOnclicListener);
        builder.create().show();
    }

}
