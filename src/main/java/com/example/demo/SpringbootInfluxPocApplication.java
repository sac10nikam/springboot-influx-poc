package com.example.demo;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

@SpringBootApplication
@EnableConfigurationProperties(InfluxDBProperties.class)
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
		for (Disk disk: disks) {
			System.out.println(disk);
		}
	}

}
