package com.example.iot_ms5_java;

public class WeatherResult {

    public int id;
    public String name;
    public int cod;
    public Coordinate coord;
    public WeatherMain main;
    public Weather[] weather;

    public WeatherResult(int id, String name, int cod, Coordinate coord, WeatherMain main, Weather[] weather) {
        this.id = id;
        this.name = name;
        this.cod = cod;
        this.coord = coord;
        this.main = main;
        this.weather = weather;
    }
}
