package com.kc.filesync;

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
					while(isThreadRunning){
						Socket sock = servsock.accept();
						switch(status){
							case FileSync.READ_METADATA:{
								fileMetadata = new FileMetadata();
								if (!manageFileMetadata(sock, fileMetadata)){
									throw new Exception("Problem in processing metadata");
								}
								status = FileSync.WRITE_FILECONTENT;
								break;
							}
							case FileSync.WRITE_FILECONTENT:{
								if (!manageFileContent(sock, fileMetadata)){
									throw new Exception("Problem in processing content");
								}
								status = FileSync.COMMIT_FILE;
								break;
							}
							case FileSync.COMMIT_FILE:{
								if (!manageCommitFile(sock)){
									throw new Exception("Problem in commiting content");
								}
								status = FileSync.READ_METADATA;
								fileMetadata = null;
							}
						}
						sock.close();
					}
					
					servsock.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
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
		try{
		    byte[] mybytearray = new byte[fileMetadata.getFileSize()];
		    InputStream is = sock.getInputStream();
		    FileOutputStream fos = new FileOutputStream("E:\\Workspace\\Misc\\Temp\\Neon\\" + fileMetadata.getFileName());
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
		    System.out.println(String.valueOf(bytesRead));
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
}
