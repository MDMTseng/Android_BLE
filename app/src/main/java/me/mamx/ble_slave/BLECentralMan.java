package me.mamx.ble_slave;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by MDM on 2015/5/5.
 */
public class BLECentralMan  {


    BluetoothManager bluetoothManager=null ;
    BluetoothAdapter mBluetoothAdapter=null ;
    ArrayList<BluetoothGatt> connectGATTList=new ArrayList<BluetoothGatt>();

    public void stopAllDevices()
    {
        for(BluetoothGatt gatt:connectGATTList)
        {
            gatt.close();

            Log.v("GetBondedLeDevice", "==================================");
        }
        connectGATTList.clear();
    }



    Context Acti=null;
    void InitInitBLE(Context c)
    {
        Acti=c;
        BluetoothManager bluetoothManager =
                (BluetoothManager) Acti.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        BTCCommIF=new BLECInterCommIf();
    }

    private CommCH_IF UICommIF=null;
    private BLECInterCommIf BTCCommIF=null;


    BLECentralMan(Context c)
    {
        InitInitBLE(c);
    }
    public CommCH_IF getCommIf()
    {
        return BTCCommIF;
    }

    public void setUICommIf(CommCH_IF UICommIF)
    {
        this.UICommIF=UICommIF;
    }


    String Connect2BTDeviceEvID =null;
    String ScanDevDeviceEvID =null;
    String ServiceDiscoverEvID =null;
    public class BLECInterCommIf extends CommCH_IF
    {
        @Override
        public boolean RecvData(final Object CH,final  byte[] data)//From UI to BLE
        {


            if (CH instanceof String)
            {
                String CHID=(String)CH;
                String datStr=(data==null)?null: new String(data);

                if(CHID.contentEquals("ScanResultReq"))
                {
                    return SendScanLeDevice("ScanResult");
                }

                if(CHID.contentEquals("ScanReq"))
                {
                    Log.v("RecvData>>ScanReq","=============="+data[0]+"====================");
                    return scanLeDevice("Scan", data[0] == 0 ? false : true);
                }
                if(CHID.contentEquals("ConnectDevNameReq"))
                {
                    return Connect2BTDevice("ConnectDevName", new String(data));
                }

                if(CHID.contentEquals("ServiceDiscReq"))
                {
                    return ServiceDiscovery("ServiceDisc");
                }


                if(CHID.contentEquals("EnableNotiReq"))
                {
                    return EnableNotification("EnableNoti");
                }



                return true;
            }
            if (CH instanceof Long)
            {

                BluetoothGattCharacteristic chara=GattCharaMap.get(CH);

                if(chara==null)return false;

                Log.v("WWWW>>", Arrays.toString(data)+"<"+data.length);

                if(!chara.setValue(data))return false;
                if(!mBluetoothGatt.writeCharacteristic(chara))return false;
                return true;
            }

            return false;
        }


        @Override
        public boolean SendData(final Object CH,final byte[] data)
        {//from BLE to UI

            if (CH instanceof BluetoothGattCharacteristic) {
                BluetoothGattCharacteristic chara = (BluetoothGattCharacteristic) CH;
                return UICommIF.RecvData(BLEUtils.GetCharaM32bit(chara), data);
            }

            if (CH instanceof String) {
                return UICommIF.RecvData(CH, data);
            }



            return false;
        }
    }



    HashMap<String,BluetoothDevice > scanDevSets=new HashMap();
    HashMap<Long,BluetoothGattCharacteristic> GattCharaMap=new HashMap();

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            int rssi = result.getRssi();
            String Name=result.getDevice().getName();

            if(Name==null)Name="null";


            if(!scanDevSets.containsKey(result.getDevice().getAddress()))

            {
                scanDevSets.put(result.getDevice().getAddress(), result.getDevice());

                BTCCommIF.SendData(ScanDevDeviceEvID, Name.getBytes());
            }
            // do something with RSSI value
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            // a scan error occurred
        }
    };


    private boolean mScanning=false;
    boolean scanLeDevice(String CHID,final boolean enable) {
        ScanDevDeviceEvID =CHID+"Ev";
        //final BluetoothLeScanner BLEScanner=mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {

            scanDevSets.clear();

            mScanning = true;

            BTCCommIF.SendData(CHID + "Rsp", "Scanning".getBytes());
            ScanSettings scanSettings;
            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            scanSettings = scanSettingsBuilder.build();
            List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();

            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters,scanSettings,scanCallback);
            return true;

        } else {
            mScanning = false;

            BTCCommIF.SendData(CHID+"Rsp","Stop".getBytes());
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);


            return true;
        }
    }
    public final static int msgBLEOnSearch =0;
    public final static int msgBLESearchFinal =1;
    public final static int msgGattConnected =2;
    public final static int msgGattServiceDiscovered =3;
    public final static int msgGattServiceDiscoverFailed =4;
    public final static int msgOnCharRead =5;
    public final static int msgOnCharReadFailed =6;
    public final static int msgOnCharNotification =7;



    boolean SendScanLeDevice(final String CHID)
    {

        JSONObject jobj=new JSONObject();
        Log.v("GetBondedLeDevice","==================================");



        for(Map.Entry<String, BluetoothDevice> bds:scanDevSets.entrySet())
        {
            try {
                jobj.put(bds.getValue().getName(),bds.getValue());
            } catch (JSONException e) { e.printStackTrace();}
            //Log.v("GetBondedLeDevice", bd.getName() + "   " + bd.getAddress());
        }
        return BTCCommIF.SendData(CHID+"Rsp",jobj.toString().getBytes());
    }


    BluetoothGatt mBluetoothGatt=null;



    boolean Connect2BTDevice(String CHID,String devName)
    {
        Connect2BTDeviceEvID =CHID+"Ev";
        if(Link2Gatt(devName,false))
        {

            return BTCCommIF.SendData(CHID+"Rsp","Connecting".getBytes());
        }

        return BTCCommIF.SendData(CHID+"Rsp","Error".getBytes());
    }
    boolean Link2Gatt(String deviceName,boolean isAuto)
    {

        /*for(BluetoothDevice bd:mBluetoothAdapter.getBondedDevices())
        {
            if( bd.getName().contentEquals(deviceName)) {
                Link2Gatt(bd, isAuto);
                return true;
            }
        }*/

        for(Map.Entry<String, BluetoothDevice> bds:scanDevSets.entrySet())
        {
            if(bds.getValue().getName()!=null&&bds.getValue().getName().contentEquals(deviceName))
            {
                Link2Gatt(bds.getValue(), isAuto);
                return true;
            }

        }
        //mBluetoothGatt = device.connectGatt(Acti, isAuto, mGattCallback);
        return false;
    }


    void Link2Gatt(BluetoothDevice device,boolean isAuto)
    {
        stopAllDevices();
        mBluetoothGatt = device.connectGatt(Acti, isAuto, mGattCallback);

    }
    boolean EnableNotification(String CHID)
    {
        for(Map.Entry<Long, BluetoothGattCharacteristic> bds:GattCharaMap.entrySet())
        {
            BluetoothGattCharacteristic chara=bds.getValue();
            if((chara.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY)!=0)
            {

                mBluetoothGatt.setCharacteristicNotification(chara, true);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (BluetoothGattDescriptor descriptor : chara.getDescriptors())
                if(descriptor.getUuid().toString().contains("00002902"))
                //important, toggle this descriptor to get Notification
                {

                    BTCCommIF.SendData(CHID+"Rsp",chara.getUuid().toString().getBytes());
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                    //BTCCommIF.SendData(ServiceDiscoverEvID,"Trigger Noti".getBytes());
                    mBluetoothGatt.writeDescriptor(descriptor);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

        }
        return BTCCommIF.SendData(CHID+"Rsp","OK".getBytes());
    }

    boolean ServiceDiscovery(String CHID)
    {
        ServiceDiscoverEvID=CHID+"Ev";
        if(!mBluetoothGatt.discoverServices()) {

            return BTCCommIF.SendData(CHID+"Rsp","Failed".getBytes());
        }

        return BTCCommIF.SendData(CHID+"Rsp","Discovering".getBytes());
    }
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback()
            {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState)
                {
                    String intentAction;

                    connectGATTList.remove(gatt);
                    if (newState == BluetoothProfile.STATE_CONNECTED)
                    {
                        connectGATTList.add(gatt);
                        BTCCommIF.SendData(Connect2BTDeviceEvID, gatt.getDevice().getName().getBytes());
                        //gatt.discoverServices();
                    }
                    else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                    {

                        GattCharaMap.clear();
                        gatt.close();
                        BTCCommIF.SendData(Connect2BTDeviceEvID, "Failed".getBytes());
                    }
                }

                protected  final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

                public boolean setCharacteristicNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicUuid,
                                                             boolean enable) {
                    BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(serviceUuid).getCharacteristic(characteristicUuid);
                    mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

                    return mBluetoothGatt.writeDescriptor(descriptor); //descriptor write operation successfully started?

                }


                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status)
                {
                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        for(BluetoothGattService s:gatt.getServices())
                            for(BluetoothGattCharacteristic chara:s.getCharacteristics())
                            {

                                GattCharaMap.put(BLEUtils.GetCharaM32bit(chara), chara);
                                BTCCommIF.SendData(ServiceDiscoverEvID, (">>" + BLEUtils.GetCharaM32bit(chara)).getBytes());





                                /*
                                */
                            }

                        BTCCommIF.SendData(ServiceDiscoverEvID,"DiscoverFinished...".getBytes());

                    }
                    else
                    {
                        GattCharaMap.clear();
                        gatt.close();
                        BTCCommIF.SendData(ServiceDiscoverEvID, "Failed".getBytes());
                    }
                }
                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status)
                {
                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        BTCCommIF.SendData(characteristic,characteristic.getValue());

                    }else
                    {
                        BTCCommIF.SendData(characteristic,null);
                    }
                }




                int PL=0;

                byte TMPVArr[]={0,0};
                int Xasdas=0;
                int OKCount;
                @Override//get notification
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic)
                {
                    //BTCCommIF.SendData(characteristic,characteristic.getValue());


                    Log.v(">>>>", Arrays.toString(characteristic.getValue())+"<");

                    BTCCommIF.SendData(characteristic, characteristic.getValue());
                }

            };
}