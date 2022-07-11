package com.tencent.iot.explorer.device.android.http.retrofit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadRequest {

    private static DownloadRequest downloadUtil;
    private OkHttpClient okHttpClient;
    private volatile int lastProgress = 0;

    public static synchronized DownloadRequest get() {
        if (downloadUtil == null) {
            downloadUtil = new DownloadRequest();
        }
        return downloadUtil;
    }

    public void release() {
        okHttpClient = null;
        downloadUtil = null;
    }

    private DownloadRequest() {
        okHttpClient = new OkHttpClient();
    }

    public String download(String url, final String saveDir, final OnDownloadListener listener) {
        // 需要token的时候可以这样做
        // SharedPreferences sp=MyApp.getAppContext().getSharedPreferences("loginInfo", MODE_PRIVATE);
        // Request request = new Request.Builder().header("token",sp.getString("token" , "")).url(url).build();

        final String requestId = String.valueOf(System.currentTimeMillis() / 1000);

        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) { // 下载失败
                listener.onDownloadFailed(requestId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException { // 下载中
                byte[] buf = new byte[2048];
                File file = new File(saveDir);
                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(file)) {

                    long total = response.body().contentLength();

                    long sum = 0;
                    int len = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);

                        // 保证一个进度只会回调一次
                        if (lastProgress != progress) {
                            listener.onDownloading(requestId, progress);   // 下载中
                            lastProgress = progress;
                        }
                    }
                    fos.flush();
                    listener.onDownloadSuccess(requestId);   // 下载完成

                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onDownloadFailed(requestId);
                }
            }
        });
        return requestId;
    }

    public interface OnDownloadListener {
        void onDownloadSuccess(String requestId);
        void onDownloading(String requestId, int progress);
        void onDownloadFailed(String requestId);
    }
}
