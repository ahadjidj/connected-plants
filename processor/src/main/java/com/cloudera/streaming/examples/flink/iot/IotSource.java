package com.cloudera.streaming.examples.flink.iot;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;

public class IotSource extends RichParallelSourceFunction<SensorReading> {

    private final long TEN_SEC = 10*1000;
    private volatile boolean running = true;
    @Override
    public void run(SourceContext<SensorReading> sourceContext) throws Exception {
        String json = "{\"sensor_id\":58,\"sensor_ts\":1568298378304175,\"is_healthy\":null,\"response\":null,\"sensor_0\":4,\"sensor_1\":12,\"sensor_2\":2,\"sensor_3\":51,\"sensor_4\":10,\"sensor_5\":93,\"sensor_6\":15,\"sensor_7\":96,\"sensor_8\":44,\"sensor_9\":9,\"sensor_10\":3,\"sensor_11\":4}";
        ObjectMapper objectMapper = new ObjectMapper();
        SensorReading reading = objectMapper.readValue(json, SensorReading.class);

        while (running){
            reading.sensor_ts = System.currentTimeMillis();
            sourceContext.collectWithTimestamp(reading, reading.sensor_ts);
            sourceContext.emitWatermark(new Watermark(reading.sensor_ts - TEN_SEC));
        }
    }

    @Override
    public void cancel() {
        running = false;
    }
}