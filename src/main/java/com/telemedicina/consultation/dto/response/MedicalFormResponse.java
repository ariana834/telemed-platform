package com.telemedicina.consultation.dto.response;

import com.telemedicina.consultation.model.QuestionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MedicalFormResponse {
    private Long id;
    private String questionText;
    private QuestionType questionType;
    private List<String> options;
    private Integer orderIndex;
    private Boolean isRequired;
}