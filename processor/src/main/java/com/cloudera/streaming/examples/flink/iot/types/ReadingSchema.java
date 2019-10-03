package com.cloudera.streaming.examples.flink.iot.types;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ReadingSchema implements KeyedSerializationSchema<SensorReading>, DeserializationSchema<SensorReading> {
    private static final Logger LOG = LoggerFactory.getLogger(ReadingSchema.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TypeInformation<SensorReading> typeInfo = new TypeHint<SensorReading>(){}.getTypeInfo();

    @Override
    public SensorReading deserialize(byte[] bytes) throws IOException {
        return objectMapper.readValue(new String(bytes), SensorReading.class);
    }

    @Override
    public boolean isEndOfStream(SensorReading sensorReading) {
        return false;
    }

    @Override
    public TypeInformation<SensorReading> getProducedType() {
        return typeInfo;
    }

    @Override
    public byte[] serializeKey(SensorReading sensorReading) {
        return ByteBuffer.allocate(4).putInt(sensorReading.machine_id).array();
    }

    @Override
    public byte[] serializeValue(SensorReading sensorReading) {
        byte[] result = new byte[0];
        try {
             result = objectMapper.writeValueAsBytes(sensorReading);
        } catch (JsonProcessingException e) {
            LOG.warn("Encountered an exception while converting {} to JSON String.", sensorReading);
        }
        return result;
    }

    @Override
    public String getTargetTopic(SensorReading sensorReading) {
        return null;
    }
}
