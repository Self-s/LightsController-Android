package com.test.lightcontroller;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.test.lightcontroller.util.Converter;
import com.test.lightcontroller.util.UdpServer;


public class ControlActivity extends AppCompatActivity {

    private ImageView switchButton, bulbStatus, devicesList;
    private TextView bulbName;
    private RelativeLayout controlLayout;
    private SeekBar brightnessSeekBar;
    private int currentBulbId = 0;
    private UdpServer udpServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        switchButton = (ImageView) findViewById(R.id.switchButton);
        bulbStatus = (ImageView) findViewById(R.id.bulbStatus);
        devicesList = (ImageView) findViewById(R.id.devicesList);
        bulbName = (TextView) findViewById(R.id.title);
        controlLayout = (RelativeLayout) findViewById(R.id.controlLayout);
        brightnessSeekBar = (SeekBar) findViewById(R.id.brightnessSeekBar);
        Intent intent = getIntent();
        currentBulbId = intent.getIntExtra("bulbId", 0);
        udpServer = ((MainApplication) getApplication()).getUdpServer();
        Device device = udpServer.getDevice(currentBulbId);
        if (device != null) {
            bulbName.setText(device.getName());
            byte brightness = device.getBrightness();
            byte status = device.getStatus();
            if (status == (byte) 0x00) {
                controlLayout.setVisibility(View.INVISIBLE);
                bulbStatus.setImageResource(R.mipmap.off);
            } else {
                controlLayout.setVisibility(View.VISIBLE);
                if (brightness < 50)
                    bulbStatus.setImageResource(R.mipmap.on_p1);
                else if (brightness < 100)
                    bulbStatus.setImageResource(R.mipmap.on_p50);
                else bulbStatus.setImageResource(R.mipmap.on_p100);
                brightnessSeekBar.setProgress(brightness);
            }
        }
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (controlLayout.getVisibility() == View.VISIBLE) {
                    controlLayout.setVisibility(View.INVISIBLE);
                    bulbStatus.setImageResource(R.mipmap.off);
                    if (udpServer.getDevice(currentBulbId) != null) {
                        udpServer.sendData(
                                Converter.intToByteArray(UdpServer.ADJUST),
                                Converter.intToByteArray(currentBulbId),
                                (byte) 0x00, (byte) 0x00
                        );

                    }

                } else {
                    controlLayout.setVisibility(View.VISIBLE);
                    int brightness = brightnessSeekBar.getProgress();
                    if (brightness < 20)
                        brightness = 20;
                    if (brightness < 50)
                        bulbStatus.setImageResource(R.mipmap.on_p1);
                    else if (brightness < 90)
                        bulbStatus.setImageResource(R.mipmap.on_p50);
                    else bulbStatus.setImageResource(R.mipmap.on_p100);
                    if (udpServer.getDevice(currentBulbId) != null) {
                        udpServer.sendData(
                                Converter.intToByteArray(UdpServer.ADJUST),
                                Converter.intToByteArray(currentBulbId),
                                (byte) 0xff, (byte) brightness
                        );

                    }
                }

            }
        });
        devicesList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();

            }
        });
        brightnessSeekBar.setMax(90);
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i < 20)
                    i = 20;
                if (i < 50) {
                    bulbStatus.setImageResource(R.mipmap.on_p1);
                } else if (i < 90) {
                    bulbStatus.setImageResource(R.mipmap.on_p50);
                } else bulbStatus.setImageResource(R.mipmap.on_p100);
                if (udpServer.getDevice(currentBulbId) != null) {
                    udpServer.sendData(
                            Converter.intToByteArray(UdpServer.ADJUST),
                            Converter.intToByteArray(currentBulbId),
                            (byte) 0xff, (byte) i
                    );

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i("TAG", "onStart called.");
    }

    //Activity从后台重新回到前台时被调用
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("TAG", "onRestart called.");

    }

    //Activity创建或者从被覆盖、后台重新回到前台时被调用
    @Override
    protected void onResume() {
        super.onResume();

        Log.i("TAG", "onResume called.");
    }


    //Activity被覆盖到下面或者锁屏时被调用
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("TAG", "onPause called.");
        //有可能在执行完onPause或onStop后,系统资源紧张将Activity杀死,所以有必要在此保存持久数据
    }

    //退出当前Activity或者跳转到新Activity时被调用
    @Override
    protected void onStop() {
        super.onStop();
        Log.i("TAG", "onStop called.");
    }

    //退出当前Activity时被调用,调用之后Activity就结束了
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("TAG", "onDestory called.");
    }


}
