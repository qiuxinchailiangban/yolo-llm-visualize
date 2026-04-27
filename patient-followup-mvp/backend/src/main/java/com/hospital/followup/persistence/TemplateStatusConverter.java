package com.hospital.followup.persistence;

import com.hospital.followup.domain.enums.TemplateStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TemplateStatusConverter implements AttributeConverter<TemplateStatus, String> {

    @Override
    public String convertToDatabaseColumn(TemplateStatus attribute) {
        return EnumConverters.write(attribute);
    }

    @Override
    public TemplateStatus convertToEntityAttribute(String dbData) {
        return EnumConverters.read(TemplateStatus.class, dbData);
    }
}
