package com.example.iot_ms5_java;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.squareup.picasso.Picasso;
import java.util.Arrays;
import java.util.UUID;
import android.widget.ImageView;
public class Fragment_1 extends Fragment {

    private static final String TAG = "dubugging";
    TextView weatherTextView;
    TextView stepsTextView;
    Button getWeatherBtn;
    Button sendWeatherBtn;
    Button connectToPIBtn;
    MQTTClient mqttClient;

    // topics for MQTT client
    String weatherTopic = "android/weatherTopic";
    String subTopic = "microbit/steps";

    // items for API
    RequestQueue queue;
    Gson gson;
    WeatherResult mostRecentWeather;
    Context thisContext;

    public Fragment_1() {
        // Required empty public constructor
        super(R.layout.fragment_1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thisContext = view.getContext();
        queue = Volley.newRequestQueue(thisContext);
        gson = new Gson();
        weatherTextView = view.findViewById(R.id.currWeatherTextView);
        stepsTextView = view.findViewById(R.id.fragOneCurrSteps);
        mqttClient = new MQTTClient(UUID.randomUUID().toString(), "tcp://192.168.4.1:1883", thisContext);

        // get weather when app is pressed
        // click listener
        getWeatherBtn = view.findViewById(R.id.getWeatherBtn);

        getWeatherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Make api weather call here
                Log.d("test1", "Printing from on click");
                requestWeather();
            }
        });

        sendWeatherBtn = view.findViewById(R.id.sendWeatherBtn);

        sendWeatherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send data to PI via publish
                try {
                    sendWeather(weatherTopic, mostRecentWeather.weather[0].main);
                } catch (Exception e) {
                    Log.d(TAG, "onClick: There was a problem getting the weather..");
                    CharSequence text = "Make sure that the weather has been gathered..";
                    int duration = Toast.LENGTH_LONG;

                    Toast toast = Toast.makeText(thisContext, text, duration);
                    toast.show();

                }
            }
        });

        connectToPIBtn = view.findViewById(R.id.connectToPIBtn);

        connectToPIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncWithPI();
            }
        });
    }

    public void sendWeather(String publishTopic, String payload) {
        mqttClient.publish(publishTopic, payload);
        // get steps..
    }

    public void requestWeather() {
        String url = "https://api.openweathermap.org/data/2.5/weather?id=4254010&appid=62823359f56f5a425fd7d24a47acaf35";

        // Request a string response
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        mostRecentWeather = gson.fromJson(response, WeatherResult.class);
                        Log.d("test2", Arrays.toString(new String[]{mostRecentWeather.weather[0].main}));
                        weatherTextView.setText(mostRecentWeather.weather[0].main);
                        String icon = mostRecentWeather.weather[0].icon;
                        String url = "https://openweathermap.org/img/wn/" + icon + "@2x.png";
                        ImageView imageView = (ImageView) getView().findViewById(R.id.imageView);
                        Picasso.with(thisContext).load(url).into(imageView);
                        //Picasso.with(this).load(url).into(imageView)
                    }
                },
                new Response.ErrorListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        weatherTextView.setText("That didn't work!");
                        CharSequence text = "Check that you are connected to the WiFi";
                        int duration = Toast.LENGTH_LONG;

                        Toast toast = Toast.makeText(thisContext, text, duration);
                        toast.show();
                    }
                });

        queue.add(stringRequest);
    }

    public void syncWithPI() {
        mqttClient.connect();
        mqttClient.client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d(TAG, "connectComplete: Connection was successful");
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String msg = message.toString();
                stepsTextView.setText(msg.substring(msg.length() - 1));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "deliveryComplete: Message was delievered!");
                mqttClient.subscribe(subTopic);
            }
        });
    }
}
