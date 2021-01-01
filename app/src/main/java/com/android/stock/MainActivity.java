package com.android.stock;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    Button mStart;
    EditText mInputEditText1;
    EditText mInputEditText2;
    EditText mInputEditText3;
    private String mNum;
    private double mCost;
    private double mHold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    public void initView() {
        mStart = findViewById(R.id.start);
        mInputEditText1 = findViewById(R.id.input1);
        mInputEditText2 = findViewById(R.id.input2);
        mInputEditText3 = findViewById(R.id.input3);
    }

    public void initListener() {
        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNum = mInputEditText1.getText().toString().trim();
                String cost = mInputEditText2.getText().toString().trim();
                String hold = mInputEditText3.getText().toString().trim();
                if (cost.equals("")) {
                    mCost = 1.000;
                } else {
                    mCost = Double.parseDouble(cost);
                }

                if (hold.equals("")) {
                    mHold = 1.000;
                } else {
                    mHold = Double.parseDouble(hold);
                }

                if (mNum.equals("")) {
                    Toast.makeText(getApplicationContext(), "不能为空，已启用默认数据", Toast.LENGTH_SHORT);
                    mNum = "sz000100";  //默认代码
                }
                startFloatingService();
            }
        });
    }

    public void startFloatingService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        } else {
            Intent intent = new Intent(MainActivity.this, FloatingService.class);
            intent.putExtra("urlType", "http://hq.sinajs.cn/list=");
            intent.putExtra("num", mNum);
            intent.putExtra("cost", mCost);
            intent.putExtra("hold", mHold);
            startService(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startFloatingService();
            }
        }
    }
}