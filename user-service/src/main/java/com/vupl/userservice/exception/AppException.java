package com.vupl.userservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;
    public AppException(String message, HttpStatus status) { super(message); this.status = status; }
    public static AppException notFound(String m)    { return new AppException(m, HttpStatus.NOT_FOUND); }
    public static AppException badRequest(String m)  { return new AppException(m, HttpStatus.BAD_REQUEST); }
    public static AppException unauthorized(String m){ return new AppException(m, HttpStatus.UNAUTHORIZED); }
    public static AppException forbidden(String m)   { return new AppException(m, HttpStatus.FORBIDDEN); }
}
