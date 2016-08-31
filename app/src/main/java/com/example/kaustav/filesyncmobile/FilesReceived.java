package com.example.kaustav.filesyncmobile;

import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
    public static final int UPDATE_THUMBNAIL = 2;
    private Timer timer;

    final Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            if(msg.what==ADD_THUMBNAIL){
                imageAdapter.updatWitheImage(String.valueOf(msg.obj));
            }
            super.handleMessage(msg);
            invalidateDefered();
        }
    };

    final Handler handlerUpdate = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            if(msg.what==UPDATE_THUMBNAIL){
                ViewGroup viewGroup = (ViewGroup) findViewById(R.id.rootViewFilesReceived);
                viewGroup.invalidate();
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_received);
        initGridView();
        timer = new Timer();
    }

    private void initGridView(){
        gridView = (GridView) findViewById(R.id.gridview);
        imageAdapter = new ImageAdapter(this);
        gridView.setAdapter(imageAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Toast.makeText(FilesReceived.this, "" + position, Toast.LENGTH_SHORT).show();
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                updateWithImages();
            }
        });
        thread.start();
    }

    private void invalidateDefered(){
        try{
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Message msg = handlerUpdate.obtainMessage();
                    msg.what = UPDATE_THUMBNAIL;
                    handlerUpdate.sendMessage(msg);
                }
            }, 200);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void update(Capsule capsule) {
        if (capsule != null){
            String fileName = String.valueOf(capsule.get("FileName"));
            System.out.println(fileName);
            Message msg = handler.obtainMessage();
            msg.what = ADD_THUMBNAIL;
            msg.obj = Environment.getExternalStorageDirectory() + "/FileSyncMobile" + "/" + fileName;
            handler.sendMessage(msg);
        }
    }

    public void updateWithImages() {
        File file = new File(Environment.getExternalStorageDirectory() + "/FileSyncMobile");
        File[] listOfFiles =  file.listFiles();
        Arrays.sort(listOfFiles, new Comparator<File>(){
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });
        if (listOfFiles.length > 0){
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    Message msg = handler.obtainMessage();
                    msg.what = ADD_THUMBNAIL;
                    msg.obj = listOfFiles[i].getAbsolutePath();
                    handler.sendMessage(msg);
                }
            }
        }
    }

}