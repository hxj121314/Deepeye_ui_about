package com.buptant.deepeye;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraPreviewFragment_define extends Fragment implements View.OnClickListener{
    private static final String TAG = "CameraPreviewFragment";
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private static final int REQUEST_CONNECT = 0;
    private static final int STATE_PREVIEW = 0;
    private static final int MAX_PREVIEW_WIDTH = 1280;
    private static final int MAX_PREVIEW_HEIGHT = 960;
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Intent intent = getActivity().getIntent();
            Bundle bd = intent.getExtras();
            int outputWidth = 320;
            int outputHeight = 240;
            openCamera(width, height, outputWidth, outputHeight);
//            int outputWidth1 = bd.getInt("width");
//            int outputHeight1 = bd.getInt("height");
            Camera.Parameters parameters = mCamera.getParameters();
//            parameters.setPreviewSize(outputWidth1,outputHeight1);
//            mCamera.setParameters(parameters);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };
    private String mCameraId;
    private String mGps = "GPS";
    private long timestamp=0;
    public static String filename = "deepeye_log";
    public static String   filename1 = "log_rec";
    public int show = 0;
    public int capture = 0;
    private ImageView imageView;
    private TextView timeDownload;
    private TextView timeChange;
    private TextView speed;
    private SocketRec clientSocket;
    ProgressBar bar;
    TextView load;
    int status=0;

    public LocationClient mLocationClient;
    private TextView positionText;

    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            showNetSpeed();
        }
    };

    private long getTotalRxBytes() {
        return TrafficStats.getUidRxBytes(getActivity().getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 :(TrafficStats.getTotalRxBytes()/1024);//转为KB
    }
    private void showNetSpeed() {
        long nowTotalRxBytes = getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long speed1 = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));//毫秒转换
        long speed2 = ((nowTotalRxBytes - lastTotalRxBytes) * 10 % (nowTimeStamp - lastTimeStamp));//毫秒转换

        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        String ob = String.valueOf(speed1) + "." + String.valueOf((int)speed2) + " kb/s";
        Message msg = new Message();
        msg.what = Constants.SPEED;  //消息(一个整型值)
        msg.obj = ob;
        msg.arg1 = (int)speed1;
        mMessageHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler

    }


    long sdf22 = 0;
    String str21;
    List<Long> list=new ArrayList<Long>();
    Long[] array;
    int i=0;
    private ImageButton mConnectButton,mBackButton;
    private SurfaceView mSurfaceView;
    Camera mCamera;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private ImageReader mImageReader;
    private int mSensorOrientation;
    private Handler mBackgroundHandler;
    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    break;
                case Constants.MESSAGE_WRITE:
                    break;
                case Constants.MESSAGE_READ:
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    showToast("Connected to " + msg.getData().getString(Constants.DEVICE_NAME));
                    break;
                case Constants.MESSAGE_TOAST:
                    showToast(Constants.TOAST);
                    break;
                case Constants.MESSAGE_COMPLETED:
                    sdf22=System.currentTimeMillis();
                    list.add(sdf22);
                    int size=list.size();
                    array = list.toArray(new Long[size]);
                    break;
                case 0x111:
                    bar.setProgress(status);
                    if(status==100){
                        bar.setVisibility(View.GONE);
                        load.setVisibility(View.GONE);
                    }
                    break;
                case Constants.MESSAGE_IMAGE_RECEIVED:
                    Bitmap myimg = (Bitmap) msg.obj;
                    Bitmap img = Bitmap.createScaledBitmap(myimg,600,420,true);
                    SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss.SSS");
                    imageView.setImageBitmap(img);
                    String str2 = sdf2.format(new Date());
                    String time2 = str2;
                    String str = time2+"\t"+"show"+show+"\n";
                    writeSDcard(str,filename1);
                    show++;
                    long sdf12=System.currentTimeMillis();
                    long timeCh=sdf12-array[i];
                    i++;
                    String timeChSt = String.valueOf(timeCh);
                    timeChange.setText(timeChSt+"ms");
                    mBackButton.setEnabled(true);
                    break;
                case Constants.MESSAGE_CONNECTED:
                    mConnectButton.setEnabled(false);
                    break;
                case Constants.MESSAGE_TIME:
                    timeDownload.setText(str21);
                    break;
            }
        }
    };
    private TextView ReslotionText;

    class TimeThread extends Thread {
        @Override
        public void run() {
                Message msg = new Message();
                msg.what = Constants.MESSAGE_TIME;  //消息(一个整型值)
                mMessageHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
            }
    }

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private ConnectionThread mConnectionThread = null;
    private int mState = STATE_PREVIEW;
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    public static CameraPreviewFragment_define newInstance() {
        return new CameraPreviewFragment_define();
    }

    public CameraPreviewFragment_define() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
//        lastTotalRxBytes = getTotalRxBytes();
//        lastTimeStamp = System.currentTimeMillis();
//        new Timer().schedule(task, 1000, 1000); // 1s后启动任务，每2s执行一次
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_camera_preview_define, container, false);

    }
    /** 上次点击返回键的时间 */
    private long lastBackPressed;
    /** 两次点击的间隔时间 */
    private static final int QUIT_INTERVAL = 2000;
    @Override
    public void onResume() {
        super.onResume();
        //得到Fragment的根布局并使该布局可以获得焦点
        getView().setFocusableInTouchMode(true);
        //得到Fragment的根布局并且使其获得焦点
        getView().requestFocus();
        //对该根布局View注册KeyListener的监听
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){
                    // handle back button
                    long backPressed = System.currentTimeMillis();
                    if (backPressed - lastBackPressed > QUIT_INTERVAL) {
                        lastBackPressed = backPressed;
                        showToast("再按一次退出");
                    } else {
                        if(mConnectionThread!=null)
                        {
                            mConnectionThread.stop();
                            mConnectionThread = null;
                        }
                        mCamera.setPreviewCallback(null) ;
                        mCamera.stopPreview();
                        mCamera.release();
                        mCamera = null;
                        getActivity().finish();
                        System.exit(0);
                    }
                    return true;
                }
                return false;
            }
        });
    }



    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mConnectButton = (ImageButton) view.findViewById(R.id.frag_main_button_connect);
        mBackButton = (ImageButton) view.findViewById(R.id.frag_main_button_back);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surfaceview);
        imageView = (ImageView) view.findViewById(R.id.image);
        timeDownload= (TextView) view.findViewById(R.id.timeDownload);
        timeChange = (TextView) view.findViewById(R.id.timeChange);
        speed = (TextView) view.findViewById(R.id.speed);
        ReslotionText = (TextView)view.findViewById(R.id.resolution_text);
        bar=(ProgressBar)view.findViewById(R.id.bar);
        load=(TextView)view.findViewById(R.id.loading);
        mConnectButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
        SurfaceHolder videoCaptureViewHolder = mSurfaceView.getHolder();
        videoCaptureViewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        videoCaptureViewHolder.addCallback(new Callback() {
            public void surfaceDestroyed(SurfaceHolder holder) {
            }

            public void surfaceCreated(SurfaceHolder holder) {
                startVideo();
            }

            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }
        });
        mLocationClient = new LocationClient(getActivity().getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        positionText = (TextView) view.findViewById(R.id.position_text_view);
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(getActivity(),Manifest.
                permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(getActivity(),Manifest.
                permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(getActivity(),Manifest.
                permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(getActivity(),permissions,1);
        }else {
            requestLocation();
        }

    }

    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
//        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        option.setScanSpan(500);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case 1:
                if (grantResults.length>0){
                    for (int result : grantResults){
                        if (result !=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(getActivity(),"必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            getActivity().finish();
                            return;
                        }
                    }
                    requestLocation();
                }else {
                    Toast.makeText(getActivity(),"发生未知错误",Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            default:
        }
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("La：").append(bdLocation.getLatitude());
            currentPosition.append("Lo：").append(bdLocation.getLongitude());
            currentPosition.append("Sp：").append(bdLocation.getSpeed());
            currentPosition.append("Di：").append(bdLocation.getDirection());
            mGps = currentPosition.toString();
            //positionText.setText(currentPosition);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void startVideo() {
        SurfaceHolder videoCaptureViewHolder = null;
        try {
            mCamera = Camera.open();
        } catch (RuntimeException e) {
            Log.e("CameraTest", "Camera Open filed");
            return;
        }
        mCamera.setErrorCallback(new Camera.ErrorCallback() {
            public void onError(int error, Camera camera) {
            }
        });
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFrameRate(30);
        parameters.setPreviewFpsRange(15000,30000);
        Intent intent = getActivity().getIntent();
        Bundle bd = intent.getExtras();
        int outputWidth = bd.getInt("width");
        int outputHeight = bd.getInt("height");
        String mResolutionSize=bd.getString("toast");
        ReslotionText.setText("图像采集分辨率："+mResolutionSize+"（像素）");
        parameters.setPreviewSize(outputWidth,outputHeight);
        //mCamera.setParameters(parameters);
        List<int[]> supportedPreviewFps=parameters.getSupportedPreviewFpsRange();
        Iterator<int[]> supportedPreviewFpsIterator=supportedPreviewFps.iterator();
        while(supportedPreviewFpsIterator.hasNext()){
            int[] tmpRate=supportedPreviewFpsIterator.next();
            StringBuffer sb=new StringBuffer();
            sb.append("supportedPreviewRate: ");
            for(int i=tmpRate.length,j=0;j<i;j++){
                sb.append(tmpRate[j]+", ");
            }
            Log.v("CameraTest",sb.toString());
        }

        List<Camera.Size> supportedPreviewSizes=parameters.getSupportedPreviewSizes();
        Iterator<Camera.Size> supportedPreviewSizesIterator=supportedPreviewSizes.iterator();
        while(supportedPreviewSizesIterator.hasNext()){
            Camera.Size tmpSize=supportedPreviewSizesIterator.next();
            Log.v("CameraTest","supportedPreviewSize.width = "+tmpSize.width+"supportedPreviewSize.height = "+tmpSize.height);
        }

        if (null != mSurfaceView)
            videoCaptureViewHolder = mSurfaceView.getHolder();
        try {
            mCamera.setPreviewDisplay(videoCaptureViewHolder);
        } catch (Throwable t) {
        }
        Log.v("CameraTest","Camera PreviewFrameRate = "+mCamera.getParameters().getPreviewFrameRate());
        Camera.Size previewSize=mCamera.getParameters().getPreviewSize();
        int dataBufferSize=(int)(previewSize.height*previewSize.width*
                (ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat())/8.0));
        mCamera.addCallbackBuffer(new byte[dataBufferSize]);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {

            public synchronized void onPreviewFrame(byte[] data, Camera camera) {
                new TimeThread().run();
                Camera.Size size = camera.getParameters().getPreviewSize();
                YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 10, outstream);
                Bitmap bmp = BitmapFactory.decodeByteArray(outstream.toByteArray(),0,outstream.size());
                bmp.compress(Bitmap.CompressFormat.JPEG, 10, outstream);
                SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd   HH:mm:ss.SSS");
                str21 = sdf.format(new Date());
                timeDownload.setText(str21);
                String picture_name = str21 +".jpg";
                Log.d("CameraTest","Time Gap = "+(System.currentTimeMillis()-timestamp));
                Intent intent = getActivity().getIntent();
                Bundle bd = intent.getExtras();
                //int outputWidth = bd.getInt("width");
                // int outputHeight = bd.getInt("height");
                //int TextSize=bd.getInt("text");
                String ssize=bd.getString("toast");
                Log.d("CameraTest", ssize);

                System.out.println(System.currentTimeMillis()-timestamp);
                System.out.println(picture_name);
                timestamp=System.currentTimeMillis();
                SimpleDateFormat sdf222 = new SimpleDateFormat("HH:mm:ss.SSS");
                String str222 = sdf222.format(new Date());
                String STR = str222+"\t"+"capture"+capture+"\n";
                writeSDcard(STR,filename);
                capture++;
                SimpleDateFormat sdf223 = new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss.SSS");
                String str223 = sdf223.format(new Date());
                timeDownload.setText(str223);
                Bitmap imagemarkBitmap = ImageUtil.createWaterMaskCenter(bmp, bmp);
                imagemarkBitmap = ImageUtil.createWaterMaskLeftBottom(getActivity(), imagemarkBitmap, bmp, 0, 0);
                imagemarkBitmap = ImageUtil.createWaterMaskRightBottom(getActivity(), imagemarkBitmap, bmp, 0, 0);
                imagemarkBitmap = ImageUtil.createWaterMaskLeftTop(getActivity(), imagemarkBitmap, bmp, 0, 0);
                imagemarkBitmap = ImageUtil.createWaterMaskRightTop(getActivity(), imagemarkBitmap, bmp, 0, 0);
                //Intent intent = getActivity().getIntent();
                //Bundle bd = intent.getExtras();
                //int outputWidth = bd.getInt("width");
               // int outputHeight = bd.getInt("height");
                int TextSize=bd.getInt("text");
               // String ssize=bd.getString("toast");
                Bitmap textBitmap = ImageUtil.drawTextToRightBottom(getActivity(), imagemarkBitmap, str21, TextSize, Color.BLUE, 0, 0);
                ByteArrayOutputStream upuploadData = new ByteArrayOutputStream();
                textBitmap.compress(Bitmap.CompressFormat.JPEG, 100, upuploadData);
                byte[] datass = upuploadData.toByteArray();
                if (mConnectionThread != null && mConnectionThread.getState() == ConnectionThread.STATE_CONNECTED) {
                    mConnectionThread.pushImage(datass, size.width, size.height);
                    mConnectionThread.setGPS(mGps);

                }
                try{
                   // saveBitmap(textBitmap ,picture_name);
                    camera.addCallbackBuffer(data);
                }catch (Exception e) {
                    Log.e("CameraTest", "addCallbackBuffer error");
                    return;
                }
                return;
            }
        });
        try {
            mCamera.startPreview();
        } catch (Throwable e) {
            mCamera.release();
            mCamera = null;
            return;
        }
    }

//    @Override
//    public void onPause() {
//        mCamera.release();
//        mCamera = null;
//        closeCamera();
//        super.onPause();
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void saveBitmap(Bitmap bitmap ,String bitName)throws IOException
    {
        File file = new File("/sdcard/DCIM/Camera/"+bitName);
        if(file.exists()){
            file.delete();
        }
        FileOutputStream out;
        try{
            out = new FileOutputStream(file);
            if(bitmap.compress(Bitmap.CompressFormat.PNG,90,out))
            {
                out.flush();
                out.close();
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void requestCameraPermission() {
        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void openCamera(int width, int height,int outputW,int outputH) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height,outputW,outputH);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void setUpCameraOutputs(int width, int height,int outputWid,int outputHei) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                mImageReader = ImageReader.newInstance(outputWid,outputHei,
                        ImageFormat.JPEG, 30);

                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
    }

    private void createCameraPreviewSession() {

    }

    @Override
    public void onClick(View view) {
        Intent intent = getActivity().getIntent();
        Bundle bd = intent.getExtras();
        int model = bd.getInt("model");
        mBackButton.setEnabled(false);
        switch (view.getId()) {
            case R.id.frag_main_button_connect:
                    bar.setAlpha(1);
                    load.setAlpha(1);
                    new Thread(){
                        public void run(){
                            while(status<100){
                                status++;
                                try {
                                    Thread.sleep(100);
                                    mMessageHandler.sendEmptyMessage(0x111);
                                }
                                catch (InterruptedException e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }.start();
                mConnectionThread = new ConnectionThread(getActivity(), mMessageHandler, model);
                mConnectionThread.connect();
                clientSocket = new SocketRec(getActivity(), mMessageHandler);
                break;
            case R.id.frag_main_button_back:
                if(mConnectionThread!=null)
                {
                    mConnectionThread.stop();
                    mConnectionThread = null;
                }
                mBackButton.setEnabled(false);
              //  task.cancel();
                mCamera.setPreviewCallback(null) ;
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_CONNECT:
                if (resultCode == Activity.RESULT_OK) {
                } else if (resultCode == Activity.RESULT_CANCELED){
                }
                break;
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";
        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

    private void writeSDcard(String str,String filename) {
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
}
