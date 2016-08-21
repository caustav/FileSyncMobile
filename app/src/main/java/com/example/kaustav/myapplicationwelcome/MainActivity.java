package com.example.kaustav.myapplicationwelcome;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kc.filesync.FSListener;
import com.kc.filesync.FileSync;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements FSListener {

    private static final int READ_REQUEST_CODE = 42;
    private static final int ADD_THUMBNAIL = 1;

    private static final int THUMB_WIDTH = 100;
    private static final int THUMB_HEIGHT = 100;

    private int currentX = 0;
    private int currentY = 40;

    FileSync fileSync = new FileSync(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button browse = (Button) findViewById(R.id.browse);
        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });

        TextView tvIPAddressSelf = (TextView) findViewById(R.id.textViewSelf);

        WifiManager  wifimanger = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifimanger.isWifiEnabled() == true){
            WifiInfo info = wifimanger.getConnectionInfo();
            tvIPAddressSelf.setText(toIPAddrInString(info.getIpAddress()));
        }
        fileSync.doSync(getApplicationContext());
        loadThumbnailsFromDirectory();
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

            EditText editText = (EditText) findViewById(R.id.editText);
            String destIPAddress = editText.getText().toString().trim();
            fileSync.syncFiles(destIPAddress, fileUris, getApplicationContext());
        }
    }

    private String toIPAddrInString(int ipAddress){
        String ip = null;
        ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        return ip;
    }

    @Override
    public void update(Intent intent) {
        if (intent != null){
            String fileName = intent.getStringExtra("FileName");
            System.out.println(fileName);
            Message msg = handler.obtainMessage();
            msg.what = ADD_THUMBNAIL;
            msg.obj = fileName;
            handler.sendMessage(msg);
        }
    }

    final Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            if(msg.what==ADD_THUMBNAIL){
                loadThumbnail(String.valueOf(getFilesDir() + "/" + msg.obj));
            }
            super.handleMessage(msg);
        }
    };

    private void loadThumbnail(String filePath){

        Bitmap bmp = BitmapFactory.decodeFile(filePath);
        ImageView imageView = new ImageView(getApplicationContext());
        Bitmap b = ThumbnailUtils.extractThumbnail(bmp, THUMB_WIDTH, THUMB_HEIGHT);
        imageView.setMinimumWidth(THUMB_WIDTH);
        imageView.setMinimumHeight(THUMB_HEIGHT);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(THUMB_WIDTH, THUMB_HEIGHT);
        layoutParams.setMargins(currentX, currentY, currentX + THUMB_WIDTH, currentY + THUMB_HEIGHT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageBitmap(ThumbnailUtils.extractThumbnail(bmp, THUMB_WIDTH, THUMB_HEIGHT));
        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.rootView);
        viewGroup.addView(imageView);
        currentX += THUMB_WIDTH + 20;
        viewGroup.invalidate();
    }

    private void loadThumbnailsFromDirectory(){
        File[] listOfFiles = getFilesDir().listFiles();
        if (listOfFiles.length > 0){
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    loadThumbnail(listOfFiles[i].getPath());
                    System.out.println("File " + listOfFiles[i].getName());
                } else if (listOfFiles[i].isDirectory()) {
                    System.out.println("Directory " + listOfFiles[i].getName());
                }
            }
        }
    }
}
