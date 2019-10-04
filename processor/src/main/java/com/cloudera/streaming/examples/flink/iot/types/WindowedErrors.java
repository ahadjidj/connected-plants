package com.cloudera.streaming.examples.flink.iot.types;

import java.sql.Timestamp;

public class WindowedErrors {
    public Timestamp window_start;
    public int plant_id;
    public int error_cnt;

    public WindowedErrors(){}

    public WindowedErrors(Timestamp window_start, int plant_id, int error_cnt) {
        this.window_start = window_start;
        this.plant_id = plant_id;
        this.error_cnt = error_cnt;
    }

    @Override
    public String toString() {
        return "WindowedErrors{" +
                "window_start=" + window_start +
                ", plant_id=" + plant_id +
                ", error_cnt=" + error_cnt +
                '}';
    }
}
