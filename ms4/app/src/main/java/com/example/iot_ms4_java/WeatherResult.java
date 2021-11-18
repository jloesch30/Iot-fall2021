package com.example.iot_ms4_java;

import java.lang.reflect.Array;

public class WeatherResult {

    public WeatherResult(int id, String name, int cod, Coordinate coord, WeatherMain main, Weather[] weather) {
        id = id;
        name = name;
        cod = cod;
        coord = coord;
        main = main;
        weather = weather;
    }
}
