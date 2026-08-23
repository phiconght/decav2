package com.trungtam.marketing.dto.response;

import com.trungtam.marketing.entity.Testimonial;

public record TestimonialItem(
        Long id,
        String name,
        String meta,
        String quote
) {
    public static TestimonialItem from(Testimonial t) {
        return new TestimonialItem(t.getId(), t.getName(), t.getMeta(), t.getQuote());
    }
}
