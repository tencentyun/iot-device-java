package com.tencent.iot.explorer.device.video.push_stream;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.tencent.iot.explorer.device.android.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PushStreamActivity extends AppCompatActivity {

    private static final String TAG = PushStreamActivity.class.getSimpleName();

    static {
        System.loadLibrary("native-lib");
    }

    private final String devFileName = "device.json";
    public static final String csAACFileName = "audio_sample44100_stereo_96kbps.aac";
    public static final String csVideoFileName = "video_size640x360_gop50_fps25.h264";
    private final String cseScript = "event_test_script.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_stream);

        String path = getFilesDir().getAbsolutePath();
        String filePath = path;
        if (!filePath.endsWith("/")) {
            filePath += "/";
        }
        Log.d(TAG, "path is " + path);
        copyFileFromAssets(getApplicationContext(), devFileName);
        File csAACFile = new File(filePath + csAACFileName);
        File csVideoFile = new File(filePath + csVideoFileName);
        if (!csAACFile.exists()) {
            Toast.makeText(this, "请先在双向通话中录制音频文件", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!csVideoFile.exists()) {
            Toast.makeText(this, "请先在双向通话中录制视频文件", Toast.LENGTH_SHORT).show();
            return;
        }
        copyFileFromAssets(getApplicationContext(), cseScript);
        nativeDemo(path);
    }

    private void copyFilesFromAssets2(Context context, String assetsPath, String savePath) {
        try {
            String fileNames[] = context.getAssets().list(assetsPath);// 获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {// 如果是目录
                File file = new File(savePath);
                file.mkdirs();// 如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    File opFile = new File(savePath + "/" + fileName);
                    if (opFile.isDirectory()) {  // 路径就递归
                        copyFilesFromAssets2(context, assetsPath + "/" + fileName,
                                savePath + "/" + fileName);
                    } else { // 文件就拷贝
                        if (!opFile.exists()) {
                            opFile.createNewFile();
                        } else {
                            continue;
                        }
                        InputStream is = context.getAssets().open(assetsPath + "/" + fileName);
                        FileOutputStream fos = new FileOutputStream(opFile);
                        byte[] buffer = new byte[1024];
                        int byteCount = 0;
                        while ((byteCount = is.read(buffer)) > 0) {// 循环从输入流读取
                            // buffer字节
                            fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
                        }
                        fos.flush();// 刷新缓冲区
                        is.close();
                        fos.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyDirFromAssets(Context context, String folder) {
        String filesDir = context.getFilesDir().getPath();
        filesDir = filesDir + "/" + folder;
        copyFilesFromAssets2(context, folder, filesDir);
    }

    private void copyFileFromAssets(Context context, String fileName) {
        String absFileName = getFilesDir().getAbsolutePath() + "/" + fileName;
        Log.d(TAG, "Create file " + absFileName);

        File f = new File(absFileName);
        if (f.exists()) {
            Log.d(TAG, "file " + absFileName + " exist");
            return;
        } else if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            InputStream is = context.getAssets().open(fileName);
            byte[] bytes = new byte[1024];
            int bt = 0;
            FileOutputStream fos = new FileOutputStream(f);
            while ((bt = is.read(bytes)) != -1) {
                fos.write(bytes, 0, bt);
            }
            fos.flush();
            is.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String nativeDemo(String path);
}