package com.android.stock;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class FloatingService extends Service {
    public static boolean isStarted = false;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private FloatingHandler mFloatingHandler;

    private Handler mTimeHandler = new Handler();

    private TextView mOptions1;
    private TextView mOptions2;
    private TextView mOptions3;
    private TextView mOptions4;
    private TextView mOptions5;
    private TextView mOptions6;
    private TextView mOptions7;
    private TextView mOptions8;
    private TextView mTime;
    private double mCost = 1.000; //成本价格
    private double mHold = 1.000; //持股数

    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = 350;
        layoutParams.height = 780;
        layoutParams.x = 300;
        layoutParams.y = 300;
        mFloatingHandler = new FloatingHandler();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        String urlType = intent.getStringExtra("urlType");
        String num = intent.getStringExtra("num");
        mCost = intent.getDoubleExtra("cost", 1.000);
        mHold = intent.getDoubleExtra("hold", 1.000);
        String url = urlType + num;


        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mTimeHandler.postDelayed(this, 1000);
                //每隔一段时间要重复执行的代码
                getData(url);
            }
        };
        mTimeHandler.postDelayed(runnable, 1000);

        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            RelativeLayout view = (RelativeLayout) View.inflate(getApplicationContext(), R.layout.suspension, null);
            mOptions1 = view.findViewById(R.id.options1);
            mOptions2 = view.findViewById(R.id.options2);
            mOptions3 = view.findViewById(R.id.options3);
            mOptions4 = view.findViewById(R.id.options4);
            mOptions5 = view.findViewById(R.id.options5);
            mOptions6 = view.findViewById(R.id.options6);
            mOptions7 = view.findViewById(R.id.options7);
            mOptions8 = view.findViewById(R.id.options8);
            mTime = view.findViewById(R.id.time);
            windowManager.addView(view, layoutParams);
            view.setOnTouchListener(new FloatingOnTouchListener());
        }
    }

    public void getData(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String result = response.body().string();

                    int last = result.lastIndexOf("\"");
                    int first = result.indexOf("\"") + 1;
                    if (last == -1) {
                        return;
                    }
                    String body1 = result.substring(first, last);

                    Pattern p = Pattern.compile("\"(.*?)\"");
                    Matcher m = p.matcher(result);
                    while (m.find()) {
                        String body2 = m.group().replace("\"", "");
                    }

                    String[] resultArray = body1.split(",");

                    Message message = new Message();
                    message.obj = resultArray;
                    message.what = 1997;
                    mFloatingHandler.sendMessage(message);

                }
            }
        });
    }

    class FloatingHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1997 && mOptions1 != null && msg.obj != null) {
                String[] resultArray = (String[]) msg.obj;
                String stockName = resultArray[0]; //股票名字
                double data1 = (Double.parseDouble(resultArray[2]) * 1000.000); //昨日收盘价
                double data2 = (Double.parseDouble(resultArray[3]) * 1000.000); //最新价格

                String data3 = resultArray[31]; //最新时间
                mTime.setText("时间 " + data3);

                double data4 = (data2 - data1); //日涨幅 = 开盘价-最新价格 每股收益
                double data44 = (mCost - data2); //总涨幅 = 成本-最新价格 每股收益

                double data5 = data4 / data1; //日涨跌幅度 = 开盘价-最新价格 / 开盘价 %
                double data55 = data44 / mCost; //总涨跌幅度 =（成本-最新价格）/ 成本 %

                double data7 = mCost / 1000.000; //成本价格（元）
                double data8 = data2 / 1000.000; //最新价格（元）

                double data6 = data4 / 1000.000; //日每股涨幅（元）
                double data9 = data44 / 1000.000; //总每股涨幅（元）

                double data10 = (data6 * mHold); //今日盈亏 = 每股涨幅（元） * 持股数
                double data11 = (data9 * mHold); //总盈亏 = 总每股涨幅（元) * 持股数

                mOptions1.setText(stockName);
                mOptions2.setText("涨跌 " + data6);

                //成本不可见
                if (mCost == 1) {
                    mOptions3.setVisibility(View.GONE);
                } else {
                    mOptions3.setVisibility(View.VISIBLE);
                }
                mOptions3.setText("成本 " + data7);
                mOptions4.setText("最新 " + data8);
                //四舍五入保留三位小数
                double increase1 = (double) Math.round(data5 * 1000 * 100) / 1000;
                double increase2 = (double) Math.round(data55 * 1000 * 100) / 1000;
                mOptions5.setText("日浮 " + increase1 + "%");
                mOptions8.setText("总浮 " + increase2 + "%");

                //持股数没有，则不显示收益
                if (mHold == 1 || mCost == 1) {
                    mOptions6.setVisibility(View.GONE);
                    mOptions7.setVisibility(View.GONE);
                    mOptions8.setVisibility(View.GONE);
                } else {
                    mOptions6.setVisibility(View.VISIBLE);
                    mOptions7.setVisibility(View.VISIBLE);
                    mOptions8.setVisibility(View.VISIBLE);
                }
                mOptions6.setText("日盈 " + data10);
                mOptions7.setText("总盈 " + data11);

                //涨跌文字变化
                if (increase1 < 0) {
                    mOptions5.setTextColor(Color.GREEN);
                    mOptions2.setTextColor(Color.GREEN);
                    mOptions8.setTextColor(Color.GREEN);
                    mOptions4.setTextColor(Color.GREEN);
                    mOptions6.setTextColor(Color.GREEN);
                    mOptions7.setTextColor(Color.GREEN);

                }
                if (increase1 == 0) {
                    mOptions5.setTextColor(Color.BLACK);
                    mOptions2.setTextColor(Color.BLACK);
                    mOptions8.setTextColor(Color.BLACK);
                    mOptions4.setTextColor(Color.BLACK);
                    mOptions6.setTextColor(Color.BLACK);
                    mOptions7.setTextColor(Color.BLACK);
                }

                if (increase1 > 0) {
                    mOptions5.setTextColor(Color.RED);
                    mOptions2.setTextColor(Color.RED);
                    mOptions8.setTextColor(Color.RED);
                    mOptions4.setTextColor(Color.RED);
                    mOptions6.setTextColor(Color.RED);
                    mOptions7.setTextColor(Color.RED);
                }

            }
        }
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
