package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.ImportResult;
import fr.siovision.voyages.infrastructure.dto.ImportRow;
import fr.siovision.voyages.infrastructure.repository.ParentChildRepository;
import fr.siovision.voyages.infrastructure.repository.ParticipantRepository;
import fr.siovision.voyages.infrastructure.repository.SectionRepository;
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
    private final SectionRepository sectionRepo;

    @Transactional
    public ImportResult importCsv(InputStream csvStream) {
        log.info("Starting CSV import");
        List<ImportRow> rows = parseCsv(csvStream);
        ImportResult result = new ImportResult();

        for (int i = 0; i < rows.size(); i++) {
            ImportRow row = rows.get(i);
            try {
                switch (row.getRole().toLowerCase()) {
                    case "student" -> handleStudent(row);
                    case "teacher" -> handleTeacher(row);
                    case "admin" -> handleAdmin(row);
                    default -> {
                        result.incSkipped();
                        result.addError(i + 1, "Rôle inconnu: " + row.getRole());
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
                        rec.get("lastName"),
                        rec.get("firstName"),
                        rec.get("email"),
                        rec.get("telephone"),
                        rec.get("gender"),
                        rec.get("section"),
                        rec.get("birthDate"),
                        rec.get("parent1_lastName"),
                        rec.get("parent1_firstName"),
                        rec.get("parent1_email"),
                        rec.get("parent1_tel"),
                        rec.get("parent2_lastName"),
                        rec.get("parent2_firstName"),
                        rec.get("parent2_email"),
                        rec.get("parent2_tel")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing CSV: " + e.getMessage(), e);
        }
        return rows;
    }

    @Transactional
    void handleStudent(ImportRow row) {
        log.info("Handling student import for email: {}", row.getEmail());
        // 1) valider
        String studentEmail = norm(row.getEmail());
        requireNonBlank(studentEmail, "Missing student email");
        requireNonBlank(row.getLastName(), "Missing student last name");
        requireNonBlank(row.getFirstName(), "Missing student first name");
        // si sexe n'est pas M ou F, le remplacer par "N"
        if (row.getGender() == null || !(row.getGender().equalsIgnoreCase("M") || row.getGender().equalsIgnoreCase("F"))) {
            row.setGender("N");
        }

        // 2) upsert User (STUDENT)
        User studentUser = userRepo.findByEmail(studentEmail)
                .map(u -> updateNamesIfChanged(u, row.getLastName(), row.getFirstName(), row.getTelephone(), UserRole.STUDENT))
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(studentEmail);
                    u.setLastName(row.getLastName().trim());
                    u.setFirstName(row.getFirstName().trim());
                    u.setTelephone(emptyToNull(row.getTelephone()));
                    u.setRole(UserRole.STUDENT);
                    return userRepo.save(u);
                });

        // 3) upsert Participant (profil élève)
        Participant participant = participantRepo.findByStudentAccount_Email(studentEmail)
                .orElseGet(() -> {
                    Participant p = new Participant();
                    p.setLastName(row.getLastName().trim());
                    p.setFirstName(row.getFirstName().trim());
                    p.setGender(row.getGender());
                    p.setEmail(studentEmail);
                    p.setTelephone(emptyToNull(row.getTelephone()));
                    Section section = sectionRepo.findByLabel(row.getSection().trim())
                            .orElseGet(() -> {
                                Section s = new Section();
                                s.setLabel(row.getSection().trim());
                                return sectionRepo.save(s);
                            });

                    p.setSection(section);
                    // Parser dateNaissance
                    if (row.getBirthDate() != null && !row.getBirthDate().isBlank()) {
                        try {
                            p.setBirthDate(java.time.LocalDate.parse(row.getBirthDate().trim()));
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid birth date for " + studentEmail + ": " + row.getBirthDate());
                        }
                    }
                    p.setStudentAccount(studentUser); // obligatoire par conception
                    return participantRepo.save(p);
                });

        // Si déjà existant, mettre à jour champs utiles
        participant.setLastName(row.getLastName().trim());
        participant.setFirstName(row.getFirstName().trim());
        participant.setTelephone(emptyToNull(row.getTelephone()));
        participantRepo.save(participant);

        // 4) parents (facultatifs) → upsert User(PARENT) + liens ParentChild
        User parent1 = upsertParent(row.getParent1Email(), row.getParent1LastName(), row.getParent1FirstName(), row.getParent1Tel());
        User parent2 = upsertParent(row.getParent2Email(), row.getParent2LastName(), row.getParent2FirstName(), row.getParent2Tel());

        // 5) liens ParentChild (uniques)
        if (parent1 != null) linkParentToChildOnce(parent1, participant); // ou MERE/PERE si tu sais inférer
        if (parent2 != null) linkParentToChildOnce(parent2, participant);

        // 6) tuteur légal principal = parent1 si fourni
        participant.setLegalGuardian(parent1 != null ? parent1 : participant.getLegalGuardian());
        participantRepo.save(participant);

    }

    @Transactional
    void handleTeacher(ImportRow row) {
        log.info("Handling teacher import for email: {}", row.getEmail());
        userRepo.findByEmail(row.getEmail()).orElseGet(() -> {
            User newuser = new User();
            newuser.setEmail(row.getEmail());
            newuser.setFirstName(row.getFirstName());
            newuser.setLastName(row.getLastName());
            newuser.setTelephone(row.getTelephone());
            newuser.setRole(UserRole.TEACHER);
            log.info("Creating new teacher user: {}", newuser);
            return userRepo.save(newuser);
        });
    }

    @Transactional
    void handleAdmin(ImportRow row) {
        log.info("Handling admin import for email: {}", row.getEmail());
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
                    p.setLastName(safe(nom));
                    p.setFirstName(safe(prenom));
                    p.setTelephone(emptyToNull(tel));
                    p.setRole(UserRole.PARENT);
                    return userRepo.save(p);
                });
    }

    private User updateNamesIfChanged(User u, String lastName, String firsName, String tel, UserRole targetRole) {
        if (lastName != null && !lastName.isBlank()) u.setLastName(lastName.trim());
        if (firsName != null && !firsName.isBlank()) u.setFirstName(firsName.trim());
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