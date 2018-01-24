package com.ykan.sdk.example.other;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.yaokan.sdk.api.JsonParser;
import com.yaokan.sdk.ir.YKanHttpListener;
import com.yaokan.sdk.ir.YkanIRInterface;
import com.yaokan.sdk.ir.YkanIRInterfaceImpl;
import com.yaokan.sdk.model.BaseResult;
import com.yaokan.sdk.model.DeviceDataStatus;
import com.yaokan.sdk.model.DeviceType;
import com.yaokan.sdk.model.KeyCode;
import com.yaokan.sdk.model.MatchRemoteControl;
import com.yaokan.sdk.model.MatchRemoteControlResult;
import com.yaokan.sdk.model.OneKeyMatchKey;
import com.yaokan.sdk.model.RemoteControl;
import com.yaokan.sdk.model.YKError;
import com.yaokan.sdk.utils.ProgressDialogUtils;
import com.yaokan.sdk.wifi.DeviceController;
import com.yaokan.sdk.wifi.listener.LearnCodeListener;
import com.ykan.sdk.example.AirControlActivity;
import com.ykan.sdk.example.BaseActivity;
import com.ykan.sdk.example.R;
import com.ykan.sdk.example.YKWifiDeviceControlActivity;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class OneKeyMatchActivity extends BaseActivity implements View.OnClickListener, LearnCodeListener, YKanHttpListener {
    public static final String TAG = OneKeyMatchActivity.class.getName();
    private int tid;
    private int bid;
    private YkanIRInterface ykanInterface;
    private DeviceController driverControl = null;
    private GizWifiDevice gizWifiDevice;
    private ProgressDialogUtils dialogUtils;
    private ListView listView;
    private List<MatchRemoteControl> list = new ArrayList<>();
    private ControlAdapter controlAdapter;
    private RemoteControl remoteControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onekey_match);
        if (getIntent() != null) {
            initView();
            dialogUtils = new ProgressDialogUtils(this);
            // 遥控云数据接口分装对象对象
            ykanInterface = new YkanIRInterfaceImpl(getApplicationContext());
            gizWifiDevice = (GizWifiDevice) getIntent().getParcelableExtra(
                    "GizWifiDevice");
            driverControl = new DeviceController(this, gizWifiDevice, null);
            driverControl.initLearn(this);
        }
    }

    private void initView() {
        listView = (ListView) findViewById(R.id.lv_control);
        tid = getIntent().getIntExtra("tid", 0);
        bid = getIntent().getIntExtra("bid", 0);
        ((TextView) findViewById(R.id.tv_type)).setText(getIntent().getStringExtra("type") + tid);
        ((TextView) findViewById(R.id.tv_brand)).setText(getIntent().getStringExtra("brand") + bid);
        controlAdapter = new ControlAdapter();
        listView.setAdapter(controlAdapter);
    }

    String key;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_match:
                switch (tid) {
                    case DeviceType.AIRCONDITION:
                        key = "on";
                        break;
                    case DeviceType.MULTIMEDIA:
                        key = "ok";
                        break;
                    default:
                        key = "power";
                }
                showDlg();
                break;
        }
    }

    private void showDlg() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(OneKeyMatchActivity.this).setMessage("请对准小苹果按" + key + "键").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        driverControl.startLearn();
                    }
                }).create().show();
            }
        });
    }

    @Override
    public void didReceiveData(DeviceDataStatus dataStatus, String data) {
        // TODO Auto-generated method stub
        switch (dataStatus) {
            case DATA_LEARNING_SUCCESS://学习成功
                final String studyValue = data;//data 表示学习接收到的数据
                dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ykanInterface.oneKeyMatched(gizWifiDevice.getMacAddress(), studyValue, tid + "", bid + "", key, OneKeyMatchActivity.this);
                    }
                }).start();
                Toast.makeText(getApplicationContext(), "学习成功", Toast.LENGTH_SHORT).show();
                break;
            case DATA_LEARNING_FAILED://学习失败
                Toast.makeText(getApplicationContext(), "学习失败", Toast.LENGTH_SHORT).show();
                driverControl.learnStop();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (driverControl != null) {
            driverControl.learnStop();
        }
    }

    private void toYKWifiDeviceControlActivity() {
        JsonParser jsonParser = new JsonParser();
        Intent intent = new Intent();
        intent.setClass(OneKeyMatchActivity.this, YKWifiDeviceControlActivity.class);
        intent.putExtra("GizWifiDevice", gizWifiDevice);
        try {
            intent.putExtra("remoteControl", jsonParser.toJson(remoteControl));
            intent.putExtra("rcCommand", jsonParser.toJson(remoteControl.getRcCommand()));
        } catch (JSONException e) {
            Log.e(TAG, "JSONException:" + e.getMessage());
        }
        startActivity(intent);
    }

    @Override
    public void onSuccess(BaseResult baseResult) {
        if (isFinishing()) {
            return;
        }
        dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
        if (baseResult instanceof OneKeyMatchKey) {
            key = ((OneKeyMatchKey) baseResult).getNext_cmp_key();
            showDlg();
        } else if (baseResult instanceof RemoteControl) {
            remoteControl = (RemoteControl) baseResult;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(OneKeyMatchActivity.this).setMessage("匹配成功，进入测试").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            toYKWifiDeviceControlActivity();
                        }
                    }).create().show();
                }
            });
        } else if (baseResult instanceof MatchRemoteControlResult) {
            final MatchRemoteControlResult result = (MatchRemoteControlResult) baseResult;
            Log.e("tttt", result.toString());
            if (result != null && result.getRs() != null && result.getRs().size() > 0) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        list.clear();
                        list.addAll(result.getRs());
                        controlAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    @Override
    public void onFail(final YKError ykError) {
        dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(OneKeyMatchActivity.this).setMessage(ykError.getError()).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
            }
        });
    }

    private class ControlAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            OneKeyMatchActivity.ControlAdapter.ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext())
                        .inflate(R.layout.lv_control, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.tv_rc_name);
                holder.btnOn = (Button) convertView.findViewById(R.id.btn_on);
                holder.btnOff = (Button) convertView.findViewById(R.id.btn_off);
                holder.btnSOff = (Button) convertView.findViewById(R.id.btn_s_off);
                holder.btnSOn = (Button) convertView.findViewById(R.id.btn_s_on);
                holder.tvDl = (TextView) convertView.findViewById(R.id.dl);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(list.get(position).getName() + "-" + list.get(position).getRmodel());

            holder.btnOff.setVisibility(View.VISIBLE);
            holder.btnOn.setVisibility(View.VISIBLE);
            holder.btnSOff.setVisibility(View.VISIBLE);
            holder.btnSOn.setVisibility(View.VISIBLE);
            if (list.get(position).getRcCommand().size() <= 2) {
                holder.btnSOff.setVisibility(View.GONE);
                holder.btnSOn.setVisibility(View.GONE);
            }
            holder.btnOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendCMD("on", position);
                }
            });
            holder.btnOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendCMD("off", position);
                }
            });
            holder.btnSOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendCMD("u0", position);
                }
            });
            holder.btnSOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendCMD("u1", position);
                }
            });
            holder.tvDl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ykanInterface
                                    .getRemoteDetails(gizWifiDevice.getMacAddress(), list.get(position).getRid(), new YKanHttpListener() {
                                        @Override
                                        public void onSuccess(BaseResult baseResult) {
                                            dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
                                            if (baseResult != null) {
                                                remoteControl = (RemoteControl) baseResult;
                                                Intent intent = new Intent();
                                                intent.setClass(OneKeyMatchActivity.this, AirControlActivity.class);
                                                intent.putExtra("GizWifiDevice", gizWifiDevice);
                                                try {
                                                    intent.putExtra("remoteControl", new JsonParser().toJson(remoteControl));
                                                } catch (JSONException e) {
                                                    Log.e(TAG, "JSONException:" + e.getMessage());
                                                }
                                                startActivity(intent);
                                            }
                                        }

                                        @Override
                                        public void onFail(final YKError ykError) {
                                            dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    new AlertDialog.Builder(OneKeyMatchActivity.this).setMessage(ykError.getError()).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    }).create().show();
                                                }
                                            });
                                            Log.e(TAG, "ykError:" + ykError.toString());
                                        }
                                    });
                        }
                    }).start();
                }
            });
            return convertView;
        }

        private class ViewHolder {
            TextView name = null;
            Button btnOn = null;
            Button btnOff = null;
            Button btnSOff = null;
            Button btnSOn = null;
            TextView tvDl = null;
        }
    }

    private void sendCMD(String mode, int position) {
        if (driverControl != null) {
            HashMap<String, KeyCode> map = list.get(position).getRcCommand();
            Set<String> set = map.keySet();
            String key = null;
            for (String s : set) {
                if (s.contains(mode)) {
                    key = s;
                }
            }
            driverControl.sendCMD(map.get(key).getSrcCode());
        }
    }
}
