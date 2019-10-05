package com.cloudera.streaming.examples.flink.iot;

import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KuduConnection implements Closeable {
    public static final String KUDU_MASTERS = "ffdemo0.field.hortonworks.com:7051";
    private String tableName = "iot-errors";
    private String kuduMasters;
    private KuduClient kuduClient;

    public KuduConnection(String kuduMasters){
        this.kuduMasters = kuduMasters;
        this.kuduClient = new KuduClient.KuduClientBuilder(this.kuduMasters).build();
    }


    public String listTables() throws KuduException {
        return kuduClient.getTablesList().getTablesList().toString();
    }

    public void createIOTTable() throws KuduException{
        List<ColumnSchema> columns = new ArrayList<>(3);
        columns.add(new ColumnSchema.ColumnSchemaBuilder("window_start", Type.UNIXTIME_MICROS)
                .key(true)
                .build());
        columns.add(new ColumnSchema.ColumnSchemaBuilder("plant_id", Type.INT32)
                .key(true)
                .build());
        columns.add(new ColumnSchema.ColumnSchemaBuilder("error_cnt", Type.INT32)
                .build());
        Schema schema = new Schema(columns);

        CreateTableOptions cto = new CreateTableOptions();
        List<String> hashKeys = new ArrayList<>(2);
        hashKeys.add("plant_id");
        hashKeys.add("window_start");
        int numBuckets = 8;
        cto.addHashPartitions(hashKeys, numBuckets);

        if (!kuduClient.tableExists(tableName)) kuduClient.createTable(tableName, schema, cto);
    }

    public void upsertSensor(String sensor, int value) throws KuduException {
        KuduTable table = kuduClient.openTable(tableName);
        KuduSession session = kuduClient.newSession();

        Upsert upsert = table.newUpsert();
        PartialRow row = upsert.getRow();
        row.addString("sensor", sensor);
        row.addInt("value", value);
        session.apply(upsert);
        session.close();
    }

    @Override
    public void close() throws IOException {
        kuduClient.close();
    }
}
