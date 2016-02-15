package me.mamx.ble_slave;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * Created by ctseng on 10/8/15.
 */
public class BLEUtils {
    public static long GetCharaM32bit(BluetoothGattCharacteristic chara)
    {
        long KK=chara.getUuid().getMostSignificantBits()>>32;
        return (KK)&(0xFFFFFFFFL);
    }


    public static String GP_XX (String SubUUID_32b)
    {
        if(SubUUID_32b.length()!=4)return null;
        return "b5f9"+SubUUID_32b+"-aa8d-11e3-9046-0002a5d5c51b";
    }

    public static String UUID16b (String SubUUID_32b)
    {
        if(SubUUID_32b.length()!=4)return null;
        return "0000"+SubUUID_32b+"-0000-1000-8000-00805f9b34fb";
    }

    public static String UUID128b_NoDash (String SubUUID_128b)
    {
        if(SubUUID_128b.length()!=32)return null;
        return SubUUID_128b.substring(0,7)+"-"+SubUUID_128b.substring(8,11)+"-"+SubUUID_128b.substring(12,15)+"-"+SubUUID_128b.substring(16,19)+"-"+SubUUID_128b.substring(20,31);
    }

    public static UUID CompleteUUID(String sUUID)
    {
        if(sUUID.startsWith("0x"))
            sUUID= UUID16b(sUUID.substring(2));
        else if(sUUID.length()==4)
            sUUID= UUID16b(sUUID);
        else if(sUUID.startsWith("GP-"))
            sUUID= GP_XX(sUUID.substring(3));
        else if(sUUID.length()==32)
            sUUID= UUID128b_NoDash(sUUID);

        try
        {
            return UUID.fromString(sUUID);
        }
        catch(Exception e)
        {
            return null;
        }
    }
    public static String CompleteUUIDStr(String sUUID)
    {
        UUID uuid=CompleteUUID(sUUID);

        if(uuid==null)return null;

        return uuid.toString();


    }

}
