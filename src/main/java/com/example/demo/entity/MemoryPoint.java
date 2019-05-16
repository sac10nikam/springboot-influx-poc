package com.example.demo.entity;
import java.time.Instant;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import lombok.Data;

@Data
@Measurement(name = "memory")
public class MemoryPoint {

    @Column(name = "time")
    private Instant time;

    @Column(name = "name")
    private String name;

    @Column(name = "free")
    private Long free;

    @Column(name = "used")
    private Long used;

    @Column(name = "buffer")
    private Long buffer;

}