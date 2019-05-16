package com.example.demo;

import java.time.Instant;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import lombok.Data;

@Measurement(name = "disk")
@Data
public class Disk {

	@Column(name = "time")
	private Instant time;
	@Column(name = "tenant")
	private String tenant;
	@Column(name = "used")
	private long used;
	@Column(name = "free")
	private long free;
	
}
