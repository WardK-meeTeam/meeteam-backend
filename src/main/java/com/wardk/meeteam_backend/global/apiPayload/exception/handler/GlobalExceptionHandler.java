package com.wardk.meeteam_backend.global.apiPayload.exception.handler;

import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {

        log.error("CustomException 발생={}",e.getMessage());

        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.getResponse(errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(response);

    }



}
