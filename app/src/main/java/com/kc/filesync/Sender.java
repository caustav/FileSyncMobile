package com.kc.filesync;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Sender {
	
	private ArrayList<Thread> threads;
	private String destIPAddress;

	private FSListener listener;
    private int fileSizeTemp = 0;
    private Capsule capsuleFileMetdata;
    private Context context;

    public Sender(){
		threads = new ArrayList<Thread>();
	}
	
//	public void sendFilePath(final String filePath){
//
//		Thread thread = new Thread(new Runnable() {
//
//			private String fPath = filePath;
//
//			@Override
//			public void run() {
//				try {
//					File file = new File(fPath);
//					manageFileMetadata(file);
//					sendFileContent(file);
//					readEOF();
//				    System.out.println("File sent as " + filePath);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//
//		threads.add(thread);
//	}

	public void sendAsFile(final Uri uriFile){

		Thread thread = new Thread(new Runnable() {

			private Uri uri = uriFile;

			@Override
			public void run() {
				try {
					manageFileMetadata(uri);
					sendFileContent(uri);
					//readEOF();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		threads.add(thread);
	}
	
	public void runDispatcher(){
		
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					
					for (Thread th : threads){
						th.start();
						th.join();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		thread.start();
	}
	
//	private boolean manageFileMetadata(File file){
//		boolean bRet = false;
//		try{
//			Socket sock = new Socket(destIPAddress, FileSync.PORT);
//			String fileName = file.getName();
//			String fileSize = String.valueOf(file.length());
//			DataOutputStream dout=new DataOutputStream(sock.getOutputStream());
//			String buffer = fileName + "," + fileSize;
//			dout.writeUTF(buffer);
//			dout.flush();
//			dout.close();
//			sock.close();
//			bRet = true;
//		}catch(Exception ex){
//			ex.printStackTrace();
//
//		}
//		return bRet;
//	}

	private boolean sendFileContent(Uri uri){
		boolean bRet = false;
        Capsule capsule = new Capsule();
        int fileLength = Integer.parseInt(capsuleFileMetdata.get("Size").toString());
		try{
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            BufferedInputStream bis = new BufferedInputStream(inputStream);
//            byte[] byteArray = new byte[1024*1024*10];
//            bis.read(byteArray, 0, byteArray.length);
			byte bufferTemp[] = new byte[1024*1024*10];
            byte buffer[] = new byte[1024*1024*10 + 1];
			int read;
			Socket sock = null;
			OutputStream outputStream = null;
            capsule.set("Status", "ON");
            capsule.set("MODE", "SEND");
            listener.update(capsule);
			while((read = bis.read(buffer, 1, buffer.length - 1)) != -1){
				sock = new Socket(destIPAddress, FileSync.PORT);
				outputStream = sock.getOutputStream();
                buffer[0] = (byte)2;
				outputStream.write(buffer, 0, read);
				outputStream.flush();
                sock.close();
				fileSizeTemp += read;
                double val = ((double)fileSizeTemp/(double)fileLength);
                int progress =  (int)(val * 100);
                capsule.set("Status", null);
                capsule.set("PROGRESS", progress);
                listener.update(capsule);
            }
//		    System.out.println(String.valueOf(bytesRead));
            capsule.set("Status", "OFF");
            listener.update(capsule);
			bRet = true;
			inputStream.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return bRet;
	}
	
	private boolean readEOF(){
		boolean bRet = false;
		DataInputStream dis;
		try {
			Socket sock = new Socket(destIPAddress, FileSync.PORT);
			dis = new DataInputStream(sock.getInputStream());
			String  eof = (String)dis.readUTF();
			System.out.println(eof);
			bRet = true;
			sock.close();
		} catch (IOException e) {
			bRet = false;
			e.printStackTrace();
		}
		return bRet;
	}

//    private boolean receiveAck(Socket sock, String text){
//        boolean bRet = false;
//        DataInputStream dis;
//        try {
//            dis = new DataInputStream(sock.getInputStream());
//            String  ackText = (String)dis.readUTF();
//            if (text.equals(ackText)){
//                bRet = true;
//            }
//            System.out.println(ackText);
//            sock.close();
//        } catch (IOException e) {
//            bRet = false;
//            e.printStackTrace();
//        }
//        return bRet;
//    }

	public void setDestinationIPAddress(String ipaddress) {
		this.destIPAddress = ipaddress;
	}

//	public void sendAsFileContainer(final FileContainer fileContainer) {
//		Thread thread = new Thread(new Runnable() {
//
//			private FileContainer file = fileContainer;
//
//			@Override
//			public void run() {
//				try {
//					manageFileMetadata(file.getMetadata());
//					sendFileContent(file.getContent());
//					readEOF();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//
//		threads.add(thread);
//	}

    private boolean manageFileMetadata(Uri fileUri){
        boolean bRet = false;
        capsuleFileMetdata = getFileMetaData(fileUri,  context);
        try{
            Socket sock = new Socket(destIPAddress, FileSync.PORT);
            String fileName = capsuleFileMetdata.get("Name");
            String fileSize = capsuleFileMetdata.get("Size");
            OutputStream os = sock.getOutputStream();
            String str = fileName + "," + fileSize;
            byte[] buffer = new byte[str.getBytes().length + 1];
            buffer[0] = 1;
            byte [] b = str.getBytes();
            for (int i = 0; i < b.length; i ++){
                buffer[i+1] = b[i];
            }
            os.write(buffer);
//            DataOutputStream dout = new DataOutputStream(sock.getOutputStream());
//            dout.writeUTF(buffer);
//            dout.flush();
//            dout.close();
            sock.close();
            bRet = true;
//            bRet = receiveAck(sock, "METADATA_RECEIVED");;
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return bRet;
    }

    private Capsule getFileMetaData(Uri uri, Context context) {
        Capsule capsule = new Capsule();
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {

                String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                capsule.set("Name", displayName);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                String size = null;
                if (!cursor.isNull(sizeIndex)) {
                    size = cursor.getString(sizeIndex);
                    capsule.set("Size", size);
                } else {
                    size = "Unknown";
                }
            }
        } finally {
            cursor.close();
        }
        return capsule;
    }

//	private boolean manageFileMetadata(FileMetadata fileMetadata){
//		boolean bRet = false;
//		try{
//			Socket sock = new Socket(destIPAddress, FileSync.PORT);
//			String fileName = fileMetadata.getFileName();
//			String fileSize = String.valueOf(fileMetadata.getFileSize());
//			DataOutputStream dout=new DataOutputStream(sock.getOutputStream());
//			String buffer = fileName + "," + fileSize;
//			dout.writeUTF(buffer);
//			dout.flush();
//			dout.close();
//			sock.close();
//			bRet = true;
//		}catch(Exception ex){
//			ex.printStackTrace();
//
//		}
//		return bRet;
//	}
//
//	private boolean sendFileContent(byte[] bytearray){
//		boolean bRet = false;
//		try{
//			Socket sock = new Socket(destIPAddress, FileSync.PORT);
//			OutputStream os = sock.getOutputStream();
//			os.write(bytearray, 0, bytearray.length);
//			os.flush();
//			sock.close();
//		}catch(Exception ex){
//			ex.printStackTrace();
//		}
//		return bRet;
//	}

	public void reset() {
		threads.clear();
	}

	public void setListener(FSListener listener) {
		this.listener = listener;
	}

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
