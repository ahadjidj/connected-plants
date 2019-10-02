package com.cloudera.streaming.examples.flink.iot;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.OutputTag;

public class IotPipeline {

    public static void main(String[] args) throws Exception {

        KuduConnection kuduConnection = new KuduConnection(KuduConnection.KUDU_MASTERS);
        kuduConnection.createIOTTable();
        System.out.println(kuduConnection.listTables());

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        final OutputTag<SensorReading> lateOutputTag = new OutputTag<SensorReading>("late-data"){};

        DataStream<SensorReading> readings = env.addSource(new IotSource());

        SingleOutputStreamOperator<SensorReading> aggregates = readings
                .keyBy(reading -> reading.sensor_id)
                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
                .allowedLateness(Time.seconds(3))
                .sideOutputLateData(lateOutputTag)
                .reduce(new ReduceFunction<SensorReading>() {
                    @Override
                    public SensorReading reduce(SensorReading sr1, SensorReading sr2) throws Exception {
                        sr1.sensor_0 += sr2.sensor_0;
                        return sr1;
                    }
                });

        DataStream<SensorReading> lateStream = aggregates.getSideOutput(lateOutputTag);

        lateStream.printToErr();
        aggregates.print();

        env.execute();
    }

}
