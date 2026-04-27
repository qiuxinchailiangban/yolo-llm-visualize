package com.hospital.followup.persistence;

import com.hospital.followup.domain.enums.PatientStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PatientStatusConverter implements AttributeConverter<PatientStatus, String> {

    @Override
    public String convertToDatabaseColumn(PatientStatus attribute) {
        return EnumConverters.write(attribute);
    }

    @Override
    public PatientStatus convertToEntityAttribute(String dbData) {
        return EnumConverters.read(PatientStatus.class, dbData);
    }
}
