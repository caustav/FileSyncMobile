package com.kc.filesync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FileSync {

	public static final int PORT = 12345;

	public static final int READ_METADATA = 0;
	public static final int WRITE_FILECONTENT = 1;
	public static final int COMMIT_FILE = 2;
	private final FSListener listener;

	private Sender sender = new Sender();
	private Receiver receiver = new Receiver();

	private static final String TAG = "FILE_SYNC";

    private Context context;

	public FileSync(FSListener listener) {
		this.listener = listener;
	}

	public void doSync(Context context){
        this.context = context;
		receiver.setListener(listener);
		receiver.setContext(context);
		receiver.receive();
	}

	public void sendAsFiles(String ipaddress, ArrayList<Uri> uries){
		sender.reset();
		for (Uri uri : uries){
			sender.sendAsFile(uri);
		}
		sender.setListener(listener);
        sender.setContext(context);
		sender.setDestinationIPAddress(ipaddress);
		sender.runDispatcher();
	}

//	public void sendFilePaths(String ipaddress, ArrayList<String> filePaths){
//		for (String file : filePaths){
//			sender.sendFilePath(file);
//		}
//
//		sender.setDestinationIPAddress(ipaddress);
//		sender.runDispatcher();
//	}
//
//	public void sendFileContainers(String ipaddress, ArrayList<FileContainer> files){
//		sender.reset();
//		for (FileContainer file : files){
//			sender.sendAsFileContainer(file);
//		}
//
//		sender.setDestinationIPAddress(ipaddress);
//		sender.runDispatcher();
//	}
//
//	private void sendAllFiles(String ipaddress){
//
//		File folder = new File("/storage/emulated/0/Download/");
//		File[] listOfFiles = folder.listFiles();
//
//		for (int i = 0; i < listOfFiles.length; i++) {
//			if (listOfFiles[i].isFile()) {
//				sender.sendFilePath(listOfFiles[i].getPath());
//				System.out.println("File " + listOfFiles[i].getName());
//			} else if (listOfFiles[i].isDirectory()) {
//				System.out.println("Directory " + listOfFiles[i].getName());
//			}
//		}
//		sender.setDestinationIPAddress(ipaddress);
//		sender.runDispatcher();
//	}

//	public void syncFiles(String ipaddress, ArrayList<Uri> fileUris, Context context){
//		ArrayList<FileContainer> containers = new ArrayList<FileContainer>();
//		for (Uri uri : fileUris){
//			FileContainer container = new FileContainer();
//			FileMetadata fileMetadata = getFileMetaData(uri, context);
//			byte [] content = getFileContentInBytes(uri, context, fileMetadata);
//			container.setMetadata(fileMetadata);
//			container.setContent(content);
//			containers.add(container);
//		}
//		sendFileContainers(ipaddress, containers);
//	}

	private FileMetadata getFileMetaData(Uri uri, Context context) {
		FileMetadata fileMetadata = new FileMetadata();
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {

				String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
				Log.i(TAG, "Display Name: " + displayName);
				fileMetadata.setFileName(displayName);
				int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
				String size = null;
				if (!cursor.isNull(sizeIndex)) {
					size = cursor.getString(sizeIndex);
					fileMetadata.setFileSize(Integer.parseInt(size));
				} else {
					size = "Unknown";
				}
				Log.i(TAG, "Size: " + size);
			}
		} finally {
			cursor.close();
		}

		return fileMetadata;
	}

	private byte[] getFileContentInBytes(Uri uri, Context context, FileMetadata fileMetadata) {
		byte[] byteArray = new byte[fileMetadata.getFileSize()];
		try{
			InputStream inputStream = context.getContentResolver().openInputStream(uri);
			BufferedInputStream bis = new BufferedInputStream(inputStream);
			bis.read(byteArray, 0, byteArray.length);
		}catch (IOException ex){
			ex.printStackTrace();
		}
		return byteArray;
	}

}