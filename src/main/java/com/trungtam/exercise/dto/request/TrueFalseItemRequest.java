package com.trungtam.exercise.dto.request;

public record TrueFalseItemRequest(
        String text,
        String image,
        boolean answer
) {}
