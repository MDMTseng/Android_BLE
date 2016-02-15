package me.mamx.ble_slave;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by ctseng on 9/17/15.
 */
public class JsonBLEProfileBuilder {


    JSONObject jObject=null;

    public JsonBLEProfileBuilder(JSONObject gattTable_Json)
    {
        SetJson(gattTable_Json);
        //jObject = new JSONObject();
    }
    public void SetJson(JSONObject gattTable_Json)
    {

        jObject=gattTable_Json;
    }

    public boolean BuildProfile(BluetoothGattServer GattServer) throws JSONException {
        return BuildProfile(GattServer, jObject);
    }

    String ver=null;

    public boolean BuildProfile(BluetoothGattServer GattServer,JSONObject jObject) throws JSONException {
        this.jObject=jObject;

        JSONObject jProfile=null;
        try {
            if(jObject==null)throw new JSONException("null Json");

            ver=jObject.optString("BLEProJsonVer");
            if(ver==null)throw new JSONException("null BLEProJsonVer");


            jProfile=jObject.optJSONObject("profile");
            if(jProfile==null)throw new JSONException("null Profile object");

            JSONArray jservices=jProfile.optJSONArray("services");
            if(jservices==null)throw new JSONException("null Profile services");


            ArrayList<BluetoothGattService> serviceList=new ArrayList<BluetoothGattService>();
            //jObject.getJSONObject();
            for(int i=0;i<jservices.length();i++)
                serviceList.add(BuildService(jservices.getJSONObject(i)));

            for(BluetoothGattService serv:serviceList)
                GattServer.addService(serv);

            return true;
        } catch (JSONException e) {


            e.printStackTrace();
            ver=null;
            throw new JSONException("Json BLE profile formate Error!!");
        }
    }


    public static BluetoothGattService BuildService(JSONObject jService)throws JSONException {

        if(jService==null)throw new JSONException("null Service");

        String uuidStr=jService.optString("uuid");

        UUID uuid=BLEUtils.CompleteUUID(uuidStr);
        if(uuid==null)throw new JSONException("wrong service UUID");


        String id=jService.optString("id");
        String description=jService.optString("description");


        JSONArray jcharas=jService.optJSONArray("characteristics");
        if(jcharas==null)throw new JSONException("null Service charas");


        BluetoothGattService Service= new BluetoothGattService(uuid,BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //Service.
        //jObject.getJSONObject();
        for(int i=0;i<jcharas.length();i++)
        {
            Service.addCharacteristic(BuildChara(jcharas.getJSONObject(i)));

        }

        Log.v("BuildService","======================");




        return Service;
    }
    public static BluetoothGattCharacteristic BuildChara(JSONObject jChara)throws JSONException {

        if(jChara==null)throw new JSONException("null Characteristic");

        String uuidStr=jChara.getString("uuid");

        UUID uuid=BLEUtils.CompleteUUID(uuidStr);
        if(uuid==null)throw new JSONException("wrong chara UUID");


        String id=jChara.optString("id");
        String description=jChara.optString("description");

        JSONArray jproperties=jChara.optJSONArray("properties");




        JSONArray jpermissions=jChara.optJSONArray("permissions");

        int properties=0;
        int permissions=0;
        if(jproperties!=null)for(int i=0;i<jproperties.length();i++)
        {
            properties|= parsePropertyNameForChar(jproperties.getString(i));

        }
        if(properties==0)properties=BluetoothGattCharacteristic.PROPERTY_READ;
        permissions=PermissionRecommendationNameFromCharProperty(properties);

        if(jpermissions!=null) {
            permissions=0;
            for (int i = 0; i < jpermissions.length(); i++) {
                permissions |= parsePermissionNameForChar(jpermissions.getString(i));
            }
        }




        String length=jChara.optString("length");

        String value=jChara.optString("value");



        BluetoothGattDescriptor de;


        BluetoothGattCharacteristic chara=new BluetoothGattCharacteristic(uuid,
                properties,permissions);

        if(value!=null)chara.setValue(value);
        //chara.setWriteType();

        /*JSONArray jdescri=jChara.optJSONArray("descriptors");


        if(jdescri!=null)for(int i=0;i<jdescri.length();i++)
        {
            chara.addDescriptor(BuildDescri(jdescri.getJSONObject(i)));

        }
*/

        Log.v("BuildChara","======================"+value);




        return chara;
    }

    public static int parsePropertyNameForChar(String propName)
    {
        if(propName.contentEquals("read"))return BluetoothGattCharacteristic.PROPERTY_READ;
        if(propName.contentEquals("write"))return BluetoothGattCharacteristic.PROPERTY_WRITE;
        if(propName.contentEquals("indicate"))return BluetoothGattCharacteristic.PROPERTY_INDICATE;
        if(propName.contentEquals("notify"))return BluetoothGattCharacteristic.PROPERTY_NOTIFY;

        //TODO more


        return 0;
    }

    public static int PermissionRecommendationNameFromCharProperty(int Property)
    {
        if((Property&
                (BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_INDICATE|BluetoothGattCharacteristic.PROPERTY_NOTIFY)
        )!=0 )
            return BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE;

        if((Property&
                (BluetoothGattCharacteristic.PROPERTY_WRITE)
        )!=0 )
            return BluetoothGattCharacteristic.PERMISSION_WRITE;

        //TODO more


        return 0;
    }
    public static int parsePermissionNameForChar(String permiName)
    {
        if(permiName.contentEquals("read"))return BluetoothGattCharacteristic.PERMISSION_READ;
        if(permiName.contentEquals("read_MITM"))return BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM;
        if(permiName.contentEquals("read_ENC"))return BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
        if(permiName.contentEquals("write"))return BluetoothGattCharacteristic.PERMISSION_WRITE;
        if(permiName.contentEquals("write_MITM"))return BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM;
        if(permiName.contentEquals("write_ENC"))return BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;

        //TODO more


        return 0;
    }
    public static BluetoothGattDescriptor BuildDescri(JSONObject jDescri)throws JSONException {

        if(jDescri==null)throw new JSONException("null Descriptor");


        BluetoothGattDescriptor descri=null;/*= new BluetoothGattService(
                UUID.fromString(("2800")),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);*/
        //descri=new BluetoothGattDescriptor();




        Log.v("BuildChara","======================");




        return descri;
    }


    public JSONObject ExportJson()
    {


        return null;
    }
}
