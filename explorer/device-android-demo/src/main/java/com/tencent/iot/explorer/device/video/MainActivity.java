package com.tencent.iot.explorer.device.video;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.tencent.iot.explorer.device.android.app.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String devFileName = "device.json";

    private void copyFileFromAssets(Context context, String fileName) {
        String absFileName = getFilesDir().getAbsolutePath() + "/" + fileName;
        Log.d(TAG, "Create file " + absFileName);

        File f = new File(absFileName);
        if ((fileName.equals(devFileName) == false) && f.exists()) {
            Log.d(TAG, "file " + absFileName + " exist");
            return;
        }

        byte[] bytes = new byte[1024];
        try (InputStream is = context.getAssets().open(fileName);
             FileOutputStream fos = new FileOutputStream(f)) {
            int btLen = 0;
            while ((btLen = is.read(bytes)) != -1) {
                fos.write(bytes, 0, btLen);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        copyFileFromAssets(getApplicationContext(), devFileName);
        Intent intent = new Intent(MainActivity.this, RecordVideoActivity.class);
        startActivity(intent);
        finish();
    }
}