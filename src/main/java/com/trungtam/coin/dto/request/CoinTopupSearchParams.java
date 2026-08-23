package com.trungtam.coin.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Tham so loc + phan trang danh sach yeu cau nap Xu (Admin). */
@Getter
@Setter
public class CoinTopupSearchParams {

    private Long studentId;
    private String status;
    private int current = 1;
    private int pageSize = 10;
}
