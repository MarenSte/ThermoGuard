package com.example.marensteinkamp.thermoguard;

public class TempSensor {

    private double temp;
    private double hum;

    public TempSensor(double temp,double hum){
        this.temp=temp;
        this.hum=hum;
    }

    public double getTemp() {
        return temp;
    }

    public double getHum() {
        return hum;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public void setHum(double hum) {
        this.hum = hum;
    }

}
