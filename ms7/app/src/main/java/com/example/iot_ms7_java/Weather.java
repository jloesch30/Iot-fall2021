package com.example.iot_ms7_java;

public class Weather {
    // make these public to be accessed elsewhere
    public int id;
    public String main;
    public String icon;
    public String description;

    public Weather(int id, String main, String description, String icon) {
       this.id = id;
       this.main = main;
       this.description = description;
       this.icon = icon;
    }
}
