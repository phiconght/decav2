package com.trungtam.coin.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Body cho POST /coin-topups/{id}/adjust — cong/tru truc tiep so Xu cua 1
 * yeu cau nap, doc lap voi buoc xac nhan (dung duoc bat cu luc nao, ke ca
 * sau khi da CONFIRMED, tru yeu cau da CANCELLED).
 */
public record AdjustCoinTopupRequest(
        @NotNull Long adjustmentCoinAmount,
        String adjustmentNote
) {}
