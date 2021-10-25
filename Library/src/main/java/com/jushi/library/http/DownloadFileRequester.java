package com.jushi.library.http;


import android.view.Gravity;

import com.jushi.library.base.BaseApplication;
import com.jushi.library.utils.LogUtil;
import com.jushi.library.utils.ToastUtil;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 下载文件
 */
public class DownloadFileRequester implements Callback {
    private static final OkHttpClient httpClient;
    private OnDownloadListener downloadListener;
    private String downloadPath;
    private String fileName;

    static {
        httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * @param url              文件下载地址
     * @param savePath         文件保存路径
     * @param fileName         保存的文件名
     * @param downloadListener
     */
    public void download(String url, String savePath, String fileName, OnDownloadListener downloadListener) {
        LogUtil.v("****************************开始下载文件*********************");
        LogUtil.v("url ：" + url);
        LogUtil.v("savePath ：" + savePath);
        LogUtil.v("fileName ：" + fileName);
        LogUtil.v("*************************************************************");
        this.downloadListener = downloadListener;
        this.downloadPath = savePath;
        this.fileName = fileName;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        httpClient.newCall(request).enqueue(this);
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        if (downloadListener != null)
            downloadListener.onError(-1,e.getMessage());
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        if (response.code() != 200) {
            if (downloadListener != null)
                downloadListener.onError(response.code(),"文件下载链接无法访问");
            return;
        }
        InputStream is = null;
        byte[] buf = new byte[2048];
        int length = 0;
        FileOutputStream fos = null;
        try {
            is = response.body().byteStream();
            long total = response.body().contentLength();
            File file = new File(downloadPath, fileName);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            long sum = 0;
            while ((length = is.read(buf)) != -1) {
                fos.write(buf, 0, length);
                sum += length;
                if (downloadListener != null)
                    downloadListener.onProgress((int) (sum * 1.0f / total * 100));
            }
            fos.flush();
            if (downloadListener != null)
                downloadListener.onSuccess(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            if (downloadListener != null)
                downloadListener.onError(-1,e.getMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnDownloadListener {
        void onProgress(int progress);

        void onSuccess(String filePath);

        void onError(int code, String msg);
    }
}
