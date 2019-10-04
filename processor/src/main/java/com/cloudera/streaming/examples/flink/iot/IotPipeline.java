package com.cloudera.streaming.examples.flink.iot;

import com.cloudera.streaming.examples.flink.iot.types.ErrorSchema;
import com.cloudera.streaming.examples.flink.iot.types.ReadingSchema;
import com.cloudera.streaming.examples.flink.iot.types.SensorReading;
import com.cloudera.streaming.examples.flink.iot.types.WindowedErrors;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumerBase;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.sql.Timestamp;
import java.util.Properties;

import static org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumerBase.KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS;

public class IotPipeline {

    private static final String KAFA_URL = "ffdemo0.field.hortonworks.com:9092";

    public static void main(String[] args) throws Exception {

        KuduConnection kuduConnection = new KuduConnection(KuduConnection.KUDU_MASTERS);
        kuduConnection.createIOTTable();
        System.out.println(kuduConnection.listTables());

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        final OutputTag<Tuple2<Integer, Integer>> lateOutputTag = new OutputTag<Tuple2<Integer, Integer>>("late-data"){};


        // Kafka Connection setup
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFA_URL);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "flink-processor");
        properties.setProperty(KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS, "60000");
        properties.setProperty("enable.auto.commit", "true");
        properties.setProperty("auto.commit.interval.ms", "1000");
        //properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "flink-processor");
        properties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                "com.hortonworks.smm.kafka.monitoring.interceptors.MonitoringConsumerInterceptor");

        FlinkKafkaConsumerBase<SensorReading> kafkaSource = new FlinkKafkaConsumer<>("iot", new ReadingSchema(), properties)
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<SensorReading>(Time.seconds(5)) {
            @Override
            public long extractTimestamp(SensorReading sensorReading) {
                return sensorReading.sensor_ts / 1000;
            }
        }).setCommitOffsetsOnCheckpoints(true);

        FlinkKafkaProducer<WindowedErrors> kafkaSink =
                new FlinkKafkaProducer<>(KAFA_URL, "iot-errors", new ErrorSchema());

        // Pipeline logic
        DataStream<SensorReading> readings = env.addSource(kafkaSource);
        DataStream<SensorReading> errors = readings
                .filter(sensorReading -> sensorReading.error);

        SingleOutputStreamOperator<WindowedErrors> numErrors = errors
                .map(new MapFunction<SensorReading, Tuple2<Integer, Integer>>() {
                    @Override
                    public Tuple2<Integer, Integer> map(SensorReading sensorReading) throws Exception {
                        return Tuple2.of(sensorReading.plant_id, 1);
                    }
                })
                .keyBy(reading -> reading.f0)
                .timeWindow(Time.seconds(15))
                .allowedLateness(Time.seconds(15))
                .sideOutputLateData(lateOutputTag)
                .reduce((t1, t2) -> Tuple2.of(t1.f0, t1.f1 + t2.f1), new WindowFunction<Tuple2<Integer, Integer>, WindowedErrors, Integer, TimeWindow>() {
                    @Override
                    public void apply(Integer integer, TimeWindow timeWindow, Iterable<Tuple2<Integer, Integer>> iterable, Collector<WindowedErrors> collector) throws Exception {
                        Tuple2<Integer, Integer> reduced = iterable.iterator().next();
                        collector.collect(new WindowedErrors(new Timestamp(timeWindow.getStart()), reduced.f0, reduced.f1));
                    }
                });

        DataStream<Tuple2<Integer, Integer>> lateStream = numErrors.getSideOutput(lateOutputTag);

        lateStream.print();
        errors.print();

        numErrors.addSink(kafkaSink);
        numErrors.printToErr();



        env.execute();
    }

}
