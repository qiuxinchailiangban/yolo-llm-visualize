package com.hospital.followup.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class PatientProcessNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String nextNo() {
        return "PP" + LocalDateTime.now().format(FORMATTER) + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
