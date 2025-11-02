package fr.siovision.voyages.application.listeners;

import fr.siovision.voyages.application.service.StorageService;
import fr.siovision.voyages.domain.events.DocumentStorageDeletionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentDeletionListener {
    private final StorageService storageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(DocumentStorageDeletionEvent evt) {
        try {
            storageService.deleteObject(evt.objectKey());
        } catch (Exception e) {
            // best-effort : log + (optionnel) mettre en file d’attente pour retry
            log.warn("S3 delete failed for key={}", evt.objectKey(), e);
        }
    }
}
