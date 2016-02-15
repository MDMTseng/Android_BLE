package me.mamx.ble_slave;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by ctseng on 9/21/15.
 */
public class WebUIMan {

    public final static long SYSInfo =-999;
    WebView webView=null;
    WebAppInterface jsIF_2BLESlaveMan=null;
    WebAppInterface jsIF_2BLEMasterMan=null;
    CommCH_IF BTMasterCommIf=null;
    CommCH_IF BTSlaveCommIf=null;

    enum IncommingDataWhich{
        BTData
    };

    WebUIMan(WebView webView,String EntryUrl)
    {
        this.webView=webView;
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                Log.v(this.getClass().getName(), "onPageFinished!!!");
                // notiTimerHandler.postDelayed(notiTimerRunnable, 2);
                //Message msg=msgHandler.obtainMessage();
                // msg.obj="Hello Web...";
                //msg.what=0;

                // msgHandler.sendMessage(msg);
                // do your stuff here
            }
        });


        webView.loadUrl(EntryUrl);



    }
    void setBTMasterCommIf(CommCH_IF BTCommIf)
    {
        this.BTMasterCommIf=BTCommIf;
        webView.addJavascriptInterface(jsIF_2BLEMasterMan = new WebAppInterface(BTMasterCommIf), "JsIF_BLEMaster");
    }

    void setBTSlaveCommIf(CommCH_IF BTCommIf)
    {
        this.BTSlaveCommIf=BTCommIf;
        webView.addJavascriptInterface(jsIF_2BLESlaveMan = new WebAppInterface(BTSlaveCommIf), "JsIF_BLESlave");
    }

    CommCH_IF getCommIf_2BLEMaster()
    {
        return jsIF_2BLEMasterMan;
    }
    CommCH_IF getCommIf_2BLESlave()
    {
        return jsIF_2BLESlaveMan;
    }


    private Handler msgHandler = new Handler( ){
        @Override
        public void handleMessage(Message inputMessage) {

            IncommingDataWhich which=IncommingDataWhich.values()[inputMessage.what];
            switch(which) {
                case BTData:
                    webView.loadUrl("javascript:InterblueScope.AndroidRECVData('"+(String)inputMessage.obj+"')");
                    break;
            }
        }
    };

    public void UIDataRecvCB(String type,JSONObject jsonData)
    {

    }

    public class WebAppInterface extends CommCH_IF {

        /** Instantiate the interface and set the context */

        CommCH_IF CHto=null;
        WebAppInterface(CommCH_IF CHto) {
            this.CHto=CHto;
        }

        final static long failedReadOpt=-66489646514L;

        @JavascriptInterface
        public void SendMsg2BT(String DATA) {
            Log.v(this.getClass().getName(),DATA);
            try {
                JSONObject jobj=new JSONObject(DATA);
                String dataType=jobj.optString("type");
                if(dataType==null||dataType.length()==0||dataType.contentEquals("gattInfo"))
                {

                    Long uuid16 = jobj.optLong("chid", failedReadOpt);
                    Object CHID=null;


                    if(uuid16==failedReadOpt)
                    {

                        CHID = jobj.optString("chid", null);
                    }
                    else
                        CHID=uuid16;

                    if(CHID==null)return;
                    String base64Data=jobj.optString("base64Data");
                    JSONArray arr=jobj.optJSONArray("uint8Arr");
                    if(base64Data.length()!=0) {
                        byte[] base64Datas = Base64.decode(jobj.getString("base64Data"), Base64.DEFAULT);
                        Log.v("base64Data",base64Datas.length+"<<<< "+(char)base64Datas[0]);
                        SendData(CHID, base64Datas);
                    }
                    else if(arr!=null)
                    {
                        byte []bArr=new byte[arr.length()];
                        Log.v("uint8Arr", arr.length() + "<<<<");
                        for(int i=0;i<bArr.length;i++)
                        {
                            bArr[i]=(byte)arr.optInt(i);
                        }
                        SendData(CHID, bArr);
                    }
                    else

                        SendData(CHID, null);



                }
                else{
                    UIDataRecvCB(dataType, jobj);
                }







            } catch (JSONException e) {
                e.printStackTrace();
            }

        }


        @Override
        public boolean RecvData(final Object CH,final  byte[] data)//may form other thread
        {

            Message msg=msgHandler.obtainMessage();

            String str=Base64.encodeToString(data, Base64.DEFAULT);
            Log.v("CommCH_IF..RecvData", str);
            String jsonStr=null;

            if(CH instanceof String)
            {
                jsonStr="{\"chid\":\""+CH+"\",\"base64Data\":\""+str+"\"}";
            }
            else
            {
                jsonStr="{\"chid\":"+CH+",\"base64Data\":\""+str+"\"}";
            }



            msg.obj=jsonStr;

            msg.what=IncommingDataWhich.BTData.ordinal();

            msgHandler.sendMessage(msg);
            return true;
        }




        @Override
        public boolean SendData(final Object CH,final byte[] data){
            return CHto.RecvData(CH,data);
        }
    }



    public boolean OnKeyDown(int keyCode, KeyEvent event)
    {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_BACK:
                    if(webView.canGoBack()){
                        webView.goBack();
                    }else{
                    }
                    return true;
            }

        }
        return false;
    }
}
