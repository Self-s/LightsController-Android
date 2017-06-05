package com.test.lightcontroller;

import android.app.Application;
import android.util.Log;

import com.test.lightcontroller.util.CrashHandler;
import com.test.lightcontroller.util.UdpServer;

/**
 * Created by JaredLee on 5/14/17.
 */

public class MainApplication extends Application {
    private UdpServer udpServer;


    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler= CrashHandler.getInstance();
        crashHandler.init(this);
        udpServer = new UdpServer();
        Log.i("TAG", "onCreate called.");
    }


    public UdpServer getUdpServer() {
        if (udpServer != null) {
            Log.i("***getUdpServer***", "status:" + udpServer.isRunning());
            if (udpServer.isRunning()) {
                Log.i("***getUdpServer***", "running");
                return udpServer;
            } else {
                Log.i("***getUdpServer***", "restart");
                udpServer.reStart();
                return udpServer;
            }
        } else {
            udpServer = new UdpServer();
            return udpServer;
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (udpServer != null) {
            if (udpServer.udpSocket != null) {
                udpServer.close();
                udpServer = null;
            }
            udpServer = null;
        }
        Log.i("TAG", "onTerminate called.");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i("TAG", "onLowMemory called.");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.i("TAG", "onTrimMemory called.");
    }
}
