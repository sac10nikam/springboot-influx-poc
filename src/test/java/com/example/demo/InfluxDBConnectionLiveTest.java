package com.example.demo;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.junit.Test;

import com.example.demo.entity.MemoryPoint;


public class InfluxDBConnectionLiveTest {

    @Test
    public void whenCorrectInfoDatabaseConnects() {

        InfluxDB connection = connectDatabase();
        assertTrue(pingServer(connection));
    }

    private InfluxDB connectDatabase() {

        // Connect to database assumed on localhost with default credentials.
        return  InfluxDBFactory.connect("http://10.1.20.25:8086", "admin", "secret");

    }

    private boolean pingServer(InfluxDB influxDB) {
        try {
            // Ping and check for version string
            Pong response = influxDB.ping();
            if (response.getVersion().equalsIgnoreCase("unknown")) {
                return false;
            } else {
                return true;
            }
        } catch (InfluxDBIOException idbo) {
            return false;
        }
    }

    @Test
    public void whenDatabaseCreatedDatabaseChecksOk() {

        InfluxDB connection = connectDatabase();

        // Create "boot" and check for it
        //connection.createDatabase("boot");
        //Query query = new Query("CREATE DATABASE IF NOT EXISTS ", "boot");
        //connection.query(query);
        //assertTrue(query.getDatabase() == "boot");

        // Verify that nonsense databases are not there
        assertFalse(connection.databaseExists("foobar"));

        // Drop "boot" and check again
        //connection.deleteDatabase("boot");
        //assertFalse(connection.databaseExists("boot"));
    }

    @Test
    public void whenPointsWrittenPointsExists() throws Exception {

        InfluxDB connection = connectDatabase();

        String dbName = "boot";
        connection.createDatabase(dbName);

        // Need a retention policy before we can proceed
        connection.createRetentionPolicy("defaultPolicy", "boot", "30d", 1, true);

        // Since we are doing a batch thread, we need to set this as a default
        connection.setRetentionPolicy("defaultPolicy");

        // Enable batch mode
        connection.enableBatch(10, 10, TimeUnit.MILLISECONDS);

        for (int i = 0; i < 10; i++) {
            Point point = Point.measurement("memory")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("name", "server1")
                    .addField("free", 4743656L)
                    .addField("used", 1015096L)
                    .addField("buffer", 1010467L)
                    .build();

            connection.write(dbName, "defaultPolicy", point);
            Thread.sleep(2);

        }

        // Unfortunately, the sleep inside the loop doesn't always add enough time to insure
        // that Influx's batch thread flushes all of the writes and this sometimes fails without
        // another brief pause.
        Thread.sleep(10);

        List<MemoryPoint> memoryPointList = getPoints(connection, "Select * from memory", "boot");

       // assertEquals(12, memoryPointList.size());

        // Turn off batch and clean up
        connection.disableBatch();
        //connection.deleteDatabase("boot");
        connection.close();

    }

    private List<MemoryPoint> getPoints(InfluxDB connection, String query, String databaseName) {

        // Run the query
        Query queryObject = new Query(query, databaseName);
        QueryResult queryResult = connection.query(queryObject);

        // Map it
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        return resultMapper.toPOJO(queryResult, MemoryPoint.class);
    }


    @Test
    public void whenBatchWrittenBatchExists() {

        InfluxDB connection = connectDatabase();

        String dbName = "boot";
        connection.createDatabase(dbName);

        // Need a retention policy before we can proceed
        // Since we are doing batches, we need not set it
        connection.createRetentionPolicy("defaultPolicy", "boot", "30d", 1, true);


        BatchPoints batchPoints = BatchPoints
                .database(dbName)
                .retentionPolicy("defaultPolicy")
                .build();
        Point point1 = Point.measurement("memory")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("free", 4743656L)
                .addField("used", 1015096L)
                .addField("buffer", 1010467L)
                .build();
        Point point2 = Point.measurement("memory")
                .time(System.currentTimeMillis() - 100, TimeUnit.MILLISECONDS)
                .addField("free", 4743696L)
                .addField("used", 1016096L)
                .addField("buffer", 1008467L)
                .build();
        batchPoints.point(point1);
        batchPoints.point(point2);
        connection.write(batchPoints);

        List<MemoryPoint> memoryPointList = getPoints(connection, "Select * from memory", "boot");

       // assertEquals(2, memoryPointList.size());
        assertTrue(4743696L == memoryPointList.get(0).getFree());


        memoryPointList = getPoints(connection, "Select * from memory order by time desc", "boot");

        //assertEquals(2, memoryPointList.size());
        assertTrue(4743656L == memoryPointList.get(0).getFree());

        // Clean up database
        //connection.deleteDatabase("boot");
        connection.close();
    }

}