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

public class ErrorSchema implements KeyedSerializationSchema<WindowedErrors>, DeserializationSchema<WindowedErrors> {
    private static final Logger LOG = LoggerFactory.getLogger(ErrorSchema.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TypeInformation<WindowedErrors> typeInfo = new TypeHint<WindowedErrors>(){}.getTypeInfo();

    @Override
    public WindowedErrors deserialize(byte[] bytes) throws IOException {
        return objectMapper.readValue(new String(bytes), WindowedErrors.class);
    }

    @Override
    public boolean isEndOfStream(WindowedErrors windowedErrors) {
        return false;
    }

    @Override
    public TypeInformation<WindowedErrors> getProducedType() {
        return typeInfo;
    }

    @Override
    public byte[] serializeKey(WindowedErrors windowedErrors) {
        return ByteBuffer.allocate(4).putInt(windowedErrors.plant_id).array();
    }

    @Override
    public byte[] serializeValue(WindowedErrors windowedErrors) {
        byte[] result = new byte[0];
        try {
             result = objectMapper.writeValueAsBytes(windowedErrors);
        } catch (JsonProcessingException e) {
            LOG.warn("Encountered an exception while converting {} to JSON String.", windowedErrors);
        }
        return result;
    }

    @Override
    public String getTargetTopic(WindowedErrors windowedErrors) {
        return null;
    }
}
