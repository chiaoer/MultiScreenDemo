package com.example.multiscreendemo;

import android.app.Presentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import java.io.IOException;

public class MyPresentation extends Presentation implements SurfaceHolder.Callback, View.OnClickListener {

    public static final int TYPE_VIDEO_Child = 0;
    public static final int TYPE_VIDEO_Young = 1;
    public static final int TYPE_VIDEO_MaleAdult = 2;
    public static final int TYPE_VIDEO_FemaleAdult = 3;
    public static final int TYPE_VIDEO_MaleSenior = 4;
    public static final int TYPE_VIDEO_FemaleSenior = 5;
    public static final int TYPE_VIDEO_MTK = 6;
    public static final int TYPE_PIC = 7;
    private String TAG = "MyPresentation";

    private final View root;
    private final SurfaceHolder surfaceHolderCurr;
    private final SurfaceView surface_view;
    private int type;
    private int id;
    private MediaPlayer mediaPlayer;
    private Button mButton;

    private Display mDisplay;
    private String picDataPath = Environment.getExternalStorageDirectory().getPath()+"/G700_launch.png";

    //Messenger for communicating with the service.
    private Messenger mService = null;
    //Flag indicating whether we have called bind on the service.
    private boolean mIsBound = false;
    private Context mContext;
    private static final int MSG_PRESENT = 0x111;

    public MyPresentation(Context outerContext, Display display, int type, int tag) {
        super(outerContext, display);
        mContext = outerContext;
        this.type = type + 6; //To set to TYPE_PIC or TYPE_VIDEO_MTK
        this.mDisplay = display;
        TAG = TAG + "-display" + tag;
        this.id = tag;
        Log.d(TAG, "type=" + type);
        root = View.inflate(getContext(), R.layout.view_video, null);
        setContentView(root);
        surface_view = root.findViewById(R.id.surface_view);
        surfaceHolderCurr = surface_view.getHolder();
        surfaceHolderCurr.addCallback(this);
        surface_view.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent();
        intent.setAction("com.example.multiscreendemo.messenger");
        intent.setPackage("com.example.multiscreendemo");
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (mIsBound) {
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
        super.onStop();
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new myPresentHandler());
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            Log.d(TAG, "onServiceConnected()");
            mService = new Messenger(service);
            mIsBound = true;

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Bundle mBundle = new Bundle();
                //一開始連線上時候,先傳送通知
                mBundle.putString("ClientName","MyPresentation");
                Message msg = Message.obtain();
                msg.what = MSG_PRESENT;
                msg.replyTo = mMessenger;
                msg.obj = mBundle;
                mService.send(msg);

            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected()");

            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mIsBound = false;
        }
    };
    /**
     * Handler of incoming messages from Server(MultiScreenDemo App).
     */
    public class myPresentHandler extends Handler {

        @Override
        public void handleMessage(@androidx.annotation.NonNull Message msgfromServer) {

            switch (msgfromServer.what) {
                case MSG_PRESENT:
                    type = msgfromServer.getData().getInt("videoType");
                    Log.d(TAG, "MyPresentation client receives msg!!...videoType = " + type);
                    surface_view.setVisibility(View.GONE);
                    surface_view.setVisibility(View.VISIBLE);
                    break;
                default:
                    super.handleMessage(msgfromServer);
            }
        }
    }
    public void changeContent() {
        Log.d(TAG, "currentType=" + type);
        switch (type) {
            case TYPE_PIC:
                type = TYPE_VIDEO_MTK;
                break;
            default:
                type = TYPE_PIC;
        }
        surface_view.setVisibility(View.GONE);
        surface_view.setVisibility(View.VISIBLE);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");

        switch (type) {
            case TYPE_PIC:
            {
                releaseVideo();
                drawImg();
                break;
            }
            case TYPE_VIDEO_MTK:
            {
//                startVideo(type);
                Intent intent = new Intent();
                intent.setClassName("com.example.mlseriesdemonstrator","com.example.mlseriesdemonstrator.MainActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
            default:
            {
                startVideo(type);
                break;
            }
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }

    private void startVideo(int videoType) {
        if(mediaPlayer == null) {
            //mediaPlayer = MediaPlayer.create(root.getContext(), R.raw.demo2);
            mediaPlayer = new MediaPlayer();
            if(mediaPlayer == null){
                Log.e("MultiScreen", "mediaPlayer == null");
                return;
            }
        }
        String dataPath;

        switch (videoType) {
            case TYPE_VIDEO_MTK:
                dataPath = Environment.getExternalStorageDirectory().getPath()+"/MediaTek Genio_Genius At The Edge.mp4";
                break;
            case TYPE_VIDEO_Child:
                dataPath = Environment.getExternalStorageDirectory().getPath()+"/Child_LEGO.mp4";
                break;
            case TYPE_VIDEO_Young:
                dataPath = Environment.getExternalStorageDirectory().getPath()+"/Young_Food.mp4";
                break;
            case TYPE_VIDEO_MaleAdult:
                dataPath = Environment.getExternalStorageDirectory().getPath()+"/Male Adult_Spin Bike.mp4";
                break;
            case TYPE_VIDEO_FemaleAdult:
                dataPath = Environment.getExternalStorageDirectory().getPath()+"/Female Adult_JAPANESE.mp4";
                break;
            case TYPE_VIDEO_MaleSenior:
                dataPath = Environment.getExternalStorageDirectory().getPath()+"/Male Senior.mp4";
                break;
            case TYPE_VIDEO_FemaleSenior:
                dataPath = Environment.getExternalStorageDirectory().getPath()+"/Female Senior.mp4";
                break;
            default:
                dataPath = Environment.getExternalStorageDirectory().getPath()+"/MediaTek Genio_Genius At The Edge.mp4";
        }
        //for spm86xx different screen show different video resource
        //String dataPath = Environment.getExternalStorageDirectory().getPath()+"/test_" + id +".mp4";
//        String dataPath = Environment.getExternalStorageDirectory().getPath()+"/test.mp4";

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(dataPath);
            mediaPlayer.setDisplay(surfaceHolderCurr);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //mediaPlayer.setDisplay(surfaceHolderCurr);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void stopVideo() {
        if(mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.stop();
            mediaPlayer.setDisplay(null);
            mediaPlayer.reset();
        }
    }

    public void releaseVideo() {
        Log.d(TAG, "releaseVideo");
        if(mediaPlayer != null){
            stopVideo();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void toggleVideo() {
        String cType = type == TYPE_PIC ? "Pic" : "Video";
        Log.d(TAG, "toggleVideo current type is" + cType);
        if(mediaPlayer != null) {
            if(mediaPlayer.isPlaying()) {
                Log.d(TAG, "stop video");
                makeToast("stop video");
                mediaPlayer.pause();
            } else {
                Log.d(TAG, "start video");
                makeToast("start video");
                mediaPlayer.start();
            }
        }
    }

    private void drawImg() {
        Log.d(TAG, "drawImg");
        Log.d(TAG, "# picDataPath =" + picDataPath );

        synchronized (surfaceHolderCurr) {
            Canvas canvas = surfaceHolderCurr.lockCanvas();
//            Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
//                    root.getContext().getResources(), R.mipmap.pannel2), 1280, 800, true);

            //get display[i] width/height
            DisplayMetrics outMetrics = new DisplayMetrics();
            mDisplay.getMetrics(outMetrics);
            Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(picDataPath),
                    outMetrics.widthPixels, outMetrics.heightPixels, true);

            Log.d(TAG, "bitmap.getHeight() >>>" + bitmap.getHeight());
            Log.d(TAG, "bitmap.getWidth() >>>" + bitmap.getWidth());
            canvas.drawBitmap(bitmap, 0, 0, null);
            surfaceHolderCurr.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.surface_view:
                toggleVideo();
                break;
        }
    }

    private void makeToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
