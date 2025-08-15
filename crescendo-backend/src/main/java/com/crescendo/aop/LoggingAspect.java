package com.crescendo.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
     * This method captures exceptions thrown by any method in the service package and logs them.
     * The log contains the name of the class, the method name and the cause if available
     * @param joinPoint The join point providing reflective access to both the state available at a join point and static information about it.
     * @param ex The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* com.crescendo..*Service.*(..))", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {
        logger.error("Exception in {}.{}() with cause = {} and message = {}",

                /// First {} is replaced by joinPoint.getSignature().getDeclaringTypeName()
                /// Example: com.crescendo.user.user_command.UserCommandService
                joinPoint.getSignature().getDeclaringTypeName(),

                /// Second {} is replaced by joinPoint.getSignature().getName()
                /// Example: registerUser
                joinPoint.getSignature().getName(),

                /// Third {} is replaced by ex.getCause() != null ? ex.getCause() : "NULL"
                /// Example: The underlying cause of the exception, or the string "NULL" if there isn't one
                ex.getCause() != null ? ex.getCause() : "NULL",

                /// Fourth {} is replaced by ex.getMessage()
                /// Example: A specific error message like "User with this email already exists."
                ex.getMessage(), ex);
    }
}
