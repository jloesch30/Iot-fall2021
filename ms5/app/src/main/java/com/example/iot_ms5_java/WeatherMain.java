package com.example.iot_ms5_java;

public class WeatherMain {
    public Double temp;
    public int pressure;
    public int humidity;
    public Double temp_min;
    public Double temp_max;

    public  WeatherMain(Double temp, int pressure, int humidity, Double temp_min, Double temp_max) {
       this.temp = temp;
       this.pressure = pressure;
       this.humidity = humidity;
       this.temp_min = temp_min;
       this.temp_max = temp_max;
    }
}
