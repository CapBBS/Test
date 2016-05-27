package com.example.administrator.test1;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Administrator on 2016-05-17.
 */
public class ClientService extends IntentService {


    private int port;
    InetAddress targetIP;
    ServerSocket serverSocket;
    private Socket socket;

    private Boolean serviceEnabled;
    boolean isConnected;
    DataInputStream dis;
    DataOutputStream dos;

    int action;

    private final IBinder cBinder = new ClientServiceBinder();


    private ResultReceiver clientResult;

    public ClientService() {
        super("ClientService");
        Log.i("TAG", "클라이언트 서비스 시작");
        serviceEnabled = true;
        isConnected = false;
    }

    public class ClientServiceBinder extends Binder {
        ClientService getService() {
            return ClientService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return cBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceEnabled = false;
        stopSelf();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        port = (Integer) intent.getExtras().get("port");
        clientResult = (ResultReceiver) intent.getExtras().get("clientResult");
        try {
            targetIP = InetAddress.getByName("192.168.49.1");
            serverSocket = new ServerSocket(Constants.DATA_SEND_PORT);
        } catch (IOException e) {
            Log.i("TAG", "클라이언트 연결 생성 오류");
        }

        while(!isConnected) {
            try {
                socket = new Socket(targetIP, port);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("TAG", "서버 접속 시도 중 오류");
                break;
            }
                isConnected = socket.isConnected();
        }

        try {
            socket.close();
        } catch (IOException e) {
            Log.i("TAG", "클라이언트 소켓 종료 중 오류");
        }


        while(serviceEnabled) {

            Log.i("TAG", "서버로 부터 접속대기중");
            try {
                socket = serverSocket.accept();

                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                Log.i("TAG", "서버로부터 접속 대기중 오류");
            }

            try {


                action = dis.readInt();

                switch (action) {

                    case Constants.SEND_MUSIC:
                        File musicfile = new File("/storage/emulated/0/Download/a.mp3");

                        byte[] buffer = new byte[4096 * 64];
                        int bytesRead;

                        FileOutputStream fos = new FileOutputStream(musicfile);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);

                        while (true) {
                            bytesRead = dis.read(buffer, 0, buffer.length);
                            if (bytesRead == -1) {
                                break;
                            }
                            bos.write(buffer, 0, bytesRead);
                            bos.flush();
                        }

                        bos.close();
                        fos.close();

                        break;

                    case Constants.SEND_STATE:
                        Log.i("TAG", "state = " + dis.readBoolean());
                        break;

                    case Constants.SEND_POSITION:
                        Log.i("TAG", "position = " + dis.readInt());
                        break;

                }

            } catch (IOException e) {
                Log.i("TAG", "데이터 수신중 에러");
            }

        }


        try {
            dis.close();
            dos.close();
            socket.close();
        } catch (IOException e) {
            Log.i("TAG","클라이언트 자원 정리중 오류");
        }

        Log.i("TAG", "클라이언트 서비스 종료");

    }
}



