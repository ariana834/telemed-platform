package com.telemedicina.consultation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FormAnswersRequest {

    @NotEmpty(message = "Trebuie sa furnizezi cel putin un raspuns")
    @Valid
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {

        @NotNull(message = "ID-ul intrebarii este obligatoriu")
        private Long questionId;

        @NotBlank(message = "Raspunsul nu poate fi gol")
        private String answerText;
    }
}