package com.thefirstlineofcode.april.boot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.thefirstlineofcode.april.boot.config.AprilApplicationProperties;


@Configuration
@EnableConfigurationProperties(AprilApplicationProperties.class)
public class BootConfiguration {}
