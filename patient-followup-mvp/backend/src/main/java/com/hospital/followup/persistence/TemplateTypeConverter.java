package com.hospital.followup.persistence;

import com.hospital.followup.domain.enums.TemplateType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TemplateTypeConverter implements AttributeConverter<TemplateType, String> {

    @Override
    public String convertToDatabaseColumn(TemplateType attribute) {
        return EnumConverters.write(attribute);
    }

    @Override
    public TemplateType convertToEntityAttribute(String dbData) {
        return EnumConverters.read(TemplateType.class, dbData);
    }
}
