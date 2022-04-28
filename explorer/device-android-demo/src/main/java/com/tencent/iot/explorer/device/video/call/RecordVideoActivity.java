package com.tencent.iot.explorer.device.video.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSON;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.app.utils.CommonUtils;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.video.call.entity.FrameRateEntity;
import com.tencent.iot.explorer.device.video.call.entity.PhoneInfo;
import com.tencent.iot.explorer.device.video.call.entity.ResolutionEntity;
import com.tencent.iot.explorer.device.video.recorder.ReadByteIO;
import com.tencent.iot.explorer.device.video.recorder.core.camera.CameraConstants;
import com.tencent.iot.explorer.device.video.recorder.core.camera.CameraUtils;
import com.tencent.iot.explorer.device.video.recorder.encoder.AudioEncoder;
import com.tencent.iot.explorer.device.video.recorder.encoder.VideoEncoder;
import com.tencent.iot.explorer.device.video.recorder.listener.OnEncodeListener;
import com.tencent.iot.explorer.device.video.recorder.param.AudioEncodeParam;
import com.tencent.iot.explorer.device.video.recorder.param.MicParam;
import com.tencent.iot.explorer.device.video.recorder.param.VideoEncodeParam;
import com.tencent.iot.explorer.device.video.recorder.utils.ByteUtils;
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoFormat;
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoNativeInteface;
import com.tencent.iot.thirdparty.android.device.video.p2p.XP2PCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RecordVideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, OnEncodeListener, SurfaceHolder.Callback {

    private String TAG = RecordVideoActivity.class.getSimpleName();
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private Camera camera;
    private Button btnSwitch;
    private String path; // 保存源文件的路径
    private IjkMediaPlayer player;
    private Surface surface;
    private TextureView playView;
    private TextView tvTcpSpeed;
    private TextView tvVCache;
    private TextView tvACache;
    private volatile PhoneInfo phoneInfo;
    private Handler handler = new Handler();
    private Button recordBtn;
    private Button hangupBtn;
    private ReadByteIO io;

    private long lastClickTime = 0L;
    //两次点击间隔不少于1000ms
    private static final int FAST_CLICK_DELAY_TIME = 1000;

    private ResolutionEntity selectedResolutionEntity;
    private FrameRateEntity selectedFrameRateEntity;
    private AudioEncoder audioEncoder;
    private VideoEncoder videoEncoder;
    private volatile boolean startEncodeVideo = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FileOutputStream outputStream;

    private int vw = 320;
    private int vh = 240;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getFilesDir().getAbsolutePath();
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getBundleExtra(PhoneInfo.TAG);
            if (bundle != null) {
                String jsonStr = bundle.getString(PhoneInfo.TAG);
                phoneInfo = JSON.parseObject(jsonStr, PhoneInfo.class);
                String resolutionJsonStr = bundle.getString(ResolutionEntity.TAG);
                if (resolutionJsonStr != null)
                    selectedResolutionEntity = JSON.parseObject(resolutionJsonStr, ResolutionEntity.class);
                String frameRateJsonStr = bundle.getString(FrameRateEntity.TAG);
                if (frameRateJsonStr != null)
                    selectedFrameRateEntity = JSON.parseObject(frameRateJsonStr, FrameRateEntity.class);
            }
        }
        setContentView(R.layout.activity_record_video);
        recordBtn = findViewById(R.id.btnRecord);
        tvTcpSpeed = findViewById(R.id.tv_tcp_speed);
        tvVCache = findViewById(R.id.tv_v_cache);
        tvACache = findViewById(R.id.tv_a_cache);
        hangupBtn = findViewById(R.id.btn_hang_up);
        hangupBtn.setOnClickListener(v -> {
            new Thread(this::finishActivity).start();
        });
        recordBtn.setText("Record \n path:" + path);
        surfaceView = findViewById(R.id.cameraView);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
        btnSwitch = findViewById(R.id.btnSwitch);
        playView = findViewById(R.id.v_play);
        playView.setSurfaceTextureListener(this);
        if (phoneInfo.getCallType() == CallingType.TYPE_AUDIO_CALL) {
            surfaceView.setVisibility(View.INVISIBLE);
            btnSwitch.setVisibility(View.GONE);
        }

        btnSwitch.setOnClickListener(v -> {
            if (System.currentTimeMillis() - lastClickTime >= FAST_CLICK_DELAY_TIME) {
                switchCamera();
                lastClickTime = System.currentTimeMillis();
            }
        });
        VideoNativeInteface.getInstance().setCallback(xP2PCallback);
        registVideoOverBrodcast();
        io = new ReadByteIO();
        io.reset();
        io.setPlayType(phoneInfo.getCallType());
        initAudioEncoder();
        initVideoEncoder();
        VideoFormat format = new VideoFormat.Builder().setVideoWidth(vw).setVideoHeight(vh).build();
        VideoNativeInteface.getInstance().initVideoFormat(format);
    }

    private void initAudioEncoder() {
        MicParam micParam = new MicParam.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setSampleRateInHz(8000) // 采样率
                .setChannelConfig(AudioFormat.CHANNEL_IN_MONO)
                .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT) // PCM
                .build();
        AudioEncodeParam audioEncodeParam = new AudioEncodeParam.Builder().build();
        audioEncoder = new AudioEncoder(micParam, audioEncodeParam);
        audioEncoder.setOnEncodeListener(this);
    }

    private void initVideoEncoder() {
        VideoEncodeParam videoEncodeParam = new VideoEncodeParam.Builder().setSize(vw, vh).build();
        videoEncoder = new VideoEncoder(videoEncodeParam);
        videoEncoder.setEncoderListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startRecord() {
        if (phoneInfo != null) {
            if (phoneInfo.getCallType() == CallingType.TYPE_VIDEO_CALL) {
                startEncodeVideo = true;
            }
            audioEncoder.start();
        }
    }

    private void stopRecord() {
        if (audioEncoder != null) {
            audioEncoder.stop();
        }
        if (videoEncoder != null) {
            videoEncoder.stop();
        }
        startEncodeVideo = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private final XP2PCallback xP2PCallback = new XP2PCallback() {

        @Override
        public void avDataRecvHandle(byte[] data, int len) {
            Log.e(TAG, "=======datalen: " + len + "bytes: " + ByteUtils.Companion.bytesToHex(data));
            io.addLast(data);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void avDataMsgHandle(int type, String msg) {
            Log.e(TAG, "avDataMsgHandle type " + type);
            if (type == 0) {
                Log.e(TAG, "start send video data");
                handler.post(() -> startRecord());
                runOnUiThread(() -> Toast.makeText(RecordVideoActivity.this, "开始推流", Toast.LENGTH_LONG).show());
            } else if (type == 1) {
                Log.e(TAG, "this call over");
                handler.post(() -> stopRecord());
                runOnUiThread(() -> Toast.makeText(RecordVideoActivity.this, "停止推流", Toast.LENGTH_LONG).show());
                if (!RecordVideoActivity.this.isDestroyed() && !RecordVideoActivity.this.isFinishing()) {
                    new Thread(() -> finishActivity()).start();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregistVideoOverBrodcast();
        if (player != null) {
            mHandler.removeMessages(MSG_UPDATE_HUD);
            player.stop();
        }
        io.close();
        executor.shutdown();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (surface != null) {
            this.surface = new Surface(surface);
            play();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) { }

    private void play() {
        player = new IjkMediaPlayer();
        player.reset();
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_HUD, 500);
        if (phoneInfo.getCallType() == CallingType.TYPE_AUDIO_CALL) {
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 64);
        } else {
//            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 80000);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 50 * 1024);
        }
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync-av-start", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "ijkio,crypto,file,http,https,tcp,tls,udp");

        player.setSurface(this.surface);
        player.setAndroidIOCallback(io);

        Uri uri = Uri.parse("ijkio:androidio:" + io.getURL_SUFFIX());
        try {
            player.setDataSource(uri.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.prepareAsync();
        player.start();
    }

    private void finishActivity() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        if (phoneInfo != null) {
            bundle.putString(PhoneInfo.TAG, JSON.toJSONString(phoneInfo));
        }
        intent.putExtra(PhoneInfo.TAG, bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishActivity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    BroadcastReceiver recevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int refreshTag = intent.getIntExtra(Utils.VIDEO_OVER, 0);
            if (refreshTag == 9) {
                if (!RecordVideoActivity.this.isDestroyed() && !RecordVideoActivity.this.isFinishing()) {
                    new Thread(() -> finishActivity()).start();
                }
            }
        }
    };

    private void registVideoOverBrodcast() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(RecordVideoActivity.this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CART_BROADCAST");
        broadcastManager.registerReceiver(recevier, intentFilter);
    }

    private void unregistVideoOverBrodcast() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(RecordVideoActivity.this);
        broadcastManager.unregisterReceiver(recevier);
    }

    @Override
    public void onAudioEncoded(byte[] datas, long pts, long seq) {
        if (executor.isShutdown()) return;
        executor.submit(() -> VideoNativeInteface.getInstance().sendAudioData(datas, pts, seq, 0));
    }

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq) {
        if (executor.isShutdown()) return;
        executor.submit(() -> VideoNativeInteface.getInstance().sendVideoData(datas, pts, seq, 0));
    }

    private void createFile() {
        File file = new File("/sdcard/test.h264");
        try {
            outputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera(camera);
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        releaseCamera(camera);
        camera = Camera.open(facing);
        //获取相机参数
        Camera.Parameters parameters = camera.getParameters();

        //设置预览格式（也就是每一帧的视频格式）YUV420下的NV21
        parameters.setPreviewFormat(ImageFormat.NV21);

        if (this.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        int cameraIndex = -1;
        if (facing == CameraConstants.facing.BACK) {
            cameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else if (facing == CameraConstants.facing.FRONT) {
            cameraIndex = Camera.CameraInfo.CAMERA_FACING_FRONT;
            camera.setDisplayOrientation(180);
        }

        try {
            camera.setDisplayOrientation(CameraUtils.getDisplayOrientation(this, cameraIndex));
        } catch (Exception e) {
            e.printStackTrace();
        }


        Camera.Size previewSize = getCameraPreviewSize(parameters);
        //设置预览图像分辨率
        parameters.setPreviewSize(vw, vh);

        //配置camera参数
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //设置监听获取视频流的每一帧
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (startEncodeVideo && videoEncoder != null) {
                    videoEncoder.encoderH264(data, facing == CameraConstants.facing.FRONT);
                }
            }
        });
        //调用startPreview()用以更新preview的surface
        camera.startPreview();
    }

    /**
     * 获取设备支持的最大分辨率
     */
    private Camera.Size getCameraPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        Camera.Size needSize = null;
        for (Camera.Size size : list) {
            Log.e(TAG, "****========== " + size.width + " " + size.height);
            if (needSize == null) {
                needSize = size;
                continue;
            }
            if (size.width >= needSize.width) {
                if (size.height > needSize.height) {
                    needSize = size;
                }
            }
        }
        return needSize;
    }

    // 默认摄像头方向
    private int facing = CameraConstants.facing.BACK;

    private void switchCamera() {
        if (facing == CameraConstants.facing.BACK) {
            facing = CameraConstants.facing.FRONT;
        } else {
            facing = CameraConstants.facing.BACK;
        }
        openCamera();
    }

    /**
     * 关闭相机
     */
    public void releaseCamera(Camera camera) {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private static final int MSG_UPDATE_HUD = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_HUD:
                    long videoCachedDuration = player.getVideoCachedDuration();
                    long audioCachedDuration = player.getAudioCachedDuration();
                    long videoCachedBytes = player.getVideoCachedBytes();
                    long audioCachedBytes = player.getAudioCachedBytes();
                    long tcpSpeed = player.getTcpSpeed();

                    tvACache.setText(String.format(Locale.US, "%s, %s",
                            CommonUtils.formatedDurationMilli(audioCachedDuration),
                            CommonUtils.formatedSize(audioCachedBytes)));
                    tvVCache.setText(String.format(Locale.US, "%s, %s",
                            CommonUtils.formatedDurationMilli(videoCachedDuration),
                            CommonUtils.formatedSize(videoCachedBytes)));
                    tvTcpSpeed.setText(String.format(Locale.US, "%s",
                            CommonUtils.formatedSpeed(tcpSpeed, 1000)));
                    removeMessages(MSG_UPDATE_HUD);
                    sendEmptyMessageDelayed(MSG_UPDATE_HUD, 500);
            }
        }
    };

}