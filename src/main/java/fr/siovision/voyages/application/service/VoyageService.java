package fr.siovision.voyages.application.service;


import fr.siovision.voyages.domain.model.Voyage;
import fr.siovision.voyages.infrastructure.dto.VoyageDTO;
import fr.siovision.voyages.infrastructure.repository.VoyageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoyageService {
    @Autowired
    private VoyageRepository voyageRepository;

    // Méthode pour créer un nouveau voyage
    public Voyage createVoyage(VoyageDTO voyageDTO) {
        // Convertir VoyageDTO en entité Voyage
        Voyage voyage = new Voyage();
        voyage.setNom(voyageDTO.getNom());
        voyage.setDescription(voyageDTO.getDescription());
        voyage.setDestination(voyageDTO.getDestination());
        voyage.setDateDepart(voyageDTO.getDateDepart());
        voyage.setDateRetour(voyageDTO.getDateRetour());

        // Enregistrer le voyage dans la base de données
        return voyageRepository.save(voyage);
    }

    // Méthode pour récupérer tous les voyages
    public Iterable<Voyage> getAllVoyages() {
        // Utiliser le repository pour récupérer tous les voyages
        return voyageRepository.findAll();
    }
}
