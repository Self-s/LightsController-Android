package com.test.lightcontroller.util;

/**
 * Created by JaredLee on 5/13/17.
 */

public interface StatusChangedListener {
     void onStatusChanged(int id,byte status,byte brightness);
}
