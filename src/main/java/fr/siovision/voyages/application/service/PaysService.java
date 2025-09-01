package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Pays;
import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.infrastructure.dto.PaysDTO;
import fr.siovision.voyages.infrastructure.repository.PaysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaysService {
    private final PaysRepository paysRepository;

    @Transactional(readOnly = true)
    public Page<PaysDTO> list(String q, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        Page<Pays> pays = paysRepository.search(query, pageable);
        return pays.map(p -> new PaysDTO(p.getId(), p.getNom()));
    }
}
