package com.tencent.iot.explorer.device.video.call;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
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
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoFormat;
import com.tencent.iot.thirdparty.android.device.video.p2p.VideoNativeInteface;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static com.tencent.iot.explorer.device.android.utils.ConvertUtils.byte2HexOnlyLatest8;
import static com.tencent.iot.explorer.device.video.recorder.consts.LogConst.RTC_TAG;

public class RecordVideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, OnEncodeListener, SurfaceHolder.Callback,
        IMediaPlayer.OnInfoListener {

    private static Timer bitRateTimer;
    private String TAG = RecordVideoActivity.class.getSimpleName();
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private Camera camera;
    private Button btnSwitch;
    private String path; // 保存源文件的路径
    private IjkMediaPlayer player = null;
    private Surface surface;
    private TextureView playView;
    private TextView tvVideoWH;
    private TextView tvVCache;
    private TextView tvACache;
    private TextView tvFPS;
    private TextView tvStreamStatus;
    private volatile PhoneInfo phoneInfo;
    private Handler handler = new Handler();
    private Button recordBtn;
    private Button hangupBtn;

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
    private int frameRate = 15;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registVideoOverBrodcast();
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
                vw = selectedResolutionEntity.getWidth();
                vh = selectedResolutionEntity.getHeight();
                String frameRateJsonStr = bundle.getString(FrameRateEntity.TAG);
                if (frameRateJsonStr != null)
                    selectedFrameRateEntity = JSON.parseObject(frameRateJsonStr, FrameRateEntity.class);
                frameRate = selectedFrameRateEntity.getRate();
            }
        }
        setContentView(R.layout.activity_record_video);
        recordBtn = findViewById(R.id.btnRecord);
        tvVideoWH = findViewById(R.id.tv_video_w_h);
        tvVCache = findViewById(R.id.tv_v_cache);
        tvACache = findViewById(R.id.tv_a_cache);
        tvFPS = findViewById(R.id.tv_fps);
        tvStreamStatus = findViewById(R.id.tv_stream_status);
        hangupBtn = findViewById(R.id.btn_hang_up);
        hangupBtn.setOnClickListener(v -> {
            new Thread(this::finishActivity).start();
        });
        recordBtn.setText("Record \n path:" + path);
        surfaceView = findViewById(R.id.cameraView);
        holder = surfaceView.getHolder();
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

        ReadByteIO.Companion.getInstance().setPlayType(phoneInfo.getCallType());
    }

    private void initAudioEncoder() {
        MicParam micParam = new MicParam.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setSampleRateInHz(16000) // 采样率
                .setChannelConfig(AudioFormat.CHANNEL_IN_MONO)
                .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT) // PCM
                .build();
        AudioEncodeParam audioEncodeParam = new AudioEncodeParam.Builder().build();
        audioEncoder = new AudioEncoder(this, micParam, audioEncodeParam, true, true);
        audioEncoder.setOnEncodeListener(this);
    }

    private void initVideoEncoder() {
        VideoEncodeParam videoEncodeParam = new VideoEncodeParam.Builder().setSize(vw, vh).setFrameRate(frameRate).setBitRate(vw*vh).build();
        videoEncoder = new VideoEncoder(videoEncodeParam);
        videoEncoder.setEncoderListener(this);
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfoSEI(IMediaPlayer mp, int what, int extra, String sei_content) {
        return false;
    }

    @Override
    public void onInfoAudioPcmData(IMediaPlayer mp, byte[] arrPcm, int length) {
        if (audioEncoder != null && length > 0) {
            audioEncoder.setPlayerPcmData(arrPcm);
        }
    }


    public class AdapterBitRateTask extends TimerTask {
        @Override
        public void run() {
            System.out.println("检测时间到:" +new Date());


            int bufsize = VideoNativeInteface.getInstance().getSendStreamBuf(0);
//        return String.format(Locale.US, "buf=>%d<=", bufsize);

//            videoEncoder.setVideoBitRate(10000);
//            RecordVideoActivity.this.videoEncoder.setVideoBitRate(10000);
            int p2p_wl_avg = VideoNativeInteface.getInstance().getAvgMaxMin(bufsize);

            int now_video_rate = RecordVideoActivity.this.videoEncoder.getVideoBitRate();

            Log.e(TAG,"send_bufsize==" + bufsize + ",now_video_rate==" + now_video_rate + ",avg_index==" + p2p_wl_avg);

            // 降码率
            // 当发现p2p的水线超过一定值时，降低视频码率，这是一个经验值，一般来说要大于 [视频码率/2]
            // 实测设置为 80%视频码率 到 120%视频码率 比较理想
            // 在10组数据中，获取到平均值，并将平均水位与当前码率比对。

            int video_rate_byte = (now_video_rate / 8) * 3 / 4;
            if (p2p_wl_avg > video_rate_byte) {

                videoEncoder.setVideoBitRate(video_rate_byte*8);

            }else if (p2p_wl_avg <  (now_video_rate / 8) / 3) {

                // 升码率
                // 测试发现升码率的速度慢一些效果更好
                // p2p水线经验值一般小于[视频码率/2]，网络良好的情况会小于 [视频码率/3] 甚至更低
                videoEncoder.setVideoBitRate(now_video_rate + (now_video_rate-p2p_wl_avg*8)/5);
            }
        }
    }

    private void startBitRateAdapter() {

        VideoNativeInteface.getInstance().resetAvg();
        bitRateTimer = new Timer();
        bitRateTimer.schedule(new AdapterBitRateTask(),3000,1000);
    }

    private void stopBitRateAdapter() {
        if (bitRateTimer != null) {
            bitRateTimer.cancel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startRecord() {
        if (phoneInfo != null) {
            if (phoneInfo.getCallType() == CallingType.TYPE_VIDEO_CALL) {
                startEncodeVideo = true;
            }
            audioEncoder.start();

            startBitRateAdapter();
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
        stopBitRateAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        holder.addCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

//    private final XP2PCallback xP2PCallback = new XP2PCallback() {
//
//        @Override
//        public void avDataRecvHandle(byte[] data, int len) {
////            Log.e(TAG, "=======datalen: " + len + "bytes: " + ByteUtils.Companion.bytesToHex(data));
//            io.addLast(data);
//        }
//
//        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//        @Override
//        public void avDataMsgHandle(int type, String msg) {
//            Log.e(TAG, "*========avDataMsgHandle type " + type);
//            if (type == 0) {
//                Log.e(TAG, "*======start send video data");
//                handler.post(() -> startRecord());
//                runOnUiThread(() -> Toast.makeText(RecordVideoActivity.this, "开始推流", Toast.LENGTH_LONG).show());
//            } else if (type == 1) {
//                Log.e(TAG, "*======this call over");
//                handler.post(() -> stopRecord());
//                runOnUiThread(() -> Toast.makeText(RecordVideoActivity.this, "停止推流", Toast.LENGTH_LONG).show());
//                if (!RecordVideoActivity.this.isDestroyed() && !RecordVideoActivity.this.isFinishing()) {
//                    new Thread(() -> finishActivity()).start();
//                }
//            }
//        }
//    };

    private void releasePlayer() {
        if (player != null) {
            mHandler.removeMessages(MSG_UPDATE_HUD);
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregistVideoOverBrodcast();
        releasePlayer();

        ReadByteIO.Companion.getInstance().reset();
        ReadByteIO.Companion.getInstance().close();
        executor.shutdown();
        releaseCamera(camera);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (surface != null) {
            this.surface = new Surface(surface);
            releasePlayer();
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
        if (this.surface == null) {
            return;
        }
        player = new IjkMediaPlayer();
        player.reset();
        player.setOnInfoListener(this);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_HUD, 500);
        /*
         * probesize & analyzeduration 可通过这两个参数进行首开延时优化
         * 单位字节 & 单位微秒 表示探测多大数据和多长时间
         * 如果推流端码率较小，可降低探测字节数，以及缩短探测时长
         * https://github.com/tencentyun/iot-link-ios/blob/master/Source/SDK/LinkVideo/doc/IoTVideo%20常见问题指引.md
         */
        if (phoneInfo.getCallType() == CallingType.TYPE_AUDIO_CALL) {
//            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000000);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 256);
        } else {
//            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1000000);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 25 * 1024);
        }
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync-av-start", 0);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        player.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "ijkio,crypto,file,http,https,tcp,tls,udp");

        player.setFrameSpeed(1.5f);
        player.setMaxPacketNum(2);
        player.setSurface(this.surface);
        player.setAndroidIOCallback(ReadByteIO.Companion.getInstance());
        player.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                Log.e(TAG, "*====== Error: " + what + "," + extra);
                return false;
            }
        });

        Uri uri = Uri.parse("ijkio:androidio:" + ReadByteIO.Companion.getURL_SUFFIX());
        try {
            player.setDataSource(uri.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.prepareAsync();
        player.start();
    }

    private void finishActivity() {
        stopRecord();

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
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            int refreshTag = intent.getIntExtra(Utils.VIDEO_OVER, 0);
            if (refreshTag == 9) {
                if (!RecordVideoActivity.this.isDestroyed() && !RecordVideoActivity.this.isFinishing()) {
                    new Thread(() -> finishActivity()).start();
                }
            } else if (refreshTag == 1) {
                Log.e(TAG, "*====== 开始推流");
                initAudioEncoder();
                initVideoEncoder();
                VideoFormat format = new VideoFormat.Builder().setVideoWidth(vw).setVideoHeight(vh).setAudioSampleRate(16000).build();
                VideoNativeInteface.getInstance().initVideoFormat(format);
                handler.post(() -> startRecord());
                runOnUiThread(() -> Toast.makeText(RecordVideoActivity.this, "开始推流", Toast.LENGTH_LONG).show());
            } else if (refreshTag == 2) {
                Log.e(TAG, "*====== 结束推流");
                handler.post(() -> stopRecord());
                runOnUiThread(() -> Toast.makeText(RecordVideoActivity.this, "停止推流", Toast.LENGTH_LONG).show());
            } else if (refreshTag == 3) {
                Log.e(TAG, "*====== 开始对讲");
                releasePlayer();
                play();
            } else if (refreshTag == 4) { //结束对讲
                Log.e(TAG, "*====== 结束对讲.");
                releasePlayer();
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
        executor.submit(() -> {
            Log.i(RTC_TAG, "VideoNativeInteface.getInstance sendAudioData: "+byte2HexOnlyLatest8(datas) + "; seq: " + seq);
            VideoNativeInteface.getInstance().sendAudioData(datas, pts, seq, 0);
        });
    }

    @Override
    public void onVideoEncoded(byte[] datas, long pts, long seq) {
        if (executor.isShutdown()) return;
        executor.submit(() -> {
            Log.i(RTC_TAG, "VideoNativeInteface.getInstance onVideoEncoded: "+byte2HexOnlyLatest8(datas) + "; seq: " + seq);
            VideoNativeInteface.getInstance().sendVideoData(datas, pts, seq, 0);
        });
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
        surfaceHolder.removeCallback(this);
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        releaseCamera(camera);
        camera = Camera.open(facing);
        Log.i(RTC_TAG, String.format("camera open isFront: %b",facing == CameraConstants.facing.FRONT));
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
        //设置帧率
        parameters.setPreviewFrameRate(frameRate);
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
                Log.i(RTC_TAG, "camera onPreviewFrame capture original frame data: " + byte2HexOnlyLatest8(data));
                if (startEncodeVideo && videoEncoder != null) {
                    videoEncoder.encoderH264(data, facing == CameraConstants.facing.FRONT);
                }
            }
        });
        //调用startPreview()用以更新preview的surface
        camera.startPreview();
        Log.i(RTC_TAG, "camera startPreview with parameters: " + camera.getParameters());
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
                    tvStreamStatus.setText(String.format(Locale.US, "%s", getSendStreamStatus()));
                    removeMessages(MSG_UPDATE_HUD);
                    sendEmptyMessageDelayed(MSG_UPDATE_HUD, 500);
            }
        }
    };

    private String getSendStreamStatus() {
        switch (VideoNativeInteface.getInstance().getSendStreamStatus(0)) {
            case 0:
                return "Unkown";
            case 1:
                return "Direct";
            case 2:
                return "Turn";
        }
        return "Unkown";
    }

}