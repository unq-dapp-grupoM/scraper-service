package com.dapp.scraper_service.audit;

import com.dapp.scraper_service.model.QueryType;
import com.dapp.scraper_service.service.QueryHistoryService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final QueryHistoryService queryHistoryService;

    public AuditAspect(QueryHistoryService queryHistoryService) {
        this.queryHistoryService = queryHistoryService;
    }

    @Around("@annotation(auditQuery)")
    public Object auditQuery(ProceedingJoinPoint joinPoint, AuditQuery auditQuery) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        String playerName = null;
        String userEmail = null;

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(PathVariable.class) && "player".equals(parameter.getAnnotation(PathVariable.class).value())) {
                playerName = (String) args[i];
            } else if (parameter.isAnnotationPresent(RequestParam.class) && "userEmail".equals(parameter.getAnnotation(RequestParam.class).name())) {
                userEmail = (String) args[i];
            }
        }

        // Proceed with method execution first
        Object result = joinPoint.proceed();

        // Record history only if the method executed successfully
        if (playerName != null && userEmail != null) {
            try {
                QueryType queryType = auditQuery.value();
                queryHistoryService.recordQuery(userEmail, playerName, queryType);
            } catch (Exception e) {
                log.error("Failed to record query history for player '{}' and user '{}'", playerName, userEmail, e);
            }
        }

        return result;
    }
}