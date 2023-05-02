package com.example.multiscreendemo;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MessengerService extends Service {

    private static final String TAG = "MessengerService";
    private static final int MSG_FACE = 0x110;
    private static final int MSG_PRESENT = 0x111;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    /**
     * Keeps track of current MyPresentation client.
     * */
    private Messenger mClient = null;

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "MessengerService...onBind()");

        return mMessenger.getBinder();
    }

    /**
     * Handler of incoming messages from clients.
     */
    public class IncomingHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msgfromClient) {
            Message msgToClient = Message.obtain(msgfromClient);

            switch (msgfromClient.what) {
                case MSG_FACE:
                    Log.i(TAG, "handleMessage()...Server received!!...MSG_FACE");

                    if(msgfromClient.obj != null) {
                        Bundle mBundle = (Bundle) msgfromClient.obj;
                        int type = mBundle.getInt("videoType");
                        if (mClient != null) {
                            Log.i(TAG, "handleMessage()...MSG_FACE...mClient != null");

                            //傳訊息給指定的client測試
                            msgToClient.what = MSG_PRESENT;
                            // 通过msg.replyTo字段可以获取到发送方指定的接收器的引用
                            Bundle bundle = new Bundle();
                            bundle.putInt("videoType", type);
                            msgToClient.setData(bundle);

                            try {
                                mClient.send(msgToClient);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        Log.d(TAG, "msg.obj is null.");
                    }

//                    msgToClient.what = MSG_FACE;
//                    // 通过msg.replyTo字段可以获取到发送方指定的接收器的引用
//                    Bundle bundle = new Bundle();
//                    bundle.putString("reply", "谢谢，Server端已收到訊息~");
//                    msgToClient.setData(bundle);
//
//                    try {
//                        msgfromClient.replyTo.send(msgToClient);
//                    } catch (RemoteException e) {
//                        throw new RuntimeException(e);
//                    }
                    break;
                case MSG_PRESENT:
                    Log.i(TAG, "handleMessage()...Server received!!...MSG_PRESENT");

                    if(msgfromClient.obj != null) {
                        Bundle mBundle = (Bundle) msgfromClient.obj;
                        String clientName = mBundle.getString("ClientName");

                        mClient = msgfromClient.replyTo;
                    } else {
                        Log.d(TAG, "msg.obj is null.");
                    }
                    break;
                default:
                    super.handleMessage(msgfromClient);
            }
        }
    }
}
