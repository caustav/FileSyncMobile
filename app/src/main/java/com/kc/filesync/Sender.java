package com.kc.filesync;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Sender {
	
	private ArrayList<Thread> threads;
	private String destIPAddress;
	
	public Sender(){
		threads = new ArrayList<Thread>();
	}
	
	public void sendFilePath(final String filePath){
		
		Thread thread = new Thread(new Runnable() {
			
			private String fPath = filePath;
			
			@Override
			public void run() {
				try {
					File file = new File(fPath);
					manageFileMetadata(file);
					sendFileContent(file);
					readEOF();
				    System.out.println("File sent as " + filePath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		threads.add(thread);
	}

	public void sendAsFile(final File fileActual){

		Thread thread = new Thread(new Runnable() {

			private File file = fileActual;

			@Override
			public void run() {
				try {
					manageFileMetadata(file);
					sendFileContent(file);
					readEOF();
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
	
	private boolean manageFileMetadata(File file){
		boolean bRet = false;
		try{
			Socket sock = new Socket(destIPAddress, FileSync.PORT);
			String fileName = file.getName();
			String fileSize = String.valueOf(file.length());
			DataOutputStream dout=new DataOutputStream(sock.getOutputStream());  
			String buffer = fileName + "," + fileSize;
			dout.writeUTF(buffer);  
			dout.flush();  
			dout.close();
			sock.close();
			bRet = true;
		}catch(Exception ex){
			ex.printStackTrace();
			
		}
		return bRet;
	}
	
	private boolean sendFileContent(File file){
		boolean bRet = false;
		try{
			Socket sock = new Socket(destIPAddress, FileSync.PORT);
		 	byte[] bytearray = new byte[(int) file.length()];
		    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		    bis.read(bytearray, 0, bytearray.length);
		    OutputStream os = sock.getOutputStream();
		    os.write(bytearray, 0, bytearray.length);
		    bis.close();
		    os.flush();	
		    sock.close();
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

	public void setDestinationIPAddress(String ipaddress) {
		this.destIPAddress = ipaddress;
		
	}

	public void sendAsFileContainer(final FileContainer fileContainer) {
		Thread thread = new Thread(new Runnable() {

			private FileContainer file = fileContainer;

			@Override
			public void run() {
				try {
					manageFileMetadata(file.getMetadata());
					sendFileContent(file.getContent());
					readEOF();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		threads.add(thread);
	}

	private boolean manageFileMetadata(FileMetadata fileMetadata){
		boolean bRet = false;
		try{
			Socket sock = new Socket(destIPAddress, FileSync.PORT);
			String fileName = fileMetadata.getFileName();
			String fileSize = String.valueOf(fileMetadata.getFileSize());
			DataOutputStream dout=new DataOutputStream(sock.getOutputStream());
			String buffer = fileName + "," + fileSize;
			dout.writeUTF(buffer);
			dout.flush();
			dout.close();
			sock.close();
			bRet = true;
		}catch(Exception ex){
			ex.printStackTrace();

		}
		return bRet;
	}

	private boolean sendFileContent(byte[] bytearray){
		boolean bRet = false;
		try{
			Socket sock = new Socket(destIPAddress, FileSync.PORT);
			OutputStream os = sock.getOutputStream();
			os.write(bytearray, 0, bytearray.length);
			os.flush();
			sock.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return bRet;
	}

	public void reset() {
		threads.clear();
	}
}
