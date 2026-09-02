package com.uccd3223.group13.foodhero.data.callback;

public class DataError {
    public static final int CODE_NETWORK_ERROR = 1001;
    public static final int CODE_INVALID_CREDENTIALS = 1002;
    public static final int CODE_NOT_FOUND = 1003;
    public static final int CODE_INSUFFICIENT_STOCK = 1004;
    public static final int CODE_PRICE_CEILING_EXCEEDED = 1005;
    public static final int CODE_OUTSIDE_CAMPUS = 1006;
    public static final int CODE_ALREADY_RESERVED = 1007;
    public static final int CODE_INVALID_TOKEN = 1008;
    public static final int CODE_SERVER_ERROR = 1009;
    public static final int CODE_UNKNOWN = 1010;

    private final int code;
    private final String message;
    private final Throwable cause;

    public DataError(int code, String message) {
        this(code, message, null);
    }

    public DataError(int code, String message, Throwable cause) {
        this.code = code;
        this.message = message;
        this.cause = cause;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }
}
