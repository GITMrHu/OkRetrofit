package com.hengda.frame.httputil;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hengda.frame.httputil.app.HdAppConfig;
import com.hengda.frame.httputil.app.HdConstants;
import com.hengda.frame.httputil.http.RetrofitHelper;
import com.hengda.frame.httputil.update.CheckCallback;
import com.hengda.frame.httputil.update.CheckUpdateActivity;
import com.hengda.zwf.httputil.httpload.RxDownload;
import com.hengda.zwf.httputil.httpload.entity.DownloadStatus;
import com.hengda.zwf.httputil.httprequest.UpdateResponse;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends CheckUpdateActivity {

    CompositeDisposable compositeDisposable = new CompositeDisposable();
    String url = "http://hengdawb-res.oss-cn-hangzhou.aliyuncs.com/HuLuDao_Res/CHINESE.zip";
    String saveName = "CHINESE.zip";
    String savePath = HdAppConfig.getDefaultFileDir();
    TextView tvDownloadStatus;
    TextView tvDownloadPrg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvDownloadStatus = (TextView) findViewById(R.id.tvDownloadStatus);
        tvDownloadPrg = (TextView) findViewById(R.id.tvDownloadPrg);

        //检查更新
        findViewById(R.id.btnDeviceNo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RetrofitHelper.getInstance()
                        .reqDeviceNo(HdConstants.APP_KIND)
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Logger.e(throwable.getMessage());
                            }
                        });
            }
        });

        //检查更新
        findViewById(R.id.btnUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkNewVersion(new CheckCallback() {
                    @Override
                    public void hasNewVersion(UpdateResponse response) {
                        showHasNewVersionDialog(response);
                    }

                    @Override
                    public void isAlreadyLatestVersion() {
                        showVersionInfoDialog();
                    }
                });
            }
        });

        //正常下载
        findViewById(R.id.btnDownload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消下载
        compositeDisposable.dispose();
    }

    private void download() {
        RxDownload.getInstance().context(this).maxThread(16).maxRetryCount(3)
                .download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        compositeDisposable.add(disposable);
                        tvDownloadStatus.setText("下载地址：" + url + "\n");
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
                        tvDownloadStatus.setText(tvDownloadStatus.getText() + "\n开始下载：" + sdf.format(new Date()));
                    }
                })
                .doOnNext(new Consumer<DownloadStatus>() {
                    @Override
                    public void accept(DownloadStatus downloadStatus) throws Exception {
                        tvDownloadPrg.setText("下载进度：" + downloadStatus.getFormatStatusString());
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        tvDownloadStatus.setText("下载失败:" + throwable.getMessage());
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
                        tvDownloadPrg.setText(tvDownloadPrg.getText() + "\n下载完成：" + sdf.format(new Date()));
                        File file = new File(savePath, saveName);
                        file.delete();
                    }
                })
                .subscribe();
    }

}
