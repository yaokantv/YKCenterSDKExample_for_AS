package com.ykan.sdk.example;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.yaokan.sdk.ir.AllKeyMatchListener;
import com.yaokan.sdk.model.AirConCatogery;
import com.yaokan.sdk.model.AirStatus;
import com.yaokan.sdk.model.MatchModel;
import com.yaokan.sdk.model.RemoteControl;
import com.yaokan.sdk.model.ShowMsg;
import com.yaokan.sdk.model.YKError;
import com.yaokan.sdk.model.kyenum.AirV3KeyMode;
import com.yaokan.sdk.utils.ProgressDialogUtils;
import com.yaokan.sdk.utils.Utility;
import com.yaokan.sdk.wifi.DeviceController;
import com.yaokan.sdk.wifi.MatchHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AllKeyMatchActivity extends BaseActivity implements AllKeyMatchListener, View.OnClickListener {

    int tid;
    int bid;
    MatchHelper helper;
    private DeviceController driverControl = null;
    private GizWifiDevice gizWifiDevice;
    private ProgressDialogUtils dialogUtils;
    RelativeLayout rlStep1, rlStep2, rlStep2Bottom;
    LinearLayout llStep2;
    Button btnNo, btnYes, btnTest, btnAllNo, btnAllYes;
    private Button mode_btn, wspeed_btn, tbspeed_btn, lrwspped_btn, power_btn, temp_add_btn, temp_rdc_btn;
    TextView tvMsg, tvAllMsg, tv_show;
    GridView gv;
    ExpandAdapter expandAdapter;
    private List<MatchModel> codeKeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_key_match);
        dialogUtils = new ProgressDialogUtils(this);
        tid = getIntent().getIntExtra("tid", 0);
        bid = getIntent().getIntExtra("bid", 0);
        ((TextView) findViewById(R.id.tv_type)).setText(getIntent().getStringExtra("type") + tid);
        ((TextView) findViewById(R.id.tv_brand)).setText(getIntent().getStringExtra("brand") + bid);

        rlStep1 = (RelativeLayout) findViewById(R.id.rl_step1);
        rlStep2 = (RelativeLayout) findViewById(R.id.rl_step2_normal);
        rlStep2Bottom = (RelativeLayout) findViewById(R.id.rl_step2_bottom);
        llStep2 = (LinearLayout) findViewById(R.id.ll_step2_air);

        tv_show = (TextView) findViewById(R.id.tv_show);
        mode_btn = (Button) findViewById(R.id.mode_btn);
        wspeed_btn = (Button) findViewById(R.id.wspeed_btn);
        tbspeed_btn = (Button) findViewById(R.id.tbspeed_btn);
        lrwspped_btn = (Button) findViewById(R.id.lrwspped_btn);
        power_btn = (Button) findViewById(R.id.power_btn);
        temp_add_btn = (Button) findViewById(R.id.temp_add_btn);
        temp_rdc_btn = (Button) findViewById(R.id.temp_rdc_btn);


        btnNo = (Button) findViewById(R.id.no);
        btnYes = (Button) findViewById(R.id.yes);
        btnAllNo = (Button) findViewById(R.id.all_no);
        btnAllYes = (Button) findViewById(R.id.all_yes);
        btnTest = (Button) findViewById(R.id.test);
        tvMsg = (TextView) findViewById(R.id.tv_test);
        tvAllMsg = (TextView) findViewById(R.id.tv_all);
        gv = (GridView) findViewById(R.id.codeGridView);

        expandAdapter = new ExpandAdapter(codeKeys);
        gv.setAdapter(expandAdapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String key = codeKeys.get(position).getKey();
                helper.test(key);
            }
        });

        btnNo.setOnClickListener(this);
        btnYes.setOnClickListener(this);
        btnAllNo.setOnClickListener(this);
        btnAllYes.setOnClickListener(this);
        btnTest.setOnClickListener(this);

        mode_btn.setOnClickListener(this);
        wspeed_btn.setOnClickListener(this);
        tbspeed_btn.setOnClickListener(this);
        lrwspped_btn.setOnClickListener(this);
        power_btn.setOnClickListener(this);
        temp_add_btn.setOnClickListener(this);
        temp_rdc_btn.setOnClickListener(this);

        gizWifiDevice = getIntent().getParcelableExtra("GizWifiDevice");
        driverControl = new DeviceController(this, gizWifiDevice, null);
        helper = new MatchHelper(AllKeyMatchActivity.this).mac(gizWifiDevice.getMacAddress())
                .type(String.valueOf(tid)).bid(String.valueOf(bid)).control(driverControl).listener(this);
        helper.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                //重置当前匹配流程
                helper.reset();
                break;
            case R.id.reset_all:
                //重置所有匹配流程
                helper.reset(MatchHelper.STEP_TEST_CODE);

                rlStep1.setVisibility(View.VISIBLE);
                rlStep2.setVisibility(View.GONE);
                rlStep2Bottom.setVisibility(View.GONE);
                llStep2.setVisibility(View.GONE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadData(final int step) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (step) {
                    case MatchHelper.STEP_GET_ALL_CTRL:
                        dialogUtils.setMessage("加载遥控数据");
                        break;
                    case MatchHelper.STEP_ALL_KEY_MATCH:
                        dialogUtils.setMessage("加载全键匹配数据");
                        break;
                    case MatchHelper.STEP_DOWNLOAD_CTRL:
                        dialogUtils.setMessage("下载遥控数据");
                        break;
                }
                dialogUtils.sendMessage(ProgressDialogUtils.SHOW);
            }
        });
    }

    @Override
    public void loadResult(final int step, final boolean s, final YKError ykError) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialogUtils.sendMessage(ProgressDialogUtils.DISMISS);
                if (s) {
                    switch (step) {
                        case MatchHelper.STEP_GET_ALL_CTRL:
                            //匹配数据获取成功
                            rlStep1.setVisibility(View.VISIBLE);
                            break;
                        case MatchHelper.STEP_ALL_KEY_MATCH:
                            //非空调全键匹配数据获取成功
                            rlStep1.setVisibility(View.GONE);
                            rlStep2.setVisibility(View.VISIBLE);
                            rlStep2Bottom.setVisibility(View.VISIBLE);
                            break;
                        case MatchHelper.STEP_ALL_KEY_MATCH_AIR:
                            //空调全键匹配数据获取成功
                            rlStep1.setVisibility(View.GONE);
                            llStep2.setVisibility(View.VISIBLE);
                            rlStep2Bottom.setVisibility(View.VISIBLE);
                            break;
                        case MatchHelper.STEP_DOWNLOAD_CTRL:
                            //下载码库成功
                            break;
                    }
                } else {
                    switch (step) {
                        case MatchHelper.STEP_GET_ALL_CTRL:
                            //匹配数据获取失败
                            show("匹配数据获取失败\n" + ykError.toString() + "\n请重新调用 helper.start()");
                            break;
                        case MatchHelper.STEP_ALL_KEY_MATCH:
                            //非空调全键匹配数据获取失败
                            show("非空调全键匹配数据获取失败\n" + ykError.toString() + "\n请重新调用 helper.next()");
                            break;
                        case MatchHelper.STEP_ALL_KEY_MATCH_AIR:
                            //空调全键匹配数据获取失败
                            show("空调全键匹配数据获取失败\n" + ykError.toString() + "\n请重新调用 helper.next()");
                            break;
                        case MatchHelper.STEP_DOWNLOAD_CTRL:
                            //下载码库失败
                            show("下载码库失败\n" + ykError.toString() + "\n请重新调用 helper.downloadCtrl()");
                            break;
                    }
                }
            }
        });
    }

    //更新数据的回调
    @Override
    public void updateMsg(final int step, final Object o) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (step) {
                    case MatchHelper.STEP_GET_ALL_CTRL:
                        break;
                    case MatchHelper.STEP_TEST_CODE:
                        if (o != null && o instanceof ShowMsg) {
                            ShowMsg msg = (ShowMsg) o;
                            tvMsg.setText(msg.getName() + " " + msg.getCurIndex() + "/" + msg.getTotal());
                            btnTest.setText("测试按键：" + msg.getMatchKey());
                        } else if (o != null && o instanceof YKError) {
                            toast(((YKError) o).getError());
                        } else {
                            tvMsg.setText("找不到您的设备");
                        }
                        break;
                    case MatchHelper.STEP_ALL_KEY_MATCH:
                        if (o != null && o instanceof List) {
                            codeKeys.clear();
                            codeKeys.addAll((Collection<? extends MatchModel>) o);
                            expandAdapter.notifyDataSetChanged();
                        } else if (o != null && o instanceof ShowMsg) {
                            ShowMsg msg = (ShowMsg) o;
                            tvAllMsg.setText(msg.getName() + " " + msg.getCurIndex() + "/" + msg.getTotal());
                        } else if (o == null) {
                            tvAllMsg.setText("找不到您的设备");
                        }
                        break;
                    case MatchHelper.STEP_DOWNLOAD_CTRL:
                        if (o != null && o instanceof RemoteControl) {
                            RemoteControl control = (RemoteControl) o;
                        }
                        break;
                    case MatchHelper.STEP_ALL_KEY_MATCH_AIR:
                        if (o != null && o instanceof AirStatus) {
                            onRefreshUI((AirStatus) o);
                        }
                        break;
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.no:
                helper.next();
                break;
            case R.id.all_no:
                helper.next();
                break;
            case R.id.yes:
                helper.startAllKeyMatch();
                break;
            case R.id.all_yes:
                helper.downloadCtrl();
                break;
            case R.id.test:
                //测试当前按键
                helper.test();
                break;
            //----------------------------------------------
            case R.id.power_btn:
                helper.test(AirConCatogery.Power, true);
                break;
            case R.id.mode_btn:
                if (checkOpen())
                    helper.test(AirConCatogery.Mode, true);
                break;
            case R.id.wspeed_btn:
                if (checkOpen())
                    helper.test(AirConCatogery.Speed, true);
                break;
            case R.id.tbspeed_btn:
                if (checkOpen())
                    helper.test(AirConCatogery.WindUp, true);
                break;
            case R.id.lrwspped_btn:
                if (checkOpen())
                    helper.test(AirConCatogery.WindLeft, true);
                break;
            case R.id.temp_add_btn:
                if (checkOpen())
                    helper.test(AirConCatogery.Temp, true);
                break;
            case R.id.temp_rdc_btn:
                if (checkOpen())
                    helper.test(AirConCatogery.Temp, false);
                break;
        }
    }

    public boolean checkOpen() {
        if (helper.isOff()) {
            show("请先打开空调");
        }
        return !helper.isOff();
    }

    private void onRefreshUI(AirStatus airStatus) {
        if (Utility.isEmpty(airStatus)) {
            return;
        }
        String content = "";
        if (helper.isOff()) {
            content = "空调已关闭";
        } else {
            content = getContent(airStatus);
        }
        tv_show.setText(content);
    }

    private String getContent(AirStatus airStatus) {
        String mode = airStatus.getMode().getName();
        String temp = "";
        if (!TextUtils.isEmpty(mode)) {
            AirV3KeyMode keyMode = null;
            switch (mode) {
                case "r":
                    keyMode = helper.getrMode();
                    break;
                case "h":
                    keyMode = helper.gethMode();
                    break;
                case "d":
                    keyMode = helper.getdMode();
                    break;
                case "w":
                    keyMode = helper.getwMode();
                    break;
                case "a":
                    keyMode = helper.getaMode();
                    break;
            }
            if (keyMode != null) {
                if (keyMode.isSpeed()) {
                    setBtnStatus(wspeed_btn, true);
                } else {
                    setBtnStatus(wspeed_btn, false);
                }
                if (keyMode.isU()) {
                    setBtnStatus(tbspeed_btn, true);
                } else {
                    setBtnStatus(tbspeed_btn, false);
                }
                if (keyMode.isL()) {
                    setBtnStatus(lrwspped_btn, true);
                } else {
                    setBtnStatus(lrwspped_btn, false);
                }
                if (keyMode.isTemp()) {
                    setBtnStatus(temp_add_btn, true);
                    setBtnStatus(temp_rdc_btn, true);
                } else {
                    temp = "--";
                    setBtnStatus(temp_add_btn, false);
                    setBtnStatus(temp_rdc_btn, false);
                }
            }
        }
        String content;
        content = "模式：" + airStatus.getMode().getChName()
                + "\n风量：" + airStatus.getSpeed().getChName()
                + "\n左右扫风：" + airStatus.getWindLeft().getChName()
                + "\n上下扫风：" + airStatus.getWindUp().getChName()
                + "\n温度：" + (TextUtils.isEmpty(temp) ? airStatus.getTemp().getChName() : temp);
        return content;
    }

    void setBtnStatus(TextView textView, boolean status) {
        textView.setEnabled(status);
        textView.setTextColor(status ? getResources().getColor(android.R.color.white) : getResources().getColor(android.R.color.black));
    }

    public class ExpandAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        public List<MatchModel> keys;

        public ExpandAdapter(List<MatchModel> keys) {
            super();
            this.keys = keys;
            inflater = LayoutInflater.from(AllKeyMatchActivity.this);
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
            keyBtn.setText(keys.get(position).getName());
            return convertView;
        }
    }

}
