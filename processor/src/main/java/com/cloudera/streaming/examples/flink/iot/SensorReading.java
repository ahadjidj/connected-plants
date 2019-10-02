package com.cloudera.streaming.examples.flink.iot;

public class SensorReading {

    public SensorReading(){}

    public int sensor_id;
    public long sensor_ts;
    public int is_healthy;
    public CDSWResponse response;
    public int sensor_0;
    public int sensor_1;
    public int sensor_2;
    public int sensor_3;
    public int sensor_4;
    public int sensor_5;
    public int sensor_6;
    public int sensor_7;
    public int sensor_8;
    public int sensor_9;
    public int sensor_10;
    public int sensor_11;

    @Override
    public String toString() {
        return "SensorReading{" +
                "sensor_id=" + sensor_id +
                ", sensor_ts=" + sensor_ts +
                ", is_healthy=" + is_healthy +
                ", response=" + response +
                ", sensor_0=" + sensor_0 +
                ", sensor_1=" + sensor_1 +
                ", sensor_2=" + sensor_2 +
                ", sensor_3=" + sensor_3 +
                ", sensor_4=" + sensor_4 +
                ", sensor_5=" + sensor_5 +
                ", sensor_6=" + sensor_6 +
                ", sensor_7=" + sensor_7 +
                ", sensor_8=" + sensor_8 +
                ", sensor_9=" + sensor_9 +
                ", sensor_10=" + sensor_10 +
                ", sensor_11=" + sensor_11 +
                '}';
    }
}
