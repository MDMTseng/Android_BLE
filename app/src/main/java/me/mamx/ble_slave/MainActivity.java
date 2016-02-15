package me.mamx.ble_slave;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    MainActivity MainAct=this;
    private static final String TAG = MainActivity.class.getCanonicalName();
    boolean isSupportBLESlave=false;
    //TextView TV=null;

    BLEPeripheralMan BLEPMan=null;
    BLECentralMan BLECMan=null;

    WebUIMan WU=null;

    Handler handler=new Handler();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TV=(TextView)findViewById(R.id.text);
        isSupportBLESlave=((BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter().isMultipleAdvertisementSupported();
        //TV.setText(((Boolean)isSupportBLESlave).toString());
        if(isSupportBLESlave)
            BLEPMan=new BLEPeripheralMan(this);

        BLECMan=new BLECentralMan(this);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }




        WU=new WebUIMan((WebView)findViewById(R.id.webView_mainUI),"file:///android_asset/Roles/A_BLE_MUI/index.html"){
            @Override
            public void UIDataRecvCB(String type,JSONObject jsonData)
            {
                if(type.contentEquals("gattProfileJson"))
                {
                    try {
                        BLEPMan.ResetByJsonProf(jsonData.optJSONObject("data"));
                        BLEPMan.StartAdvertising();
                        //BLEMan.
                    } catch (JSONException e) {
                        //e.printStackTrace();
                    }
                }



            }
        };


        if(BLEPMan!=null) {
            WU.setBTSlaveCommIf(BLEPMan.getCommIf());
            BLEPMan.setUICommIf(WU.getCommIf_2BLESlave());

        }

        {
            WU.setBTMasterCommIf(BLECMan.getCommIf());
            BLECMan.setUICommIf(WU.getCommIf_2BLEMaster());
        }



    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //exe shortcircuit
        return WU.OnKeyDown(keyCode, event)|| super.onKeyDown(keyCode, event);
    }


}


