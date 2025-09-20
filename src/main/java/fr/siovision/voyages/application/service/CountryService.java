package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Country;
import fr.siovision.voyages.infrastructure.dto.CountryDTO;
import fr.siovision.voyages.infrastructure.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CountryService {
    private final CountryRepository countryRepository;

    @Transactional(readOnly = true)
    public Page<CountryDTO> list(String q, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        Page<Country> countries = countryRepository.search(query, pageable);
        return countries.map(p -> new CountryDTO(p.getId(), p.getName()));
    }
}
