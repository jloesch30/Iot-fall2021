package com.example.iot_ms5_java;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTClient {
    private static final String TAG = "MQTT client";
    public String clientID;
    public String serverID;
    public MqttAndroidClient client;
    public Context thisContext;

    public MQTTClient(String clientID, String serverID, Context thisContext) {
       // construct client
       this.clientID = clientID;
       this.serverID = serverID;
       this.thisContext = thisContext;
    }

    public void connect() {
        Log.d(TAG, "connect: Connecting to PI");
        client = new MqttAndroidClient(this.thisContext, this.serverID, this.clientID);

        try {
            client.connect().setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess: Client connected");
                    CharSequence text = "Connection Successful";
                    int duration = Toast.LENGTH_LONG;

                    Toast toast = Toast.makeText(thisContext, text, duration);
                    toast.show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "onFailure: Client did not connect");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Log.d(TAG, "connect: There was a problem..");
        }
    }

    public void publish(String publishTopic, String payload) {
        MqttMessage message = new MqttMessage(payload.getBytes());
        try {
            Log.d(TAG, "publish: Publishing message...");
            client.publish(publishTopic, message).setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess: The message was sent");
                    CharSequence text = "Published Data";
                    int duration = Toast.LENGTH_LONG;

                    Toast toast = Toast.makeText(thisContext, text, duration);
                    toast.show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "onFailure: The message was not sent");
                    CharSequence text = "Publish failed :(";
                    int duration = Toast.LENGTH_LONG;

                    Toast toast = Toast.makeText(thisContext, text, duration);
                    toast.show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Log.d(TAG, "publish: There was a problem");
        }
    }
    public void subscribe(String subTopic) {
        try {
            this.client.subscribe(subTopic, 0).setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess: Subscribed correctly");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
