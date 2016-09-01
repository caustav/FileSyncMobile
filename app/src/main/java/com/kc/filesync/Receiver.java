package com.kc.filesync;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Receiver {

    private boolean isThreadRunning;

	public boolean isThreadRunning() {
		return isThreadRunning;
	}

	public void setThreadRunning(boolean isThreadRunning) {
		this.isThreadRunning = isThreadRunning;
	}

	private Context context;

	public void setContext(Context context){
		this.context = context;
	}

	private FSListener listener;

    private int fileSizeTemp;

	public void receive(){
		
		isThreadRunning = true; 
		
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					boolean isThreadRunning = true;
					FileMetadata fileMetadata = null;
					try{
						ServerSocket servsock = new ServerSocket(FileSync.PORT);
                        Capsule capsule = new Capsule();
						while(isThreadRunning){
							Socket sock = servsock.accept();
							InputStream is = sock.getInputStream();
							byte[] flag = new byte[1];
							is.read(flag, 0, flag.length);
							if (flag[0] == 1){
                                fileSizeTemp = 0;
                                capsule.set("Status", "ON");
                                listener.update(capsule);
								fileMetadata = new FileMetadata();
								readFileMetadata(is, fileMetadata);
							}else if (flag[0] == 2){
								readFileContent(is, fileMetadata);
							}else if (flag[0] == 3){
                                fileMetadata = null;
                                capsule.set("Status", "OFF");
                                listener.update(capsule);
                            }
							sock.close();
						}
						servsock.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		thread.start();
	}
	private void readFileMetadata(InputStream is, FileMetadata fileMetadata){
		byte[] arrayMetadata = new byte[1024];
		try {
			is.read(arrayMetadata, 0, arrayMetadata.length);
			String str = new String(arrayMetadata, "UTF-8");
			String [] fileInfo = str.split(",");
			if (fileMetadata != null){
				String fName = fileInfo[0].replace("\"", "");
				String fSize = fileInfo[1].replace("\"", "").trim();
				fileMetadata.setFileName(fName);
				fileMetadata.setFileSize(Integer.parseInt(fSize));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readFileContent(InputStream is, FileMetadata fileMetadata){
        Capsule capsule = new Capsule();
		byte[] fileContent = new byte[1024*1024*10];
		try{
            boolean isAppending = true;
            if (fileSizeTemp == 0){
                isAppending = false;
            }
            FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/FileSyncMobile" + "/" + fileMetadata.getFileName(), isAppending);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
            int bytesRead = 0;
            int byteOffset;
			while(bytesRead < fileContent.length){
				byteOffset = is.read(fileContent, bytesRead, fileContent.length - bytesRead);
				if (byteOffset == -1){
					break;
				}else{
					bytesRead += byteOffset;
				}
			}
            fileSizeTemp += bytesRead;
            double val = ((double)fileSizeTemp/(double)fileMetadata.getFileSize());
            int progress =  (int)(val * 100);
            capsule.set("MODE", "RECEIVE");
            capsule.set("PROGRESS", progress);
            listener.update(capsule);
			bos.write(fileContent, 0, bytesRead);
			bos.close();
        }catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public void setListener(FSListener listener) {
		this.listener = listener;
	}
}
