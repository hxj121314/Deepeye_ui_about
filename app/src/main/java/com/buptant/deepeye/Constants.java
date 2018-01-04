package com.buptant.deepeye;

/**
 * Created by JiaoJiao on 2017/8/18.
 */

public class Constants {

    public static final int REQUEST_STREAMING = 1;
    public static final int REQUEST_DISCONNECT = 9;
    public static final int REQUEST_OK = 98;
    public static final int REQUEST_IDLE = 99;
    public static final String REQUEST_FIELD = "request";
    public static final String REQUEST_FIELD_BYTE = "bytes";
    public static final String REQUEST_FIELD_WIDTH = "width";
    public static final String REQUEST_FIELD_HEIGHT = "height";
    public static final String REQUEST_ACKNOWLEDGE_NAME = "acknowledge";
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_IMAGE_RECEIVED = 6;
    public static final int MESSAGE_CONNECTED = 7;
    public static final int MESSAGE_TIME = 8;
    public static final int MESSAGE_COMPLETED = 9;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "Error";
    public static final int Small = 0;
    public static final int Large = 1;
    public static final int SPEED = 100;
    public static final String REQUEST_GPS = "GPS";
}
