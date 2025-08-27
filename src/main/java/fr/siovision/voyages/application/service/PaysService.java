package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Pays;
import fr.siovision.voyages.infrastructure.dto.PaysDTO;
import fr.siovision.voyages.infrastructure.repository.PaysRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PaysService {
    @Autowired
    private PaysRepository paysRepository;

    public Page<PaysDTO> list(String q, Pageable pageable) {
        Page<Pays> pays = paysRepository.search(q, pageable);
        return pays.map(p -> new PaysDTO(p.getId(), p.getNom()));
    }
}
