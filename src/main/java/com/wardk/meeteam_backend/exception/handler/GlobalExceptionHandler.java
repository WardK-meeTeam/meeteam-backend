package com.wardk.meeteam_backend.exception.handler;

import com.wardk.meeteam_backend.exception.CustomException;
import com.wardk.meeteam_backend.exception.ErrorCode;
import com.wardk.meeteam_backend.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {

        log.error("CustomException 발생={}",e.getMessage(),e);

        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.getResponse(errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(response);

    }



}
