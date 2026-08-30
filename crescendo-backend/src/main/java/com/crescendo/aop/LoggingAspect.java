package com.crescendo.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class LoggingAspect {

    /**
     * Logger for LoggingAspect
     * This logger is used to log exceptions thrown in the application.
     * It helps in debugging and tracking errors in the service layers.
     */
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Logs exceptions thrown in any service class.
     * Captures unhandled service exceptions. Expected 4xx client responses
     * (e.g. bad password, duplicate email, missing resource) are logged at DEBUG
     * level to avoid polluting test and production logs with alarming ERROR traces.
     */
    @AfterThrowing(pointcut = "execution(* com.crescendo..*Service.*(..))", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {
        if (ex instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            logger.debug("Client error response in {}.{}(): status={} reason={}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    rse.getStatusCode(),
                    rse.getReason());
            return;
        }

        logger.error("Exception in {}.{}() with cause = {} and message = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                ex.getCause() != null ? ex.getCause() : "NULL",
                ex.getMessage(),
                ex);
    }
}
