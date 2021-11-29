package com.example.iot_ms5_java;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;

public class Fragment_2 extends Fragment {
    private static final String TAG = "dubugging";
    TextView currStepsTextView;
    Button sendSteps;
    Button getSteps;
    Button syncWithPi;
    MQTTClient mqttClient;

    Context thisContext;
    Steps stepsObject;
    String subTopic = "microbit/steps";

    public Fragment_2() {
        // Required empty public constructor
        super(R.layout.fragment_2);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thisContext = view.getContext();

        currStepsTextView = view.findViewById(R.id.txtNumOfSteps);
        mqttClient = new MQTTClient(UUID.randomUUID().toString(), "tcp://192.168.4.1:1883", thisContext);

        // connect to PI
        syncWithPi = view.findViewById(R.id.syncWithPiBtn);

        syncWithPi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttClient.connect();
                mqttClient.client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        Log.d(TAG, "connectComplete: Connection was completed");
                        mqttClient.subscribe(subTopic);
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.d(TAG, "connectionLost: Connection was lost");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        Log.d(TAG, "messageArrived: Message has arrived");
                        String msg = message.toString();
                        currStepsTextView.setText(msg.substring(msg.length() - 1));
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        });

    }
}