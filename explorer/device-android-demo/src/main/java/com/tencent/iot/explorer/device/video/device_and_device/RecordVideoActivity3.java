package com.tencent.iot.explorer.device.video.device_and_device;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.iot.explorer.device.android.app.R;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RecordVideoActivity3 extends AppCompatActivity implements TextureView.SurfaceTextureListener, OnEncodeListener, SurfaceHolder.Callback {

    private static final String TAG = RecordVideoActivity3.class.getSimpleName();
    private static final int MAX_CONNECT_NUM = 4;
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private Camera camera;
    private Button btnSwitch;
    private Button btnStop;
    private Button btnSendCmd;
    private IjkMediaPlayer player;
    private Surface surface;
    private TextureView playView;
    private volatile PhoneInfo phoneInfo;
    private Handler handler = new Handler();
    private ReadByteIO io;
    private volatile HashMap<Integer, Boolean> visitors = new HashMap<>(4);

    private AudioEncoder audioEncoder;
    private VideoEncoder videoEncoder;
    private volatile boolean startEncodeVideo = false;
    private volatile boolean isRecording = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int vw = 640;
    private int vh = 480;
    private int frameRate = 15;

    private ResolutionEntity selectedResolutionEntity;
    private FrameRateEntity selectedFrameRateEntity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getBundleExtra(PhoneInfo.TAG);
            if (bundle != null) {
                String jsonStr = bundle.getString(PhoneInfo.TAG);
                phoneInfo = JSON.parseObject(jsonStr, PhoneInfo.class);
                String resolutionJsonStr = bundle.getString(ResolutionEntity.TAG);
                if (resolutionJsonStr != null)
                    selectedResolutionEntity = JSON.parseObject(resolutionJsonStr, ResolutionEntity.class);
                vw = selectedResolutionEntity.getWidth();
                vh = selectedResolutionEntity.getHeight();
                String frameRateJsonStr = bundle.getString(FrameRateEntity.TAG);
                if (frameRateJsonStr != null)
                    selectedFrameRateEntity = JSON.parseObject(frameRateJsonStr, FrameRateEntity.class);
                frameRate = selectedFrameRateEntity.getRate();
            }
        }
        setContentView(R.layout.activity_record_video3);
        surfaceView = findViewById(R.id.cameraView);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
        btnSwitch = findViewById(R.id.btnSwitch);
        btnStop = findViewById(R.id.btnStop);
        btnSendCmd = findViewById(R.id.btnSendCommand);
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
            new Thread(() -> Log.d(TAG, "sendMsgToPeer:" + VideoNativeInteface.getInstance().sendMsgToPeer(0,"hello", 1*1000))).start();
        });
        player = new IjkMediaPlayer();
        VideoNativeInteface.getInstance().setCallback(xP2PCallback);
        for (int i = 0; i < MAX_CONNECT_NUM; i++) {
            visitors.put(i, false);
        }
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
        audioEncoder = new AudioEncoder(this, micParam, audioEncodeParam);
        audioEncoder.setOnEncodeListener(this);
    }

    private void initVideoEncoder() {
        VideoEncodeParam videoEncodeParam = new VideoEncodeParam.Builder().setSize(vw, vh).setFrameRate(frameRate).setBitRate(vw*vh).build();
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

    private XP2PCallback xP2PCallback = new XP2PCallback() {

        @Override
        public void avDataRecvHandle(byte[] data, int len) {
//            Log.e(TAG, "====datalen: " + len + "bytes: " + ByteUtils.Companion.bytesToHex(data));
            if (io != null) {
//                Log.e(TAG, "********datalen: " + len + "bytes: " + ByteUtils.Companion.bytesToHex(data));
                io.addLast(data);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void avDataMsgHandle(int type, String msg) {
            Log.e(TAG, "====avDataMsgHandle type: " + type + " msg: " + msg);
            JSONObject obj = JSON.parseObject(msg);
            int visitorId = -1;
            if (obj != null) {
                visitorId = obj.getIntValue("visitor");
            }
            if (type == 0) { //开始预览
                visitors.put(visitorId, true);
                Log.e(TAG, "visitor " + visitorId + " 开始预览.");
            } else if (type == 1) { //结束预览
                visitors.put(visitorId, false);
                Log.e(TAG, "visitor " + visitorId + " 结束预览.");
            } else if (type == 2) { //开始对讲
                Log.e(TAG, "visitor " + visitorId + " 开始对讲.");
                play();
            } else if (type == 3) { //结束对讲
                Log.e(TAG, "visitor " + visitorId + " 结束对讲.");
                new Thread(() -> {
                    player.stop();
                    player.release();
                    if (io != null) {
                        io.reset();
                    }
                }).start();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoNativeInteface.getInstance().setCallback(null);
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

    private void play() {
        player = new IjkMediaPlayer();
        player.reset();
        if (phoneInfo.getCallType() == CallingType.TYPE_AUDIO_CALL) {
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 64);
        } else {
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000000);
        }
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync-av-start", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec",1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "ijkio,crypto,file,http,https,tcp,tls,udp");

        io = new ReadByteIO();
        io.reset();
        io.setPlayType(phoneInfo.getCallType());
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
        for (int i = 0; i < MAX_CONNECT_NUM; i++) {
            if (visitors.get(i)) {
                if (executor.isShutdown()) return;
                int finalI = i;
                executor.submit(() -> VideoNativeInteface.getInstance().sendAudioData(datas, System.currentTimeMillis(), seq, finalI));
            }
        }
    }

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq) {
        for (int i = 0; i < MAX_CONNECT_NUM; i++) {
            if (visitors.get(i)) {
                if (executor.isShutdown()) return;
                int finalI = i;
                executor.submit(() -> VideoNativeInteface.getInstance().sendVideoData(datas, System.currentTimeMillis(), seq, finalI));
            }
        }
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
                if (startEncodeVideo && videoEncoder != null && visitors.containsValue(true)) {
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surface created.");
        openCamera();
        if (!isRecording) {
            handler.post(this::startRecord);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surface changed.");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surface destroyed.");
    }
}

