package com.ykan.sdk.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.gizwits.gizwifisdk.api.GizDeviceScheduler;
import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizScheduleWeekday;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.google.gson.reflect.TypeToken;
import com.yaokan.sdk.api.JsonParser;
import com.yaokan.sdk.model.KeyCode;
import com.yaokan.sdk.model.RemoteControl;
import com.yaokan.sdk.model.SendType;
import com.yaokan.sdk.utils.Logger;
import com.yaokan.sdk.utils.Utility;
import com.yaokan.sdk.wifi.DeviceController;
import com.yaokan.sdk.wifi.YKDeviceSchedulerCenterListener;
import com.yaokan.sdk.wifi.YKSchedulerCenter;
import com.yaokan.sdk.wifi.listener.IDeviceControllerListener;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class YKWifiDeviceControlActivity extends Activity implements IDeviceControllerListener {

    protected static final String TAG = YKWifiDeviceControlActivity.class.getSimpleName();

    /**
     * The tv MAC
     */
    private TextView tvMAC;

    /**
     * The GizWifiDevice device
     */
    private GizWifiDevice device;

    private String rcCommand = "";

    private GridView gridView;

    private HashMap<String, KeyCode> codeDatas = new HashMap<String, KeyCode>();

    private List<String> codeKeys = new ArrayList<String>();

    private DeviceController driverControl = null;

    RemoteControl control;

    JsonParser jsonParser = new JsonParser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_control);
        initDevice();
        initView();
        YKSchedulerCenter.setListener(ykDeviceSchedulerCenterListener);
    }

    private void initView() {
        driverControl = new DeviceController(getApplicationContext(), device, this);
        //获取设备硬件相关信息
        driverControl.getDevice().getHardwareInfo();
        //修改设备显示名称
        driverControl.getDevice().setCustomInfo("alias", "遥控中心产品");
        tvMAC = (TextView) findViewById(R.id.tvMAC);
        gridView = (GridView) findViewById(R.id.codeGridView);
        if (null != device) {
            tvMAC.setText("MAC: " + device.getMacAddress().toString());
        }
        findViewById(R.id.night).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (driverControl != null) {
                    driverControl.sendNightLight();
                }
            }
        });
        if (!Utility.isEmpty(rcCommand)) {
            codeDatas = new HashMap<String, KeyCode>();
            Type type = new TypeToken<HashMap<String, KeyCode>>() {
            }.getType();
            //解析数据
            codeDatas = jsonParser.parseObjecta(rcCommand, type);
            codeKeys = new ArrayList<String>(codeDatas.keySet());
            ExpandAdapter expandAdapter = new ExpandAdapter(getApplicationContext(), codeKeys);
            gridView.setAdapter(expandAdapter);

            //点击按钮 如果按钮上有code 就会把code发送到遥控中心
            gridView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    String key = codeKeys.get(position);
                    KeyCode keyCode = codeDatas.get(key);
                    String code;
                    code = keyCode.getSrcCode();//从Api中取到的code
                    if (!TextUtils.isEmpty(code))
                        driverControl.sendCMD(code, SendType.Infrared);
                }
            });

            //如果是学习模式 长按按钮 按键会闪烁并且进入学习状态
            gridView.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent,
                                               View view, int position, long id) {
                    String key = codeKeys.get(position);
                    KeyCode keyCode = codeDatas.get(key);
                    createScheduler(keyCode.getSrcCode());
                    return true;
                }

            });
        }
    }

    private void createScheduler(final String code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(YKWifiDeviceControlActivity.this);
        builder.setTitle("创建定时任务").setItems(new CharSequence[]{"一次性定时任务", "周重复的定时任务", "月重复的定时任务"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GizDeviceScheduler scheduler = null;
                // 创建设备的定时任务，mDevice为在设备列表中得到的设备对象
                switch (which) {
                    case 0:
                        scheduler = new GizDeviceScheduler(null, "2018-10-16", "09:54", true, "任务名称");
                        break;
                    case 1:
                        //我们现在让定时任务按周重复执行，现在要每周的周一至周五早上6点30分都执行任务。
                        List<GizScheduleWeekday> weekDays = new ArrayList<GizScheduleWeekday>();
                        weekDays.add(GizScheduleWeekday.GizScheduleMonday);
                        weekDays.add(GizScheduleWeekday.GizScheduleTuesday);
                        weekDays.add(GizScheduleWeekday.GizScheduleWednesday);
                        weekDays.add(GizScheduleWeekday.GizScheduleThursday);
                        weekDays.add(GizScheduleWeekday.GizScheduleFriday);
                        scheduler = new GizDeviceScheduler(null, "09:54", weekDays, true, "任务名称");
                        break;
                    case 2:
                        //我们现在让定时任务按周重复执行，现在要每个月的1号、15号早上6点30分都执行任务。
                        //注意不要同时设置按周重复，如果同时设置了按周重复，按月重复会被忽略。
                        List<Integer> monthDays = new ArrayList<>();
                        monthDays.add(1);
                        monthDays.add(15);
                        scheduler = new GizDeviceScheduler("09:54", null, monthDays, true, "任务名称");
                        break;
                }
                YKSchedulerCenter.createScheduler(Constant.UID, Constant.TOKEN, device, scheduler, null, code);
            }
        }).create().show();
    }

    YKDeviceSchedulerCenterListener ykDeviceSchedulerCenterListener = new YKDeviceSchedulerCenterListener() {
        @Override
        public void didUpdateSchedulers(GizWifiErrorCode result, GizWifiDevice schedulerOwner, List<GizDeviceScheduler> schedulerList) {
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                // 定时任务创建成功
                Logger.e(TAG, "定时任务创建成功");
            } else {
                // 创建失败
                Logger.e(TAG, "创建失败" + result.name() + " --" + result.getResult());
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        driverControl.learnStop();
        driverControl.learnStop433or315();
    }

    /**
     * 接收intent传过来的数据
     */
    private void initDevice() {
        Intent intent = getIntent();
        device = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
        rcCommand = intent.getStringExtra("rcCommand");
        control = new JsonParser().parseObjecta(intent.getStringExtra("remoteControl"), RemoteControl.class);
    }


    public class ExpandAdapter extends BaseAdapter {

        private Context mContext;

        private LayoutInflater inflater;

        public List<String> keys;

        public ExpandAdapter(Context mContext, List<String> keys) {
            super();
            this.mContext = mContext;
            this.keys = keys;
            inflater = LayoutInflater.from(mContext);

        }

        @Override
        public int getCount() {
            return keys.size();
        }

        @Override
        public Object getItem(int position) {
            return keys.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.yk_ctrl_adapter_expand, null);
            }
            TextView keyBtn = (TextView) convertView.findViewById(R.id.key_btn);
            keyBtn.setText(keys.get(position));
            return convertView;
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
}
