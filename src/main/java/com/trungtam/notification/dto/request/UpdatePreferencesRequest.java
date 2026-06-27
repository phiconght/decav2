package com.trungtam.notification.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cap nhat toan bo bang tuy chon nhan thong bao cua nguoi dung hien tai.
 */
public record UpdatePreferencesRequest(
        @NotNull @Valid List<UpdatePreferenceRequest> items
) {
}
