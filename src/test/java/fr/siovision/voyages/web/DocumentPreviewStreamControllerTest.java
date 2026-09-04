package fr.siovision.voyages.web;

import fr.siovision.voyages.application.security.EncryptionService;
import fr.siovision.voyages.application.security.TripSecurity;
import fr.siovision.voyages.application.service.CurrentUserService;
import fr.siovision.voyages.application.service.StorageService;
import fr.siovision.voyages.domain.model.Document;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentPreviewStreamControllerTest {

    @Mock CurrentUserService currentUserService;
    @Mock StorageService storageService;
    @Mock EncryptionService encryptionService;
    @Mock DocumentRepository documentRepository;
    @Mock TripSecurity tripSecurity;

    @Test
    void preview_rejectsTeacherWhoCannotPreviewTheDocumentBeforeStorageRead() {
        UUID documentId = UUID.randomUUID();
        User teacher = user(10L, UserRole.TEACHER);
        User owner = user(20L, UserRole.STUDENT);
        Document document = new Document();
        document.setPublicId(documentId);
        document.setUser(owner);

        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(documentRepository.findByPublicId(documentId)).thenReturn(Optional.of(document));
        when(tripSecurity.canPreviewDocument(documentId.toString())).thenReturn(false);

        DocumentPreviewStreamController controller = new DocumentPreviewStreamController(
                currentUserService, storageService, encryptionService, documentRepository, tripSecurity);

        assertThatThrownBy(() -> controller.preview(documentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verify(storageService, never()).getObject(org.mockito.ArgumentMatchers.anyString());
    }

    private static User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
