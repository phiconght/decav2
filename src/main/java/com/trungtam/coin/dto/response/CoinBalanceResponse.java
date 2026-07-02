package com.trungtam.coin.dto.response;

/**
 * So du Xu hien tai cua 1 hoc vien (kem thong tin dinh danh de FE hien thi).
 */
public record CoinBalanceResponse(
        Long userId,
        String fullName,
        String username,
        Long balance
) {
}
