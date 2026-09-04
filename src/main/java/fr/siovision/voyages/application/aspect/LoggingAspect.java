package fr.siovision.voyages.application.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Profile("dev")
@Slf4j // Utilise SLF4J
public class LoggingAspect {

    // Pointcut qui cible toutes les méthodes dans les classes annotées @RestController
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logAroundRest(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();

        // 1. Log des Entrées (Endpoint, Méthode HTTP et Payload)
        String endpoint = request.getRequestURI();
        String httpMethod = request.getMethod();

        log.info("---- REQUÊTE ENTRÉE ----");
        log.info("Endpoint: {} {}", httpMethod, endpoint);
        log.info("Méthode: {}.{}", className, methodName);
        log.info("------------------------");

        long startTime = System.currentTimeMillis();
        Object result;

        try {
            // Exécution de la méthode du contrôleur
            result = joinPoint.proceed();
        } catch (Throwable e) {
            long timeTaken = System.currentTimeMillis() - startTime;
            log.error("!!!! ERREUR {}.{} - {} ms : {}", className, methodName, timeTaken, e.getMessage());
            throw e;
        }

        // 2. Log de la Sortie (Réponse, Contenu et Temps)
        long timeTaken = System.currentTimeMillis() - startTime;

        log.info("---- RÉPONSE SORTIE ----");
        log.info("Endpoint: {} {}", httpMethod, endpoint);
        log.info("Temps: {} ms", timeTaken);
        log.info("------------------------");

        return result;
    }
}
