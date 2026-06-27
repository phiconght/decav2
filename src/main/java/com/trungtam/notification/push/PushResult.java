package com.trungtam.notification.push;

/**
 * Ket qua gui push toi 1 token.
 * - SUCCESS: gui thanh cong.
 * - FAILED: that bai tam thoi (co the retry).
 * - UNREGISTERED: token khong con hop le -> nen set active=false.
 */
public record PushResult(Status status, String message) {

    public enum Status {
        SUCCESS,
        FAILED,
        UNREGISTERED
    }

    public static PushResult success() {
        return new PushResult(Status.SUCCESS, null);
    }

    public static PushResult failed(String message) {
        return new PushResult(Status.FAILED, message);
    }

    public static PushResult unregistered(String message) {
        return new PushResult(Status.UNREGISTERED, message);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
