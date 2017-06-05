package com.test.lightcontroller;

import com.test.lightcontroller.util.Converter;

import java.io.Serializable;

/**
 * Created by JaredLee on 4/14/17.
 */

public class Device {
    private String deviceNname = "";
    private int id = 0;
    private byte status = 0;
    private byte brightness = 0;
    public Device (byte[] data){
        byte[] id = {data[4],data[5],data[6],data[7]};
        this.id = Converter.byteArrayToInt(id);
        this.status = data[8];
        this.brightness = data[9];
    }
    public Device(Device device){
        this.deviceNname = device.getName();
        this.id =device.getId();
        this.status = device.getStatus();
        this.brightness = device.getBrightness();
    }
    public int getId(){
        return this.id;
    }
    public void setId(int id){
        this.id = id;
    }
    public String getName(){
        return this.deviceNname;
    }
    public void setName(String name){
        this.deviceNname = name;
    }
    public byte getStatus(){
        return this.status;
    }
    public Device setStatus(byte status){
        this.status = status;
        return this;
    }
    public byte getBrightness(){
        return this.brightness;
    }
    public Device setBrightness(byte brightness){
        this.brightness = brightness;
        return this;
    }

}
