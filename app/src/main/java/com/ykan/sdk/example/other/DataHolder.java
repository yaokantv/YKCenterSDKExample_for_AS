package com.ykan.sdk.example.other;

import com.yaokan.sdk.model.RemoteControl;

public class DataHolder {
    private static volatile DataHolder instance = null;

    private DataHolder() {
    }

    public static DataHolder getInstance() {
        if (instance == null) {
            synchronized (DataHolder.class) {
                if (instance == null) {
                    instance = new DataHolder();
                }
            }
        }

        return instance;
    }

    private RemoteControl object;

    public void putExtra(RemoteControl object) {
        this.object = object;
    }

    public RemoteControl getExtra() {
        RemoteControl extra = object;
        this.object = null;
        return extra;
    }
}
