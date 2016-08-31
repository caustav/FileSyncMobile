package com.example.kaustav.filesyncmobile;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Kaustav on 21-Aug-16.
 */
public class ImageAdapter extends BaseAdapter {
    private Context context;

    private ArrayList<Uri> thumbNails;

    public ImageAdapter(Context c) {
        context = c;
        thumbNails = new ArrayList<Uri>();
    }

    public int getCount() {
        return thumbNails.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(MainActivity.THUMB_WIDTH, MainActivity.THUMB_HEIGHT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//            imageView.setPadding(2, 2, 2, 2);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageURI(thumbNails.get(position));
        return imageView;
    }

    public void updatWitheImage(String filePath){
        File file = new File(filePath);
        thumbNails.add(Uri.fromFile(file));
    }
}