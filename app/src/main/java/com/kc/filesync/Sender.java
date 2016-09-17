package com.kc.filesync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
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

    public boolean isStopped() {
        return isStopped;
    }

    public void setStopped(boolean stopped) {
        isStopped = stopped;
    }

    private boolean isStopped = false;

    public Sender(){
		threads = new ArrayList<>();
	}

	public void sendAsFile(final Uri uriFile){

        fileSizeTemp = 0;

		Thread thread = new Thread(new Runnable() {

			private Uri uri = uriFile;

			@Override
			public void run() {
				try {
					manageFileMetadata(uri);
					sendFileContent(uri);
                    sendEOF();
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
                        if (isStopped){
                            break;
                        }
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

	private boolean sendFileContent(Uri uri){
		boolean bRet = false;
        Capsule capsule = new Capsule();
        int fileLength = Integer.parseInt(capsuleFileMetdata.get("Size"));
		try{
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
			if (null == inputStream){
				throw new FileNotFoundException("Selected file not found in the device.");
			}
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            byte buffer[] = new byte[1024*1024*10 + 1];
			int read;
			Socket sock;
			OutputStream outputStream;
            capsule.set("Status", "ON");
            capsule.set("MODE", "SEND");
            listener.update(capsule);
            while(bis.available() > 0){
                if (isStopped){
                    break;
                }
                buffer[0] = (byte)2;
                int bytesRead = 1;
                int byteOffset = 0;
                while(bytesRead < buffer.length){
                    if (isStopped){
                        break;
                    }
                    byteOffset = bis.read(buffer, bytesRead, buffer.length - bytesRead);
                    if (byteOffset == -1){
                        break;
                    }else{
                        bytesRead += byteOffset;
                    }
                }
                sock = new Socket(destIPAddress, FileSync.PORT);
                outputStream = sock.getOutputStream();
                outputStream.write(buffer, 0, bytesRead);
                sock.close();
                fileSizeTemp += bytesRead;
                double val = ((double)fileSizeTemp/(double)fileLength);
                int progress =  (int)(val * 100);
                capsule.set("Status", null);
                capsule.set("PROGRESS", progress);
                listener.update(capsule);
            }
            capsule.set("Status", "OFF");
            listener.update(capsule);
			bRet = true;
			inputStream.close();
		}catch(Exception ex){
            stop();
			ex.printStackTrace();
		}
		return bRet;
	}

	public void setDestinationIPAddress(String ipaddress) {
		this.destIPAddress = ipaddress;
	}

    private boolean sendEOF(){
        boolean bRet = false;
        try{
            Socket sock = new Socket(destIPAddress, FileSync.PORT);
            OutputStream os = sock.getOutputStream();
            byte[] buffer = new byte[1];
            buffer[0] = 3;
            os.write(buffer);
            sock.close();
            bRet = true;
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return bRet;
    }

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
            sock.close();
            bRet = true;
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
                String size;
                if (!cursor.isNull(sizeIndex)) {
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }
				capsule.set("Size", size);
            }
        } finally {
			if (cursor != null)
            	cursor.close();
        }
        return capsule;
    }

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

    public void sendConnectionInfo(final String conInfo) {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try{
                    Socket sock = new Socket(destIPAddress, FileSync.PORT);
                    OutputStream os = sock.getOutputStream();
                    byte[] buffer = new byte[conInfo.getBytes().length + 1];
                    buffer[0] = 4;
                    byte [] b = conInfo.getBytes();
                    for (int i = 0; i < b.length; i ++){
                        buffer[i+1] = b[i];
                    }
                    os.write(buffer);
                    sock.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }

    public void stopPeering() {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try{
                    Socket sock = new Socket(destIPAddress, FileSync.PORT);
                    OutputStream os = sock.getOutputStream();
                    byte[] buffer = new byte[1];
                    buffer[0] = 5;
                    os.write(buffer);
                    sock.close();
                }catch(Exception e){

                }
            }
        });

        thread.start();

    }

    public void stop() {
        Capsule capsule = new Capsule();
        capsule.set("Status", "OFF");
        listener.update(capsule);
        isStopped = true;
    }
}
