package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.ParentChild;
import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.dto.ImportResult;
import fr.siovision.voyages.infrastructure.dto.ImportRow;
import fr.siovision.voyages.infrastructure.repository.ParentChildRepository;
import fr.siovision.voyages.infrastructure.repository.ParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final UserRepository userRepo;
    private final ParticipantRepository participantRepo;
    private final ParentChildRepository parentChildRepo;

    @Transactional
    public ImportResult importCsv(InputStream csvStream) {
        log.info("Starting CSV import");
        List<ImportRow> rows = parseCsv(csvStream);
        ImportResult result = new ImportResult();

        for (int i = 0; i < rows.size(); i++) {
            ImportRow row = rows.get(i);
            try {
                switch (row.role().toLowerCase()) {
                    case "student" -> handleStudent(row);
                    case "teacher" -> handleTeacher(row);
                    case "admin" -> handleAdmin(row);
                    default -> {
                        result.incSkipped();
                        result.addError(i + 1, "Rôle inconnu: " + row.role());
                        continue;
                    }
                }
                result.incImported();
            } catch (Exception e) {
                result.addError(i + 1, e.getMessage());
            }
        }
        return result;
    }

    private List<ImportRow> parseCsv(InputStream csvStream) {
        List<ImportRow> rows = new ArrayList<>();
        try {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .get()
                    .parse(new InputStreamReader(csvStream));
            for (CSVRecord rec : records) {
                rows.add(new ImportRow(
                        rec.get("role"),
                        rec.get("nom"),
                        rec.get("prenom"),
                        rec.get("email"),
                        rec.get("telephone"),
                        rec.get("parent1_nom"),
                        rec.get("parent1_prenom"),
                        rec.get("parent1_email"),
                        rec.get("parent1_tel"),
                        rec.get("parent2_nom"),
                        rec.get("parent2_prenom"),
                        rec.get("parent2_email"),
                        rec.get("parent2_tel")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur de parsing CSV: " + e.getMessage(), e);
        }
        return rows;
    }

    @Transactional
    void handleStudent(ImportRow row) {
        log.info("Handling student import for email: {}", row.email());
        // 1) valider
        String studentEmail = norm(row.email());
        requireNonBlank(studentEmail, "Email étudiant manquant");
        requireNonBlank(row.nom(), "Nom étudiant manquant");
        requireNonBlank(row.prenom(), "Prénom étudiant manquant");

        // 2) upsert User (STUDENT)
        User studentUser = userRepo.findByEmail(studentEmail)
                .map(u -> updateNamesIfChanged(u, row.nom(), row.prenom(), row.telephone(), UserRole.STUDENT))
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(studentEmail);
                    u.setNom(row.nom().trim());
                    u.setPrenom(row.prenom().trim());
                    u.setTelephone(emptyToNull(row.telephone()));
                    u.setRole(UserRole.STUDENT);
                    return userRepo.save(u);
                });

        // 3) upsert Participant (profil élève)
        Participant participant = participantRepo.findByStudentAccount_Email(studentEmail)
                .orElseGet(() -> {
                    Participant p = new Participant();
                    p.setNom(row.nom().trim());
                    p.setPrenom(row.prenom().trim());
                    p.setEmail(studentEmail);
                    p.setTelephone(emptyToNull(row.telephone()));
                    // TODO section: à ajouter dans le CSV et gérer ici
                    // p.setSection(sectionService.resolve(...));
                    p.setStudentAccount(studentUser); // obligatoire par conception
                    return participantRepo.save(p);
                });

        // Si déjà existant, mettre à jour champs utiles
        participant.setNom(row.nom().trim());
        participant.setPrenom(row.prenom().trim());
        participant.setTelephone(emptyToNull(row.telephone()));
        participantRepo.save(participant);

        // 4) parents (facultatifs) → upsert User(PARENT) + liens ParentChild
        User parent1 = upsertParent(row.parent1Email(), row.parent1Nom(), row.parent1Prenom(), row.parent1Tel());
        User parent2 = upsertParent(row.parent2Email(), row.parent2Nom(), row.parent2Prenom(), row.parent2Tel());

        // 5) liens ParentChild (uniques)
        if (parent1 != null) linkParentToChildOnce(parent1, participant); // ou MERE/PERE si tu sais inférer
        if (parent2 != null) linkParentToChildOnce(parent2, participant);

        // 6) tuteur légal principal = parent1 si fourni
        participant.setLegalGuardian(parent1 != null ? parent1 : participant.getLegalGuardian());
        participantRepo.save(participant);

    }

    @Transactional
    void handleTeacher(ImportRow row) {
        log.info("Handling teacher import for email: {}", row.email());
        User user = userRepo.findByEmail(row.email()).orElseGet(() -> {
            User newuser = new User();
            newuser.setEmail(row.email());
            newuser.setPrenom(row.prenom());
            newuser.setNom(row.nom());
            newuser.setTelephone(row.telephone());
            newuser.setRole(UserRole.TEACHER);
            log.info("Creating new teacher user: {}", newuser);
            return userRepo.save(newuser);
        });
    }

    @Transactional
    void handleAdmin(ImportRow row) {
        log.info("Handling admin import for email: {}", row.email());
        // TODO: créer User admin + KC
    }


    private User upsertParent(String email, String nom, String prenom, String tel) {
        String e = norm(email);
        if (e == null) return null;
        return userRepo.findByEmail(e)
                .map(u -> updateNamesIfChanged(u, nom, prenom, tel, UserRole.PARENT))
                .orElseGet(() -> {
                    User p = new User();
                    p.setEmail(e);
                    p.setNom(safe(nom));
                    p.setPrenom(safe(prenom));
                    p.setTelephone(emptyToNull(tel));
                    p.setRole(UserRole.PARENT);
                    return userRepo.save(p);
                });
    }

    private User updateNamesIfChanged(User u, String nom, String prenom, String tel, UserRole targetRole) {
        if (nom != null && !nom.isBlank()) u.setNom(nom.trim());
        if (prenom != null && !prenom.isBlank()) u.setPrenom(prenom.trim());
        if (tel != null && !tel.isBlank()) u.setTelephone(tel.trim());
        // ne rétrograde pas un ADMIN ; sinon, harmonise
        if (u.getRole() == null || u.getRole() == UserRole.STUDENT || u.getRole() == UserRole.PARENT)
            u.setRole(targetRole == UserRole.ADMIN ? u.getRole() : targetRole);
        return userRepo.save(u);
    }

    private void linkParentToChildOnce(User parent, Participant child) {
        if (!parentChildRepo.existsByParentIdAndChildId(parent.getId(), child.getId())) {
            ParentChild pc = new ParentChild();
            pc.setParent(parent);
            pc.setChild(child);
            parentChildRepo.save(pc);
        }
    }

    private static String norm(String email) {
        if (email == null) return null;
        String s = email.trim().toLowerCase();
        return s.isEmpty() ? null : s;
    }
    private static String emptyToNull(String s){ return (s==null||s.isBlank())?null:s.trim(); }
    private static void requireNonBlank(String v, String msg){
        if (v==null || v.isBlank()) throw new IllegalArgumentException(msg);
    }
    private static String safe(String s){ return s==null?null:s.trim(); }
}