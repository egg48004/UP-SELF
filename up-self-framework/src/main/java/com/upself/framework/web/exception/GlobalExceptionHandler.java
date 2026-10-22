package com.upself.framework.web.exception;

import com.upself.common.core.domain.AjaxResult;
import com.upself.common.exception.ServiceException;

import org.slf4j.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public AjaxResult service(ServiceException e) {
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult denied() {
        return AjaxResult.error(403, "You do not have permission to perform this action.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult validation(MethodArgumentNotValidException e) {
        return AjaxResult.error(
                400, e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler({
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    public AjaxResult format() {
        return AjaxResult.error(400, "Invalid request parameters.");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public AjaxResult duplicate() {
        return AjaxResult.error(400, "The account already exists.");
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public AjaxResult binding(org.springframework.validation.BindException e) {
        return AjaxResult.error(
                400, e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler({
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class
    })
    public AjaxResult missing() {
        return AjaxResult.error(400, "A required request field is missing.");
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public AjaxResult uploadSize() {
        return AjaxResult.error(400, "The upload exceeds the permitted size.");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public AjaxResult dataIntegrity() {
        return AjaxResult.error(
                400, "The data could not be saved. Check field lengths and linked records.");
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult unexpected(Exception e) {
        LOG.error("Request processing failed", e);
        return AjaxResult.error(
                500, "The service is temporarily unavailable. Please try again later.");
    }
}
