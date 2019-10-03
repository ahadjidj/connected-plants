package com.cloudera.streaming.examples.flink.iot.types;

public class CDSWResponse {
    public CDSWResponse(){}

    public int result;

    @Override
    public String toString() {
        return "CDSWResponse{" +
                "result=" + result +
                '}';
    }
}
