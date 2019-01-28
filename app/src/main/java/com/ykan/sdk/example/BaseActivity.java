package com.ykan.sdk.example;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

    }

    protected void toast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    protected void toast(int text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    public Dialog show(String msg) {
        return show(msg, null);
    }

    public Dialog show(String msg, DialogInterface.OnClickListener ok) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).setMessage(msg).
                setPositiveButton("ok", ok).create();
        alertDialog.show();
        return alertDialog;
    }
}
