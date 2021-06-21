/*
 * Copyright (c) 2018 Samsung Electronics Co., Ltd. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that
 * the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation and/or
 *       other materials provided with the distribution.
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse
 *       or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.cybernetic87.GAssist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.assistant.embedded.v1alpha2.AssistConfig;
import com.google.assistant.embedded.v1alpha2.AssistRequest;
import com.google.assistant.embedded.v1alpha2.AudioInConfig;
import com.google.assistant.embedded.v1alpha2.ScreenOutConfig;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAAgentV2;
import com.samsung.android.sdk.accessory.SAAuthenticationToken;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static com.google.assistant.embedded.v1alpha2.AudioInConfig.Encoding.FLAC;
import static com.google.assistant.embedded.v1alpha2.AudioInConfig.Encoding.LINEAR16;

public class ProviderService extends SAAgent {
    private static final String TAG = "ProviderService";
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private final IBinder mBinder = new LocalBinder();
    private ServiceConnection mConnectionHandler = null;
    private EmbeddedAssistant mEmbeddedAssistant;
    private MainActivity mMainActivity;

    public ProviderService() {
        super(TAG, SASOCKET_CLASS);
        Log.println(Log.INFO, "inside", "provider Service");
    }

    private boolean ValidateJsonFile(String path, String property) {
        String text = null;
        JSONObject obj = null;
        File credentialsFile = new File(path);

        if (credentialsFile.canRead()) {
            try {
                text = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                obj = new JSONObject(Objects.requireNonNull(text));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (!Objects.requireNonNull(obj).has(property)) {
                String message = String.format("Wrong file: %s", new File(path).getName());
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationManager notificationManager;
        String channel_id = "sample_channel_01";

        String channel_name = "Accessory_SDK_Sample";
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notiChannel = new NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_LOW);
        Objects.requireNonNull(notificationManager).createNotificationChannel(notiChannel);

        int notifyID = 1;
        Notification notification = new Notification.Builder(this.getBaseContext(), channel_id)
                .setContentTitle(TAG)
                .setContentText("")
                .setChannelId(channel_id)
                .build();

        startForeground(notifyID, notification);

        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // try to handle SsdkUnsupportedException
            if (processUnsupportedException(e)) {
            }
        } catch (Exception e1) {

            e1.printStackTrace();
        }

        Assistant assistant = new Assistant(getApplicationContext(), this);
        try {
            mEmbeddedAssistant = assistant.startAssistant();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void sendData(byte[] data) {
//        int start = 0;
//        int chunkSize = mConnectionHandler.getConnectedPeerAgent().getMaxAllowedDataSize();
//        while (start < data.length) {
//            int end = Math.min(data.length, start + mConnectionHandler.getConnectedPeerAgent().getMaxAllowedDataSize());
        if (mConnectionHandler != null) {
            try {
                mConnectionHandler.send(getServiceChannelId(0), data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        Log.d(TAG, "onFindPeerAgentResponse : result =" + result);
        Log.println(Log.INFO, "onFindPeerAgentResponse : result =", Integer.toString(result));
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        Log.println(Log.INFO, "Service connection request: ", peerAgent.getAppName());
        String credentialsPath = getApplicationContext().getFilesDir().getAbsolutePath() + "/credentials.json";
        if (peerAgent != null && ValidateJsonFile(credentialsPath, "type")
                && new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/device.json").exists()) {
            Assistant assistant = new Assistant(getApplicationContext(), this);
            try {
                mEmbeddedAssistant = assistant.startAssistant();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            acceptServiceConnectionRequest(peerAgent);
            Log.println(Log.INFO, "Service connection:", "accepted");
        } else {
            rejectServiceConnectionRequest(Objects.requireNonNull(peerAgent));
            Log.println(Log.ERROR, "Service connection:", "rejected");
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        Log.println(Log.INFO, "peerAgent", peerAgent.getAppName());

        if (result == SAAgentV2.CONNECTION_SUCCESS) {
            if (socket != null) {
                mConnectionHandler = (ServiceConnection) socket;
                 Toast.makeText(getBaseContext(), "Galaxy Watch connection is established",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (result == SAAgentV2.CONNECTION_ALREADY_EXIST) {
             Toast.makeText(getBaseContext(), "Galaxy Watch connection already exist",
                    Toast.LENGTH_SHORT).show();
        }
    }

//    @Override
//    protected void onAuthenticationResponse(SAPeerAgent peerAgent, SAAuthenticationToken authToken, int error) {
//        /*
//         * The authenticatePeerAgent(peerAgent) API may not be working properly depending on the firmware
//         * version of accessory device. Please refer to another sample application for Security.
//         */
//    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {

        if (mMainActivity != null) {
            mMainActivity.finish();
        }
        super.onError(peerAgent, errorMessage, errorCode);
    }

    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        int errType = e.getType();
        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
            /*
             * Your application can not use Samsung Accessory SDK. You application should work smoothly
             * without using this SDK, or you may want to notify user and close your app gracefully (release
             * resources, stop Service threads, close UI thread, etc.)
             */
        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
            Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");
            Toast.makeText(getApplicationContext(), "You need to install Samsung Accessory Service to use this application.", Toast.LENGTH_LONG).show();

            final String appPackageName = "com.samsung.accessory"; // getPackageName() from Context or Activity object
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException anfe) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
            Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
            Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");
            return false;
        }
        return true;
    }

    public void SetmProviderActivity(MainActivity mMainActivity) {
        this.mMainActivity = mMainActivity;
    }



    class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
            Log.e("SERVICE CONNECTION", "GOT HERE");
            if (mMainActivity != null) {
                //mMainActivity.updateTextView("Connected");
                //mMainActivity.SetStatusIcon(R.mipmap.ic_tick_green);
            }
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
            mConnectionHandler.close();
            mConnectionHandler = null;
            if (mEmbeddedAssistant.mAssistantRequestObserver != null) {
                mEmbeddedAssistant.stopConversation();
                mEmbeddedAssistant.destroy();
            }

            if (mMainActivity != null) {
                //mMainActivity.updateTextView("Disconnected");
                //mMainActivity.SetStatusIcon(R.mipmap.ic_x_red);
            }

            Log.e(TAG, "Connection is not alive Error: " + errorMessage + " " + errorCode);
        }

        /**
         * Divide an array of byte in chunks of chunkSize bytes
         *
         * @param source    the source byte array
         * @param chunkSize the size of a chunk
         * @return an array of chunks
         * @see <a href="http://stackoverflow.com/questions/3405195/divide-array-into-smaller-parts">Divide array into smaller parts</a>
         */
        private byte[][] divideArray(byte[] source, int chunkSize) {
            byte[][] ret = new byte[(int) Math.ceil(source.length / (double) chunkSize)][chunkSize];

            int start = 0;

            for (int i = 0; i < ret.length; i++) {
                ret[i] = Arrays.copyOfRange(source, start, start + chunkSize);
                start += chunkSize;
            }

            return ret;
        }

        @Override
        public void onReceive(int channelId, byte[] data) {

            String message = new String(data);
            //ByteString audioIn = ByteString.copyFrom(data);
            Log.println(Log.INFO, "#insideReceive#", message);
            Log.println(Log.INFO, "#insideReceive#", data.toString());
            if (mConnectionHandler == null) {
                return;
            }

            // Divide the audio request into chunks
          //  byte[][] chunks = divideArray(data, assistantConf.getChunkSize());
            mEmbeddedAssistant.connect();
            mEmbeddedAssistant.setResponseFormat(ScreenOutConfig.ScreenMode.PLAYING);
            mEmbeddedAssistant.startConversation();
            byte [][] chunks = divideArray(data, 1024);

            for(byte[] chunk : chunks) {
                ByteString audioIn = ByteString.copyFrom(chunk);
                AssistRequest ar = AssistRequest.newBuilder().setAudioIn(audioIn).build();
                mEmbeddedAssistant.mAssistantRequestObserver.onNext(ar);
            }

            mEmbeddedAssistant.mAssistantRequestObserver.onCompleted();
            //mEmbeddedAssistant.stopConversation();
//            mEmbeddedAssistant.mAssistantRequestObserver.onCompleted();

        }

        /* @Override
        public void onReceive(int channelId, byte[] data) {
            String message = new String(data);
            Log.println(Log.INFO, "#insideReceive#", message);
            Log.println(Log.INFO, "#insideReceive#", data.toString());
            if (mConnectionHandler == null) {
                return;
            }

            try {
                /*AudioInConfig audioInConfig = AudioInConfig.newBuilder().build();
                AssistConfig config = AssistConfig.newBuilder()
                        .setAudioInConfig(audioInConfig)
                        .setAudioOutConfig()
                        .setDebugConfig()
                        .setDialogStateIn()
                        .setScreenOutConfig()
                        .build();
                AssistRequest ar = AssistRequest.parseFrom(data);

                Log.println(Log.INFO, "#insideReceived#", "testing encoding FLAC");
                if (ar.getConfig().getAudioInConfig().getEncoding() == FLAC) {
                    mEmbeddedAssistant.stopConversation();
                    return;
                }

                Log.println(Log.INFO, "#insideReceived#", "testing encoding LINEAR16");
                if (ar.getConfig().getAudioInConfig().getEncoding() == LINEAR16) {
                    //clq.clear();
                    if (mEmbeddedAssistant.mAssistantRequestObserver != null) {
                        mEmbeddedAssistant.stopConversation();
                        //mEmbeddedAssistant.mAssistantRequestObserver = null;
                    }
                    mEmbeddedAssistant.setResponseFormat(ar.getConfig().getScreenOutConfig().getScreenMode());
                    mEmbeddedAssistant.startConversation();

                    Log.e(TAG, "Started conversation");

                    return;
                }
                Log.println(Log.INFO, "#insideReceived#", "parsing data");
                if (mEmbeddedAssistant.mAssistantRequestObserver != null && !ar.getAudioIn().isEmpty()) {

                    mEmbeddedAssistant.mAssistantRequestObserver.onNext(AssistRequest.parseFrom(data));
                }
//                else if (ar.getConfig().getAudioInConfig().getEncoding() == ENCODING_UNSPECIFIED && mEmbeddedAssistant.isConversation) {
//                    mEmbeddedAssistant.stopConversation();
//                    mEmbeddedAssistant.isConversation = false;
//                    Log.e(TAG, "GOT REQUEST");
//                    return;
//                }


            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                String error = "";
                Log.println(Log.ERROR, "#insideReceived#", e.toString());
                Log.println(Log.ERROR, "#insideReceived#", e.getMessage());
                for(StackTraceElement elem : e.getStackTrace()) {
                        error += elem.toString() + "\n";
                }
                Log.println(Log.ERROR, "#insideReceived#", error);
            }

        } */

        @Override
        protected void onServiceConnectionLost(int reason) {
            mConnectionHandler.close();
            mConnectionHandler = null;
            if (mEmbeddedAssistant.mAssistantRequestObserver != null) {
                mEmbeddedAssistant.stopConversation();
            }
            mEmbeddedAssistant.channel.shutdownNow();
            mEmbeddedAssistant.destroy();

//            if (mMainActivity != null) {
//                //mMainActivity.updateTextView("Disconnected");
//                //mMainActivity.SetStatusIcon(R.mipmap.ic_x_red);
//            }
        }
    }


    class LocalBinder extends Binder {
        public ProviderService getService() {
            return ProviderService.this;
        }
    }
}
