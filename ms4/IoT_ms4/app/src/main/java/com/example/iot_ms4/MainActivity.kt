package com.example.iot_ms4

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.RequestQueue
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    // getting all of the inits ready
    lateinit var textView: TextView
    lateinit var getWeatherBtn: Button
    lateinit var changeFragmentBtn: Button
    lateinit var weatherIcon : ImageView

    // imported from other project
    lateinit var queue: RequestQueue
    lateinit var gson: Gson
    lateinit var mostRecentWeatherResult: WeatherResult

    // called when the app is created
    // main function is embedded
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
