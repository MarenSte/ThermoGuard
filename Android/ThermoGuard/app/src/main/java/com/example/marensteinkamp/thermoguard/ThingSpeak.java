package com.example.marensteinkamp.thermoguard;

public class ThingSpeak {

    private int channel_id;
    private String write_key;
    private String read_key;
    private String channel_name;

    public ThingSpeak(int channel_id, String write_key, String read_key, String channel_name){
        this.channel_id=channel_id;
        this.write_key=write_key;
        this.read_key=read_key;
        this.channel_name=channel_name;
    }

    public int getChannel_id() {
        return channel_id;
    }

    public void setChannel_id(int channel_id) {
        this.channel_id = channel_id;
    }

    public String getWrite_key() {
        return write_key;
    }

    public void setWrite_key(String write_key) {
        this.write_key = write_key;
    }

    public String getRead_key() {
        return read_key;
    }

    public void setRead_key(String read_key) {
        this.read_key = read_key;
    }

    public String getChannel_name() {
        return channel_name;
    }

    public void setChannel_name(String channel_name) {
        this.channel_name = channel_name;
    }
}
