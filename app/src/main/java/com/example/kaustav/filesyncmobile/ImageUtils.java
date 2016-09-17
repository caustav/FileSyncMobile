package com.example.kaustav.filesyncmobile;

import android.content.Context;
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

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Kaustav on 17-Sep-16.
 */
public class ImageUtils {

    private static final int THUMBNAIL_MAX_WIDTH = 300;
    private static final int THUMBNAIL_MAX_HEIGHT = 300;

    public static void setContext(Context context) {
        ImageUtils.context = context;
    }

    private static Context context;

    public static void createThumbnail(String fileName){
        String filePath = Environment.getExternalStorageDirectory() + "/FileSyncMobile" + "/" + fileName;
        Bitmap bitmap = getThumbnailBitmap(filePath);
        saveImageToInternalStorage(bitmap, fileName);
    }

    private static Bitmap getThumbnailBitmap(String path){
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
            String fileName = getFileNameFromPath(path);
            if (fileName!= null && fileName.length() > 10){
                fileName = fileName.substring(0, 10);
                fileName += "..";
            }else if (fileName == null){
                fileName = "Unknown";
            }
            canvas.drawText(fileName, bmpOri.getWidth()/2 , bmpOri.getHeight()/2, paintText);

            if (isMusic(path)){
                Drawable drawble = context.getResources().getDrawable(R.drawable.play1);
                Bitmap bmpPlay = ((BitmapDrawable) drawble).getBitmap();
                if (bmpPlay != null)
                    canvas.drawBitmap(bmpPlay, bitmap.getWidth()/2 - bmpPlay.getWidth()/2,
                            bitmap.getHeight()/2 - bmpPlay.getHeight()/2, null);
            }
        }
        return bitmap;
    }

    private static Bitmap getBitmap(String path){
        return decodeBitmapFromFilePath(path, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
    }

    private static Bitmap decodeBitmapFromFilePath(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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

    private static String getFileNameFromPath(String filePath){
        String filePathComp[] = filePath.split("/");
        return filePathComp[filePathComp.length - 1];
    }

    private static boolean isMusic(String path) {
        boolean bRet = false;
        String extn = getFileExtensionFromPath(path);
        if (extn.compareToIgnoreCase("mp3") == 0){
            bRet = true;
        }
        return bRet;
    }

    private static boolean isVideo(String path){
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

    public static String getFileExtensionFromPath(String filePath){
        int i = filePath.lastIndexOf('.');
        String extension = "";
        if (i > 0) {
            extension = filePath.substring(i + 1);
        }
        return extension;
    }

    private static boolean saveImageToInternalStorage(Bitmap bitmap, String fileName) {

        try {
            FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/FileSyncMobile/.thumbnails/" + fileName));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fos);
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
