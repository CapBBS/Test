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

        serverResult = (ResultReceiver) intent.getExtras().get(Constants.RESULT_RECEIVER);

        try {
            serverSocket = new ServerSocket(Constants.CONNECT_PORT);
        } catch (IOException e) {
            Log.i("TAG", "서버소켓 생성 오류");
        }

        Bundle result = new Bundle();

        while (serviceEnabled) {

            try {

                Log.i("TAG", "클라이언트 접속 대기중");
                socket = serverSocket.accept();

                String ip = socket.getInetAddress().toString().replaceAll("/", "");

                Log.i("TAG", ip);

                result.putString(Constants.ADDRESS, ip);
                serverResult.send(Constants.CLIENT_ADDRESS_SEND, result);

                result.clear();

                socket.close();
            } catch (IOException e) {
                Log.i ("TAG", "클라이언트 접속 대기중 오류");
                serviceEnabled = false;
            }
        }

    }

}
