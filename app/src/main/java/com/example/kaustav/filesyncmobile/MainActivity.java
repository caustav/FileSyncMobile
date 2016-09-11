package com.example.kaustav.filesyncmobile;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.kc.filesync.Capsule;
import com.kc.filesync.FSListener;
import com.kc.filesync.FileSync;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements  FSListener, ZXingScannerView.ResultHandler {

    private static final int READ_REQUEST_CODE = 42;
//    private static final int READ_QR_CODE = 43;

    public static final int THUMB_WIDTH = 200;
    public static final int THUMB_HEIGHT = 180;
    private static final int REQUEST_READ_WRITE_CAMERA_BT = 89;

    FileSync fileSync = new FileSync(this);

    SharedPreferences prefs = null;

    private String ipAddressFromWifi;
    private String destIPAddress;
    private String destDeviceName;

    private ZXingScannerView mScannerView;
    private ProgressDialog progressBar;
    FloatingActionMenu materialDesignFAM;
    FloatingActionButton floatingActionConnect;
    FloatingActionButton floatingActionFiles;
    FloatingActionButton floatingActionGallery;

    TextView connectionInfo;
    ImageView imageViewQR;
    Button buttonDisconnect;

    private int STATUS_ON = 1;
    private int STATUS_OFF = 0;
    private int STATUS_PROGRESS = 2;
    private int STATUS_CONNECTED = 3;
    private int STATUS_DISCONNECTED = 4;
    private int STATUS_COMPLETE = 5;

    int notificationNumber = 0;

    final Handler handlerProgress = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            if(msg.what==STATUS_ON){
                Capsule capsule = (Capsule)msg.obj;
                String s = capsule.get("MODE");
                if (s.equals("SEND")){
                    startProgress("Sending file ...");
                }else if (s.equals("RECEIVE")){
                    startProgress("Receiving file ...");
                }
            }else if (msg.what==STATUS_OFF){
                if (progressBar != null){
                    progressBar.dismiss();
                }
            }else if (msg.what == STATUS_PROGRESS){
                Capsule capsule = (Capsule)msg.obj;
                String s = capsule.get("PROGRESS");
                progressBar.setProgress(Integer.parseInt(s));
            }else if(msg.what == STATUS_CONNECTED){
                Capsule capsule = (Capsule)msg.obj;
                manageConnectionUI(capsule);
            }else if(msg.what == STATUS_DISCONNECTED){
                Capsule capsule = (Capsule)msg.obj;
                manageConnectionUI(null);
            }else if (msg.what == STATUS_COMPLETE){
                if (progressBar != null){
                    progressBar.dismiss();
                }
                Capsule capsule = (Capsule)msg.obj;
                String s = capsule.get("FileName");
                buildNotification("Halver", s + " Received");
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiManager  wifimanger = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifimanger.isWifiEnabled() == true){
            WifiInfo info = wifimanger.getConnectionInfo();
            ipAddressFromWifi = toIPAddrInString(info.getIpAddress());
        }

        initUI();

        fileSync.doSync(getApplicationContext());
        prefs = getSharedPreferences("com.example.kaustav.filesyncmobile", MODE_PRIVATE);

        createDirIfNotExists("/FileSyncMobile");
    }

    private void initUI(){
        setContentView(R.layout.activity_main);
        initButtons();
        generateQRScreen();
        connectionInfo = (TextView)findViewById(R.id.textViewConnectionInfo);
        connectionInfo.setVisibility(View.GONE);
    }

    private void initTouch(){
        View view = findViewById(R.id.rootView);
        view.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeRight() {
                Intent intent = new Intent(getApplicationContext(), FilesReceived.class);
                startActivity(intent);
            }
        });
    }

    private void initButtons(){
        materialDesignFAM = (FloatingActionMenu) findViewById(R.id.material_design_android_floating_action_menu);
        floatingActionConnect = (FloatingActionButton) findViewById(R.id.connect);
        floatingActionFiles = (FloatingActionButton) findViewById(R.id.files);
        floatingActionGallery = (FloatingActionButton) findViewById(R.id.gallery);

        floatingActionConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                captureQRImage();

            }
        });
        floatingActionFiles.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                shareFiles();

            }
        });
        floatingActionGallery.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FilesReceived.class);
                startActivity(intent);
            }
        });

        buttonDisconnect = (Button) findViewById(R.id.buttonDisconnect);
        buttonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manageConnectionUI(null);
                fileSync.stop();
            }
        });
        buttonDisconnect.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (prefs.getBoolean("firstrun", true) && currentapiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            checkPermission();
        }

        prefs.edit().putBoolean("firstrun", false).commit();
    }

    public boolean createDirIfNotExists(String path) {
        boolean ret = true;

        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                ret = false;
            }
        }
        return ret;
    }

    private void checkPermission(){
//        int permissionCheck1 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
//        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
//                    REQUEST_READ_WRITE_CAMERA_BT);
//        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, 
                        Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_PRIVILEGED},
                REQUEST_READ_WRITE_CAMERA_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            ArrayList<Uri> fileUris = new ArrayList<Uri>();
            if (data.getData() instanceof Uri){
                fileUris.add(uri);
            }else{
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item path = clipData.getItemAt(i);
                    fileUris.add(path.getUri());
                }
            }
            fileSync.sendAsFiles(destIPAddress, fileUris);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_READ_WRITE_CAMERA_BT) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                //initGridView();
            }
        }
    }

    private String toIPAddrInString(int ipAddress){
        String ip;
        ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        return ip;
    }

    @Override
    public void update(Capsule capsule) {

        if (capsule != null){
            Message msg = handlerProgress.obtainMessage();
            if (capsule.get("Status") != null && capsule.get("Status").equals("ON")){
                msg.what = STATUS_ON;
                msg.obj = capsule;
                handlerProgress.sendMessage(msg);
            }else if (capsule.get("Status") != null && capsule.get("Status").equals("OFF")){
                msg.what = STATUS_OFF;
                msg.obj = capsule;
                handlerProgress.sendMessage(msg);
            }else if (capsule.get("Status") != null && capsule.get("Status").equals("CONNECTED")){
                msg.what = STATUS_CONNECTED;
                msg.obj = capsule;
                handlerProgress.sendMessage(msg);
            }else if (capsule.get("Status") != null && capsule.get("Status").equals("DISCONNECTED")){
                msg.what = STATUS_DISCONNECTED;
                msg.obj = capsule;
                handlerProgress.sendMessage(msg);
            }else if (capsule.get("Status") != null && capsule.get("Status").equals("COMPLETE")){
                msg.what = STATUS_COMPLETE;
                msg.obj = capsule;
                handlerProgress.sendMessage(msg);
            }else if(capsule.get("PROGRESS") != null){
                msg.what = STATUS_PROGRESS;
                msg.obj = capsule;
                handlerProgress.sendMessage(msg);
            }
        }

    }

    private void shareFiles(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void generateQRScreen(){
        Hashtable hintMap = new Hashtable();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        String qrText = ipAddressFromWifi + ":" + getPhoneName();
        try{
            BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, 500, 500, hintMap);
            int height = bitMatrix.getHeight();
            int width = bitMatrix.getWidth();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++){
                for (int y = 0; y < height; y++){
                    bmp.setPixel(x, y, bitMatrix.get(x,y) ? Color.BLACK : Color.WHITE);
                }
            }
            imageViewQR = (ImageView) findViewById(R.id.imageViewQR);
            imageViewQR.setImageBitmap(bmp);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScannerView != null){
            mScannerView.stopCamera();
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        String qrTetx = rawResult.getText();
        String str[] = qrTetx.split(":");
        if (str.length > 1){
            destIPAddress = str[0];
            destDeviceName = str[1];
            fileSync.sendConnectionInfo(destIPAddress, getPhoneName(), ipAddressFromWifi);
            if (mScannerView != null){
                mScannerView.stopCamera();
                initUI();
            }
            Capsule capsule = new Capsule();
            capsule.set("CONNECTION-INFO", destDeviceName);
            manageConnectionUI(capsule);
        }
    }

    public void captureQRImage(){
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();         // Start camera
    }

    private void startProgress(String s){
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setMessage(s);
        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressBar.show();
    }

    private String getPhoneName(){
        BluetoothAdapter device = BluetoothAdapter.getDefaultAdapter();
        String deviceName = device.getName();
        return deviceName;
    }

    private void manageConnectionUI(Capsule capsule){
        if (capsule != null) {
            String s = capsule.get("CONNECTION-INFO");
            s = "Now you are connected to " + s;
            imageViewQR.setVisibility(View.GONE);
            connectionInfo.setVisibility(View.VISIBLE);
            connectionInfo.setText(s);
            buttonDisconnect.setVisibility(View.VISIBLE);
        }else {
            imageViewQR.setVisibility(View.VISIBLE);
            connectionInfo.setVisibility(View.GONE);
            buttonDisconnect.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mScannerView != null && (mScannerView.isEnabled() || mScannerView.isActivated())){
            mScannerView.stopCamera();
            initUI();
        }
    }

    private void buildNotification(String title, String text){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.connect)
                        .setContentTitle(title)
                        .setContentText(text);

        int mNotificationId = notificationNumber ++;
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }
}
