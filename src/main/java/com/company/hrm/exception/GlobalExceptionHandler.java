package com.company.hrm.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException exception, HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        log.warn("Business rule rejected: {}", exception.getMessage());
        String referer = request.getHeader("Referer");
        String target = "/dashboard";
        if (referer != null) {
            try {
                URI candidate = URI.create(referer);
                boolean sameOrigin = !candidate.isAbsolute()
                        ? candidate.getRawPath() != null && candidate.getRawPath().startsWith("/")
                            && !candidate.getRawPath().startsWith("//")
                        : request.getServerName().equalsIgnoreCase(candidate.getHost());
                if (sameOrigin) {
                    target = candidate.getRawPath();
                    if (candidate.getRawQuery() != null) {
                        target += "?" + candidate.getRawQuery();
                    }
                    if (candidate.getRawFragment() != null) {
                        target += "#" + candidate.getRawFragment();
                    }
                }
            } catch (IllegalArgumentException ignored) {
                target = "/dashboard";
            }
        }
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:" + target;
    }
}