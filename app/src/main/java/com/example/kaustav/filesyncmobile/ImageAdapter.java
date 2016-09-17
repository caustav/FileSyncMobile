package com.example.kaustav.filesyncmobile;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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

    private ArrayList<String> thumbNails;

    public ImageAdapter(Context c) {
        context = c;
        thumbNails = new ArrayList<String>();
    }

    public int getCount() {
        return thumbNails.size();
    }

    public String getItem(int position) {
        return thumbNails.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(MainActivity.THUMB_WIDTH, MainActivity.THUMB_HEIGHT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }
        String fileName = thumbNails.get(position);
        Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/FileSyncMobile/.thumbnails/" + fileName);
        imageView.setImageBitmap(bitmap);
        return imageView;
    }

    public void updatWitheImage(String filePath){
        thumbNails.add(filePath);
        notifyDataSetChanged();
    }
//
//    private String getRealPathFromURI(Uri contentURI) {
//        String result;
//        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
//        if (cursor == null) { // Source is Dropbox or other similar local file path
//            result = contentURI.getPath();
//        } else {
//            cursor.moveToFirst();
//            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
//            result = cursor.getString(idx);
//            cursor.close();
//        }
//        return result;
//    }
}