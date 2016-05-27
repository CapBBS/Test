package com.example.administrator.test1;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import app.minimize.com.seek_bar_compat.SeekBarCompat;

/**
 * Created by Administrator on 2016-05-12.
 */
public class MainActivity extends ActivityGroup {
    public Button btnFindpeer;  //피어찾기 버튼
    Button stopPlayBtn, backMusicBtn, frontMusicBtn; // 뒤로가기 재생 앞으로가기 버튼
    SeekBarCompat seekbar; // 시크바
    MediaPlayer music = null; // 현재 재생되는 MediaPlayer
    ListView lvFileControl; // 공유할 음악 리스트
    ImageView mimage; // 앨범이미지 출력할 이미지뷰
    int Marg, Lleng = 0;
    ListView peerListview;
    ArrayList<String> deviceNameList;

    boolean finishFlag = false; // 뒤로가기 버튼 클릭시
    private List mFileList = new ArrayList(); // List뷰에서 음악파일 리스트
    private List mList = new ArrayList();    //   위와 같음.
    private File Musicfolder1 = new File(Environment.getExternalStorageDirectory() + "/Music", "");  // 뮤직폴더에서 찾기위해
    private File Musicfolder2 = new File(Environment.getExternalStorageDirectory() + "/Download", ""); // 다운로드폴더에서 찾기위해
    private static final String[] FTYPE = {"mp3", "wav"}; // 찾는타입 (.mp3 , .wav)형식 찾음
    private static String file_nm = null;  //음악파일의 uri를 string으로 받음

    ArrayList<String> clientIpList;

    File sendtofile;

    private WifiManager wifiMgr;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private IntentFilter wifiP2pIntentFilter;
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    protected WifiP2pDeviceList wifiP2pDeviceList;
    NotificationManager nmanager;

    private int currentpos = 100;
    private boolean firstclientmusicstart;
    private boolean musicState;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private ClientService cService;

    private ServerService sService;

    private ServiceConnection sConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //ServerService.ServerServiceBinder sbinder = (ServerService.ServerServiceBinder) service;
            //sService = sbinder.getService();
            //sService.registerCallback(sCallback);

            sService = ((ServerService.ServerServiceBinder)service).getService();

            Log.i("TAG", "ServerService Bind Success");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sService = null;
        }
    };

    private ServiceConnection cConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // ClientService.ClientServiceBinder binder = (ClientService.ClientServiceBinder) service;
            //  cService = binder.getService();
            // cService.registerCallback(cCallback);

            cService = ((ClientService.ClientServiceBinder)service).getService();

            Log.i("TAG", "ClientService Bind Success");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            cService = null;
        }
    };


    public void startClientService(){
        Intent clientServiceIntent = new Intent(this, ClientService.class);
        clientServiceIntent.putExtra("port", Integer.valueOf(Constants.CONNECT_PORT));
        clientServiceIntent.putExtra("clientResult", new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, final Bundle resultData) {
                switch (resultCode) {

                }
            }
        });
        if(cService == null) {
            startService(clientServiceIntent);
            bindService(clientServiceIntent, cConnection, Context.BIND_AUTO_CREATE);
        }

    }

    public void startServerService(){
        Intent serverServiceIntent = new Intent(this,ServerService.class);
        serverServiceIntent.putExtra("serverResult", new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, final Bundle resultData) {

                if(resultCode == Constants.CLIENT_ADDRESS_SEND )
                {
                    clientIpList.add(resultData.getString("client"));
                }

            }
        });


        if(sService == null) {
            startService(serverServiceIntent);// 서버 서비스 시작
            bindService(serverServiceIntent, sConnection, Context.BIND_AUTO_CREATE);
        }

    }

    public void startDataSendService(int action) {

        Intent intent = new Intent(this, DataSendService.class);

        intent.putExtra("iplist", clientIpList);
        intent.putExtra("action", action);


        switch (action) {

            case Constants.SEND_MUSIC :
                intent.putExtra("music", sendtofile);
                break;

            case Constants.SEND_STATE :
                intent.putExtra("state", musicState);
                break;

            case Constants.SEND_POSITION :
                intent.putExtra("position", currentpos);
                break;

        }

        intent.putExtra("datasendResult", new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
            }
        });

        startService(intent);
    }




    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TabHost tab_host = (TabHost) findViewById(R.id.tabhost);
        tab_host.setup();

        TabHost.TabSpec ts1 = tab_host.newTabSpec("tab1");
        ts1.setIndicator("", getResources().getDrawable(R.drawable.musicplayer));
        ts1.setContent(R.id.tab1);
        tab_host.addTab(ts1);

        TabHost.TabSpec ts2 = tab_host.newTabSpec("tab2");
        ts2.setIndicator("", getResources().getDrawable(R.drawable.musiclist));
        ts2.setContent(R.id.tab2);
        tab_host.addTab(ts2);

        TabHost.TabSpec ts3 = tab_host.newTabSpec("tab3");
        ts3.setIndicator("", getResources().getDrawable(R.drawable.musicshare));
        ts3.setContent(R.id.tab3);
        tab_host.addTab(ts3);

        TabHost.TabSpec ts4 = tab_host.newTabSpec("tab4");
        ts4.setIndicator("", getResources().getDrawable(R.drawable.musicsettings));
        ts4.setContent(R.id.tab4);
        tab_host.addTab(ts4);


        tab_host.setCurrentTab(0);
        Log.i("TAG", "메인엑티비티 시작");
        btnFindpeer = (Button) findViewById(R.id.btnFindpeer);


        stopPlayBtn = (Button) findViewById(R.id.button1);
        backMusicBtn = (Button) findViewById(R.id.button2);
        frontMusicBtn = (Button) findViewById(R.id.button3);
        seekbar = (SeekBarCompat) findViewById(R.id.seekBar1);
        mimage = (ImageView) findViewById(R.id.Mimage);

        stopPlayBtn.setEnabled(false);
        backMusicBtn.setEnabled(false);
        frontMusicBtn.setEnabled(false);
        seekbar.setEnabled(false);
        seekbar.setThumbColor(Color.RED);
        seekbar.setProgressColor(Color.WHITE);
        seekbar.setProgressBackgroundColor(Color.GRAY);
        seekbar.setThumbAlpha(255);

        mimage.setVisibility(View.INVISIBLE);

        nmanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotification();


        lvFileControl = (ListView) findViewById(R.id.lvFileControl);
        firstclientmusicstart = true;
        musicState = true;


        mFileList.clear();
        loadAllAudioList(Musicfolder1);// music
        loadAllAudioList(Musicfolder2);// download

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.musiclist_item, mFileList);
        lvFileControl.setAdapter(adapter);
        lvFileControl.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                Lleng = mList.size();
                file_nm = (String) mList.get(arg2);
                Marg = arg2;
                setFilename(file_nm);
                mediaCreate();
                File file = new File(file_nm);
                mimage.setImageBitmap(getAlbumArt(getApplicationContext(), file));
                sendtofile = file;


                stopPlayBtn.setEnabled(true);
                backMusicBtn.setEnabled(true);
                frontMusicBtn.setEnabled(true);
                seekbar.setEnabled(true);
                iniusicstart();
                tab_host.setCurrentTab(0);

            }
        });


        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                if (fromUser)
                    music.seekTo(progress);
                currentpos = progress;
            }
        });


        peerListview = (ListView) findViewById(R.id.peerlist);


        wifiP2pIntentFilter = new IntentFilter();
        wifiP2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifiP2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifiP2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifiP2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        wifiBroadcastReceiver = new WifiBroadcastReceiver(manager, channel, this);

        registerReceiver(wifiBroadcastReceiver, wifiP2pIntentFilter);


        btnFindpeer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!wifiMgr.isWifiEnabled()) {
                    Toast.makeText(getApplicationContext(), "와이파이를 켜주세요!", Toast.LENGTH_LONG).show();
                    tab_host.setCurrentTab(3);
                }

                manager.discoverPeers(channel, null);
                Log.i("TAG", "피어찾기를 시작함");
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        clientIpList = new ArrayList<>();
    }

    public void wifiOnOFF(View view) {

        if (!wifiMgr.isWifiEnabled()) {
            wifiMgr.setWifiEnabled(true);
            Toast.makeText(this, "와이파이가 켜집니다.", Toast.LENGTH_LONG).show();
        } else {
            wifiMgr.setWifiEnabled(false);
            Toast.makeText(this, "와이파이가 꺼집니다.", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    protected void onResume() {
        finishFlag = false;
        super.onResume();
    }


    public void setNetworkToReadyState(WifiP2pInfo info) {
        Log.i("TAG", "네트워크 정보가 저장됨");
        if (info.isGroupOwner) {
            Log.i("TAG", "그룹 오너임");
            if(sService == null) {
                startServerService();
            }
        } else {
            Log.i("TAG", "그룹 오너가 아님");
            if(cService == null) {
                startClientService();
            }
        }
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
        if(sService != null) {
            unbindService(sConnection);
        }
        if(cService != null) {
            unbindService(cConnection);
        }
        nmanager.cancel(1);
        Log.i("TAG","앱꺼짐");
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(wifiBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /*
    피어를 찾았을때 찾은 피어를 버튼으로 보여주는 함수
     */
    protected void displayPeers(WifiP2pDeviceList peers) {
        deviceNameList = new ArrayList<String>();
        for (WifiP2pDevice device : peers.getDeviceList()) {
            deviceNameList.add(device.deviceName);
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.musiclist_item, deviceNameList);
        peerListview.setAdapter(arrayAdapter);
        peerListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                    if (device.deviceName.equals(deviceNameList.get(position))) {
                        WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                        wifiP2pConfig.deviceAddress = device.deviceAddress;
                        wifiP2pConfig.groupOwnerIntent = 0;
                        manager.connect(channel, wifiP2pConfig, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.i("TAG", "와이파이 다이렉트가 연결됨");
                                manager.stopPeerDiscovery(channel, null);
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.i("TAG", "와이파이 다이렉트 연결에 실패함");
                            }
                        });

                    }
                }

            }
        });


    }

    @Override
    public void onBackPressed() {
        if(music==null){
            nmanager.cancel(1);
        }else{
            if(!music.isPlaying())
                nmanager.cancel(1);
        }

        if (finishFlag) {
            if(music==null){
                super.onBackPressed();
            }else{
                if(music.isPlaying()) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.MAIN");
                    intent.addCategory("android.intent.category.HOME");
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_FORWARD_RESULT
                            | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(intent);
                } 
                else{
                    super.onBackPressed();
                }
            }
        }
        else {
            Toast.makeText(getBaseContext(), "back키를 한번 더 누르면 종료합니다.", Toast.LENGTH_SHORT).show();
            finishFlag = true;
        }

    }



    public void button1(View v) {

        startDataSendService(Constants.SEND_MUSIC);

        startDataSendService(Constants.SEND_POSITION);

        if (music.isPlaying()) {
// 재생중이면 실행될 작업 (일시 정지)
            music.pause();
            try {
                music.prepare();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            music.getCurrentPosition();

            stopPlayBtn.setBackgroundResource(R.drawable.play);
            seekbar.setProgress(music.getCurrentPosition());
        } else {
// 재생중이 아니면 실행될 작업 (재생)

            music.start();
            mimage.setVisibility(View.VISIBLE);
            stopPlayBtn.setBackgroundResource(R.drawable.stop);

            Thread();


        }

    }

    public void iniusicstart() {
        mimage.setVisibility(View.VISIBLE);

        if (music.isPlaying()) {
            stopPlayBtn.setBackgroundResource(R.drawable.play);
        } else {
            music.start();
            stopPlayBtn.setBackgroundResource(R.drawable.stop);
            Thread();
        }
    }

    public void button2(View v) {
        if (Marg == 0) {
            Marg = Lleng;
        }
        music.stop();
        Marg = Marg - 1;
        music = MediaPlayer.create(getApplicationContext(), Uri.parse((String) mList.get(Marg)));
        File file = new File((String) mList.get(Marg));
        sendtofile = file;
        mimage.setImageBitmap(getAlbumArt(getApplicationContext(), file));
        seekbar.setMax(music.getDuration());
        music.start();
        stopPlayBtn.setBackgroundResource(R.drawable.stop);
        setFilename((String) mList.get(Marg));
        Thread();
    }

    public void button3(View v) {
        if (Marg == Lleng - 1) {
            Marg = -1;
        }
        music.stop();
        Marg = Marg + 1;
        music = MediaPlayer.create(getApplicationContext(), Uri.parse((String) mList.get(Marg)));
        File file = new File((String) mList.get(Marg));
        sendtofile = file;
        mimage.setImageBitmap(getAlbumArt(getApplicationContext(), file));
        seekbar.setMax(music.getDuration());
        music.start();
        stopPlayBtn.setBackgroundResource(R.drawable.stop);
        setFilename((String) mList.get(Marg));
        Thread();
    }

    public void Thread() {
        Runnable task = new Runnable() {
            public void run() {
                /**
                 * while문을 돌려서 음악이 실행중일때 게속 돌아가게
                 */
                while (music.isPlaying()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    /**
                     * music.getCurrentPosition()은 현재 음악 재생 위치를 가져오는 구문
                     */
                    seekbar.setProgress(music.getCurrentPosition());

                    music.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                        public void onCompletion(MediaPlayer mp) {
                            if (Marg == Lleng - 1) {
                                Marg = -1;
                            }
                            Marg = Marg + 1;
                            music = MediaPlayer.create(getApplicationContext(), Uri.parse((String) mList.get(Marg)));
                            File file = new File((String) mList.get(Marg));
                            mimage.setImageBitmap(getAlbumArt(getApplicationContext(), file));
                            sendtofile = file;
                            seekbar.setMax(music.getDuration());
                            music.start();
                            setFilename((String) mList.get(Marg));
                            Thread();
                        }
                    });
                }
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }


    private void loadAllAudioList(File file) {
        if (file != null && file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {

                for (int i = 0; i < children.length; i++) {
                    if (children[i] != null) {
                        for (int j = 0; j < FTYPE.length; j++) {
                            if (FTYPE[j].equals(children[i].getName().substring(children[i].getName().lastIndexOf(".") + 1,
                                    children[i].getName().length()))) {
                                mFileList.add(children[i].getName());
                                mList.add(children[i].getAbsolutePath());

                            }
                        }
                    }
                    loadAllAudioList(children[i]);
                }
            }
        }
    }


    public void setFilename(String file_name) {
        TextView tx = (TextView) findViewById(R.id.tvPath);
        //String path = file_name;
        String fileName = new File(file_name).getName();
        tx.setText(fileName);
    }


    public void mediaCreate() {
        if (music != null) {
            music.stop();
            stopPlayBtn.setBackgroundResource(R.drawable.play);
        }
        music = MediaPlayer.create(getApplicationContext(), Uri.parse(file_nm));
        music.setLooping(false);
        stopPlayBtn.setBackgroundResource(R.drawable.play);

        seekbar.setMax(music.getDuration());


    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.administrator.test1/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(music==null){
            nmanager.cancel(1);
        }else{
            if(!music.isPlaying())
                nmanager.cancel(1);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.administrator.test1/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }





    private Bitmap getAlbumArt(Context context, File mp3File) {
        Uri ArtworkUri = Uri.parse("content://media/external/audio/albumart");
        long albumId = 0;
        String mediaPath = mp3File.getAbsolutePath();
        String projection[] = {MediaStore.Audio.Media.ALBUM_ID};
        String selection = MediaStore.Audio.Media.DATA + " LIKE ? ";
        String selectionArgs[] = {mediaPath};
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            }
            cursor.close();
        }
        if (albumId > 0) {
            Uri albumArtUri = ContentUris.withAppendedId(ArtworkUri, albumId);
            ContentResolver res = context.getContentResolver();
            Bitmap bitmap = null;
            try {
                InputStream input = res.openInputStream(albumArtUri);
                bitmap = BitmapFactory.decodeStream(res.openInputStream(albumArtUri));
                input.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.start);
        return bitmap;
    }

    public Bitmap getCircleBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        int size = (bitmap.getWidth() / 2);
        canvas.drawCircle(size, size, size, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public void startmusic() {
        if (music != null) {
            music.stop();
            music = null;
        }
        File file = new File("/storage/emulated/0/Download/a.mp3");
        String a = file.getAbsolutePath();
        music = MediaPlayer.create(getApplicationContext(), Uri.parse(a));
        music.setLooping(false);
        music.start();


    }

    public void setpos(int pos) {
        music.seekTo(pos);
    }

    public void createNotification() {


        Intent intentMain = new Intent(Intent.ACTION_MAIN);
        intentMain.addCategory(Intent.CATEGORY_LAUNCHER);
        intentMain.setComponent(new ComponentName(this,MainActivity.class));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,intentMain,0);


        Notification.Builder mBuilder = new Notification.Builder(this);
        mBuilder.setSmallIcon(android.R.drawable.ic_media_play);
        mBuilder.setTicker("Notification.Builder");
        mBuilder.setContentTitle("Notification.Builder Title");
        mBuilder.setContentText("Notification.Builder Massage");
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setOngoing(true);
        mBuilder.setAutoCancel(false);


        nmanager.notify(1, mBuilder.build());

    }




}
