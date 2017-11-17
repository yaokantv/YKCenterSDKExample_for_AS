package com.ykan.sdk.example;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.gizwits.gizwifisdk.api.GizDeviceScheduler;
import com.gizwits.gizwifisdk.api.GizDeviceSchedulerCenter;
import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizScheduleWeekday;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.gizwifisdk.listener.GizDeviceSchedulerCenterListener;
import com.gizwits.gizwifisdk.listener.GizDeviceSchedulerListener;
import com.yaokan.sdk.wifi.YKSchedulerCenter;

import java.util.ArrayList;
import java.util.List;

public class SchedulerListActivity extends BaseActivity {

    private GizWifiDevice currGizWifiDevice;
    private List<GizDeviceScheduler> list = new ArrayList<>();
    SchedulerAdapter schedulerAdapter;
    ListView listView;
    public static final String TAG = SchedulerListActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduler_list);
        listView = (ListView) findViewById(R.id.list_view);
        schedulerAdapter = new SchedulerAdapter();
        listView.setAdapter(schedulerAdapter);
        currGizWifiDevice = (GizWifiDevice) getIntent().getParcelableExtra(
                "GizWifiDevice");
        // 设置定时任务监听
        GizDeviceSchedulerCenter.setListener(mListener);
        // 同步更新设备的定时任务列表，mDevice为在设备列表中得到的设备对象
        GizDeviceSchedulerCenter.updateSchedulers(Constant.UID, Constant.TOKEN, currGizWifiDevice);
    }

    GizDeviceSchedulerCenterListener mListener = new GizDeviceSchedulerCenterListener() {
        @Override
        public void didUpdateSchedulers(GizWifiErrorCode result, GizWifiDevice schedulerOwner, List<GizDeviceScheduler> schedulerList) {
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) {
                if (schedulerList != null) {
                    list.clear();
                    list.addAll(schedulerList);
                    schedulerAdapter.notifyDataSetChanged();
                    Log.e(TAG, schedulerList.toString());
                }
            } else {
            }
        }
    };

    GizDeviceSchedulerListener schedulerListener = new GizDeviceSchedulerListener() {
        @Override
        public void didUpdateSchedulerInfo(GizDeviceScheduler scheduler, GizWifiErrorCode result) {
            if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS) { // 接收变更的定时任务信息
                Log.e(TAG, "修改成功");
            } else {
                // 失败处理
                Log.e(TAG, "修改失败" + result.toString());
            }
        }
    };

    private class SchedulerAdapter extends BaseAdapter {

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
            SchedulerListActivity.SchedulerAdapter.ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext())
                        .inflate(R.layout.lv_item, parent, false);
                holder = new SchedulerListActivity.SchedulerAdapter.ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.tv_item);
                holder.btnUpdate = (Button) convertView.findViewById(R.id.btn_item);
                holder.btnUpdate.setText("修改");
                holder.btnDelete = (Button) convertView.findViewById(R.id.btn_delete);
                holder.btnDelete.setVisibility(View.VISIBLE);
                convertView.setTag(holder);
            } else {
                holder = (SchedulerListActivity.SchedulerAdapter.ViewHolder) convertView.getTag();
            }
            holder.name.setText(list.get(position).getRemark());
            holder.btnUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SchedulerListActivity.this);
                    builder.setTitle("修改定时任务").setItems(new CharSequence[]{"一次性定时任务", "周重复的定时任务", "月重复的定时任务"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //从任务列表里面获取任务对象
                            GizDeviceScheduler mScheduler = list.get(position);
                            mScheduler.setListener(schedulerListener);
                            mScheduler.setDate("2018-01-16");
                            mScheduler.setTime("09:40");
                            mScheduler.setRemark("任务" + System.currentTimeMillis());
                            // 创建设备的定时任务，mDevice为在设备列表中得到的设备对象
                            switch (which) {
                                case 0:
                                    break;
                                case 1:
                                    //我们现在让定时任务按周重复执行，现在要每周的周一至周五早上6点30分都执行任务。
                                    List<GizScheduleWeekday> weekDays = new ArrayList<GizScheduleWeekday>();
                                    weekDays.add(GizScheduleWeekday.GizScheduleMonday);
                                    weekDays.add(GizScheduleWeekday.GizScheduleTuesday);
                                    weekDays.add(GizScheduleWeekday.GizScheduleWednesday);
                                    weekDays.add(GizScheduleWeekday.GizScheduleThursday);
                                    weekDays.add(GizScheduleWeekday.GizScheduleFriday);
                                    mScheduler.setWeekdays(weekDays);
                                    break;
                                case 2:
                                    //我们现在让定时任务按周重复执行，现在要每个月的1号、15号早上6点30分都执行任务。
                                    //注意不要同时设置按周重复，如果同时设置了按周重复，按月重复会被忽略。
                                    List<Integer> monthDays = new ArrayList<>();
                                    monthDays.add(1);
                                    monthDays.add(15);
                                    mScheduler.setMonthDays(monthDays);
                                    break;
                            }
                            //此code为遥控码，需要从API里获取，或者用户将遥控数据存储到本地在获取
                            String code = "019CR9Q+zdAFhasaQ7sPQuzCgZg7GGZHHZzRtQpqkd6KlDR0lK8FOaJdPXqk63EnLFnHLAB//9lDpaPeWJK9nuRg==";
                            YKSchedulerCenter.editScheduler(Constant.UID, Constant.TOKEN, mScheduler, code);
                        }
                    }).create().show();
                }
            });
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GizDeviceScheduler scheduler = list.get(position);
                    YKSchedulerCenter.deleteScheduler(Constant.UID, Constant.TOKEN, currGizWifiDevice, scheduler);
                }
            });
            return convertView;
        }

        private class ViewHolder {
            TextView name = null;
            Button btnUpdate = null;
            Button btnDelete = null;
        }
    }
}
