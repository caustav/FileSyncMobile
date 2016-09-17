package com.kc.filesync;

import android.content.Context;
import android.net.Uri;

import java.util.ArrayList;

public class FileSync implements ReaderListener{

	public static final int PORT = 8888;

//	public static final int READ_METADATA = 0;
//	public static final int WRITE_FILECONTENT = 1;
//	public static final int COMMIT_FILE = 2;
    public static final int SYNCING_MODE_SEND = 1;
    public static final int SYNCING_MODE_RECEIVE = 2;

	private final FSListener listener;

	private Sender sender = new Sender();
	private Receiver receiver = new Receiver();

	private static final String TAG = "FILE_SYNC";

    private Context context;

    private ConnectionInfo connectionInfo;

    private int syncingMode;

    public FileSync(FSListener listener) {
		this.listener = listener;
        this.connectionInfo = new ConnectionInfo();
	}

	public void doSync(Context context){
        this.context = context;
		receiver.setListener(listener);
        receiver.setReaderListener(this);
		receiver.setContext(context);
		receiver.receive();

        sender.setListener(listener);
        sender.setContext(context);
	}

	public void sendAsFiles(String ipaddress, ArrayList<Uri> uries){
		sender.reset();
		for (Uri uri : uries){
			sender.sendAsFile(uri);
		}
		sender.setDestinationIPAddress(ipaddress);
		sender.runDispatcher();
	}

	public void sendConnectionInfo(String destIPAddress, String deviceName, String ipAddress) {
		sender.setDestinationIPAddress(destIPAddress);
        this.syncingMode = SYNCING_MODE_SEND;
        connectionInfo.setSenderDeviceName(deviceName);
        connectionInfo.setSenderDeviceIpAddress(ipAddress);
		String s = deviceName + ":" + ipAddress;
		sender.sendConnectionInfo(s);
        sender.setStopped(false);
	}

    @Override
    public void updateConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
        this.syncingMode = SYNCING_MODE_RECEIVE;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public void stop() {
        if (syncingMode == SYNCING_MODE_RECEIVE){
            receiver.stop();
            sender.setDestinationIPAddress(connectionInfo.getSenderDeviceIpAddress());
            sender.stopPeering();
        }else if (syncingMode == SYNCING_MODE_SEND){
            sender.stop();
            sender.stopPeering();
        }
    }

    public void enable(){
        receiver.setStopped(false);
        sender.setStopped(false);
    }
}