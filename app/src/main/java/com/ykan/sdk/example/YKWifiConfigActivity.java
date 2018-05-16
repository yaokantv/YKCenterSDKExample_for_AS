package com.ykan.sdk.example;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.yaokan.sdk.giz.net.NetUtils;
import com.yaokan.sdk.wifi.DeviceConfig;
import com.yaokan.sdk.wifi.listener.IDeviceConfigListener;

import java.util.Timer;
import java.util.TimerTask;

public class YKWifiConfigActivity extends Activity implements View.OnClickListener, IDeviceConfigListener {

    private RotateAnimation animation;
    private CheckBox cbLaws;
    private EditText etPsw;
    private TextView etSSID;
    private ImageView ivRadar;

    public int toastTime = 2000;
    int secondleft = 60;
    Timer timer;
    private String timerText;
    private TextView tvFinding;
    private DeviceConfig deviceConfig;
    private LinearLayout wifill;
    private String workSSID;
    /**
     * The handler.
     */

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            HandlerKey key = HandlerKey.values()[msg.what];
            switch (key) {
                case TIMER_TEXT:
                    tvFinding.setText(timerText);
                    break;
                case START_TIMER:
                    isStartTimer();
                    break;
                case SUCCESSFUL:
                    Toast.makeText(getApplicationContext(), R.string.configuration_successful, toastTime).show();
                    stopConfig(true);
                    break;
                case FAILED:
                    Toast.makeText(YKWifiConfigActivity.this, (String) msg.obj, YKWifiConfigActivity.this.toastTime).show();
                    stopConfig(false);
                    break;
                default:
                    break;

            }
        }

    };

    /**
     * 初始化布局
     */
    private void initView() {
        this.cbLaws = ((CheckBox) findViewById(R.id.switchpwd));
        this.etSSID = ((TextView) findViewById(R.id.currentwifi));
        this.etPsw = ((EditText) findViewById(R.id.wifipwd));
        this.tvFinding = ((TextView) findViewById(R.id.tv_finding));
        this.wifill = ((LinearLayout) findViewById(R.id.wifi_ll));
        this.ivRadar = ((ImageView) findViewById(R.id.iv_radar));
        this.animation = new RotateAnimation(0.0F, 359.0F, 1, 0.5F, 1, 0.5F);
        this.animation.setRepeatCount(-1);
        this.animation.setDuration(2000L);
        LinearInterpolator localLinearInterpolator = new LinearInterpolator();
        this.animation.setInterpolator(localLinearInterpolator);
    }

    private void setListener() {
        cbLaws.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    etPsw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    etPsw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });
    }

    /**
     * 启动配置遥控中心
     */
    private void startConfigAirlink() {
        String str = etPsw.getText().toString();
        deviceConfig.setPwdSSID(workSSID, str);
        deviceConfig.startAirlink(workSSID, str);
        mHandler.sendEmptyMessage(HandlerKey.START_TIMER.ordinal());
        timerText = getResources().getString(R.string.finding_smart_tv, 0) + "%)";
        mHandler.sendEmptyMessage(HandlerKey.TIMER_TEXT.ordinal());
        wifill.setVisibility(View.VISIBLE);
        ivRadar.startAnimation(animation);
    }

    public void clickLeft(View paramView) {
        onBackPressed();
    }

    // 倒计时
    public void isStartTimer() {
        secondleft = 60;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                secondleft--;
                int progress = (int) ((60 - secondleft) * (100 / 60.0));
                if (progress <= 100) {
                    timerText = getResources().getString(R.string.finding_smart_tv, progress) + "%)";
                    mHandler.sendEmptyMessage(HandlerKey.TIMER_TEXT.ordinal());
                }

            }
        }, 1000, 1000);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addwifi:
                startConfigAirlink();
                break;
            default:
                break;
        }

    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.act_wifi_config);
        //实例化配置对象
        deviceConfig = new DeviceConfig(getApplicationContext(), this);
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        workSSID = NetUtils.getCurentWifiSSID(this);
        etSSID.setText(workSSID);
        etPsw.setText(deviceConfig.getPwdBySSID(workSSID));
    }

    protected void stopConfig(boolean successful) {
        this.wifill.setVisibility(View.GONE);
        tvFinding.setText(timerText);
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }
        if (successful) {
            finish();
        }
    }

    public enum HandlerKey {

        /**
         * 倒计时通知
         */
        TIMER_TEXT,

        /**
         * 倒计时开始
         */
        START_TIMER,

        /**
         * 配置成功
         */
        SUCCESSFUL,

        /**
         * 配置失败
         */
        FAILED,

    }

    @Override
    public void didSetDeviceOnboarding(GizWifiErrorCode result, String mac,
                                       String did, String productKey) {

    }

    @Override
    public void didSetDeviceOnboardingX(GizWifiErrorCode result, GizWifiDevice gizWifiDevice) {
        if (GizWifiErrorCode.GIZ_SDK_DEVICE_CONFIG_IS_RUNNING == result) {
            return;
        }
        Message message = new Message();
        if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
            message.obj = gizWifiDevice.getMacAddress();
            message.what = HandlerKey.SUCCESSFUL.ordinal();
        } else {
            message.what = HandlerKey.FAILED.ordinal();
            message.obj = toastError(this, result);
        }
        mHandler.sendMessage(message);
    }

    private String toastError(Context ctx, GizWifiErrorCode errorCode) {
        String errorString = (String) ctx.getResources().getText(R.string.UNKNOWN_ERROR);
        switch (errorCode) {
            case GIZ_SDK_PARAM_FORM_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_PARAM_FORM_INVALID);
                break;
            case GIZ_SDK_CLIENT_NOT_AUTHEN:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_CLIENT_NOT_AUTHEN);
                break;
            case GIZ_SDK_CLIENT_VERSION_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_CLIENT_VERSION_INVALID);
                break;
            case GIZ_SDK_UDP_PORT_BIND_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_UDP_PORT_BIND_FAILED);
                break;
            case GIZ_SDK_DAEMON_EXCEPTION:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DAEMON_EXCEPTION);
                break;
            case GIZ_SDK_PARAM_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_PARAM_INVALID);
                break;
            case GIZ_SDK_APPID_LENGTH_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_APPID_LENGTH_ERROR);
                break;
            case GIZ_SDK_LOG_PATH_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_LOG_PATH_INVALID);
                break;
            case GIZ_SDK_LOG_LEVEL_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_LOG_LEVEL_INVALID);
                break;
            case GIZ_SDK_DEVICE_CONFIG_SEND_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONFIG_SEND_FAILED);
                break;
            case GIZ_SDK_DEVICE_CONFIG_IS_RUNNING:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONFIG_IS_RUNNING);
                break;
            case GIZ_SDK_DEVICE_CONFIG_TIMEOUT:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONFIG_TIMEOUT);
                break;
            case GIZ_SDK_DEVICE_DID_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_DID_INVALID);
                break;
            case GIZ_SDK_DEVICE_MAC_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_MAC_INVALID);
                break;
            case GIZ_SDK_DEVICE_PASSCODE_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_PASSCODE_INVALID);
                break;
            case GIZ_SDK_DEVICE_NOT_SUBSCRIBED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_NOT_SUBSCRIBED);
                break;
            case GIZ_SDK_DEVICE_NO_RESPONSE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_NO_RESPONSE);
                break;
            case GIZ_SDK_DEVICE_NOT_READY:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_NOT_READY);
                break;
            case GIZ_SDK_DEVICE_NOT_BINDED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_NOT_BINDED);
                break;
            case GIZ_SDK_DEVICE_CONTROL_WITH_INVALID_COMMAND:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONTROL_WITH_INVALID_COMMAND);
                break;
        /*case GIZ_SDK_DEVICE_CONTROL_FAILED:
		 errorString= (String)
		ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONTROL_FAILED);
		 break;*/
            case GIZ_SDK_DEVICE_GET_STATUS_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_GET_STATUS_FAILED);
                break;
            case GIZ_SDK_DEVICE_CONTROL_VALUE_TYPE_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONTROL_VALUE_TYPE_ERROR);
                break;
            case GIZ_SDK_DEVICE_CONTROL_VALUE_OUT_OF_RANGE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONTROL_VALUE_OUT_OF_RANGE);
                break;
            case GIZ_SDK_DEVICE_CONTROL_NOT_WRITABLE_COMMAND:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONTROL_NOT_WRITABLE_COMMAND);
                break;
            case GIZ_SDK_BIND_DEVICE_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_BIND_DEVICE_FAILED);
                break;
            case GIZ_SDK_UNBIND_DEVICE_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_UNBIND_DEVICE_FAILED);
                break;
            case GIZ_SDK_DNS_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DNS_FAILED);
                break;
            case GIZ_SDK_M2M_CONNECTION_SUCCESS:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_M2M_CONNECTION_SUCCESS);
                break;
            case GIZ_SDK_SET_SOCKET_NON_BLOCK_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_SET_SOCKET_NON_BLOCK_FAILED);
                break;
            case GIZ_SDK_CONNECTION_TIMEOUT:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_CONNECTION_TIMEOUT);
                break;
            case GIZ_SDK_CONNECTION_REFUSED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_CONNECTION_REFUSED);
                break;
            case GIZ_SDK_CONNECTION_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_CONNECTION_ERROR);
                break;
            case GIZ_SDK_CONNECTION_CLOSED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_CONNECTION_CLOSED);
                break;
            case GIZ_SDK_SSL_HANDSHAKE_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_SSL_HANDSHAKE_FAILED);
                break;
            case GIZ_SDK_DEVICE_LOGIN_VERIFY_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_LOGIN_VERIFY_FAILED);
                break;
            case GIZ_SDK_INTERNET_NOT_REACHABLE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_INTERNET_NOT_REACHABLE);
                break;
            case GIZ_SDK_HTTP_ANSWER_FORMAT_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_HTTP_ANSWER_FORMAT_ERROR);
                break;
            case GIZ_SDK_HTTP_ANSWER_PARAM_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_HTTP_ANSWER_PARAM_ERROR);
                break;
            case GIZ_SDK_HTTP_SERVER_NO_ANSWER:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_HTTP_SERVER_NO_ANSWER);
                break;
            case GIZ_SDK_HTTP_REQUEST_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_HTTP_REQUEST_FAILED);
                break;
            case GIZ_SDK_OTHERWISE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_OTHERWISE);
                break;
            case GIZ_SDK_MEMORY_MALLOC_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_MEMORY_MALLOC_FAILED);
                break;
            case GIZ_SDK_THREAD_CREATE_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_THREAD_CREATE_FAILED);
                break;
            case GIZ_SDK_TOKEN_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_TOKEN_INVALID);
                break;
            case GIZ_SDK_GROUP_ID_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_GROUP_ID_INVALID);
                break;
            case GIZ_SDK_GROUP_PRODUCTKEY_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_GROUP_PRODUCTKEY_INVALID);
                break;
            case GIZ_SDK_GROUP_GET_DEVICE_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_GROUP_GET_DEVICE_FAILED);
                break;
            case GIZ_SDK_DATAPOINT_NOT_DOWNLOAD:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DATAPOINT_NOT_DOWNLOAD);
                break;
            case GIZ_SDK_DATAPOINT_SERVICE_UNAVAILABLE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DATAPOINT_SERVICE_UNAVAILABLE);
                break;
            case GIZ_SDK_DATAPOINT_PARSE_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DATAPOINT_PARSE_FAILED);
                break;
            // case GIZ_SDK_NOT_INITIALIZED:
            // errorString= (String)ctx.getResources().getText(R.string.GIZ_SDK_SDK_NOT_INITIALIZED);
            // break;
            case GIZ_SDK_APK_CONTEXT_IS_NULL:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_APK_CONTEXT_IS_NULL);
                break;
            case GIZ_SDK_APK_PERMISSION_NOT_SET:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_APK_PERMISSION_NOT_SET);
                break;
            case GIZ_SDK_CHMOD_DAEMON_REFUSED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_CHMOD_DAEMON_REFUSED);
                break;
            case GIZ_SDK_EXEC_DAEMON_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_EXEC_DAEMON_FAILED);
                break;
            case GIZ_SDK_EXEC_CATCH_EXCEPTION:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_EXEC_CATCH_EXCEPTION);
                break;
            case GIZ_SDK_APPID_IS_EMPTY:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_APPID_IS_EMPTY);
                break;
            case GIZ_SDK_UNSUPPORTED_API:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_UNSUPPORTED_API);
                break;
            case GIZ_SDK_REQUEST_TIMEOUT:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_REQUEST_TIMEOUT);
                break;
            case GIZ_SDK_DAEMON_VERSION_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DAEMON_VERSION_INVALID);
                break;
            case GIZ_SDK_PHONE_NOT_CONNECT_TO_SOFTAP_SSID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_PHONE_NOT_CONNECT_TO_SOFTAP_SSID);
                break;
            case GIZ_SDK_DEVICE_CONFIG_SSID_NOT_MATCHED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_DEVICE_CONFIG_SSID_NOT_MATCHED);
                break;
            case GIZ_SDK_NOT_IN_SOFTAPMODE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_NOT_IN_SOFTAPMODE);
                break;
            // case GIZ_SDK_PHONE_WIFI_IS_UNAVAILABLE:
            // errorString= (String)
            //ctx.getResources().getText(R.string.GIZ_SDK_PHONE_WIFI_IS_UNAVAILABLE);
            // break;
            case GIZ_SDK_RAW_DATA_TRANSMIT:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_RAW_DATA_TRANSMIT);
                break;
            case GIZ_SDK_PRODUCT_IS_DOWNLOADING:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_PRODUCT_IS_DOWNLOADING);
                break;
            case GIZ_SDK_START_SUCCESS:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SDK_START_SUCCESS);
                break;
            case GIZ_SITE_PRODUCTKEY_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SITE_PRODUCTKEY_INVALID);
                break;
            case GIZ_SITE_DATAPOINTS_NOT_DEFINED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SITE_DATAPOINTS_NOT_DEFINED);
                break;
            case GIZ_SITE_DATAPOINTS_NOT_MALFORME:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_SITE_DATAPOINTS_NOT_MALFORME);
                break;
            case GIZ_OPENAPI_MAC_ALREADY_REGISTERED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_MAC_ALREADY_REGISTERED);
                break;
            case GIZ_OPENAPI_PRODUCT_KEY_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_PRODUCT_KEY_INVALID);
                break;
            case GIZ_OPENAPI_APPID_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_APPID_INVALID);
                break;
            case GIZ_OPENAPI_TOKEN_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_TOKEN_INVALID);
                break;
            case GIZ_OPENAPI_USER_NOT_EXIST:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_USER_NOT_EXIST);
                break;
            case GIZ_OPENAPI_TOKEN_EXPIRED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_TOKEN_EXPIRED);
                break;
            case GIZ_OPENAPI_M2M_ID_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_M2M_ID_INVALID);
                break;
            case GIZ_OPENAPI_SERVER_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_SERVER_ERROR);
                break;
            case GIZ_OPENAPI_CODE_EXPIRED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_CODE_EXPIRED);
                break;
            case GIZ_OPENAPI_CODE_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_CODE_INVALID);
                break;
            case GIZ_OPENAPI_SANDBOX_SCALE_QUOTA_EXHAUSTED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_SANDBOX_SCALE_QUOTA_EXHAUSTED);
                break;
            case GIZ_OPENAPI_PRODUCTION_SCALE_QUOTA_EXHAUSTED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_PRODUCTION_SCALE_QUOTA_EXHAUSTED);
                break;
            case GIZ_OPENAPI_PRODUCT_HAS_NO_REQUEST_SCALE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_PRODUCT_HAS_NO_REQUEST_SCALE);
                break;
            case GIZ_OPENAPI_DEVICE_NOT_FOUND:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_DEVICE_NOT_FOUND);
                break;
            case GIZ_OPENAPI_FORM_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_FORM_INVALID);
                break;
            case GIZ_OPENAPI_DID_PASSCODE_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_DID_PASSCODE_INVALID);
                break;
            case GIZ_OPENAPI_DEVICE_NOT_BOUND:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_DEVICE_NOT_BOUND);
                break;
            case GIZ_OPENAPI_PHONE_UNAVALIABLE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_PHONE_UNAVALIABLE);
                break;
            case GIZ_OPENAPI_USERNAME_UNAVALIABLE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_USERNAME_UNAVALIABLE);
                break;
            case GIZ_OPENAPI_USERNAME_PASSWORD_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_USERNAME_PASSWORD_ERROR);
                break;
            case GIZ_OPENAPI_SEND_COMMAND_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_SEND_COMMAND_FAILED);
                break;
            case GIZ_OPENAPI_EMAIL_UNAVALIABLE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_EMAIL_UNAVALIABLE);
                break;
            case GIZ_OPENAPI_DEVICE_DISABLED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_DEVICE_DISABLED);
                break;
            case GIZ_OPENAPI_FAILED_NOTIFY_M2M:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_FAILED_NOTIFY_M2M);
                break;
            case GIZ_OPENAPI_ATTR_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_ATTR_INVALID);
                break;
            case GIZ_OPENAPI_USER_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_USER_INVALID);
                break;
            case GIZ_OPENAPI_FIRMWARE_NOT_FOUND:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_FIRMWARE_NOT_FOUND);
                break;
            case GIZ_OPENAPI_JD_PRODUCT_NOT_FOUND:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_JD_PRODUCT_NOT_FOUND);
                break;
            case GIZ_OPENAPI_DATAPOINT_DATA_NOT_FOUND:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_DATAPOINT_DATA_NOT_FOUND);
                break;
            case GIZ_OPENAPI_SCHEDULER_NOT_FOUND:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_SCHEDULER_NOT_FOUND);
                break;
            case GIZ_OPENAPI_QQ_OAUTH_KEY_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_QQ_OAUTH_KEY_INVALID);
                break;
            case GIZ_OPENAPI_OTA_SERVICE_OK_BUT_IN_IDLE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_OTA_SERVICE_OK_BUT_IN_IDLE);
                break;
            case GIZ_OPENAPI_BT_FIRMWARE_UNVERIFIED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_BT_FIRMWARE_UNVERIFIED);
                break;
            case GIZ_OPENAPI_BT_FIRMWARE_NOTHING_TO_UPGRADE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_SAVE_KAIROSDB_ERROR);
                break;
            case GIZ_OPENAPI_SAVE_KAIROSDB_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_SAVE_KAIROSDB_ERROR);
                break;
            case GIZ_OPENAPI_EVENT_NOT_DEFINED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_EVENT_NOT_DEFINED);
                break;
            case GIZ_OPENAPI_SEND_SMS_FAILED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_SEND_SMS_FAILED);
                break;
            // case GIZ_OPENAPI_APPLICATION_AUTH_INVALID:
            // errorString= (String)
            //ctx.getResources().getText(R.string.GIZ_OPENAPI_APPLICATION_AUTH_INVALID);
            // break;
            case GIZ_OPENAPI_NOT_ALLOWED_CALL_API:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_NOT_ALLOWED_CALL_API);
                break;
            case GIZ_OPENAPI_BAD_QRCODE_CONTENT:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_BAD_QRCODE_CONTENT);
                break;
            case GIZ_OPENAPI_REQUEST_THROTTLED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_REQUEST_THROTTLED);
                break;
            case GIZ_OPENAPI_DEVICE_OFFLINE:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_DEVICE_OFFLINE);
                break;
            case GIZ_OPENAPI_TIMESTAMP_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_TIMESTAMP_INVALID);
                break;
            case GIZ_OPENAPI_SIGNATURE_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_SIGNATURE_INVALID);
                break;
            case GIZ_OPENAPI_DEPRECATED_API:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_DEPRECATED_API);
                break;
            case GIZ_OPENAPI_RESERVED:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_OPENAPI_RESERVED);
                break;
            case GIZ_PUSHAPI_BODY_JSON_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_BODY_JSON_INVALID);
                break;
            case GIZ_PUSHAPI_DATA_NOT_EXIST:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_DATA_NOT_EXIST);
                break;
            case GIZ_PUSHAPI_NO_CLIENT_CONFIG:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_NO_CLIENT_CONFIG);
                break;
            case GIZ_PUSHAPI_NO_SERVER_DATA:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_NO_SERVER_DATA);
                break;
            case GIZ_PUSHAPI_GIZWITS_APPID_EXIST:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_GIZWITS_APPID_EXIST);
                break;
            case GIZ_PUSHAPI_PARAM_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_PARAM_ERROR);
                break;
            case GIZ_PUSHAPI_AUTH_KEY_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_AUTH_KEY_INVALID);
                break;
            case GIZ_PUSHAPI_APPID_OR_TOKEN_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_APPID_OR_TOKEN_ERROR);
                break;
            case GIZ_PUSHAPI_TYPE_PARAM_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_TYPE_PARAM_ERROR);
                break;
            case GIZ_PUSHAPI_ID_PARAM_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_ID_PARAM_ERROR);
                break;
            case GIZ_PUSHAPI_APPKEY_SECRETKEY_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_APPKEY_SECRETKEY_INVALID);
                break;
            case GIZ_PUSHAPI_CHANNELID_ERROR_INVALID:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_CHANNELID_ERROR_INVALID);
                break;
            case GIZ_PUSHAPI_PUSH_ERROR:
                errorString = (String) ctx.getResources().getText(R.string.GIZ_PUSHAPI_PUSH_ERROR);
                break;
            default:
                errorString = (String) ctx.getResources().getText(R.string.UNKNOWN_ERROR);
                break;
        }
        return errorString;
    }
}
