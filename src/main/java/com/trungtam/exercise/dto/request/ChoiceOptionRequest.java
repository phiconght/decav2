package com.trungtam.exercise.dto.request;

public record ChoiceOptionRequest(
        String text,
        String image,
        boolean isCorrect
) {}
