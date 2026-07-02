package com.trungtam.coin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Yeu cau cong/tru Xu cho hoc vien. amount duong = cong, am = tru (khac 0 — validate o service).
 */
public record AdjustCoinRequest(
        @NotNull Long amount,
        @NotBlank @Size(max = 255) String reason
) {
}
