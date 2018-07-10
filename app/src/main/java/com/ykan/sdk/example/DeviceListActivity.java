package com.ykan.sdk.example;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.yaokan.sdk.utils.Logger;
import com.yaokan.sdk.utils.Utility;
import com.yaokan.sdk.wifi.DeviceManager;
import com.yaokan.sdk.wifi.GizWifiCallBack;

import java.util.ArrayList;
import java.util.List;

public class DeviceListActivity extends BaseActivity {
    private String TAG = DeviceListActivity.class.getSimpleName();
    private ListView lvDevice;
    private DeviceManager mDeviceManager;
    List<GizWifiDevice> wifiDevices = new ArrayList<GizWifiDevice>();
    DeviceAdapter adapter;
    private List<String> deviceNames = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_device_list);
        mDeviceManager = DeviceManager
                .instanceDeviceManager(getApplicationContext());

        initView();
    }


    private GizWifiCallBack mGizWifiCallBack = new GizWifiCallBack() {

        @Override
        public void didUnbindDeviceCd(GizWifiErrorCode result, String did) {
            super.didUnbindDeviceCd(result, did);
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                // 解绑成功
                Logger.d(TAG, "解除绑定成功");
            } else {
                // 解绑失败
                Logger.d(TAG, "解除绑定失败");
            }
        }

        @Override
        public void didBindDeviceCd(GizWifiErrorCode result, String did) {
            super.didBindDeviceCd(result, did);
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                // 绑定成功
                Logger.d(TAG, "绑定成功");
            } else {
                // 绑定失败
                Logger.d(TAG, "绑定失败");
            }
        }

        @Override
        public void didSetSubscribeCd(GizWifiErrorCode result, GizWifiDevice device, boolean isSubscribed) {
            super.didSetSubscribeCd(result, device, isSubscribed);
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                // 解绑成功
                Logger.d(TAG, (isSubscribed ? "订阅" : "取消订阅") + "成功");
            } else {
                // 解绑失败
                Logger.d(TAG, "订阅失败");
            }
        }

        @Override
        public void discoveredrCb(GizWifiErrorCode result,
                                  List<GizWifiDevice> deviceList) {
            Logger.d(TAG,
                    "discoveredrCb -> deviceList size:" + deviceList.size()
                            + "  result:" + result);
            switch (result) {
                case GIZ_SDK_SUCCESS:
                    Logger.e(TAG, "load device  sucess");
                    update(deviceList);
//                    if(deviceList.get(0).getNetStatus()==GizWifiDeviceNetStatus.GizDeviceOffline)

                    break;
                default:
                    break;

            }

        }
    };

    private void initView() {
        lvDevice = (ListView) findViewById(R.id.lv_device);
        lvDevice.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent = new Intent(DeviceListActivity.this, YKCodeAPIActivity.class);
                final GizWifiDevice mGizWifiDevice = wifiDevices.get(position);
                intent.putExtra("GizWifiDevice", wifiDevices.get(position));
                // 绑定
                if (!Utility.isEmpty(mGizWifiDevice) && !mGizWifiDevice.isBind()) {
                    mDeviceManager.bindRemoteDevice(mGizWifiDevice);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceManager.setSubscribe(mGizWifiDevice, true);
                        }
                    }, 1000);
                }
                startActivity(intent);
            }
        });
        adapter = new DeviceAdapter();
        lvDevice.setAdapter(adapter);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mDeviceManager.setGizWifiCallBack(mGizWifiCallBack);
        update(mDeviceManager.getCanUseGizWifiDevice());
    }

    private String getBindInfo(boolean isBind) {
        String strReturn = "";
        if (isBind == true)
            strReturn = "已绑定";
        else
            strReturn = "未绑定";
        return strReturn;
    }

    private String getLANInfo(boolean isLAN) {
        String strReturn = "";
        if (isLAN == true)
            strReturn = "局域网内设备";
        else
            strReturn = "远程设备";
        return strReturn;
    }


    private String getOnlineInfo(GizWifiDeviceNetStatus netStatus) {
        String strReturn = "";
        if (mDeviceManager.isOnline(netStatus) == true)//判断是否在线的方法
            strReturn = "在线";
        else
            strReturn = "离线";
        return strReturn;
    }


    void update(List<GizWifiDevice> gizWifiDevices) {
        if (gizWifiDevices != null) {
            Log.e("DeviceListActivity", gizWifiDevices.size() + "");
        }
        if (gizWifiDevices == null) {
            deviceNames.clear();
        } else if (gizWifiDevices != null && gizWifiDevices.size() >= 1) {
            wifiDevices.clear();
            wifiDevices.addAll(gizWifiDevices);
            deviceNames.clear();
            for (int i = 0; i < wifiDevices.size(); i++) {
                deviceNames.add(wifiDevices.get(i).getProductName() + "("
                        + wifiDevices.get(i).getMacAddress() + ") "
                        + getBindInfo(wifiDevices.get(i).isBind()) + "\n"
                        + getLANInfo(wifiDevices.get(i).isLAN()) + "  " + getOnlineInfo(wifiDevices.get(i).getNetStatus()));

            }
        }

        adapter.notifyDataSetChanged();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_link:
                startActivity(new Intent(DeviceListActivity.this,
                        YKWifiConfigActivity.class));
                break;
            case R.id.btn_refresh:
                update(mDeviceManager.getCanUseGizWifiDevice());
                break;
            default:
                break;
        }
    }

    private class DeviceAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return deviceNames.size();
        }

        @Override
        public Object getItem(int position) {
            return deviceNames.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext())
                        .inflate(R.layout.lv_item, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.tv_item);
                holder.btn = (Button) convertView.findViewById(R.id.btn_item);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(deviceNames.get(position));

            final int pos = position;
            holder.btn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    GizWifiDevice mGizWifiDevice = wifiDevices.get(pos);
                    // 已经绑定的设备才去解绑
                    if (mGizWifiDevice.isBind()) {
                        // 解绑该设备
                        mDeviceManager.unbindDevice(mGizWifiDevice.getDid());
                        update(mDeviceManager.getCanUseGizWifiDevice());
                    } else {
                        toast("该设备未绑定");
                    }
                    if (mGizWifiDevice.isSubscribed()) {
                        mDeviceManager.setSubscribe(mGizWifiDevice, false);
                    }

                }
            });
            return convertView;
        }

        private class ViewHolder {
            TextView name = null;
            Button btn = null;
        }
    }
}
