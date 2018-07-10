package com.ykan.sdk.example;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.larksmart7618.sdk.communication.tools.commen.ToastTools;
import com.yaokan.sdk.api.YkanSDKManager;
import com.yaokan.sdk.ir.InitYkanListener;
import com.yaokan.sdk.model.YKUserAccountType;
import com.yaokan.sdk.utils.Logger;
import com.yaokan.sdk.utils.ProgressDialogUtils;
import com.yaokan.sdk.wifi.DeviceManager;
import com.yaokan.sdk.wifi.GizWifiCallBack;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends BaseActivity implements InitYkanListener {
    protected String TAG = MainActivity.class.getSimpleName();
    private EditText etName, etPsw, etMac;
    private ProgressDialogUtils dialogUtils;
    private boolean isInitSuccess = false;
    private Button btnGetCode;
    Timer timer;
    /**
     * 验证码重发倒计时
     */
    int secondleft = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        dialogUtils = new ProgressDialogUtils(this);
        // 初始化SDK
        YkanSDKManager.init(this, this);
        //需要剥离机智云的用户调用此方法初始化
//        YkanSDKManager.custInit(this,false);
        // 设置Log信息是否打印
        YkanSDKManager.getInstance().setLogger(true);

//        DeviceManager.instanceDeviceManager(this).setGizWifiCallBack(null);
//        GizWifiSDK.sharedInstance().setListener(null);
    }

    @Override
    public void onInitStart() {
        dialogUtils.setMessage("SDK初始化中");
        dialogUtils.setCancelable(false);
        dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
    }

    @Override
    public void onInitFinish(final int status, final String errorMsg) {
        dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
        if (status == INIT_SUCCESS) {
            isInitSuccess = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastTools.short_Toast(MainActivity.this, "SDK初始化成功");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            List<GizWifiDevice> gizWifiDevices = DeviceManager.instanceDeviceManager(getApplicationContext()).getCanUseGizWifiDevice();
                            if (gizWifiDevices != null) {
                                Log.e("MainActivity", gizWifiDevices.size() + "");
                            }
                        }
                    }, 2000);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastTools.short_Toast(MainActivity.this, errorMsg);
                    new AlertDialog.Builder(MainActivity.this).setTitle("error").setMessage(errorMsg).setPositiveButton("ok", null).create().show();
                }
            });
        }
    }

    private void initListener() {
        DeviceManager.instanceDeviceManager(getApplicationContext()).setGizWifiCallBack(new GizWifiCallBack() {

            @Override
            public void didBindDeviceCd(GizWifiErrorCode result, String did) {
                super.didBindDeviceCd(result, did);
            }

            @Override
            public void didTransAnonymousUser(GizWifiErrorCode result) {
                super.didTransAnonymousUser(result);
            }

            /** 用于用户登录的回调 */
            @Override
            public void userLoginCb(GizWifiErrorCode result, String uid, String token) {
                Logger.d(TAG, "didUserLogin result:" + result + " uid:" + uid + " token:" + token);
                dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
                if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {// 登陆成功
                    Constant.UID = uid;
                    Constant.TOKEN = token;
                    toDeviceList();
                } else if (result == GizWifiErrorCode.GIZ_OPENAPI_USER_NOT_EXIST) {// 用户不存在
                    toast(R.string.GIZ_OPENAPI_USER_NOT_EXIST);
                } else if (result == GizWifiErrorCode.GIZ_OPENAPI_USERNAME_PASSWORD_ERROR) {//// 用户名或者密码错误
                    toast(R.string.GIZ_OPENAPI_USERNAME_PASSWORD_ERROR);
                } else {
                    toast("登陆失败，请重新登录");
                }
            }

            /** 用于用户注册的回调 */
            @Override
            public void registerUserCb(GizWifiErrorCode result, String uid, String token) {
                Logger.d(TAG, "registerUserCb result:" + result + " uid:" + uid + " token:" + token);
                dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
                /** 用于用户注册 */
                if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                    Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_LONG).show();
                    toDeviceList();
                    finish();
                } else if (result == GizWifiErrorCode.GIZ_OPENAPI_USERNAME_UNAVALIABLE) {
                    Toast.makeText(getApplicationContext(), "userName  unavaliabale", Toast.LENGTH_LONG).show();
                } else if (result == GizWifiErrorCode.GIZ_OPENAPI_CODE_INVALID) {
                    Toast.makeText(getApplicationContext(), "验证码不正确", Toast.LENGTH_LONG).show();
                } else if (result == GizWifiErrorCode.GIZ_OPENAPI_EMAIL_UNAVALIABLE) {
                    Toast.makeText(getApplicationContext(), "该邮箱已️注册或该邮箱无效", Toast.LENGTH_LONG).show();
                } else if (result == GizWifiErrorCode.GIZ_OPENAPI_PHONE_UNAVALIABLE) {
                    Toast.makeText(getApplicationContext(), "该手机已️注册或该手机号码无效", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "注册失败，请重新注册", Toast.LENGTH_LONG).show();
                }

            }

            /** 用于发送验证码的回调 */
            @Override
            public void didRequestSendPhoneSMSCodeCb(GizWifiErrorCode result) {
                if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                    handler.sendEmptyMessage(SEND_SUCCESSFUL);
                    // 请求成功
                } else {
                    // 请求失败
                }
            }

            /** 用于重置密码的回调 */
            @Override
            public void didChangeUserPasswordCd(GizWifiErrorCode result) {
                dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
                if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                    toast("重置密码成功！");
                    // 请求成功
                } else {
                    // 请求失败
                }
            }
        });
    }

    private void toDeviceList() {
        toast("登陆成功");
        startActivity(new Intent(this, DeviceListActivity.class));
        finish();
    }

    private void initView() {
        etName = (EditText) findViewById(R.id.et_name);
        etPsw = (EditText) findViewById(R.id.et_psw);
        etMac = (EditText) findViewById(R.id.et_mac);
        btnGetCode = (Button) findViewById(R.id.btn_mac);
    }

    public void onClick(View view) {
        if (!isInitSuccess) {
            toast("请先完成初始化操作");
            return;
        }
        switch (view.getId()) {
            case R.id.btn_mac://获取验证码
                if (!TextUtils.isEmpty(etName.getText().toString())) {
                    DeviceManager.instanceDeviceManager(getApplicationContext()).sendPhoneSmsCode(getApplicationContext(), etName.getText().toString());
                    handler.sendEmptyMessage(GET_CODE);
                } else {
                    toast("请输入完整信息");
                }
                break;
            case R.id.btn_login://登录
                if (!TextUtils.isEmpty(etName.getText().toString()) && !TextUtils.isEmpty(etPsw.getText().toString())) {
                    dialogUtils.setMessage("login...");
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    DeviceManager.instanceDeviceManager(getApplicationContext()).userLogin(etName.getText().toString(), etPsw.getText().toString(), YKUserAccountType.YKUserNormal);
                } else {
                    toast("请输入完整信息");
                }
                break;
            case R.id.btn_login_phone://登录
                if (!TextUtils.isEmpty(etName.getText().toString()) && !TextUtils.isEmpty(etPsw.getText().toString())) {
                    dialogUtils.setMessage("login...");
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    DeviceManager.instanceDeviceManager(getApplicationContext()).userLogin(etName.getText().toString(), etPsw.getText().toString(), YKUserAccountType.YKUserPhone);
                } else {
                    toast("请输入完整信息");
                }
                break;
            case R.id.btn_register_normal://普通注册
                if (!TextUtils.isEmpty(etName.getText().toString()) && !TextUtils.isEmpty(etPsw.getText().toString())) {
                    dialogUtils.setMessage("register...");
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    DeviceManager.instanceDeviceManager(getApplicationContext()).register(etName.getText().toString(), etPsw.getText().toString(), null, YKUserAccountType.YKUserNormal);
                } else {
                    toast("请输入完整信息");
                }
                break;
            case R.id.btn_register_email://邮箱注册
                if (!TextUtils.isEmpty(etName.getText().toString()) && !TextUtils.isEmpty(etPsw.getText().toString())) {
                    dialogUtils.setMessage("register...");
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    DeviceManager.instanceDeviceManager(getApplicationContext()).register(etName.getText().toString(), etPsw.getText().toString(), null, YKUserAccountType.YKUserEmail);
                } else {
                    toast("请输入完整信息");
                }
                break;
            case R.id.btn_register_phone://手机注册
                if (!TextUtils.isEmpty(etName.getText().toString()) && !TextUtils.isEmpty(etPsw.getText().toString()) && !TextUtils.isEmpty(etMac.getText().toString())) {
                    dialogUtils.setMessage("register...");
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    DeviceManager.instanceDeviceManager(getApplicationContext()).register(etName.getText().toString(), etPsw.getText().toString(), etMac.getText().toString(), YKUserAccountType.YKUserPhone);
                } else {
                    toast("请输入完整信息");
                }
                break;
            case R.id.btn_reset_psw_phone:
                if (!TextUtils.isEmpty(etName.getText().toString()) && !TextUtils.isEmpty(etPsw.getText().toString()) && !TextUtils.isEmpty(etMac.getText().toString())) {
                    dialogUtils.setMessage("register...");
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    DeviceManager.instanceDeviceManager(getApplicationContext()).resetPassword(etName.getText().toString(), etPsw.getText().toString(), etMac.getText().toString(), YKUserAccountType.YKUserPhone);
                } else {
                    toast("请输入完整信息");
                }
                break;
            case R.id.btn_reset_psw_email:
                if (!TextUtils.isEmpty(etName.getText().toString()) && !TextUtils.isEmpty(etPsw.getText().toString())) {
                    dialogUtils.setMessage("register...");
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    DeviceManager.instanceDeviceManager(getApplicationContext()).resetPassword(etName.getText().toString(), etPsw.getText().toString(), null, YKUserAccountType.YKUserEmail);
                } else {
                    toast("请输入完整信息");
                }
                break;
            case R.id.btn_anonymous_login:
                dialogUtils.setMessage("login...");
                dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                DeviceManager.instanceDeviceManager(getApplicationContext()).userLoginAnonymous();
                break;
            default:
                break;
        }
    }

    private final static int GET_CODE = 0;
    private final static int SEND_SUCCESSFUL = 1;
    private final static int TICK_TIME = 2;
    Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case GET_CODE:
                    dialogUtils.setMessage("获取验证码");
                    dialogUtils.setCancelable(false);
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    break;
                case SEND_SUCCESSFUL:
                    isStartTimer();
                    break;
                case TICK_TIME:
                    String getCodeAgain = "重新获取验证码";
                    String timerMessage = "秒后重新获取";
                    secondleft--;
                    if (secondleft <= 0) {
                        timer.cancel();
                        btnGetCode.setEnabled(true);
                        btnGetCode.setText(getCodeAgain);
                    } else {
                        btnGetCode.setText(secondleft + timerMessage);
                    }
                    break;
            }
        }
    };

    /**
     * 倒计时
     */
    public void isStartTimer() {
        dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
        btnGetCode.setEnabled(false);
        secondleft = 60;
        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                handler.sendEmptyMessage(TICK_TIME);
            }
        }, 1000, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        System.exit(0);
    }
}
