package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.RectificationRequest;
import fr.siovision.voyages.domain.model.RectificationStatus;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestAdminDTO;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestCreateRequest;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestDTO;
import fr.siovision.voyages.infrastructure.dto.rectification.RectificationRequestResolveRequest;
import fr.siovision.voyages.infrastructure.repository.RectificationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RectificationRequestServiceTest {

    @Mock RectificationRequestRepository rectificationRequestRepository;
    @Mock CurrentUserService currentUserService;

    private RectificationRequestService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RectificationRequestService(rectificationRequestRepository, currentUserService);

        user = new User();
        user.setId(1L);
        user.setPublicId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setFirstName("Alice");
        user.setLastName("Dupont");
        user.setEmail("alice@example.com");
    }

    @Test
    void submit_persistsPendingRequestForCurrentUser() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        RectificationRequestCreateRequest request = new RectificationRequestCreateRequest();
        request.setField("LAST_NAME");
        request.setRequestedValue("Martin");
        request.setReason("Mariage");

        RectificationRequestDTO result = service.submit(request);

        ArgumentCaptor<RectificationRequest> captor = ArgumentCaptor.forClass(RectificationRequest.class);
        verify(rectificationRequestRepository).save(captor.capture());
        RectificationRequest saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getStatus()).isEqualTo(RectificationStatus.PENDING);
        assertThat(saved.getRequestedValue()).isEqualTo("Martin");
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.field()).isEqualTo("LAST_NAME");
    }

    @Test
    void submit_rejectsUnknownField() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        RectificationRequestCreateRequest request = new RectificationRequestCreateRequest();
        request.setField("PASSWORD");
        request.setRequestedValue("whatever");

        assertThatThrownBy(() -> service.submit(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rectificationRequestRepository, never()).save(any());
    }

    @Test
    void submit_rejectsBlankRequestedValue() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        RectificationRequestCreateRequest request = new RectificationRequestCreateRequest();
        request.setField("FIRST_NAME");
        request.setRequestedValue("   ");

        assertThatThrownBy(() -> service.submit(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rectificationRequestRepository, never()).save(any());
    }

    @Test
    void submit_rejectsInvalidEmailFormat() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        RectificationRequestCreateRequest request = new RectificationRequestCreateRequest();
        request.setField("EMAIL");
        request.setRequestedValue("not-an-email");

        assertThatThrownBy(() -> service.submit(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submit_rejectsInvalidBirthDateFormat() {
        when(currentUserService.getCurrentUser()).thenReturn(user);

        RectificationRequestCreateRequest request = new RectificationRequestCreateRequest();
        request.setField("BIRTH_DATE");
        request.setRequestedValue("not-a-date");

        assertThatThrownBy(() -> service.submit(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolve_marksRequestAppliedWithProcessingMetadata() {
        RectificationRequest existing = new RectificationRequest();
        existing.setUser(user);
        existing.setField(fr.siovision.voyages.domain.model.RectificationField.LAST_NAME);
        existing.setRequestedValue("Martin");
        existing.setStatus(RectificationStatus.PENDING);
        UUID requestId = UUID.randomUUID();

        User admin = new User();
        admin.setPublicId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

        when(rectificationRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        RectificationRequestResolveRequest request = new RectificationRequestResolveRequest();
        request.setStatus("APPLIED");
        request.setAdminComment("Fait le 2026-07-02");

        RectificationRequestDTO result = service.resolve(requestId, request);

        assertThat(result.status()).isEqualTo("APPLIED");
        assertThat(existing.getProcessedBy()).isEqualTo(admin);
        assertThat(existing.getProcessedAt()).isNotNull();
        assertThat(existing.getAdminComment()).isEqualTo("Fait le 2026-07-02");
    }

    @Test
    void resolve_rejectsResolvingBackToPending() {
        RectificationRequest existing = new RectificationRequest();
        existing.setUser(user);
        UUID requestId = UUID.randomUUID();
        when(rectificationRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        RectificationRequestResolveRequest request = new RectificationRequestResolveRequest();
        request.setStatus("PENDING");

        assertThatThrownBy(() -> service.resolve(requestId, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(rectificationRequestRepository, never()).save(any());
    }

    @Test
    void resolve_rejectsUnknownRequestId() {
        UUID requestId = UUID.randomUUID();
        when(rectificationRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        RectificationRequestResolveRequest request = new RectificationRequestResolveRequest();
        request.setStatus("APPLIED");

        assertThatThrownBy(() -> service.resolve(requestId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listByStatus_mapsUserIdentityIntoAdminDTO() {
        RectificationRequest existing = new RectificationRequest();
        existing.setUser(user);
        existing.setField(fr.siovision.voyages.domain.model.RectificationField.LAST_NAME);
        existing.setRequestedValue("Martin");
        existing.setStatus(RectificationStatus.PENDING);

        Pageable pageable = Pageable.ofSize(20);
        Page<RectificationRequest> page = new PageImpl<>(List.of(existing));
        when(rectificationRequestRepository.findByStatus(RectificationStatus.PENDING, pageable)).thenReturn(page);

        Page<RectificationRequestAdminDTO> result = service.listByStatus(RectificationStatus.PENDING, pageable);

        assertThat(result.getContent()).hasSize(1);
        RectificationRequestAdminDTO dto = result.getContent().get(0);
        assertThat(dto.userPublicId()).isEqualTo(user.getPublicId());
        assertThat(dto.userFullName()).isEqualTo("Alice Dupont");
        assertThat(dto.userEmail()).isEqualTo("alice@example.com");
        assertThat(dto.requestedValue()).isEqualTo("Martin");
    }
}
