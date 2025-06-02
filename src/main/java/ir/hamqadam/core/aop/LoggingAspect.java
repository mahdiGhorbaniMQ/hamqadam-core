package ir.hamqadam.core.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Pointcut for all methods in the service package.
     */
    @Pointcut("within(ir.hamqadam.core.service..*)")
    public void servicePackagePointcut() {
        // Method is empty as this is just a Pointcut declaration
    }

    /**
     * Pointcut for all public methods in the controller package.
     */
    @Pointcut("within(ir.hamqadam.core.controller..*) && execution(public * *(..))")
    public void controllerPackagePointcut() {
        // Method is empty as this is just a Pointcut declaration
    }

    /**
     * Around advice for logging method execution in specified packages.
     * Logs entry, exit, execution time, arguments, and return values or exceptions.
     *
     * @param joinPoint The proceeding join point.
     * @return The result of the method execution.
     * @throws Throwable If an error occurs during method execution.
     */
    @Around("servicePackagePointcut() || controllerPackagePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        if (logger.isInfoEnabled()) { // More performant to check if logging level is enabled
            logger.info("Enter: {}.{}() with argument[s] = {}", className, methodName, Arrays.toString(joinPoint.getArgs()));
        }

        long startTime = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            if (logger.isInfoEnabled()) {
                logger.info("Exit: {}.{}() with result = {}; Execution time = {} ms", className, methodName, result, (endTime - startTime));
            }
            return result;
        } catch (IllegalArgumentException e) {
            logger.error("Illegal argument: {} in {}.{}()", Arrays.toString(joinPoint.getArgs()), className, methodName);
            throw e;
        } catch (Throwable t) {
            logger.error("Exception in {}.{}() with cause = '{}' and exception = '{}'", className, methodName,
                    t.getCause() != null ? t.getCause() : "NULL", t.getMessage(), t);
            throw t;
        }
    }
}