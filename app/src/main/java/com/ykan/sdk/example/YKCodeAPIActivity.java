package com.ykan.sdk.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.yaokan.sdk.api.JsonParser;
import com.yaokan.sdk.api.YkanSDKManager;
import com.yaokan.sdk.ir.OnTrunkReceiveListener;
import com.yaokan.sdk.ir.YKanHttpListener;
import com.yaokan.sdk.ir.YkanIRInterface;
import com.yaokan.sdk.ir.YkanIRInterfaceImpl;
import com.yaokan.sdk.model.BaseResult;
import com.yaokan.sdk.model.Brand;
import com.yaokan.sdk.model.BrandResult;
import com.yaokan.sdk.model.DeviceDataStatus;
import com.yaokan.sdk.model.DeviceType;
import com.yaokan.sdk.model.DeviceTypeResult;
import com.yaokan.sdk.model.KeyCode;
import com.yaokan.sdk.model.MatchRemoteControl;
import com.yaokan.sdk.model.MatchRemoteControlResult;
import com.yaokan.sdk.model.RemoteControl;
import com.yaokan.sdk.model.SendType;
import com.yaokan.sdk.model.YKError;
import com.yaokan.sdk.utils.Logger;
import com.yaokan.sdk.utils.ProgressDialogUtils;
import com.yaokan.sdk.utils.Utility;
import com.yaokan.sdk.wifi.DeviceController;
import com.yaokan.sdk.wifi.DeviceManager;
import com.yaokan.sdk.wifi.listener.IDeviceControllerListener;
import com.yaokan.sdk.wifi.listener.LearnCodeListener;
import com.ykan.sdk.example.other.AnimStudy;
import com.ykan.sdk.example.other.OneKeyMatchActivity;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class YKCodeAPIActivity extends Activity implements View.OnClickListener, LearnCodeListener {

    private ProgressDialogUtils dialogUtils;

    private YkanIRInterface ykanInterface;

    private String TAG = YKCodeAPIActivity.class.getSimpleName();

    private TextView tvDevice, tvTrunkSend, tvTrunkReceive;
    private EditText etTrunk;
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

    private String study433_315_key = "";
    private String studyKey = "02UGu+Ph01n8QYmtch2ZeSamsYZu4NGxa+SYk736OHXCeHwjBJFnxjR4evQyiJkmsVT8yfzJ576YHznuZRhTXCXUNTGF0L5v3OZWHbPbilAVOhlU6NLO2/0wgmTv1emdxpAbA/fVL2c+pi7WNoOc1PPkky46urVxSsVAxVujQeQoUJYzbs6n2vvEBS4EgTDXgT4KaVoDkOwN8r3Iww9zKHbfvZn3qJtth3LCzAZjN8UNg5vzS4tReF/p9NMDSXLNCywvnHtXEhS/aMkjs4UarXGw==";

    protected AnimStudy animStudy;
    TextView textView;
    StringBuffer stringBuffer = new StringBuffer();
    SimpleDateFormat simpleFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_codeapi);
        // 遥控云数据接口分装对象对象
        ykanInterface = new YkanIRInterfaceImpl(getApplicationContext());
        // 遥控云数据接口分装对象对象
        ykanInterface = new YkanIRInterfaceImpl(getApplicationContext());
        textView = (TextView) findViewById(R.id.showText);
        simpleFormatter = new SimpleDateFormat("HH:mm:ss");
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
        driverControl = new DeviceController(getApplicationContext(), currGizWifiDevice, new IDeviceControllerListener() {
            @Override
            public void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus) {
                stringBuffer.append("time" + simpleFormatter.format(new Date()) + "\n" + "mac :" + device.getMacAddress() + " status:" + netStatus + "\n");
                textView.setText(stringBuffer.toString());
            }

            @Override
            public void didGetHardwareInfo(GizWifiErrorCode result, GizWifiDevice device, ConcurrentHashMap<String, String> hardwareInfo) {

            }

            @Override
            public void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device) {
                Log.e(TAG, result.toString());
            }
        });
        //设置学习回调
        driverControl.initLearn(this);

        findViewById(R.id.study).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(studyKey)) {
                    driverControl.sendCMD(studyKey, SendType.Infrared);
                } else {
                    Toast.makeText(YKCodeAPIActivity.this, "您还没学习到码值，长按按钮进入学习模式", Toast.LENGTH_LONG).show();
                }
            }
        });
        findViewById(R.id.study).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                findViewById(R.id.study).setTag("small_square");
                animStudy.startAnim(findViewById(R.id.study));
                driverControl.startLearn();
                return true;
            }
        });

        findViewById(R.id.night).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                DeviceManager.instanceDeviceManager(YKCodeAPIActivity.this).setGizWifiCallBack(new GizWifiCallBack() {
//                    @Override
//                    public void didTransAnonymousUser(GizWifiErrorCode result) {
//                        super.didTransAnonymousUser(result);
//                        if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
//                            Log.e(TAG, "转换成功 ");
//                        } else {
//                            Log.e(TAG, "转换失败 ");
//                        }
//                    }
//                });
//                DeviceManager.instanceDeviceManager(YKCodeAPIActivity.this).transAnonymousUser("13728855026", "Xy_123456");
//                driverControl.sendNightLight();
//                stringBuffer.append("time"+simpleFormatter.format(new Date())+"发送指令\n");
//                textView.setText(stringBuffer.toString());
                driverControl.lightTest();
            }
        });
        findViewById(R.id.delete_device_code).setVisibility(View.VISIBLE);
        findViewById(R.id.download_code_to_device).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> codes = new ArrayList<>();
//                codes.add("010H0LcZx8VWVBEZd1E+5LELySvt1bSCvQDXtcLeX31eV2ARamsw/tpF+E+VeEnUIO");
//                codes.add("010H0LcZx8VWVBEZd1E+5LEHn8RvEzEnHMBromunFrCZsFJbt2VMLWvLIQJHJEPXBz");
//                codes.add("010H0LcZx8VWVBEZd1E+5LEHn8RvEzEnHMBromunFrCZsFJbt2VMLWvLIQJHJEPXBz");
                codes.add("182609000136091E1E0B1E0A1E0A091E091E1E0B091F1E0A091E091E1E0B1E0A1E0A1E0A091E091F081F1E0A091F091E091E1E0A1E0A09000136091E1E0B1E0A1E0A091E091E1E0B091F1E0A091E091E1E0B1E0A1E0A1E0A091E091F081F1E0A091F091E091E1E0A1E0A09000136091E1E0B1E0A1E0A091E091E1E0B091F1E0A091E091E1E0B1E0A1E0A1E0A091E091F081F1E0A091F091E091E1E0A1E0A09000136091E1E0B1E0A1E0A091E091E1E0B091F1E0A091E091E1E0B1E0A1E0A1E0A091E091F081F1E0A091F091E091E1E0A1E0A");
                int room = 0;
                int position = 0;
                for (String code : codes) {
                    Message message = sendHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putInt("size", codes.size());//遥控码数组的大小
                    bundle.putInt("room", room);//下载到第几个场景0-8之间
                    bundle.putInt("position", position);//场景中的第几位0-9之间
                    bundle.putString("code", code);//码值
                    message.what = 1;
                    message.setData(bundle);
                    sendHandler.sendMessageDelayed(message, position * 500);
                    position++;
                }
            }
        });
        findViewById(R.id.delete_device_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //删除第几个场景0-8之间
                driverControl.deleteScene(0);
            }
        });
        findViewById(R.id.send_scene).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //发送第几个场景0-8之间
                driverControl.sendScene(0);
            }
        });
//        currGizWifiDevice.setCustomInfo("aaa","bbb");
        //-----------------   小苹果2代新增的功能start  -------------------------
        if (YkanSDKManager.getLittleAppleVersion(YKCodeAPIActivity.this, currGizWifiDevice) >= 2) {
            findViewById(R.id.study_433_315).setVisibility(View.VISIBLE);
            findViewById(R.id.sc_trunk).setVisibility(View.VISIBLE);
            findViewById(R.id.download_code_to_device).setVisibility(View.VISIBLE);

            driverControl.setOnTrunkReceiveListener(new OnTrunkReceiveListener() {
                @Override
                public void onTrunkReceive(byte[] data) {
                    if (data != null && data.length > 0) {
                        Log.e("TTTT", Utility.bytesToHexString(data));
                        tvTrunkReceive.setText(Utility.bytesToHexString(data));
                    }
                }
            });
            findViewById(R.id.trunk).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String data = etTrunk.getText().toString();
                    if (!TextUtils.isEmpty(data)) {
                        tvTrunkSend.setText(data);
                        driverControl.sendCMD(data, SendType.Trunk);
                    }

                }
            });
            //-------------------    433/315 模块start   ----------------------
            findViewById(R.id.study_433_315).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!TextUtils.isEmpty(study433_315_key)) {
                        driverControl.sendCMD(study433_315_key, SendType.RadioFrequency);
                    } else {
                        Toast.makeText(YKCodeAPIActivity.this, "您还没学习到码值，长按按钮进入433/315学习模式", Toast.LENGTH_LONG).show();
                    }
                }
            });
            findViewById(R.id.study_433_315).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    findViewById(R.id.study_433_315).setTag("small_square");
                    animStudy.startAnim(findViewById(R.id.study_433_315));
                    driverControl.startLearn433or315();
                    return true;
                }
            });
            //-------------------    433/315 模块 end   ----------------------
            //-----------------   小苹果2代新增的功能end  -------------------------
        }
    }

    Handler sendHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Bundle bundle = msg.getData();
                driverControl.saveCodeInDevice(bundle.getInt("size"), bundle.getInt("room"), bundle.getInt("position"), bundle.getString("code"));
            }
        }
    };

    private void initView() {
        animStudy = new AnimStudy(this);
        dialogUtils = new ProgressDialogUtils(this);
        etTrunk = (EditText) findViewById(R.id.et_trunk);
        tvDevice = (TextView) findViewById(R.id.tv_device);
        tvTrunkSend = (TextView) findViewById(R.id.tv_trunk_send);
        tvTrunkReceive = (TextView) findViewById(R.id.tv_trunk_receive);

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
            case R.id.one_key_match:
                if (currDeviceType != null && currBrand != null) {
                    Intent intent = new Intent(this, OneKeyMatchActivity.class);
                    intent.putExtra("tid", currDeviceType.getTid());
                    intent.putExtra("type", currDeviceType.getName());
                    intent.putExtra("bid", currBrand.getBid());
                    intent.putExtra("brand", currBrand.getName());
                    intent.putExtra("GizWifiDevice", currGizWifiDevice);
                    currGizWifiDevice = getIntent().getParcelableExtra(
                            "GizWifiDevice");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "请获取设备品牌", Toast.LENGTH_SHORT).show();
                }
                break;
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
                                driverControl.sendCMD(sCode[which], SendType.Infrared);
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
                // 进入遥控控制面板
                Log.d(TAG, "remoteControl:" + remoteControl);
                if (remoteControl == null) {
                    Toast.makeText(getApplicationContext(), "没有下载遥控器数据",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent();
                JsonParser jsonParser = new JsonParser();
                int airTid = 7;// 空调
                if (remoteControl.gettId() == airTid) {
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

    @Override
    public void didReceiveData(DeviceDataStatus dataStatus, String data) {
        switch (dataStatus) {
            case DATA_LEARNING_SUCCESS:
                studyKey = data;//data 表示学习接收到的数据
                animStudy.stopAnim(1);
                Toast.makeText(getApplicationContext(), "学习成功", Toast.LENGTH_SHORT).show();
                break;
            case DATA_LEARNING_SUCCESS_315://学习成功
            case DATA_LEARNING_SUCCESS_433://学习成功
                study433_315_key = data;//data 表示学习接收到的数据
                animStudy.stopAnim(1);
                Toast.makeText(getApplicationContext(), "学习成功", Toast.LENGTH_SHORT).show();
                break;
            case DATA_LEARNING_FAILED://学习失败
                Logger.d(TAG, "学习失败");
                animStudy.stopAnim(1);
                Toast.makeText(getApplicationContext(), "学习失败", Toast.LENGTH_SHORT).show();
                driverControl.learnStop433or315();
                break;
            case DATA_SAVE_SUCCESS:
                Toast.makeText(getApplicationContext(), "写入成功", Toast.LENGTH_SHORT).show();
                break;
            default:
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
        public void handleMessage(Message msg) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (driverControl != null) {
            driverControl.learnStop433or315();
            driverControl.learnStop();
        }
    }
}
