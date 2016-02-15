package me.mamx.ble_slave;

/**
 * Created by ctseng on 9/18/15.
 */
public abstract class CommCH_IF {


    abstract boolean RecvData(final Object CH,final  byte[] data);


    boolean SetData(final Object CH,final byte[] data){return false;}


    boolean GetData(final Object CH,final byte[] data){return false;}


    abstract boolean SendData(final Object CH,final byte[] data);

}
