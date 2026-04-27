package com.hospital.followup.persistence;

import com.hospital.followup.domain.enums.QuestionnaireTaskStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class QuestionnaireTaskStatusConverter implements AttributeConverter<QuestionnaireTaskStatus, String> {

    @Override
    public String convertToDatabaseColumn(QuestionnaireTaskStatus attribute) {
        return EnumConverters.write(attribute);
    }

    @Override
    public QuestionnaireTaskStatus convertToEntityAttribute(String dbData) {
        return EnumConverters.read(QuestionnaireTaskStatus.class, dbData);
    }
}
