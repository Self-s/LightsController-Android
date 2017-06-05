package com.test.lightcontroller;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;


import com.test.lightcontroller.esptouch.EsptouchTask;
import com.test.lightcontroller.esptouch.IEsptouchListener;
import com.test.lightcontroller.esptouch.IEsptouchResult;
import com.test.lightcontroller.esptouch.IEsptouchTask;
import com.test.lightcontroller.esptouch.EspWifiAdminSimple;
import com.test.lightcontroller.esptouch.task.__IEsptouchTask;
import com.test.lightcontroller.util.FoundDevicesListener;
import com.test.lightcontroller.util.StatusChangedListener;
import com.test.lightcontroller.util.UdpErrorListener;
import com.test.lightcontroller.util.UdpServer;
import com.test.lightcontroller.util.ViewHolder;

import java.util.List;


public class DevicesActivity extends AppCompatActivity {

    private ImageView addDevices;
    private ListView devicesList;
    private Spinner sortBy;
    private int clickedBulbId = 0;
    private EspWifiAdminSimple mWifiAdmin;
    private UdpServer udpServer;
    private DevicesAdapter devicesAdapter;
    private CountDownTimer cdt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.devices_layout);
        addDevices = (ImageView) findViewById(R.id.addDevices);
        devicesList = (ListView) findViewById(R.id.devicesList);
        sortBy = (Spinner)findViewById(R.id.sortby);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this,
                R.array.sortby, R.layout.sort_by_item);
        adapter.setDropDownViewResource(R.layout.sort_by_item);
        sortBy.setAdapter(adapter);
        mWifiAdmin = new EspWifiAdminSimple(DevicesActivity.this);
        udpServer = getUdpServer();
        udpServer.findDevices();
        Toast.makeText(DevicesActivity.this,"Searching for available devices...",Toast.LENGTH_LONG).show();
        cdt = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                udpServer.findDevices();
            }
            @Override
            public void onFinish() {
                if(udpServer.devices.size()<1)
                    Toast.makeText(DevicesActivity.this,"Do not find more available devices.If your device is turned on,please configure the network parameters first.",Toast.LENGTH_LONG).show();
            }
        };
        cdt.start();
        devicesAdapter = new DevicesAdapter(this, udpServer.devices);
        devicesList.setAdapter(devicesAdapter);
        udpServer.setStatusChangedLister(new StatusChangedListener() {
            @Override
            public void onStatusChanged(int id, byte status, byte brightness) {
                devicesAdapter.updatData(udpServer.devices,sortBy.getSelectedItemPosition());
            }
        });
        udpServer.setFoundDevicesListener(new FoundDevicesListener() {
            @Override
            public void onFoundaDevice(int id) {
                if(udpServer.devices.get(id)!=null) {
                    SharedPreferences sp = getSharedPreferences("name", Context.MODE_PRIVATE);
                    String name = sp.getString("id_" + id,  (id&0x0ffffffffL) + " Not named yet");
                    udpServer.devices.get(id).setName(name);
                    devicesAdapter.updatData(udpServer.devices,sortBy.getSelectedItemPosition());
                }
            }
        });
        udpServer.setUdpServerListener(new UdpErrorListener() {
            @Override
            public void onError() {
                Toast.makeText(DevicesActivity.this, "Failed to open tunnel port.There may be other app associated with it,please try to restart the app.", Toast.LENGTH_LONG).show();
            }
        });
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                clickedBulbId = ((ViewHolder) (view.getTag())).buldId;
                Intent intent = new Intent(DevicesActivity.this, ControlActivity.class);
                intent.putExtra("bulbId", clickedBulbId);
                startActivity(intent);
            }
        });
        sortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                devicesAdapter.updatData(udpServer.devices,i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        devicesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                clickedBulbId = ((ViewHolder) (view.getTag())).buldId;
                LayoutInflater factory = LayoutInflater.from(DevicesActivity.this);//提示框
                final View v = factory.inflate(R.layout.change_name, null);
                final EditText changeName = (EditText) v.findViewById(R.id.changeName);
                Device device = udpServer.devices.get(clickedBulbId);
                if (!(device.getName()).equals("")) {
                    changeName.setText(device.getName());
                }
                new AlertDialog.Builder(DevicesActivity.this)
                        .setTitle("Change the device alias")//提示框标题
                        .setView(v)
                        .setPositiveButton("Confirm",//提示框的两个按钮
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        String newName = changeName.getText().toString();
                                        if (newName.trim().equals(""))
                                            newName = (clickedBulbId&0x0ffffffffL) + " Not named yet";
                                        SharedPreferences sp = getSharedPreferences("name", Context.MODE_PRIVATE);
                                        sp.edit().putString("id_" + clickedBulbId, newName).apply();
                                        udpServer.devices.get(clickedBulbId).setName(newName);
                                        devicesAdapter.updatData(udpServer.devices,sortBy.getSelectedItemPosition());

                                    }
                                }).setNegativeButton("Cancel", null).create().show();
                return true;
            }
        });
        addDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater factory = LayoutInflater.from(DevicesActivity.this);//提示框
                final View v = factory.inflate(R.layout.esptouch, null);
                final EditText ssid = (EditText) v.findViewById(R.id.ssid);
                final EditText wifipw = (EditText) v.findViewById(R.id.wifipw);
                final String apSsid = mWifiAdmin.getWifiConnectedSsid();
                if (apSsid != null) {
                    ssid.setText(apSsid);
                } else {
                    ssid.setText("");
                }
                new AlertDialog.Builder(DevicesActivity.this)
                        .setTitle("Setting up a wireless network")//提示框标题
                        .setView(v)
                        .setPositiveButton("Confirm",//提示框的两个按钮
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        boolean isApSsidEmpty = TextUtils.isEmpty(apSsid);
                                        if (!isApSsidEmpty) {
                                            String apSsid = ssid.getText().toString();
                                            String apPassword = wifipw.getText().toString();
                                            String apBssid = mWifiAdmin.getWifiConnectedBssid();
                                            String taskResultCountStr = Integer.toString(1);
                                            new EsptouchAsyncTask().execute(apSsid, apBssid, apPassword, taskResultCountStr);
                                        } else {
                                            Toast.makeText(DevicesActivity.this, "Make sure your phone is connected to wifi!", Toast.LENGTH_LONG).show();
                                        }

                                    }
                                }).setNegativeButton("Cancel", null).create().show();
            }
        });
    }

    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String text = result.getInetAddress() + " is connected to the wifi！";
                Toast.makeText(DevicesActivity.this, text,
                        Toast.LENGTH_LONG).show();
                udpServer.findDevices();
            }

        });
    }

    private UdpServer getUdpServer() {
        return ((MainApplication) getApplication()).getUdpServer();
    }

    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };

    private class EsptouchAsyncTask extends AsyncTask<String, Void, List<IEsptouchResult>> {

        private ProgressDialog mProgressDialog;

        private IEsptouchTask mEsptouchTask;
        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private final Object mLock = new Object();

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(DevicesActivity.this);
            mProgressDialog
                    .setMessage("Device is configuring, please wait for a moment...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {
                        if (__IEsptouchTask.DEBUG) {
                            Log.i("DEBUG", "progress dialog is canceled");
                        }
                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    "Waiting..", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            mProgressDialog.show();
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(false);
        }

        @Override
        protected List<IEsptouchResult> doInBackground(String... params) {
            int taskResultCount = -1;
            synchronized (mLock) {
                // !!!NOTICE
                String apSsid = mWifiAdmin.getWifiConnectedSsidAscii(params[0]);
                String apBssid = params[1];
                String apPassword = params[2];
                String taskResultCountStr = params[3];
                taskResultCount = Integer.parseInt(taskResultCountStr);
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, DevicesActivity.this);
                mEsptouchTask.setEsptouchListener(myListener);
            }
            List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
            return resultList;
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(true);
            mProgressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(
                    "Confirm");
            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    for (IEsptouchResult resultInList : result) {
                        sb.append("Connection succeeded! ");
                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
                    if (count < result.size()) {
                        sb.append((result.size())
                                + " connections.\n");
                    }
                    mProgressDialog.setMessage(sb.toString());
                } else {
                    mProgressDialog.setMessage("Connection failed");
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        udpServer = getUdpServer();
        cdt.start();
        Log.i("TAG", "onResume called.");
    }
}
