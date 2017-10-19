package com.ykan.sdk.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.yaokan.sdk.api.JsonParser;
import com.yaokan.sdk.api.YkanSDKManager;
import com.yaokan.sdk.ir.YKanHttpListener;
import com.yaokan.sdk.ir.YkanIRInterface;
import com.yaokan.sdk.ir.YkanIRInterfaceImpl;
import com.yaokan.sdk.model.BaseResult;
import com.yaokan.sdk.model.Brand;
import com.yaokan.sdk.model.BrandResult;
import com.yaokan.sdk.model.DeviceType;
import com.yaokan.sdk.model.DeviceTypeResult;
import com.yaokan.sdk.model.KeyCode;
import com.yaokan.sdk.model.MatchRemoteControl;
import com.yaokan.sdk.model.MatchRemoteControlResult;
import com.yaokan.sdk.model.RemoteControl;
import com.yaokan.sdk.model.YKError;
import com.yaokan.sdk.utils.Logger;
import com.yaokan.sdk.utils.ProgressDialogUtils;
import com.yaokan.sdk.utils.Utility;
import com.yaokan.sdk.wifi.DeviceController;
import com.yaokan.sdk.wifi.DeviceManager;

public class YKCodeAPIActivity extends Activity implements View.OnClickListener {

    private ProgressDialogUtils dialogUtils;

    private YkanIRInterface ykanInterface;

    private String TAG = YKCodeAPIActivity.class.getSimpleName();

    private TextView tvDevice;

    private GizWifiDevice currGizWifiDevice;

    private String deviceId;

    private List<DeviceType> deviceType = new ArrayList<DeviceType>();// 设备类型

    private List<Brand> brands = new ArrayList<Brand>(); // 品牌

    private List<MatchRemoteControl> remoteControls = new ArrayList<MatchRemoteControl>();// 遥控器列表

    private List<String> nameType = new ArrayList<String>();
    private List<String> nameBrands = new ArrayList<String>();
    private List<String> nameRemote = new ArrayList<String>();

    private MatchRemoteControlResult controlResult = null;// 匹配列表

    private RemoteControl remoteControl = null; // 遥控器对象

    private MatchRemoteControl currRemoteControl = null; // 当前匹配的遥控器对象

    private DeviceType currDeviceType = null; // 当前设备类型

    private Brand currBrand = null; // 当前品牌

    private Spinner spType, spBrands, spRemotes;

    private ArrayAdapter<String> typeAdapter, brandAdapter, remoteAdapter;
    private DeviceController driverControl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_codeapi);
        // 遥控云数据接口分装对象对象
        ykanInterface = new YkanIRInterfaceImpl(getApplicationContext());
        // 遥控云数据接口分装对象对象
        ykanInterface = new YkanIRInterfaceImpl(getApplicationContext());
        initView();
        initDevice();
        List<GizWifiDevice> gizWifiDevices = DeviceManager
                .instanceDeviceManager(getApplicationContext()).getCanUseGizWifiDevice();
        if (gizWifiDevices != null) {
            Log.e("YKCodeAPIActivity", gizWifiDevices.size() + "");
        }
    }

    private void initDevice() {
        currGizWifiDevice = (GizWifiDevice) getIntent().getParcelableExtra(
                "GizWifiDevice");
        if (currGizWifiDevice != null) {
            deviceId = currGizWifiDevice.getDid();
            tvDevice.setText(currGizWifiDevice.getProductName() + "("
                    + currGizWifiDevice.getMacAddress() + ") ");
            // 在下载数据之前需要设置设备ID，用哪个设备去下载
            YkanSDKManager.getInstance().setDeviceId(deviceId);
            if (currGizWifiDevice.isSubscribed() == false) {
                currGizWifiDevice.setSubscribe(true);
            }
        }
        //小苹果 小夜灯
        driverControl = new DeviceController(getApplicationContext(), currGizWifiDevice, null);
        findViewById(R.id.night).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driverControl.sendNightLight();
            }
        });
    }

    private void initView() {
        dialogUtils = new ProgressDialogUtils(this);
        tvDevice = (TextView) findViewById(R.id.tv_device);
        spType = (Spinner) findViewById(R.id.spType);
        spBrands = (Spinner) findViewById(R.id.spBrand);
        spRemotes = (Spinner) findViewById(R.id.spData);
        typeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, nameType);
        brandAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, nameBrands);
        remoteAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, nameRemote);
        typeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        brandAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        remoteAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);
        spBrands.setAdapter(brandAdapter);
        spRemotes.setAdapter(remoteAdapter);
        spType.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                currDeviceType = deviceType.get(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spBrands.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                currBrand = brands.get(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spRemotes.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                currRemoteControl = remoteControls.get(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void toYKWifiDeviceControlActivity(Intent intent, JsonParser jsonParser) {
        intent.setClass(this, YKWifiDeviceControlActivity.class);
        intent.putExtra("GizWifiDevice", currGizWifiDevice);
        try {
            intent.putExtra("remoteControl", jsonParser.toJson(remoteControl));
            intent.putExtra("rcCommand", jsonParser.toJson(remoteControl.getRcCommand()));
        } catch (JSONException e) {
            Log.e(TAG, "JSONException:" + e.getMessage());
        }
        startActivity(intent);
    }

    private void toAirControlActivity(Intent intent, JsonParser jsonParser) {
        intent.setClass(this, AirControlActivity.class);
        intent.putExtra("GizWifiDevice", currGizWifiDevice);
        try {
            intent.putExtra("remoteControl", jsonParser.toJson(remoteControl));
        } catch (JSONException e) {
            Log.e(TAG, "JSONException:" + e.getMessage());
        }
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.match:
                if (currRemoteControl != null) {
                    if (currRemoteControl.getRcCommand() != null && currRemoteControl.getRcCommand().size() > 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(YKCodeAPIActivity.this);
                        CharSequence c[] = new CharSequence[currRemoteControl.getRcCommand().size()];
                        final String sCode[] = new String[currRemoteControl.getRcCommand().size()];
                        Iterator<Map.Entry<String, KeyCode>> iterator = currRemoteControl.getRcCommand().entrySet().iterator();
                        int i = 0;
                        while (iterator.hasNext()) {
                            Map.Entry<String, KeyCode> entry = iterator.next();
                            c[i] = entry.getKey();
                            sCode[i] = entry.getValue().getSrcCode();
                            i++;
                        }
                        builder.setTitle("测试匹配").setItems(c, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                driverControl.sendCMD(sCode[which]);
                                dialog.dismiss();
                            }
                        }).create().show();
                    }
                }
                break;
            case R.id.scheduler_list:
                Intent intent1 = new Intent(YKCodeAPIActivity.this, SchedulerListActivity.class);
                intent1.putExtra("GizWifiDevice", currGizWifiDevice);
                startActivity(intent1);
                break;
            case R.id.wifitest:
            case R.id.study:
                // 进入遥控控制面板
                Log.d(TAG, "remoteControl:" + remoteControl);
                if (remoteControl == null) {
                    Toast.makeText(getApplicationContext(), "没有下载遥控器数据",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra("type", v.getId() == R.id.study ? true : false);
//                intent.putExtra("tid", currDeviceType.getTid());
//                intent.putExtra("bid", currBrand.getBid());
//                intent.putExtra("rid", currRemoteControl.getRid());
                JsonParser jsonParser = new JsonParser();
                int airTid = 7;// 空调
                if (remoteControl.gettId() == airTid) {
                    if (v.getId() == R.id.study) {
                        Toast.makeText(this, "空调不支持学习", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    toAirControlActivity(intent, jsonParser);
                } else {
                    toYKWifiDeviceControlActivity(intent, jsonParser);
                }

                break;
            default:
                new DownloadThread(v.getId()).start();
                break;
        }
    }

    class DownloadThread extends Thread {
        private int viewId;
        String result = "";

        public DownloadThread(int viewId) {
            this.viewId = viewId;
        }

        @Override
        public void run() {
            dialogUtils.sendMessage(1);

            final Message message = mHandler.obtainMessage();
            switch (viewId) {
                case R.id.getDeviceType:
                    ykanInterface.getDeviceType(currGizWifiDevice.getMacAddress(), new YKanHttpListener() {
                        @Override
                        public void onSuccess(BaseResult baseResult) {
                            DeviceTypeResult deviceResult = (DeviceTypeResult) baseResult;
                            deviceType = deviceResult.getRs();
                            result = deviceResult.toString();
                            message.what = 0;
                            Log.d(TAG, " getDeviceType result:" + result);
                        }

                        @Override
                        public void onFail(YKError ykError) {
                            Log.e(TAG, "ykError:" + ykError.toString());
                        }
                    });
                    break;
                case R.id.getBrandByType:
                    // int type = 7 ;//1:机顶盒，2：电视机
                    if (currDeviceType != null) {
                        ykanInterface
                                .getBrandsByType(currGizWifiDevice.getMacAddress(), currDeviceType.getTid(), new YKanHttpListener() {
                                    @Override
                                    public void onSuccess(BaseResult baseResult) {
                                        BrandResult brandResult = (BrandResult) baseResult;
                                        brands = brandResult.getRs();
                                        result = brandResult.toString();
                                        message.what = 1;
                                        Log.d(TAG, " getBrandByType result:" + brandResult);
                                    }

                                    @Override
                                    public void onFail(YKError ykError) {
                                        Log.e(TAG, "ykError:" + ykError.toString());
                                    }
                                });
                    } else {
                        result = "请调用获取设备接口";
                    }
                    break;
                case R.id.getMatchedDataByBrand:
                    if (currBrand != null) {
                        ykanInterface.getRemoteMatched(currGizWifiDevice.getMacAddress(),
                                currBrand.getBid(), currDeviceType.getTid(), new YKanHttpListener() {
                                    @Override
                                    public void onSuccess(BaseResult baseResult) {
                                        controlResult = (MatchRemoteControlResult) baseResult;
                                        remoteControls = controlResult.getRs();
                                        result = controlResult.toString();
                                        message.what = 2;
                                        Log.d(TAG, " getMatchedDataByBrand result:" + result);
                                    }

                                    @Override
                                    public void onFail(YKError ykError) {
                                        Log.e(TAG, "ykError:" + ykError.toString());
                                    }
                                });
                    } else {
                        result = "请调用获取设备接口";
                    }
                    break;
                case R.id.getDetailByRCID:
                    if (!Utility.isEmpty(currRemoteControl)) {
                        ykanInterface
                                .getRemoteDetails(currGizWifiDevice.getMacAddress(), currRemoteControl.getRid(), new YKanHttpListener() {
                                    @Override
                                    public void onSuccess(BaseResult baseResult) {
                                        if (baseResult != null) {
                                            remoteControl = (RemoteControl) baseResult;
                                            result = remoteControl.toString();
                                        }
                                    }

                                    @Override
                                    public void onFail(YKError ykError) {
                                        Log.e(TAG, "ykError:" + ykError.toString());
                                    }
                                });
                    } else {
                        result = "请调用匹配数据接口";
                        Log.e(TAG, " getDetailByRCID 没有遥控器设备对象列表");
                    }
                    Log.d(TAG, " getDetailByRCID result:" + result);
                    break;
//                case R.id.getFastMatched:
//                    ykanInterface
//                            .getFastMatched(currGizWifiDevice.getMacAddress(), 87, 7,
//                                    "1,38000,341,169,24,64,23,22,23,22,23,64,24,21,24,21,24,63,24,21,24,21,24,63,24,21,24,63,24,21,24,21,24,21,24,21,24,21,24,21,24,21,25,21,24,21,24,21,24,21,24,21,24,21,24,21,24,21,24,21,24,63,24,21,24,64,24,21,23,22,23,64,24,21,24",
//                                    new YKanHttpListener() {
//                                        @Override
//                                        public void onSuccess(BaseResult baseResult) {
//                                            MatchRemoteControlResult rcFastMatched = (MatchRemoteControlResult) baseResult;
//                                            result = rcFastMatched.toString();
//                                            Log.d(TAG, " getFastMatched result:" + result);
//                                        }
//
//                                        @Override
//                                        public void onFail(YKError ykError) {
//                                            Log.e(TAG, "ykError:" + ykError.toString());
//                                        }
//                                    });
//                    break;
                default:
                    break;
            }
            message.obj = result;
            mHandler.sendMessage(message);
            dialogUtils.sendMessage(0);
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    if (deviceType != null) {
                        nameType.clear();
                        for (int i = 0; i < deviceType.size(); i++) {
                            nameType.add(deviceType.get(i).getName());
                        }
                    }
                    typeAdapter.notifyDataSetInvalidated();
                    spType.setAdapter(typeAdapter);
                    break;
                case 1:
                    if (brands != null) {
                        nameBrands.clear();
                        for (int i = 0; i < brands.size(); i++) {
                            nameBrands.add(brands.get(i).getName());
                        }
                    }
                    brandAdapter.notifyDataSetInvalidated();
                    spBrands.setAdapter(brandAdapter);
                    break;
                case 2:
                    if (remoteControls != null) {
                        nameRemote.clear();
                        for (int i = 0; i < remoteControls.size(); i++) {
                            nameRemote.add(remoteControls.get(i).getName() + "-"
                                    + remoteControls.get(i).getRmodel());
                        }
                    }
                    remoteAdapter.notifyDataSetInvalidated();
                    spRemotes.setAdapter(remoteAdapter);
                    break;

                default:
                    break;
            }
        }

        ;
    };

    @Override
    public void onBackPressed() {
        finish();
    }

    ;

}
