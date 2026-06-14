package com.trungtam.exercise.dto.response;

import com.trungtam.exercise.entity.TrueFalseItem;

public record TrueFalseItemResponse(
        Long id,
        int order,
        String text,
        String image,
        boolean answer
) {
    public static TrueFalseItemResponse from(TrueFalseItem t) {
        return new TrueFalseItemResponse(
                t.getId(),
                t.getSortOrder(),
                t.getText(),
                t.getImage(),
                t.isAnswer()
        );
    }
}
