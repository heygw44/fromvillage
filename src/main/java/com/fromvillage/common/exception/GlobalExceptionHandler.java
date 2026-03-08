package com.fromvillage.common.exception;

import com.fromvillage.common.response.ErrorResponse;
import com.fromvillage.common.response.ValidationErrorData;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        List<ValidationErrorData> errors = exception.getConstraintViolations().stream()
                .map(this::toValidationError)
                .toList();

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception exception) {
        return ResponseEntity.status(ErrorCode.COMMON_INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INTERNAL_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return badRequest(fromBindingResult(exception.getBindingResult()));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<ValidationErrorData> errors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors && parameterErrors.hasFieldErrors()) {
                parameterErrors.getFieldErrors().forEach(fieldError ->
                        errors.add(new ValidationErrorData(fieldError.getField(), fieldError.getDefaultMessage()))
                );
                continue;
            }

            String field = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable resolvableError : result.getResolvableErrors()) {
                errors.add(new ValidationErrorData(field, resolvableError.getDefaultMessage()));
            }
        }

        return badRequest(errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return badRequest(List.of());
    }

    private ResponseEntity<Object> badRequest(List<ValidationErrorData> errors) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, errors));
    }

    private List<ValidationErrorData> fromBindingResult(BindingResult bindingResult) {
        List<ValidationErrorData> errors = new ArrayList<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.add(new ValidationErrorData(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        for (ObjectError globalError : bindingResult.getGlobalErrors()) {
            errors.add(new ValidationErrorData(globalError.getObjectName(), globalError.getDefaultMessage()));
        }
        return errors;
    }

    private ValidationErrorData toValidationError(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() == null ? null : violation.getPropertyPath().toString();
        return new ValidationErrorData(field, violation.getMessage());
    }
}
