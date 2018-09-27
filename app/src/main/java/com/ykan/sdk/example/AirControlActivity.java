package com.ykan.sdk.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.yaokan.sdk.api.JsonParser;
import com.yaokan.sdk.model.AirConCatogery;
import com.yaokan.sdk.model.AirEvent;
import com.yaokan.sdk.model.AirStatus;
import com.yaokan.sdk.model.AirV1Command;
import com.yaokan.sdk.model.AirV3Command;
import com.yaokan.sdk.model.KeyCode;
import com.yaokan.sdk.model.RemoteControl;
import com.yaokan.sdk.model.SendType;
import com.yaokan.sdk.model.kyenum.Mode;
import com.yaokan.sdk.model.kyenum.Speed;
import com.yaokan.sdk.model.kyenum.Temp;
import com.yaokan.sdk.model.kyenum.WindH;
import com.yaokan.sdk.model.kyenum.WindV;
import com.yaokan.sdk.utils.Logger;
import com.yaokan.sdk.utils.Utility;
import com.yaokan.sdk.wifi.DeviceController;
import com.yaokan.sdk.wifi.listener.IDeviceControllerListener;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class AirControlActivity extends BaseActivity implements IDeviceControllerListener, View.OnClickListener {
    protected static final String TAG = AirControlActivity.class.getSimpleName();

    private TextView tvMAC, tv_show;

    private GizWifiDevice device;

    private HashMap<String, KeyCode> codeDatas = new HashMap<String, KeyCode>();

    private DeviceController driverControl = null;

    private JsonParser jsonParser = new JsonParser();

    private Button mode_btn, wspeed_btn, tbspeed_btn, lrwspped_btn, power_btn, temp_add_btn, temp_rdc_btn;

    private static final int V3 = 3;

    private int airVerSion = V3;

    private AirEvent airEvent = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.air_control);
        initDevice();
        initView();
        setOnClickListener();
    }

    private RemoteControl remoteControl;

    private void initDevice() {
        Intent intent = getIntent();
        device = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
        driverControl = new DeviceController(getApplicationContext(), device, this);
        //获取设备硬件相关信息
        driverControl.getDevice().getHardwareInfo();
        //修改设备显示名称
        driverControl.getDevice().setCustomInfo("alias", "遥控中心产品");
        remoteControl = jsonParser.parseObjecta(intent.getStringExtra("remoteControl"), RemoteControl.class);
        if (!Utility.isEmpty(remoteControl)) {
            airVerSion = remoteControl.getVersion();
            codeDatas = remoteControl.getRcCommand();
            airEvent = getAirEvent(codeDatas);
        }

    }

    private void initView() {
        tvMAC = (TextView) findViewById(R.id.tvMAC);
        tv_show = (TextView) findViewById(R.id.tv_show);
        mode_btn = (Button) findViewById(R.id.mode_btn);
        wspeed_btn = (Button) findViewById(R.id.wspeed_btn);
        tbspeed_btn = (Button) findViewById(R.id.tbspeed_btn);
        lrwspped_btn = (Button) findViewById(R.id.lrwspped_btn);
        power_btn = (Button) findViewById(R.id.power_btn);
        temp_add_btn = (Button) findViewById(R.id.temp_add_btn);
        temp_rdc_btn = (Button) findViewById(R.id.temp_rdc_btn);
        //v1没有左右扫风，上下扫风，风量
        if (airVerSion != V3) {
            wspeed_btn.setVisibility(View.GONE);
            tbspeed_btn.setVisibility(View.GONE);
            lrwspped_btn.setVisibility(View.GONE);
        }
        if (null != device) {
            tvMAC.setText("MAC: " + device.getMacAddress().toString());
        }

        findViewById(R.id.get_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //注意！！！！注意！！！！注意！！！！注意！！！！
                //此方法是从SDK抽离出来的，不参与onRefreshUI（）的UI逻辑，需要用户自己实现。
                KeyCode keyCode = airEvent.getAirCode(Mode.WIND, Speed.S0, WindV.OFF, WindH.OFF, Temp.T24);
                KeyCode keyCode2 =  airEvent.getAirCode(Mode.WIND, Speed.S1, WindV.OFF, WindH.OFF, Temp.T24);
                KeyCode keyCode3 =  airEvent.getAirCode(Mode.WIND, Speed.S2, WindV.OFF, WindH.OFF, Temp.T24);
                if (keyCode != null) {
                    driverControl.sendCMD(keyCode.getSrcCode(), SendType.Infrared);
                }
            }
        });
    }

    private void setOnClickListener() {
        mode_btn.setOnClickListener(this);
        wspeed_btn.setOnClickListener(this);
        tbspeed_btn.setOnClickListener(this);
        lrwspped_btn.setOnClickListener(this);
        power_btn.setOnClickListener(this);
        temp_add_btn.setOnClickListener(this);
        temp_rdc_btn.setOnClickListener(this);
    }

    private AirEvent getAirEvent(HashMap<String, KeyCode> codeDatas) {
        AirEvent airEvent = null;
        if (!Utility.isEmpty(codeDatas)) {
            if (airVerSion == V3) {
                airEvent = new AirV3Command(codeDatas);
            } else {
                airEvent = new AirV1Command(codeDatas);
            }
        }
        return airEvent;
    }

    @Override
    public void onClick(View v) {
        if (Utility.isEmpty(airEvent)) {
            toast("airEvent is null");
            return;
        }
        //空调必须先打开，才能操作其他按键
        if (airEvent.isOff() && !(v.getId() == R.id.power_btn)) {
            toast("请先打开空调");
            return;
        }
        KeyCode mKeyCode = null;
        switch (v.getId()) {
            case R.id.power_btn:
                mKeyCode = airEvent.getNextValueByCatogery(AirConCatogery.Power);
                break;
            case R.id.mode_btn:
                mKeyCode = airEvent.getNextValueByCatogery(AirConCatogery.Mode);
                break;
            case R.id.wspeed_btn:
                mKeyCode = airEvent.getNextValueByCatogery(AirConCatogery.Speed);
                break;
            case R.id.tbspeed_btn:
                mKeyCode = airEvent.getNextValueByCatogery(AirConCatogery.WindUp);
                break;
            case R.id.lrwspped_btn:
                mKeyCode = airEvent.getNextValueByCatogery(AirConCatogery.WindLeft);
                break;
            case R.id.temp_add_btn:
                mKeyCode = airEvent.getNextValueByCatogery(AirConCatogery.Temp);
                break;
            case R.id.temp_rdc_btn:
                mKeyCode = airEvent.getForwardValueByCatogery(AirConCatogery.Temp);
                break;
            default:
                break;
        }
        if (!Utility.isEmpty(mKeyCode)) {
            driverControl.sendCMD(mKeyCode.getSrcCode(), SendType.Infrared);
            onRefreshUI(airEvent.getCurrStatus());
        }
    }

    private void onRefreshUI(AirStatus airStatus) {
        if (Utility.isEmpty(airStatus)) {
            return;
        }
        String content = "";
        if (airEvent.isOff()) {
            content = "空调已关闭";
        } else {
            content = getContent(airStatus);
        }
        tv_show.setText(content);
    }

    //
    private String getContent(AirStatus airStatus) {
        String content = "";
        if (airVerSion == 1) {
            content = "模式：" + airStatus.getMode().getChName()
                    + "\n温度：" + airStatus.getTemp().getChName();
        } else {
            content = "模式：" + airStatus.getMode().getChName()
                    + "\n风量：" + airStatus.getSpeed().getChName()
                    + "\n左右扫风：" + airStatus.getWindLeft().getChName()
                    + "\n上下扫风：" + airStatus.getWindUp().getChName()
                    + "\n温度：" + airStatus.getTemp().getChName();
        }
        return content;

    }

    @Override
    public void didGetHardwareInfo(GizWifiErrorCode result, GizWifiDevice device, ConcurrentHashMap<String, String> hardwareInfo) {
        Logger.d(TAG, "获取设备信息 :");
        if (GizWifiErrorCode.GIZ_SDK_SUCCESS == result) {
            Logger.d(TAG, "获取设备信息 : hardwareInfo :" + hardwareInfo);
        } else {

        }
    }

    @Override
    public void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device) {
        Logger.d(TAG, "自定义设备信息回调");
        if (GizWifiErrorCode.GIZ_SDK_SUCCESS == result) {
            Logger.d(TAG, "自定义设备信息成功");
        }
    }

    @Override
    public void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus) {
        switch (device.getNetStatus()) {
            case GizDeviceOffline:
                Logger.d(TAG, "设备下线");
                break;
            case GizDeviceOnline:
                Logger.d(TAG, "设备上线");
                break;
            default:
                break;
        }
    }
}
