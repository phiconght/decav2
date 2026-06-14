package com.trungtam.exercise.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trungtam.exercise.entity.ChoiceOption;

public record ChoiceOptionResponse(
        Long id,
        int order,
        String text,
        String image,
        @JsonProperty("isCorrect") boolean isCorrect
) {
    public static ChoiceOptionResponse from(ChoiceOption o) {
        return new ChoiceOptionResponse(
                o.getId(),
                o.getSortOrder(),
                o.getText(),
                o.getImage(),
                o.isCorrect()
        );
    }
}
