package fr.siovision.voyages.application.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
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

        // Le 'payload' d'entrée correspond aux arguments de la méthode du contrôleur
        String inputPayload = Arrays.toString(joinPoint.getArgs());

        log.info("---- REQUÊTE ENTRÉE ----");
        log.info("Endpoint: {} {}", httpMethod, endpoint);
        log.info("Méthode: {}.{}", className, methodName);
        log.info("Payload: {}", inputPayload);
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

        // La 'réponse' correspond à la valeur de retour de la méthode
        String outputResponse = result != null ? result.toString() : "void/null";

        log.info("---- RÉPONSE SORTIE ----");
        log.info("Endpoint: {} {}", httpMethod, endpoint);
        log.info("Temps: {} ms", timeTaken);
        // *ATTENTION* : Logguer le toString() d'objets complexes peut être verbeux.
        // Assurez-vous d'implémenter toString() sur vos DTOs ou d'utiliser un mapper JSON ici.
        log.info("Réponse Contenu: {}", outputResponse);
        log.info("------------------------");

        return result;
    }
}