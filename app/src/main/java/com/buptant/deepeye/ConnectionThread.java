package com.buptant.deepeye;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by JiaoJiao on 2017/8/19.
 */

public class ConnectionThread {

    private static final String TAG = "ConnectionThread";
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    private final int DEFAULT_BUFFER_SIZE = 1024;
    private final int LARGE_SIZE_BUFFER_SIZE = 8192;
    private final Handler mHandler;
    private int mState;
    private int mModel;
    private String IMEI;
    private String mGPS;
//    private String mIPAddress="2001:da8:215:509:fe4d:d4ff:fe3d:a6bd";
//    private String mIPAddress="120.55.170.85";
    private String mIPAddress="2001:da8:215:6a01::b198";
    private int mPort = 1050;
//    private int mPort = 50001;
    private int mTimeout = 3000;
    private Socket mSocket;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private LinkedList<ImageData> mImageQueue = new LinkedList<ImageData>();
    private Semaphore mImageQueueLock = new Semaphore(1);
//    public static String filename = "log_send";
    public static String filename1 = "log_tran";
    public int start = 0;
    public int end = 0;
    private Context mContext;
    public ConnectionThread(Context context, Handler handler, int model) {
        mState = STATE_NONE;
        mHandler = handler;
        mModel = model;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Activity.TELEPHONY_SERVICE);
        if (tm != null) {
            IMEI = tm.getDeviceId();
        }
    }
    public synchronized void connect() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectThread();
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    public int getState() {
        return mState;
    }
    public void setGPS(String GPS){
        mGPS = GPS;
        Log.i("qbr", mGPS);
    }
    public synchronized void connected(Socket socket) {
        Log.d(TAG, "connected, IP address: " + mIPAddress);
        if (mConnectThread != null) {
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, "Cloud");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private void connectionLost() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private class ConnectThread extends Thread {
        private final Socket mSocket;
        public ConnectThread() {
            mSocket = new Socket();
        }
        public void run() {
            Log.i(TAG, "BEGIN ConnectThread");
            try {
                InetAddress inetAddress = InetAddress.getByName(mIPAddress);
                SocketAddress socketAddress = new InetSocketAddress(inetAddress, mPort);
                mSocket.connect(socketAddress, mTimeout);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Invalid IP address", e);
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "Unable to connect the device with IP and port number of: "
                        + mIPAddress + ":" + mPort);
                e.printStackTrace();
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "unable to close() socket during connection failure", e1);
                }
                connectionFailed();
                return;
            }
            if (!mSocket.isConnected() || mSocket.isClosed()) {
                connectionFailed();
            }
            connected(mSocket);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final Socket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;
        private LinkedList<Integer> mRequestQueue = new LinkedList<Integer>();
        private Object requestLock = new Object();
        public ConnectedThread(Socket socket) {
            Log.d(TAG, "create ConnectedThread");
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket does not created: " + e.getMessage(), e);
            }
            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN ConnectedThread");
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            byte[] largeBuffer = new byte[LARGE_SIZE_BUFFER_SIZE];
            int bytes, remainingBytes = -1;
            mHandler.obtainMessage(Constants.MESSAGE_CONNECTED).sendToTarget();
            pushRequest(Constants.REQUEST_STREAMING);

            JSONObject jsonObj;
            jsonObj = new JSONObject();
            try {
                 jsonObj.put("imei", IMEI);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            write(jsonObj.toString().getBytes());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                return;
            }

            boolean run = true;
            JSONObject jsonObjSend;

            while (run) {
                try {
                    int request_code = popRequest();

                    switch (request_code) {
                        case Constants.REQUEST_STREAMING:
                            ImageData imageData = null;
                            while (imageData == null) {
                                imageData = popImage();
                            }
                            jsonObjSend = new JSONObject();
                            try {
                                jsonObjSend.put(Constants.REQUEST_FIELD, Constants.REQUEST_STREAMING);
                                jsonObjSend.put(Constants.REQUEST_FIELD_BYTE, imageData.ImageData.length);
                                jsonObjSend.put(Constants.REQUEST_GPS, mGPS);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            write(jsonObjSend.toString().getBytes());
//                            writelog("start"+start , filename);
                            writelog("start"+start , filename1);
                            start ++;
                            // Receive an acknowledgement
                           bytes = mInStream.read(buffer);
                            try {
                                JSONObject jsonObjReceive = new JSONObject(new String(buffer, 0, bytes));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mOutStream.write(imageData.ImageData);
                            // Receive an acknowledgement
                            bytes = mInStream.read(buffer);
                            try {
                                JSONObject jsonObjReceive = new JSONObject(new String(buffer, 0, bytes));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Message msg = mHandler.obtainMessage(Constants.MESSAGE_COMPLETED);
                            mHandler.sendMessage(msg);
//                            writelog("end"+end , filename);
                            end++;
                            pushRequest(Constants.REQUEST_STREAMING);
                            break;
                        case Constants.REQUEST_DISCONNECT:
                            // Send a streaming request
                            jsonObjSend = new JSONObject();
                            try {
                                jsonObjSend.put(Constants.REQUEST_FIELD, Constants.REQUEST_DISCONNECT);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            write(jsonObjSend.toString().getBytes());
                            resetRequest();
                            run = false;
                            break;
                        default:
                            break;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                Log.d(TAG, "Write " + buffer.length + "bytes");
                mOutStream.write(buffer);
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void writelog(String log, String filename){
            SimpleDateFormat sdf4 = new SimpleDateFormat("HH:mm:ss.SSS");
            String str4 = sdf4.format(new Date());
            String time4 = str4;
            String str = time4+"\t"+log+"\n";
            try {
                if (Environment.getExternalStorageState().equals(
                        Environment.MEDIA_MOUNTED)) {
                    File sdDire = Environment.getExternalStorageDirectory();
                    FileOutputStream outFileStream = new FileOutputStream(
                            sdDire.getCanonicalPath() + "/" + filename + ".txt", true);
                    outFileStream.write(str.getBytes());
                    outFileStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                pushRequest(Constants.REQUEST_DISCONNECT);
                this.join(2000);
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread does not respond, force to stop the thread", e);
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "close() of connect socket failed", e);
                }
            }
        }

        public int popRequest() {
            synchronized (requestLock) {
                while (mRequestQueue.size() == 0) {
                    try {
                        requestLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return -1;
                    }
                }

                return mRequestQueue.poll();
            }
        }

        public void pushRequest(int request) {
            synchronized (requestLock) {
                mRequestQueue.add(request);
                requestLock.notify();
            }
        }

        public void resetRequest() {
            synchronized (mRequestQueue) {
                mRequestQueue.clear();
            }
        }
    }

    public void pushImage(byte[] raw_data, int width, int height) {
        try {
            if (!mImageQueueLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock the image queue");
            }
            if (mImageQueue.size() > 3) {
                mImageQueue.poll();
            }
            mImageQueue.push(new ImageData(raw_data, width, height));
            mImageQueueLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ImageData popImage() {
        ImageData imageData = null;
        try {
            if (!mImageQueueLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock the image queue");
            }
            imageData = mImageQueue.poll();
            mImageQueueLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return imageData;
    }

    private class ImageData {
        public byte[] ImageData;
        public int Width;
        public int Height;

        ImageData(byte[] data, int width, int height) {
            ImageData = data;
            Width = width;
            Height = height;
        }

    }
}
