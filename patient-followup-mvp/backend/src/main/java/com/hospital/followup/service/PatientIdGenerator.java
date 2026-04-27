package com.hospital.followup.service;

import com.hospital.followup.repository.PatientRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class PatientIdGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final PatientRepository patientRepository;

    public PatientIdGenerator(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public String nextId() {
        String prefix = "PT" + LocalDate.now().format(FORMATTER);
        String candidate;
        do {
            candidate = prefix + String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
        } while (patientRepository.existsByPatientId(candidate) || patientRepository.existsByPatientNo(candidate));
        return candidate;
    }
}
