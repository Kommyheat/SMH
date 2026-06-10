package org.example.controller;

import jakarta.validation.ConstraintViolationException;
import org.example.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "요청 값이 올바르지 않습니다.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MessageResponse> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("요청 값이 올바르지 않습니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("요청 형식이 올바르지 않습니다. 날짜 형식(yyyy-MM-dd)을 확인해주세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("서버 오류가 발생했습니다."));
    }
}
