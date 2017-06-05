package com.test.lightcontroller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.test.lightcontroller.util.ViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by JaredLee on 4/14/17.
 */

public class DevicesAdapter  extends BaseAdapter {

    private LayoutInflater mInflater;
    private ArrayList<Device> devices = new ArrayList<>();
    public DevicesAdapter(Context context, HashMap<Integer,Device> devices) {
        this.mInflater = LayoutInflater.from(context);
        Iterator iter = devices.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            this.devices.add((Device) entry.getValue());
        }
    }
    public void updatData(HashMap<Integer,Device> devices,int i){
        this.devices.clear();
        Iterator iter = devices.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            this.devices.add((Device) entry.getValue());
        }
        if(i==0){
            Collections.sort(this.devices, new Comparator<Device>() {
                @Override
                public int compare(Device d1, Device d2) {
                    return d1.getName().compareTo(d2.getName());
                }
            });
        }else{
            Collections.sort(this.devices, new Comparator<Device>() {
                @Override
                public int compare(Device d1, Device d2) {
                    if(d1.getId()<d2.getId()) return 1;
                    else return -1;
                }
            });
        }
        notifyDataSetChanged();
    }
    @Override
    public int getCount(){
        return this.devices.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.device, null);
            holder = new ViewHolder();

            holder.status = (ImageView) convertView.findViewById(R.id.status);
            holder.text = (TextView) convertView.findViewById(R.id.bulbName);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }
        Device device = devices.get(position);
        if(device.getStatus()==(byte)0xff)
            holder.status.setImageResource(R.mipmap.status_on);
        else
            holder.status.setImageResource(R.mipmap.status_off);
        holder.text.setText(device.getName());
        holder.buldId = device.getId();

        return convertView;
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

}
