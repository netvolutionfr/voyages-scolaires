package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.exception.ErasureBlockedException;
import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserErasureServiceTest {

    @Mock UserRepository userRepository;
    @Mock DocumentRepository documentRepository;
    @Mock StudentHealthFormRepository studentHealthFormRepository;
    @Mock ParentChildRepository parentChildRepository;
    @Mock TripUserRepository tripUserRepository;
    @Mock TripPreferenceRepository tripPreferenceRepository;
    @Mock TripRepository tripRepository;
    @Mock RectificationRequestRepository rectificationRequestRepository;
    @Mock ApplicationEventPublisher events;

    private UserErasureService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserErasureService(
                userRepository, documentRepository, studentHealthFormRepository,
                parentChildRepository, tripUserRepository, tripPreferenceRepository,
                tripRepository, rectificationRequestRepository, events
        );

        user = new User();
        user.setId(7L);
        user.setRole(UserRole.PARENT);
        user.setStatus(UserStatus.ACTIVE);

        lenient().when(documentRepository.findAllByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        lenient().when(tripUserRepository.findActiveByUser(eq(7L), any(), any(LocalDate.class))).thenReturn(List.of());
        lenient().when(userRepository.existsByLegalGuardianAndStatus(user, UserStatus.ACTIVE)).thenReturn(false);
        lenient().when(parentChildRepository.existsByParent_IdAndChild_Status(7L, UserStatus.ACTIVE)).thenReturn(false);
        lenient().when(tripRepository.findByChaperonesContaining(eq(user), any())).thenReturn(Page.empty());
    }

    @Test
    void erase_deletesUserAndDependenciesWhenNoGuardRailTriggered() {
        service.erase(user);

        verify(documentRepository).deleteUserDocumentLinks(7L);
        verify(studentHealthFormRepository).deleteByStudentId(7L);
        verify(tripUserRepository).deleteTripUsersJoinRows(7L);
        verify(tripUserRepository).deleteAllByUser(user);
        verify(tripPreferenceRepository).deleteAllByUser(user);
        verify(parentChildRepository).deleteAllByParentIdOrChildId(7L);
        verify(rectificationRequestRepository).clearProcessedBy(user);
        verify(rectificationRequestRepository).deleteAllByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    void erase_deletesDocumentsAndPublishesStorageDeletionEvent() {
        Document doc = new Document();
        doc.setObjectKey("docs/7/passport.pdf");
        when(documentRepository.findAllByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(doc));

        service.erase(user);

        verify(documentRepository).delete(doc);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOf(fr.siovision.voyages.domain.events.DocumentStorageDeletionEvent.class)
                .extracting(e -> ((fr.siovision.voyages.domain.events.DocumentStorageDeletionEvent) e).objectKey())
                .isEqualTo("docs/7/passport.pdf");
    }

    @Test
    void erase_removesUserFromChaperoneListsOfTrips() {
        Trip trip = new Trip();
        trip.setChaperones(new java.util.ArrayList<>(List.of(user)));
        when(tripRepository.findByChaperonesContaining(eq(user), any()))
                .thenReturn(new PageImpl<>(List.of(trip)));

        service.erase(user);

        assertThat(trip.getChaperones()).doesNotContain(user);
        verify(tripRepository).save(trip);
    }

    @Test
    void erase_blocksLastActiveAdmin() {
        user.setRole(UserRole.ADMIN);
        when(userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> service.erase(user))
                .isInstanceOf(ErasureBlockedException.class)
                .hasMessage("last_active_admin");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void erase_allowsAdminDeletionWhenOtherActiveAdminsRemain() {
        user.setRole(UserRole.ADMIN);
        when(userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).thenReturn(2L);

        service.erase(user);

        verify(userRepository).delete(user);
    }

    @Test
    void erase_blocksActiveTripRegistration() {
        TripUser registration = new TripUser();
        when(tripUserRepository.findActiveByUser(eq(7L), any(), any(LocalDate.class)))
                .thenReturn(List.of(registration));

        assertThatThrownBy(() -> service.erase(user))
                .isInstanceOf(ErasureBlockedException.class)
                .hasMessage("active_trip_registration");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void erase_blocksLegalGuardianOfActiveChild() {
        when(userRepository.existsByLegalGuardianAndStatus(user, UserStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.erase(user))
                .isInstanceOf(ErasureBlockedException.class)
                .hasMessage("guardian_of_active_student");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void erase_blocksParentOfActiveChildViaParentChildLink() {
        when(parentChildRepository.existsByParent_IdAndChild_Status(7L, UserStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.erase(user))
                .isInstanceOf(ErasureBlockedException.class)
                .hasMessage("guardian_of_active_student");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void eraseSelf_blocksStudentSelfErasure() {
        user.setRole(UserRole.STUDENT);

        assertThatThrownBy(() -> service.eraseSelf(user))
                .isInstanceOf(ErasureBlockedException.class)
                .hasMessage("student_self_erasure_forbidden");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void eraseSelf_allowsNonStudentRoles() {
        service.eraseSelf(user);

        verify(userRepository).delete(user);
    }
}
