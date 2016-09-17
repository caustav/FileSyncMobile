package com.example.kaustav.filesyncmobile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.kc.filesync.Capsule;
import com.kc.filesync.FSListener;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

public class FilesReceived extends AppCompatActivity implements FSListener {

    private ImageAdapter imageAdapter;
    private GridView gridView;
    private static final int ADD_THUMBNAIL = 1;
//    public static final int UPDATE_THUMBNAIL = 2;
    private Timer timer;

    final Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            if(msg.what==ADD_THUMBNAIL){
                imageAdapter.updatWitheImage(String.valueOf(msg.obj));
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_received);
        timer = new Timer();
        gridView = (GridView) findViewById(R.id.gridview);
        imageAdapter = new ImageAdapter(this);
        gridView.setAdapter(imageAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                //Toast.makeText(FilesReceived.this, "" + position, Toast.LENGTH_SHORT).show();
                openFile(position);
            }
        });

        updateWithImages();
    }

    private void openFile(int position){
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        String fileName = imageAdapter.getItem(position);
        String filePath = Environment.getExternalStorageDirectory() + "/FileSyncMobile/" + fileName;
        Uri fileUri = Uri.fromFile(new File(filePath));
        String mimeType = myMime.getMimeTypeFromExtension(ImageUtils.getFileExtensionFromPath(filePath));
        newIntent.setDataAndType(fileUri, mimeType);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            getApplicationContext().startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), "No handler for this type of file.", Toast.LENGTH_LONG).show();
        }
    }

//    private void invalidateDefered(){
//        try{
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    Message msg = handlerUpdate.obtainMessage();
//                    msg.what = UPDATE_THUMBNAIL;
//                    handlerUpdate.sendMessage(msg);
//                }
//            }, 200);
//        }catch (Exception ex){
//            ex.printStackTrace();
//        }
//    }

    @Override
    public void update(Capsule capsule) {
        if (capsule != null){
            String fileName = String.valueOf(capsule.get("FileName"));
            Message msg = handler.obtainMessage();
            msg.what = ADD_THUMBNAIL;
//            msg.obj = Environment.getExternalStorageDirectory() + "/FileSyncMobile" + "/" + fileName;
            msg.obj = fileName;
            handler.sendMessage(msg);
        }
    }

    public void updateWithImages() {

        File file = new File(Environment.getExternalStorageDirectory() + "/FileSyncMobile/.thumbnails");
        final File[] listOfFiles =  file.listFiles();

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < listOfFiles.length; i ++){
                    Capsule capsule = new Capsule();
                    capsule.set("FileName", listOfFiles[i].getName());
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    update(capsule);
                }
            }
        });
        thread.start();
    }
}
