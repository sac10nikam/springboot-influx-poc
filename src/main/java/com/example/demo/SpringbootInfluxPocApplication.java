package com.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.influxdb.InfluxDBProperties;
import org.springframework.data.influxdb.InfluxDBTemplate;

import com.example.demo.entity.MemoryPoint;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableConfigurationProperties(InfluxDBProperties.class)
@Slf4j
public class SpringbootInfluxPocApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootInfluxPocApplication.class, args);
	}

	@Autowired
	private InfluxDBTemplate<Point> influxDBTemplate;

	@Override
	public void run(String... args) throws Exception {
		influxDBTemplate.createDatabase();
		final Point p = Point.measurement("disk")
				.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.tag("tenant", "default")
				.addField("used", 80L)
				.addField("free", 1L)
				.build();
		influxDBTemplate.write(p);
		final Query q = new Query("SELECT * FROM disk", influxDBTemplate.getDatabase());
		QueryResult queryResult = influxDBTemplate.query(q);

		InfluxDBResultMapper resultMapper = new InfluxDBResultMapper(); // thread-safe - can be reused
		List<Disk> disks = resultMapper.toPOJO(queryResult, Disk.class);
		disks.stream().forEach(System.out::println);
		
		
		// create one more table named as memory
		 BatchPoints batchPoints = BatchPoints
	                .database(influxDBTemplate.getDatabase())
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
	        
	        List<Point> list = new ArrayList<>();
	        list.add(point1);
	        list.add(point2);
	        influxDBTemplate.write(list);

	        List<MemoryPoint> memoryPointList = getPoints(influxDBTemplate.getConnection(), "Select * from memory order by time desc", "boot");
	        memoryPointList.stream().forEach(System.out::println);
	}

	private static List<MemoryPoint> getPoints(InfluxDB connection, String query, String databaseName) {

        // Run the query
        Query queryObject = new Query(query, databaseName);
        QueryResult queryResult = connection.query(queryObject);

        // Map it
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        return resultMapper.toPOJO(queryResult, MemoryPoint.class);
    }

}
