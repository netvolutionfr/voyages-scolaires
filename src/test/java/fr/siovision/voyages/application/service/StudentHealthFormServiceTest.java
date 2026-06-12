package fr.siovision.voyages.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.siovision.voyages.domain.model.StudentHealthForm;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.StudentHealthFormRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentHealthFormServiceTest {

    @Mock
    StudentHealthFormRepository studentHealthFormRepository;

    @Mock
    CurrentUserService currentUserService;

    private ObjectMapper objectMapper;
    private StudentHealthFormService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        HealthFormPayloadRenderer healthFormPayloadRenderer = new HealthFormPayloadRenderer(objectMapper);
        service = new StudentHealthFormService(
                studentHealthFormRepository,
                currentUserService,
                healthFormPayloadRenderer,
                objectMapper
        );
    }

    @Test
    void getFormByStudent_usesPersistedUpdatedAtInResponseWithoutSaving() throws Exception {
        User user = new User();
        user.setId(42L);

        Instant persistedUpdatedAt = Instant.parse("2026-01-02T03:04:05Z");
        StudentHealthForm form = new StudentHealthForm();
        form.setStudent(user);
        form.setUpdatedAt(persistedUpdatedAt);
        form.setPayload("""
                {"allergies":"none","updatedAt":"2099-01-01T00:00:00Z"}
                """);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentHealthFormRepository.findByStudentId(42L)).thenReturn(form);

        String response = service.getFormByStudent();

        assertThat(objectMapper.readTree(response).get("updatedAt").asText())
                .isEqualTo(persistedUpdatedAt.toString());
        verify(studentHealthFormRepository, never()).save(any(StudentHealthForm.class));
    }

    @Test
    void getFormByStudent_isReadOnly() throws NoSuchMethodException {
        Method method = StudentHealthFormService.class.getMethod("getFormByStudent");
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }
}
