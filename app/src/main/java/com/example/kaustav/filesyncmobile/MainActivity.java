package com.example.kaustav.filesyncmobile;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.kc.filesync.Capsule;
import com.kc.filesync.FSListener;
import com.kc.filesync.FileMetadata;
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
    private static final int REQUEST_READ_WRITE_CAMERA = 89;

    FileSync fileSync = new FileSync(this);

    SharedPreferences prefs = null;

    String ipAddressFromWifi;
    private String destIPAddress;

    private ZXingScannerView mScannerView;

    private ProgressDialog progressBar;

    private int STATUS_ON = 1;
    private int STATUS_OFF = 0;
    private int STATUS_PROGRESS = 2;
    final Handler handlerProgress = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            if(msg.what==STATUS_ON){
                Capsule capsule = (Capsule)msg.obj;
                String s = capsule.get("MODE");
                if (s.equals("SEND")){
                    startProgress("Sending file ...");
                }else{
                    startProgress("Receiving file ...");
                }
            }else if (msg.what==STATUS_OFF){
                progressBar.dismiss();
            }else if (msg.what == STATUS_PROGRESS){
                Capsule capsule = (Capsule)msg.obj;
                String s = capsule.get("PROGRESS");
                progressBar.setProgress(Integer.parseInt(s));
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

        Button share = (Button) findViewById(R.id.buttonShare);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareFiles();
            }
        });

        Button buttonYou = (Button) findViewById(R.id.buttonYou);
        buttonYou.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureQRImage();
            }
        });

        generateQRScreen();

        fileSync.doSync(getApplicationContext());
        prefs = getSharedPreferences("com.example.kaustav.filesyncmobile", MODE_PRIVATE);
        View view = findViewById(R.id.rootView);
        view.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeRight() {
                Intent intent = new Intent(getApplicationContext(), FilesReceived.class);
                startActivity(intent);
            }
        });

        createDirIfNotExists("/FileSyncMobile");
    }

    private void initUI(){
        setContentView(R.layout.activity_main);
        Button share = (Button) findViewById(R.id.buttonShare);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareFiles();
            }
        });

        Button buttonYou = (Button) findViewById(R.id.buttonYou);
        buttonYou.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureQRImage();
            }
        });

        View view = findViewById(R.id.rootView);
        view.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeRight() {
                Intent intent = new Intent(getApplicationContext(), FilesReceived.class);
                startActivity(intent);
            }
        });

        generateQRScreen();
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
        int permissionCheck1 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED || permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                    REQUEST_READ_WRITE_CAMERA);
        }
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
//            fileSync.syncFiles(destIPAddress, fileUris, getApplicationContext());
            fileSync.sendAsFiles(destIPAddress, fileUris);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_READ_WRITE_CAMERA) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                //initGridView();
            }
        }
    }

    private String toIPAddrInString(int ipAddress){
        String ip = null;
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
        try{
            BitMatrix bitMatrix = qrCodeWriter.encode(ipAddressFromWifi, BarcodeFormat.QR_CODE, 500, 500, hintMap);
            int height = bitMatrix.getHeight();
            int width = bitMatrix.getWidth();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++){
                for (int y = 0; y < height; y++){
                    bmp.setPixel(x, y, bitMatrix.get(x,y) ? Color.BLACK : Color.WHITE);
                }
            }
            ImageView imageView = (ImageView) findViewById(R.id.imageViewQR);
            imageView.setImageBitmap(bmp);
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
        Log.e("handler", rawResult.getText()); // Prints scan results
        Log.e("handler", rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode)
        destIPAddress = rawResult.getText();
        if (mScannerView != null){
            mScannerView.stopCamera();
            initUI();
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

//    private void loadThumbnail(String filePath){
//
//        Bitmap bmp = BitmapFactory.decodeFile(filePath);
//        ImageView imageView = new ImageView(getApplicationContext());
//        Bitmap b = ThumbnailUtils.extractThumbnail(bmp, THUMB_WIDTH, THUMB_HEIGHT);
//        imageView.setMinimumWidth(THUMB_WIDTH);
//        imageView.setMinimumHeight(THUMB_HEIGHT);
//        imageView.setScaleType(ImageView.ScaleType.CENTER);
//        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(THUMB_WIDTH, THUMB_HEIGHT);
//        layoutParams.setMargins(currentX, currentY, currentX + THUMB_WIDTH, currentY + THUMB_HEIGHT);
//        imageView.setLayoutParams(layoutParams);
//        imageView.setImageBitmap(ThumbnailUtils.extractThumbnail(bmp, THUMB_WIDTH, THUMB_HEIGHT));
//        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.rootView);
//        viewGroup.addView(imageView);
//        currentX += THUMB_WIDTH + 20;
//        viewGroup.invalidate();
//    }
}
