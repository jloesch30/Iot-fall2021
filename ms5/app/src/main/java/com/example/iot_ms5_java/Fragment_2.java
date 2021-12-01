package com.example.iot_ms5_java;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

public class Fragment_2 extends Fragment {
    private static final String TAG = "dubugging";
    private static final String LOG_TAG = "Logging";

    TextView currStepsTextView;
    MQTTClient mqttClient;
    StepGoal stepGoal;

    Button getGoalButton;
    TextView goalText;

    // API Items
    RequestQueue queue;
    Gson gson = new Gson();
    WeatherResult mostRecentWeather;
    DailyWeather dailyForecast;

    boolean weatherDataGathered = false;
    boolean connectedToPi = false;
    Context thisContext;
    String subTopic = "microbit/stepGoal";
    String publishTopic = "android/weatherTopic";

    // weather to send to Raspberry PI
    Double temp_min_curr;
    Double temp_max_curr;
    int humidity_curr;
    Double temp_min_tomorrow;
    Double temp_max_tomorrow;
    int humidity_tomorrow;
    String currUrl;
    // location
    Double lon;
    Double lat;

    // networking
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable getWeatherRunnable;

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

        mqttClient = new MQTTClient(UUID.randomUUID().toString(), "tcp://192.168.4.1:1883", thisContext);

        queue = Volley.newRequestQueue(thisContext);

        goalText = view.findViewById(R.id.goalTxt);

        // changed url to return imperial units
        currUrl = "https://api.openweathermap.org/data/2.5/weather?units=imperial&id=4254010&appid=62823359f56f5a425fd7d24a47acaf35";

        // button for goal checking
        getGoalButton = view.findViewById(R.id.getGoalButton);

        // start runnable to get weather
        startRunnable();

        getGoalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkGoal();
            }
        });
    }
    public void getWeather(String url) {
        if (!weatherDataGathered) {
            StringRequest currWeatherReq = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "onResponse: Full response for current weather is: " + response);

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mostRecentWeather = gson.fromJson(response, WeatherResult.class);
                                    temp_min_curr = mostRecentWeather.main.temp_min;
                                    temp_max_curr = mostRecentWeather.main.temp_max;
                                    humidity_curr = mostRecentWeather.main.humidity;
                                    lat = mostRecentWeather.coord.lat;
                                    lon = mostRecentWeather.coord.lon;
                                }
                            }, 1000);

                            // Getting forecast here
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    String forecastUrl = String.format("https://api.openweathermap.org/data/2.5/onecall?units=imperial&lat=%s&lon=%s&exclude=current,minutely,hourly,alerts&appid=62823359f56f5a425fd7d24a47acaf35", lat, lon);
                                    StringRequest forecastWeatherReq = new StringRequest(Request.Method.GET, forecastUrl,
                                            new Response.Listener<String>() {
                                                @Override
                                                public void onResponse(String response) {
                                                    Log.d(TAG, "onResponse: Full response for forecast is: " + response);
                                                    dailyForecast = gson.fromJson(response, DailyWeather.class);
                                                    temp_max_tomorrow = dailyForecast.daily[1].temp.max;
                                                    temp_min_tomorrow = dailyForecast.daily[1].temp.min;
                                                    humidity_tomorrow = dailyForecast.daily[1].humidity;

                                                    // change weather data flag
                                                    weatherDataGathered = true;
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
                                    queue.add(forecastWeatherReq);
                                }
                            }, 1000);
                        }
                    },
                    new Response.ErrorListener() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            handler.removeCallbacks(getWeatherRunnable);
                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                            startActivity(intent);
                                            handler.postDelayed(getWeatherRunnable, 10000);
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            break;
                                    }
                                }
                            };
                            AlertDialog.Builder builder = new AlertDialog.Builder(thisContext);
                            builder.setMessage("It looks like you are not connected to the internet\n\nWould you like to connect?").setPositiveButton("Yes", dialogClickListener)
                                    .setNegativeButton("No", dialogClickListener).show();
                        }
                    });
            queue.add(currWeatherReq);
        } else {
            Log.d(TAG, "getWeather: Weather data already gathered");
        }
    }

    public void checkGoal() {
        syncWithPI();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (weatherDataGathered && connectedToPi) {
                    // send weather info and get goal
                    sendWeather();
                } else if (!weatherDataGathered) {
                    getWeather(currUrl);
                } else if (!connectedToPi) {
                    // prompt user to change wifi to the PI
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                    startActivity(intent);
                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;
                            }
                        }
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(thisContext);
                    builder.setMessage("It looks like you have not connected to the PI\n\nWould you like to connect?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            }
        }, 2000);
    }

    public void syncWithPI() {
        try {
            mqttClient.connect();
            mqttClient.client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "connectComplete: Connection was successful");
                    mqttClient.subscribe(subTopic);
                    connectedToPi = true;
                }

                @Override
                public void connectionLost(Throwable cause) {
                    connectedToPi = false;
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // goal will come here
                    Log.d(TAG, "messageArrived: Message incoming");
                    String res = message.toString();
                    stepGoal = gson.fromJson(res, StepGoal.class);
                    Log.d(TAG, String.valueOf(stepGoal.currSteps));
                    if (stepGoal.currSteps >= stepGoal.stepGoalToday) {
                        goalText.setText("You did it!");
                    } else {
                        goalText.setText("Keep Going!");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "deliveryComplete: Message was delivered!");
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "syncWithPI: There was a problem connecting to the PI");
        }

    }
    public void startRunnable() {
        // checks if the user is connected to wifi and has not retrieved the weather data yet
        getWeatherRunnable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 3000);
                if (!weatherDataGathered) {
                    Log.d(TAG, "run: Attempting to get weather");
                    getWeather(currUrl);
                } else {
                    handler.removeCallbacks(this);
                }
            }
        };
        handler.postDelayed(getWeatherRunnable, 0000);
    }
    public void sendWeather() {
       if (!weatherDataGathered) {
           getWeather(currUrl);
       } else {
           // publish the weather
           try {
               JSONObject jsonPayload = new JSONObject();
               jsonPayload.put("temp_min_curr", temp_min_curr);
               jsonPayload.put("temp_max_curr", temp_max_curr);
               jsonPayload.put("humidity_curr", humidity_curr);
               jsonPayload.put("temp_max_tomorrow", temp_max_tomorrow);
               jsonPayload.put("temp_min_tomorrow", temp_min_tomorrow);
               jsonPayload.put("humidity_tomorrow", humidity_tomorrow);
               mqttClient.publish(publishTopic, jsonPayload.toString());
           } catch (Exception e) {
               Log.d(TAG, "onClick: There was a sending the weather..");
               CharSequence text = "Make sure that the weather has been gathered..";
               int duration = Toast.LENGTH_LONG;

               Toast toast = Toast.makeText(thisContext, text, duration);
               toast.show();
           }
       }
    }
}