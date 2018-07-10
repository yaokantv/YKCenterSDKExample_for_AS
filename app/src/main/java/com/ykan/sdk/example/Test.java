package com.ykan.sdk.example;

import com.yaokan.sdk.model.KeyCode;
import com.yaokan.sdk.model.RcDevice;
import com.yaokan.sdk.model.RemoteControl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 将遥控器对象转化为Serializable
 */
public class Test extends RcDevice implements Serializable {

    public void trans(RemoteControl control) {
        this.beRmodel = control.getBeRmodel();
        this.code = control.getCode();
        //...如此类推
        Iterator<Map.Entry<String, KeyCode>> it = control.getRcCommand().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, KeyCode> en = it.next();
            NewKeyCode newKeyCode = new NewKeyCode();
            newKeyCode.setSrcCode(en.getValue().getSrcCode());
            //....如此类推
            rcCommand.put(en.getKey(), newKeyCode);
        }
    }

    public HashMap<String, NewKeyCode> getRcCommand() {
        return rcCommand;
    }

    private HashMap<String, NewKeyCode> rcCommand;

    public static class NewKeyCode implements Serializable {
        private String kn;
        private String learnCode;
        private String srcCode;
        private String shortCode;
        private int order;

        public String getSrcCode() {
            return srcCode;
        }

        public void setSrcCode(String srcCode) {
            this.srcCode = srcCode;
        }
    }
}
