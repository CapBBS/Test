package com.example.administrator.test1;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Administrator on 2016-05-17.
 */
public class ServerService extends IntentService {

    private boolean serviceEnabled;

    private ResultReceiver serverResult;
    private int port;

    boolean isConnected;

    ServerSocket serverSocket;
    DataInputStream dis;
    DataOutputStream dos;

    Socket socket = null;


    private final IBinder sBinder = new ServerServiceBinder();


    public ServerService() {
        super("ServerService");
        Log.i("TAG", "서버 서비스 생성");
        serviceEnabled = true;
        isConnected = false;
    }

    public class ServerServiceBinder extends Binder{
        ServerService getService(){
            return ServerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sBinder;
    }

    public void onDestroy() {

        serviceEnabled = false;

        stopSelf();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        port = ((Integer) intent.getExtras().get("port")).intValue();
        serverResult = (ResultReceiver) intent.getExtras().get("serverResult");

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            Log.i("TAG", "서버소켓 생성 오류");
        }

        while (serviceEnabled) {

            try {

                Log.i("TAG", "클라이언트 접속 대기중");
                socket = serverSocket.accept();

                String ip = socket.getInetAddress().toString().replaceAll("/", "");

                Log.i("TAG", ip);
                Bundle bundle = new Bundle();
                bundle.putString("client", ip);
                serverResult.send(port, bundle);

                socket.close();
            } catch (IOException e) {
                Log.i ("TAG", "클라이언트 접속 대기중 오류");
                serviceEnabled = false;
            }
        }

    }

}
