package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock FileService fileService;

    @Test
    void presignRequiresAnAuthorizedTrip() throws Exception {
        Method method = FileController.class.getMethod(
                "presign", Long.class, String.class, String.class, long.class);

        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value())
                .contains("hasAnyRole('ADMIN','TEACHER')")
                .contains("@tripSecurity.canViewTrip(#tripId)");
    }

    @Test
    void presignRejectsOversizedUploadsBeforeSigning() {
        FileController controller = new FileController(fileService);

        assertThatThrownBy(() -> controller.presign(
                42L, "cover.png", "image/png", 12L * 1024 * 1024 + 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verify(fileService, never()).presignPut(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong());
    }
}
