package fr.siovision.voyages.application.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock ProceedingJoinPoint joinPoint;
    @Mock Signature signature;
    @Mock HttpServletRequest request;

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logAroundRest_doesNotReadRequestArgumentsOrStringifyResponse() throws Throwable {
        Object sensitiveResponse = new Object() {
            @Override
            public String toString() {
                throw new AssertionError("A response containing a token must not be stringified");
            }
        };
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("verify");
        when(signature.getDeclaringType()).thenReturn((Class) OtpController.class);
        when(request.getRequestURI()).thenReturn("/api/otp/verify");
        when(request.getMethod()).thenReturn("POST");
        when(joinPoint.proceed()).thenReturn(sensitiveResponse);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Object result = new LoggingAspect().logAroundRest(joinPoint);

        assertThat(result).isSameAs(sensitiveResponse);
        verify(joinPoint, never()).getArgs();
    }
}
