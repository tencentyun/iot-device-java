package com.tencent.iot.explorer.device.video.device_and_device;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.MediaRecorder;
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
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.tencent.iot.explorer.device.android.app.R;
import com.tencent.iot.explorer.device.android.app.utils.CommonUtils;
import com.tencent.iot.explorer.device.common.stateflow.entity.CallingType;
import com.tencent.iot.explorer.device.video.call.entity.FrameRateEntity;
import com.tencent.iot.explorer.device.video.call.entity.PhoneInfo;
import com.tencent.iot.explorer.device.video.call.entity.ResolutionEntity;
import com.tencent.iot.explorer.device.video.device_and_device.entity.DeviceStatus;
import com.tencent.iot.explorer.device.video.push_stream.RecordVideoActivity2;
import com.tencent.iot.explorer.device.video.recorder.ReadByteIO;
import com.tencent.iot.explorer.device.video.recorder.core.camera.CameraConstants;
import com.tencent.iot.explorer.device.video.recorder.core.camera.CameraUtils;
import com.tencent.iot.explorer.device.video.recorder.encoder.AudioEncoder;
import com.tencent.iot.explorer.device.video.recorder.encoder.VideoEncoder;
import com.tencent.iot.explorer.device.video.recorder.listener.OnEncodeListener;
import com.tencent.iot.explorer.device.video.recorder.param.AudioEncodeParam;
import com.tencent.iot.explorer.device.video.recorder.param.MicParam;
import com.tencent.iot.explorer.device.video.recorder.param.VideoEncodeParam;
import com.tencent.iot.thirdparty.flv.FLVListener;
import com.tencent.iot.thirdparty.flv.FLVPacker;
import com.tencent.xnet.XP2P;
import com.tencent.xnet.XP2PCallback;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RecordVideoActivity4 extends AppCompatActivity implements TextureView.SurfaceTextureListener, OnEncodeListener, SurfaceHolder.Callback {

    private static final String TAG = RecordVideoActivity2.class.getSimpleName();
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private Camera camera;
    private Button btnSwitch;
    private Button btnStop;
    private Button btnSendCmd;
    private TextView tvVideoWH;
    private TextView tvVCache;
    private TextView tvACache;
    private TextView tvFPS;
    private IjkMediaPlayer player;
    private Surface surface;
    private TextureView playView;
    private volatile PhoneInfo phoneInfo;
    private Handler handler = new Handler();
    private ReadByteIO io;

    private AudioEncoder audioEncoder;
    private VideoEncoder videoEncoder;
    private volatile boolean startEncodeVideo = false;
    private volatile boolean isRecording = false;
    private volatile boolean reconnect = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FLVPacker flvPacker;
    private int vw = 640;
    private int vh = 480;
    private int frameRate = 15;

    private String deviceId;
    private String productId;
    private String deviceName;
    private String xp2pInfo;
    private final FLVListener flvListener =
            data -> {
//                Log.e(TAG, "===== dataLen:" + data.length);
                XP2P.dataSend(deviceId, data, data.length);
            };

    private ResolutionEntity selectedResolutionEntity;
    private FrameRateEntity selectedFrameRateEntity;

    private volatile long basePts = 0;
    private boolean mIsVideo;  //是否为视频对话，true为视频 false为音频

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getBundleExtra(PhoneInfo.TAG);
            if (bundle != null) {
                String jsonStr = bundle.getString(PhoneInfo.TAG);
                phoneInfo = JSON.parseObject(jsonStr, PhoneInfo.class);
                mIsVideo = phoneInfo.getCallType() == CallingType.TYPE_VIDEO_CALL;
                String resolutionJsonStr = bundle.getString(ResolutionEntity.TAG);
                if (resolutionJsonStr != null)
                    selectedResolutionEntity = JSON.parseObject(resolutionJsonStr, ResolutionEntity.class);
                vw = selectedResolutionEntity.getWidth();
                vh = selectedResolutionEntity.getHeight();
                String frameRateJsonStr = bundle.getString(FrameRateEntity.TAG);
                if (frameRateJsonStr != null)
                    selectedFrameRateEntity = JSON.parseObject(frameRateJsonStr, FrameRateEntity.class);
                frameRate = selectedFrameRateEntity.getRate();

                productId = bundle.getString("productId");
                deviceName = bundle.getString("deviceName");
                deviceId = productId + "/" + deviceName;
                xp2pInfo = bundle.getString("xp2pInfo");
            }
        }
        setContentView(R.layout.activity_record_video4);
        surfaceView = findViewById(R.id.cameraView);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
        btnSwitch = findViewById(R.id.btnSwitch);
        btnStop = findViewById(R.id.btnStop);
        btnSendCmd = findViewById(R.id.btnSendCommand);
        tvVideoWH = findViewById(R.id.tv_video_w_h);
        tvVCache = findViewById(R.id.tv_v_cache);
        tvACache = findViewById(R.id.tv_a_cache);
        tvFPS = findViewById(R.id.tv_fps);
        playView = findViewById(R.id.v_play);
        playView.setSurfaceTextureListener(this);
        if (phoneInfo.getCallType() == CallingType.TYPE_AUDIO_CALL) {
            surfaceView.setVisibility(View.INVISIBLE);
            btnSwitch.setVisibility(View.GONE);
        }

        btnSwitch.setOnClickListener(v -> switchCamera());
        btnStop.setOnClickListener(v -> {
            new Thread(() -> new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)).start();
        });
        btnSendCmd.setOnClickListener(v -> {
            byte[] command = "action=user_define&channel=0&cmd=ptz_left".getBytes();
            String repStatus = XP2P.postCommandRequestSync(deviceId,
                    command, command.length, 2 * 1000 * 1000);
            Log.e(TAG, "cmd=ptz_left repStatus: " + repStatus);
        });
        player = new IjkMediaPlayer();
        startXP2PService();
    }

    private void startXP2PService() {

        XP2P.setCallback(xp2pCallback);
        XP2P.startService(deviceId, productId, deviceName);
        XP2P.setParamsForXp2pInfo(deviceId, "", "", xp2pInfo);
    }

    private XP2PCallback xp2pCallback =  new XP2PCallback() {
        @Override
        public void fail(String msg, int errorCode) {

        }

        @Override
        public void commandRequest(String id, String msg) {

        }

        @Override
        public void xp2pEventNotify(String id, String msg, int event) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    switch (event) {
                        case 1003:
                            Toast.makeText(RecordVideoActivity4.this, "p2p链路断开，尝试重连", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "=========p2p链路断开，尝试重连");
                            startXP2PService();
                            reconnect = true;
                            break;

                        case 1004:
                            Toast.makeText(RecordVideoActivity4.this, "p2p链路初始化成功", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "=========p2p链路初始化成功");
                            initAudioEncoder();
                            initVideoEncoder();
                            if (getDeviceStatus()) {
                                play(phoneInfo.getCallType());
                            }
                            break;

                        case 1005:
                            Toast.makeText(RecordVideoActivity4.this, "p2p链路初始化失败", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "=========p2p链路初始化失败");
                            break;
                    }
                }
            });
        }

        @Override
        public void avDataRecvHandle(String id, byte[] data, int len) {

        }

        @Override
        public void avDataCloseHandle(String id, String msg, int errorCode) {

        }

        @Override
        public String onDeviceMsgArrived(String id, byte[] data, int len) {
            return "";
        }
    };

    private void initAudioEncoder() {
        MicParam micParam = new MicParam.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setSampleRateInHz(8000) // 采样率
                .setChannelConfig(AudioFormat.CHANNEL_IN_MONO)
                .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT) // PCM
                .build();
        AudioEncodeParam audioEncodeParam = new AudioEncodeParam.Builder().build();
        audioEncoder = new AudioEncoder(this, micParam, audioEncodeParam);
        audioEncoder.setOnEncodeListener(this);
    }

    private void initVideoEncoder() {
        VideoEncodeParam videoEncodeParam = new VideoEncodeParam.Builder().setSize(vw, vh).setFrameRate(frameRate).setBitRate(vw*vh).build();
        videoEncoder = new VideoEncoder(videoEncodeParam);
        videoEncoder.setEncoderListener(this);
    }

    private void startRecord() {
        if (phoneInfo != null) {
            if (phoneInfo.getCallType() == CallingType.TYPE_VIDEO_CALL) {
                startEncodeVideo = true;
            }
            audioEncoder.start();
        }
        isRecording = true;
    }

    private void stopRecord() {
        if (audioEncoder != null) {
            audioEncoder.stop();
        }
        if (videoEncoder != null) {
            videoEncoder.stop();
        }
        startEncodeVideo = false;
        isRecording = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XP2P.setCallback(null);
        XP2P.stopSendService(deviceId, null);
        XP2P.stopService(deviceId);
        if (player != null) {
            player.release();
        }
        stopRecord();
        if (io != null) {
            io.close();
        }
        executor.shutdown();
        releaseCamera(camera);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (surface != null) {
            this.surface = new Surface(surface);
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {return false; }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) { }

    private boolean getDeviceStatus() {
        byte[] command = "action=inner_define&channel=0&cmd=get_device_st&type=live&quality=standard".getBytes();
        String repStatus = XP2P.postCommandRequestSync(deviceId,
                command, command.length, 2 * 1000 * 1000);
        List<DeviceStatus> deviceStatuses = JSONArray.parseArray(repStatus, DeviceStatus.class);
        // 0   接收请求
        // 1   拒绝请求
        // 404 error request message
        // 405 connect number too many
        // 406 current command don't support
        // 407 device process error
        if (deviceStatuses != null && deviceStatuses.size() > 0) {
            switch (deviceStatuses.get(0).status) {
                case 0:
                    Toast.makeText(this, "设备状态正常", Toast.LENGTH_LONG).show();
                    return true;
                case 1:
                    Toast.makeText(this, "设备状态异常, 拒绝请求: " + repStatus, Toast.LENGTH_LONG).show();
                    return false;
                case 404:
                    Toast.makeText(this, "设备状态异常, error request message: " + repStatus, Toast.LENGTH_LONG).show();
                    return false;
                case 405:
                    Toast.makeText(this, "设备状态异常, connect number too many: " + repStatus, Toast.LENGTH_LONG).show();
                    return false;
                case 406:
                    Toast.makeText(this, "设备状态异常, current command don't support: " + repStatus, Toast.LENGTH_LONG).show();
                    return false;
                case 407:
                    Toast.makeText(this, "设备状态异常, device process error: " + repStatus, Toast.LENGTH_LONG).show();
                    return false;
            }
        } else {
            Toast.makeText(this, "获取设备状态失败", Toast.LENGTH_LONG).show();
            return false;
        }
        return false;
    }

    private void play(int callType) {
        if (player != null) {
            player.stop();
            player.setDisplay(null);
            player.release();
        }
        player = new IjkMediaPlayer();
        player.reset();
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_HUD, 500);
        if (!mIsVideo) {
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 64);
        } else {
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000000);
//            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 50 * 1024);
        }
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync-av-start", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec",1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);

        player.setFrameSpeed(1.5f);
        player.setMaxPacketNum(2);
        if (!getDeviceStatus()) return;
        player.setSurface(surface);
        String url = XP2P.delegateHttpFlv(deviceId) + "ipc.flv?action=live";
        Toast.makeText(this, url, Toast.LENGTH_LONG).show();
        Log.e(TAG, "======" + url);
        try {
            player.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.prepareAsync();
        player.start();

        // 开始推流
        XP2P.runSendService(deviceId, "channel=0", false);
        handler.post(() -> startRecord());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            if (phoneInfo != null) {
                bundle.putString(PhoneInfo.TAG, JSON.toJSONString(phoneInfo));
            }
            intent.putExtra(PhoneInfo.TAG, bundle);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onAudioEncoded(byte[] datas, long pts, long seq) {
        if (executor.isShutdown()) return;
        executor.submit(() -> {
            if (flvPacker == null) {
                flvPacker = new FLVPacker(flvListener, true, true);
                basePts = pts;
            }
            flvPacker.encodeFlv(datas, FLVPacker.TYPE_AUDIO, pts - basePts);
        });
    }

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq) {
        if (executor.isShutdown()) return;
        executor.submit(() -> {
            if (flvPacker == null) {
                flvPacker = new FLVPacker(flvListener, true, true);
                basePts = pts;
            }
            flvPacker.encodeFlv(datas, FLVPacker.TYPE_VIDEO, pts - basePts);
        });
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
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
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
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surface created.");
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surface changed.");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surface destroyed.");
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
                    float vdps = player.getVideoDecodeFramesPerSecond();
                    float vfps = player.getVideoOutputFramesPerSecond();
                    long tcpSpeed = player.getTcpSpeed();

                    tvACache.setText(String.format(Locale.US, "%s, %s",
                            CommonUtils.formatedDurationMilli(audioCachedDuration),
                            CommonUtils.formatedSize(audioCachedBytes)));
                    tvVCache.setText(String.format(Locale.US, "%s, %s",
                            CommonUtils.formatedDurationMilli(videoCachedDuration),
                            CommonUtils.formatedSize(videoCachedBytes)));
                    tvVideoWH.setText(player.getVideoWidth() + " x " + player.getVideoHeight());
                    tvFPS.setText(String.format(Locale.US, "%.2f / %.2f", vdps, vfps));
                    removeMessages(MSG_UPDATE_HUD);
                    sendEmptyMessageDelayed(MSG_UPDATE_HUD, 500);
            }
        }
    };
}

