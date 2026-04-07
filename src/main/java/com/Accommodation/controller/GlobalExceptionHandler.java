package com.Accommodation.controller;

import com.Accommodation.exception.AdminException;
import com.Accommodation.exception.MemberException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(MemberException.class)
    public String handleMemberException(MemberException exception,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        response.setStatus(exception.getErrorCode().getStatus().value());
        request.setAttribute("errorMessage", exception.getMessage());
        return "error/4xx";
    }

    @ExceptionHandler(AdminException.class)
    public String handleAdminException(AdminException exception,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        response.setStatus(exception.getErrorCode().getStatus().value());
        request.setAttribute("errorMessage", exception.getMessage());
        return "error/4xx";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleEntityNotFound(EntityNotFoundException exception,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        request.setAttribute("errorMessage",
                exception.getMessage() == null || exception.getMessage().isBlank()
                        ? "요청한 정보를 찾을 수 없습니다."
                        : exception.getMessage());
        return "error/4xx";
    }
}
