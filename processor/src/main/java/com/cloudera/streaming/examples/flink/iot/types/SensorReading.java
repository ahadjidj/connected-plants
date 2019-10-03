package com.cloudera.streaming.examples.flink.iot.types;

import java.sql.Timestamp;

public class SensorReading {

    public SensorReading(){}

    public int machine_id;
    public int plant_id;
    public long sensor_ts;
    public int is_healthy;
    public CDSWResponse response;
    public int speed;
    public int pressure;
    public boolean error;
    public int vibration;
    public int noise;
    public int temperature;
    public int batch_size;

    @Override
    public String toString() {
        return "SensorReading{" +
                "machine_id=" + machine_id +
                ", plant_id=" + plant_id +
                ", sensor_ts=" + new Timestamp(sensor_ts / 1000).toString() +
                ", is_healthy=" + is_healthy +
                ", response=" + response +
                ", speed=" + speed +
                ", pressure=" + pressure +
                ", error=" + error +
                ", vibration=" + vibration +
                ", noise=" + noise +
                ", temperature=" + temperature +
                ", batch_size=" + batch_size +
                '}';
    }
}
