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
    private static final int THUMBNAIL_MAX_WIDTH = 300;
    private static final int THUMBNAIL_MAX_HEIGHT = 300;
    private Context context;

    private ArrayList<Uri> thumbNails;

    public ImageAdapter(Context c) {
        context = c;
        thumbNails = new ArrayList<Uri>();
    }

    public int getCount() {
        return thumbNails.size();
    }

    public Uri getItem(int position) {
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

        imageView.setImageBitmap(getThumbnailBitmap(getRealPathFromURI(thumbNails.get(position)), position));
        return imageView;
    }

    public void updatWitheImage(String filePath){
        File file = new File(filePath);
        thumbNails.add(Uri.fromFile(file));
        notifyDataSetChanged();
    }

    private Bitmap getThumbnailBitmap(String path, int position){
        Bitmap bitmap;
        if ((bitmap = getBitmap(path)) != null) {
            // This is an image file.
        }
        else if (isVideo(path)){
            bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
            Canvas canvas = new Canvas(bitmap);
            Drawable drawble = context.getResources().getDrawable(R.drawable.play1);
            Bitmap bmpPlay = ((BitmapDrawable) drawble).getBitmap();
            if (bmpPlay != null)
                canvas.drawBitmap(bmpPlay, bitmap.getWidth()/2 - bmpPlay.getWidth()/2,
                    bitmap.getHeight()/2 - bmpPlay.getHeight()/2, null);
        }else {
            Bitmap bmpOri = BitmapFactory.decodeResource(context.getResources(), R.drawable.defaultfile);
            bitmap = Bitmap.createBitmap(bmpOri.getWidth(), bmpOri.getHeight(),  bmpOri.getConfig());
            Canvas canvas = new Canvas(bitmap);
            Paint paintText = new Paint();
            paintText.setColor(Color.GRAY);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(30);
            canvas.drawBitmap(bmpOri, 0, 0, null);
            String fileName = getFileNameFromUri(thumbNails.get(position));
            if (fileName!= null && fileName.length() > 10){
                fileName = fileName.substring(0, 10);
                fileName += "..";
            }else if (fileName == null){
                fileName = "Unknown";
            }
            canvas.drawText(fileName, bmpOri.getWidth()/2 , bmpOri.getHeight()/2, paintText);

            if (isMusic(thumbNails.get(position))){
                Drawable drawble = context.getResources().getDrawable(R.drawable.play1);
                Bitmap bmpPlay = ((BitmapDrawable) drawble).getBitmap();
                if (bmpPlay != null)
                    canvas.drawBitmap(bmpPlay, bitmap.getWidth()/2 - bmpPlay.getWidth()/2,
                            bitmap.getHeight()/2 - bmpPlay.getHeight()/2, null);
            }
        }
        return bitmap;
    }

    private boolean isMusic(Uri uri) {
        boolean bRet = false;
        String extn = getFileExtensionFromUri(uri);
        if (extn.compareToIgnoreCase("mp3") == 0){
            bRet = true;
        }
        return bRet;
    }

    private boolean isVideo(String path){
        boolean isVideo;
        try{
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            String hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
            isVideo = "yes".equals(hasVideo);
        }catch(Exception ex){
            isVideo = false;
        }
        return isVideo;
    }

    private Bitmap getBitmap(String path){
        return decodeBitmapFromFilePath(path, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public Bitmap decodeBitmapFromFilePath(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private String getFileNameFromUri(Uri uri){
        String filePath = uri.getPath();
        String filePathComp[] = filePath.split("/");
        return filePathComp[filePathComp.length - 1];
    }

    private String getFileExtensionFromUri(Uri uri){
        String filePath = uri.getPath();
        int i = filePath.lastIndexOf('.');
        String extension = "";
        if (i > 0) {
            extension = filePath.substring(i + 1);
        }
        return extension;
    }
}