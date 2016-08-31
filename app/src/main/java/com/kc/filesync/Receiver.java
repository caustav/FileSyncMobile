package com.kc.filesync;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
					ServerSocket servsock = new ServerSocket(FileSync.PORT);
					System.out.println("Server listening at " + String.valueOf(FileSync.PORT));
					int status = FileSync.READ_METADATA;
					FileMetadata fileMetadata = null;
					Capsule capsule = new Capsule();
					while(isThreadRunning){
						Socket sock = servsock.accept();
						switch(status){
							case FileSync.READ_METADATA:{
                                fileSizeTemp = 0;
								capsule.set("Status", "ON");
                                capsule.set("MODE", "RECEIVE");
                                listener.update(capsule);
								fileMetadata = new FileMetadata();
								if (!manageFileMetadata(sock, fileMetadata)){
									throw new Exception("Problem in processing metadata");
								}
								status = FileSync.WRITE_FILECONTENT;
								break;
							}
                            case FileSync.WRITE_FILECONTENT:{
                                InputStream is = sock.getInputStream();
                                if (is.available() > 0){
                                    if (!manageFileContent(sock, fileMetadata)){
                                        throw new Exception("Problem in processing content");
                                    }
                                }else{
                                    if (!manageCommitFile(sock)){
                                        throw new Exception("Problem in commiting content");
                                    }
                                    status = FileSync.READ_METADATA;
                                    fileMetadata = null;
                                    capsule.set("Status", "OFF");
                                    listener.update(capsule);
                                }
                                break;
                            }
						}
						sock.close();
					}
					
					servsock.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		thread.start();
	}
	
	private boolean manageFileMetadata(Socket s, FileMetadata fileMetadata){
		boolean bRet = false;
		DataInputStream dis;
		try {
			dis = new DataInputStream(s.getInputStream());
			String  buffer = (String)dis.readUTF();
			String [] fileInfo = buffer.split(",");
			if (fileMetadata != null){
				fileMetadata.setFileName(fileInfo[0]);
				fileMetadata.setFileSize(Integer.parseInt(fileInfo[1]));
			}
			bRet = true;
		} catch (IOException e) {
			bRet = false;
			e.printStackTrace();
		}
		return bRet;
	}
	
	private boolean manageFileContent(Socket sock, FileMetadata fileMetadata){
		boolean bRet = false;
		Capsule capsule = new Capsule();
		try{
		    byte[] mybytearray = new byte[1024*1024*10];
		    InputStream is = sock.getInputStream();
            boolean isAppending = true;
            if (fileSizeTemp == 0){
                isAppending = false;
            }
		    FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/FileSyncMobile" + "/" + fileMetadata.getFileName(), isAppending);
		    BufferedOutputStream bos = new BufferedOutputStream(fos);
		    int bytesRead = is.read(mybytearray, 0, mybytearray.length);
		    if (bytesRead == -1){
		    	bytesRead = 0;
		    }
		    int byteOffset = -1;
		    while(bytesRead < mybytearray.length){
		    	byteOffset = is.read(mybytearray, bytesRead, mybytearray.length - bytesRead);
		    	if (byteOffset == -1){
		    		break;	
		    	}else{
		    		bytesRead += byteOffset;
		    	}
		    }
            fileSizeTemp += bytesRead;
			double val = ((double)fileSizeTemp/(double)fileMetadata.getFileSize());
            int progress =  (int)(val * 100);
//		    System.out.println(String.valueOf(bytesRead));
			capsule.set("PROGRESS", progress);
			listener.update(capsule);
		    bos.write(mybytearray, 0, bytesRead);
		    bos.close();
		    bRet = true;
		}catch(Exception ex){
			ex.printStackTrace();
			bRet = false;
		}
		return bRet;
	}
	
	private boolean manageCommitFile(Socket sock){
		boolean bRet = false;
		try{
			DataOutputStream dout=new DataOutputStream(sock.getOutputStream());  
			dout.writeUTF("FILE_COMMIT");  
			dout.flush();  
			dout.close();  
			bRet = true;
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return bRet;
	}

	public void setListener(FSListener listener) {
		this.listener = listener;
	}
}
