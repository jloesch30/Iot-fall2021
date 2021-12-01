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
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    DailyWeather dailyForecast;
    Context thisContext;

    // weather to send to Raspberry PI
    Double temp_min_curr;
    Double temp_max_curr;
    int humidity_curr;
    Double temp_min_tomorrow;
    Double temp_max_tomorrow;
    int humidity_tomorrow;

    // location
    Double lon;
    Double lat;

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
                    JSONObject jsonPayload = new JSONObject();
                    jsonPayload.put("temp_min_curr", temp_min_curr);
                    jsonPayload.put("temp_max_curr", temp_max_curr);
                    jsonPayload.put("humidity_curr", humidity_curr);
                    jsonPayload.put("temp_max_tomorrow", temp_max_tomorrow);
                    jsonPayload.put("temp_min_tomorrow", temp_min_tomorrow);
                    jsonPayload.put("humidity_tomorrow", humidity_tomorrow);
                    sendWeather(weatherTopic, jsonPayload);
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

    public void sendWeather(String publishTopic, JSONObject payload) {
        if (mqttClient.connected == false) {
            syncWithPI();
        }
        mqttClient.publish(publishTopic, payload.toString());
        // get steps..
    }

    public void requestWeather() {
        // changed url to return imperial units
        String currUrl = "https://api.openweathermap.org/data/2.5/weather?units=imperial&id=4254010&appid=62823359f56f5a425fd7d24a47acaf35";

        // Request a string response
        StringRequest currStringRequest = new StringRequest(Request.Method.GET, currUrl,
                new Response.Listener<String>() {

                   // Get all of the weather data
                    // 1. daily high temp
                    // 2. daily low temp
                    // 3. daily humidity
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: Full response for current weather is: " + response);

                        mostRecentWeather = gson.fromJson(response, WeatherResult.class);
                        temp_min_curr = mostRecentWeather.main.temp_min;
                        temp_max_curr = mostRecentWeather.main.temp_max;
                        humidity_curr = mostRecentWeather.main.humidity;
                        lat = mostRecentWeather.coord.lat;
                        lon = mostRecentWeather.coord.lon;

                        weatherTextView.setText(mostRecentWeather.weather[0].main);

                        // Getting forecast here
                        String forecastUrl = String.format("https://api.openweathermap.org/data/2.5/onecall?units=imperial&lat=%s&lon=%s&exclude=current,minutely,hourly,alerts&appid=62823359f56f5a425fd7d24a47acaf35", lat, lon);
                        StringRequest forecastStringReq = new StringRequest(Request.Method.GET, forecastUrl,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d(TAG, "onResponse: Full response for forecast is: " + response);
                                        dailyForecast = gson.fromJson(response, DailyWeather.class);
                                        temp_max_tomorrow = dailyForecast.daily[1].temp.max;
                                        temp_min_tomorrow = dailyForecast.daily[1].temp.min;
                                        humidity_tomorrow = dailyForecast.daily[1].humidity;
                                    }
                                },
                                new Response.ErrorListener() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        CharSequence text = "Was unable to get forecast";
                                        int duration = Toast.LENGTH_LONG;

                                        Toast toast = Toast.makeText(thisContext, text, duration);
                                        toast.show();
                                    }
                                });
                        queue.add(forecastStringReq);
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
        queue.add(currStringRequest);
    }

    public void syncWithPI() {
        try {
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
        } catch (Exception e) {
            Log.d(TAG, "syncWithPI: There was a problem connecting a ");
        }
    }
}
