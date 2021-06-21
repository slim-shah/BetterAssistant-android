package com.cybernetic87.Gassist;

import android.util.Log;

import com.cybernetic87.GAssist.EmbeddedAssistant;
import com.google.assistant.embedded.v1alpha2.AssistRequest;
import com.google.assistant.embedded.v1alpha2.AssistResponse;
import com.google.assistant.embedded.v1alpha2.DeviceLocation;
import com.google.assistant.embedded.v1alpha2.DialogStateOut;
import com.google.auth.oauth2.UserCredentials;
import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentValues.TAG;

public class AssistantTest1 {

    private EmbeddedAssistant mEmbeddedAssistant = null;

    @Before
    public void setUp() {
        UserCredentials userCredentials = null;

        String path = "../GAssist.Provider/src/main/assets/credentials.json";
        //String path = ;
        InputStream is = null;

        try {
            is = new FileInputStream(path);
           // is = getClass().getClassLoader().getResourceAsStream("assets/credentials.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            userCredentials = UserCredentials.fromStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // List & adapter to store and display the history of Assistant Requests.
        mEmbeddedAssistant = new EmbeddedAssistant.Builder()
                .setCredentials(userCredentials)
                .setDeviceInstanceId("galaxy_watch_instance")
                .setDeviceModelId("gassist-292601_galaxy_watch")
                .setDeviceLocation(DeviceLocation.newBuilder().build())
                .setLanguageCode("en-US")
                .setAudioSampleRate(16000)
                .setAudioVolume(100)
                .setRequestCallback(new EmbeddedAssistant.RequestCallback() {
                })
                .setConversationCallback(new EmbeddedAssistant.ConversationCallback() {
                    @Override
                    public void onResponseStarted(byte[] response) {
                        super.onResponseStarted(response);
                        Log.println(Log.INFO, "#Response", response.toString());
                    }

                    @Override
                    public void onResponseFinished() {
                        super.onResponseFinished();
                    }


                    @Override
                    public void onError(Throwable throwable) {
                        DialogStateOut dso = DialogStateOut.newBuilder().setSupplementalDisplayText(throwable.getMessage()).build();

                        AssistResponse ar = AssistResponse
                                .newBuilder()
                                .setDialogStateOut(dso)
                                .setEventType(AssistResponse.EventType.END_OF_UTTERANCE)
                                .build();

                        Log.e(TAG, "assist error: " + throwable.getMessage(), throwable);
                        Log.e(TAG, ar.getDialogStateOut().getSupplementalDisplayText());
                    }


                    @Override
                    public void onConversationFinished() {
                        Log.i(TAG, "assistant conversation finished");
                    }

                }).build();
        mEmbeddedAssistant.connect();
    }

    @Test
    public void startAssistantTest() {
        byte [] data = "what is the weather in new york ?".getBytes();
        try {
            AssistRequest ar = AssistRequest.parseFrom(data);
            mEmbeddedAssistant.setResponseFormat(ar.getConfig().getScreenOutConfig().getScreenMode());
            mEmbeddedAssistant.startConversation();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
