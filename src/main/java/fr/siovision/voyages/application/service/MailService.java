package fr.siovision.voyages.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import fr.siovision.voyages.domain.model.User;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.activation-url}")
    private String activationBaseUrl;

    @Value("${spring.mail.properties.sender}")
    private String senderEmail;

    public void sendActivationLink(User user) {
        var toEmail = user.getEmail();
        var token = UUID.randomUUID().toString();
        var url = activationBaseUrl + "?token=" + token; // ex: http://localhost:5173/activate?token=...
        var msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setFrom(senderEmail);
        msg.setSubject("Active ton compte Campus Away");
        msg.setText("""
                Bonjour,
                
                Pour créer ta Passkey et activer ton compte, clique sur ce lien :
                %s
                
                Ce lien expirera bientôt. Si tu n'es pas à l'origine de cette demande, ignore simplement ce message.
                
                — Campus Away
                """.formatted(url));
        mailSender.send(msg);
    }
}
