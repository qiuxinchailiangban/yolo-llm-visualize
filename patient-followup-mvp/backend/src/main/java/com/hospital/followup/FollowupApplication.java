package com.hospital.followup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FollowupApplication {

    public static void main(String[] args) {
        SpringApplication.run(FollowupApplication.class, args);
    }
}
