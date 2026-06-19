package com.trungtam.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Mat khau moi khong duoc de trong")
        @Size(min = 6, max = 100, message = "Mat khau phai tu 6 den 100 ky tu")
        String newPassword
) {}
