package cn.rongcloud.im.ui.activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.Set;

import cn.rongcloud.im.R;
import cn.rongcloud.im.model.Resource;
import cn.rongcloud.im.model.Status;
import cn.rongcloud.im.qrcode.SealQrCodeUISelector;
import cn.rongcloud.im.ui.BaseActivity;
import cn.rongcloud.im.utils.ToastUtils;
import cn.rongcloud.im.viewmodel.SplashViewModel;
import cn.rongcloud.im.utils.log.SLog;
import io.rong.imlib.common.DeviceUtils;
import io.rong.push.PushType;

public class SplashActivity extends BaseActivity {
    private Uri intentUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 处理小米手机按 home 键重新进入会重新打开初始化的页面
        if (!this.isTaskRoot()) {
            Intent mainIntent = getIntent();
            String action = mainIntent.getAction();
            if (mainIntent.hasCategory(Intent.CATEGORY_LAUNCHER) && action.equals(Intent.ACTION_MAIN)) {
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_splash);

        Intent intent = getIntent();
        if (intent != null) {
            intentUri = intent.getData();
        }
        initViewModel();
        // 初始化通知渠道,目前只有华为推送需要配置
        initNotificationChannel();
    }

    /**
     * 初始化ViewModel
     */
    private void initViewModel() {
        SplashViewModel splashViewModel = ViewModelProviders.of(this).get(SplashViewModel.class);
        splashViewModel.getAutoLoginResult().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean result) {
                SLog.d("ss_auto", "result = " + result);

                if (result) {
                    goToMain();
                } else {
                    if (intentUri != null) {
                        ToastUtils.showToast(R.string.seal_qrcode_jump_without_login);
                    }
                    goToLogin();
                }
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        if (intentUri != null) {
            goWithUri();
        } else {
            finish();
        }
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * 通过 uri 进行跳转
     */
    private void goWithUri() {
        String uri = intentUri.toString();

        // 判断是否是二维码跳转产生的 uri
        SealQrCodeUISelector uiSelector = new SealQrCodeUISelector(this);
        LiveData<Resource<String>> result = uiSelector.handleUri(uri);

        result.observe(this, new Observer<Resource<String>>() {
            @Override
            public void onChanged(Resource<String> resource) {
                if (resource.status != Status.LOADING) {
                    result.removeObserver(this);
                }

                if (resource.status == Status.SUCCESS) {
                    finish();
                }
            }
        });

    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String os = DeviceUtils.getDeviceManufacturer().toLowerCase();
            if (os.contains("HUAWEI".toLowerCase())) {
                String channelId = "hwNotification";
                String channelName = "华为推送";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                createNotificationChannel(channelId, channelName, importance, 0);
                String channelIdImage = "hwImageNotification";
                String channelNameImage = "华为推送图片";
                int importanceImage = NotificationManager.IMPORTANCE_DEFAULT;
                createNotificationChannel(channelIdImage, channelNameImage, importanceImage, 0);
                String channelIdGif = "hwGifNotification";
                String channelNameGif = "华为推送动图";
                int importanceGif = NotificationManager.IMPORTANCE_DEFAULT;
                createNotificationChannel(channelIdGif, channelNameGif, importanceGif, 0);
                //增加铃声渠道,可选择资源文件里面的音频文件
                String channelIdKanong = "NotificationKanong";
                String channelNameKanong = "铃声Kanong";
                createNotificationChannel(channelIdKanong, channelNameKanong, importance, R.raw.kanong);
                String channelIdAihjj = "NotificationAihjj";
                String channelNameAihjj = "铃声Aihjj";
                createNotificationChannel(channelIdAihjj, channelNameAihjj, importance, R.raw.aihjj);
                String channelIdVoip = "NotificationVoip";
                String channelNameVoip = "铃声Voip";
                createNotificationChannel(channelIdVoip, channelNameVoip, importance, R.raw.voip);
            } else if (os.contains("Xiaomi".toLowerCase())) {
                String channelIdVoip = "rongcloud_kanong";
                String channelNameVoip = "sound";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                createNotificationChannel(channelIdVoip, channelNameVoip, importance, R.raw.kanong);
            }
        }
    }

    //创建通知渠道
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName, int importance, int rawSource) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        if (rawSource != 0) {
            String uriStr = "android.resource://" + this.getPackageName() + "/" + rawSource;
            Uri uri = Uri.parse(uriStr);
            channel.setSound(uri, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
}
