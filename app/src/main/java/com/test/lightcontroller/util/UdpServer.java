package com.test.lightcontroller.util;

import android.os.Handler;
import android.os.Message;

import com.test.lightcontroller.Device;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by JaredLee on 4/14/17.
 */

public class UdpServer {
    public final static int ADJUST = 0xAAAAAAAA;
    private final static int ACKNOWLEDGE = 0xBBBBBBBB;
    private final static int LOOKUP = 0xCCCCCCCC;
    private final static int FOUND = 0xDDDDDDDD;
    public DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private DatagramPacket packet;
    private int localPort = 5001;
    private int remotePort = 5000;
    private Handler dataHandler;
    private ExecutorService SendDataThreadExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService receiveDataThreadExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService instructThreadExecutor = Executors.newFixedThreadPool(5);
    public HashMap<Integer, Device> devices = new HashMap<>();
    private StatusChangedListener statusChangedListener;
    private FoundDevicesListener foundDevicesListener;
    private UdpServerListener udpServerListener;
    private UdpErrorListener udpErrorListener;

    public UdpServer() {
        statusChangedListener = new StatusChangedListener() {
            @Override
            public void onStatusChanged(int id, byte status, byte brightness) {

            }
        };
        foundDevicesListener = new FoundDevicesListener() {
            @Override
            public void onFoundaDevice(int id) {

            }
        };
        udpErrorListener = new UdpErrorListener() {
            @Override
            public void onError() {

            }
        };
        dataHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Device tmp = (Device) msg.obj;
                Device device = devices.get(tmp.getId());
                switch (msg.what) {
                    case ACKNOWLEDGE:
                        if (device != null) {
                            device.setBrightness(tmp.getBrightness());
                            device.setStatus(tmp.getStatus());
                        } else
                             devices.put(tmp.getId(), new Device(tmp));
                        statusChangedListener.onStatusChanged(tmp.getId(), tmp.getStatus(), tmp.getBrightness());
                        break;
                    case FOUND:
                        if (device != null) {
                            device.setBrightness(tmp.getBrightness());
                            device.setStatus(tmp.getStatus());
                        } else
                            devices.put(tmp.getId(), new Device(tmp));
                        foundDevicesListener.onFoundaDevice(tmp.getId());
                        break;
                }
            }
        };
        try {
            serverAddress = InetAddress.getByName("255.255.255.255");
            udpSocket = new DatagramSocket(localPort);
            udpSocket.setBroadcast(true);
            udpServerListener = new UdpServerListener(udpSocket);
            receiveDataThreadExecutor.execute(udpServerListener);
        } catch (Exception e) {
            udpErrorListener.onError();
        }
    }

    public void reStart() {
        try {
            if (udpSocket != null) {
                udpSocket.close();
                udpSocket = null;
            }
            serverAddress = InetAddress.getByName("255.255.255.255");
            udpSocket = new DatagramSocket(localPort);
            udpSocket.setBroadcast(true);
            udpServerListener = new UdpServerListener(udpSocket);
            receiveDataThreadExecutor.execute(udpServerListener);

        } catch (Exception e) {
            udpErrorListener.onError();
        }

    }

    public boolean isRunning() {
        if (udpSocket != null)
            return udpSocket.isBound();
        else return false;
    }

    public void close() {
        try {
            udpSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setStatusChangedLister(StatusChangedListener statusChangedLister) {
        this.statusChangedListener = statusChangedLister;
    }

    public void setFoundDevicesListener(FoundDevicesListener foundDevicesListener) {
        this.foundDevicesListener = foundDevicesListener;
    }

    public void setUdpServerListener(UdpErrorListener udpServerListener) {
        this.udpErrorListener = udpServerListener;
    }

    public void sendData(byte[] header, byte[] id, byte status, byte brightness) {
        SendDataThreadExecutor.execute(new SendRunable(header, id, status, brightness));
    }

    public Device getDevice(int id) {
        return devices.get(id);
    }

    public void findDevices() {
        for (int i = 0; i < 3; i++)
            sendData(
                    Converter.intToByteArray(LOOKUP),
                    Converter.intToByteArray(0xffffffff),
                    (byte) 0xff, (byte) 0x00);
    }

    class UdpServerListener implements Runnable {
        public Boolean IsThreadDisable = false;
        public DatagramSocket udpServer;

        public UdpServerListener(DatagramSocket udpServer) {
            this.udpServer = udpServer;
        }


        @Override
        public void run() {

            byte[] data = new byte[12];
            DatagramPacket datagramPacket = new DatagramPacket(data,
                    data.length);
            try {
                while (!IsThreadDisable) {
                    udpSocket.receive(datagramPacket);
                    instructThreadExecutor.execute(new ReceivedRunable(datagramPacket.getData(), dataHandler));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ReceivedRunable implements Runnable {
        private byte[] data;
        private Handler handler;

        public ReceivedRunable(byte[] data, Handler handler) {
            this.data = data;
            this.handler = handler;
        }

        @Override
        public void run() {
            byte[] CRC = new byte[]{
                    (byte) (data[0] & data[2] & data[4] & data[6] & data[8]),
                    (byte) (data[1] | data[3] | data[5] | data[7] | data[9])
            };
            if (CRC[0] == data[10] && CRC[1] == data[11]) {
                Message message = Message.obtain();
                byte[] header = {data[0], data[1], data[2], data[3]};
                switch (Converter.byteArrayToInt(header)) {
                    case ACKNOWLEDGE:
                        message.what = ACKNOWLEDGE;
                        break;
                    case FOUND:
                        message.what = FOUND;
                        break;
                }
                message.obj = new Device(data);
                message.setTarget(handler);
                message.sendToTarget();
            }
        }
    }

    class SendRunable implements Runnable {
        private byte[] header;
        private byte[] id;
        private byte status;
        private byte brightness;

        public SendRunable(byte[] header, byte[] id, byte status, byte brightness) {
            this.header = header;
            this.id = id;
            this.status = status;
            this.brightness = brightness;
        }

        @Override
        public void run() {
            try {
                byte[] data = new byte[]{
                        header[0], header[1], header[2], header[3],
                        id[0], id[1], id[2], id[3],
                        status, brightness, 0, 0
                };
                data[10] = (byte) (data[0] & data[2] & data[4] & data[6] & data[8]);
                data[11] = (byte) (data[1] | data[3] | data[5] | data[7] | data[9]);
                packet = new DatagramPacket(data, 12, serverAddress, remotePort);
                udpSocket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
