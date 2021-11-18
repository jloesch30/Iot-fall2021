package com.example.iot_ms4_java;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

public class Fragment_1 extends Fragment {

    TextView weatherTextView;
    Button getWeatherBtn;
    Button sendWeatherBtn;

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
    }

    public void requestWeather() {
        String url = "https://api.openweathermap.org/data/2.5/weather?id=4254010&appid=62823359f56f5a425fd7d24a47acaf35";

        // Request a string response
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        weatherTextView.setText(response.substring(0, 500));
                    }
                },
                new Response.ErrorListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        weatherTextView.setText("That didn't work!");
                    }
                });

                queue.add(stringRequest);

                Log.d("toString", (String) url);
    }
}
