package com.cloudera.streaming.examples.flink.iot;

import com.cloudera.streaming.examples.flink.iot.types.ReadingSchema;
import com.cloudera.streaming.examples.flink.iot.types.SensorReading;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumerBase;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.PropertiesUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import javax.annotation.Nullable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumerBase.KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS;

public class IotPipeline {

    public static void main(String[] args) throws Exception {

        KuduConnection kuduConnection = new KuduConnection(KuduConnection.KUDU_MASTERS);
        kuduConnection.createIOTTable();
        System.out.println(kuduConnection.listTables());

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        final OutputTag<Tuple2<Integer, Integer>> lateOutputTag = new OutputTag<Tuple2<Integer, Integer>>("late-data"){};

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "ffdemo0.field.hortonworks.com:9092");
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "flink-processor");
        properties.setProperty(KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS, "60000");

        FlinkKafkaConsumerBase<SensorReading> kafkaSource = new FlinkKafkaConsumer<>("iot", new ReadingSchema(), properties)
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<SensorReading>(Time.minutes(5)) {
            @Override
            public long extractTimestamp(SensorReading sensorReading) {
                return sensorReading.sensor_ts;
            }
        });

        DataStream<SensorReading> readings = env.addSource(kafkaSource);

        DataStream<Tuple2<Integer, Integer>> numErrors = readings
                .filter(sensorReading -> sensorReading.error)
                .map(new MapFunction<SensorReading, Tuple2<Integer, Integer>>() {
                    @Override
                    public Tuple2<Integer, Integer> map(SensorReading sensorReading) throws Exception {
                        return Tuple2.of(sensorReading.plant_id, 1);
                    }
                })
                .keyBy(reading -> reading.f0)
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .allowedLateness(Time.minutes(3))
                .sideOutputLateData(lateOutputTag)
                .sum(1);

        readings.print();
        numErrors.printToErr();

        env.execute();
    }

}
