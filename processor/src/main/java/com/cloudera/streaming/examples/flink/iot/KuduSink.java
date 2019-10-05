package com.cloudera.streaming.examples.flink.iot;

import com.cloudera.streaming.examples.flink.iot.types.WindowedErrors;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.kudu.client.*;

public class KuduSink extends RichSinkFunction<WindowedErrors> {
    private String tableName = "iot-errors";
    private transient KuduClient client;
    private transient KuduTable table;
    private transient KuduSession session;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        client = new KuduClient.KuduClientBuilder(KuduConnection.KUDU_MASTERS).build();
        table = client.openTable(tableName);
        session = client.newSession();
    }

    @Override
    public void close() throws Exception {
        super.close();
        session.close();
        client.close();
    }

    @Override
    public void invoke(WindowedErrors value, Context context) throws Exception {
        Upsert upsert = table.newUpsert();
        PartialRow row = upsert.getRow();
        row.addInt("plant_id", value.plant_id);
        row.addInt("error_cnt", value.error_cnt);
        row.addTimestamp("window_start", value.window_start);
        session.apply(upsert);
    }
}
