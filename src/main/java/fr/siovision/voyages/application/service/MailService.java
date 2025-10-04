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

    public void sendOtpEmail(User user, String plainCode, Long otpTtlMinutes) {
        var toEmail = user.getEmail();

        var msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setFrom(senderEmail);
        msg.setSubject("Votre code de vérification pour Campus Away");
        msg.setText("""
                Bonjour,
                
                Voici votre code de vérification : %s
                Il expire dans %d minute(s).

                Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail.

                — Campus Away
                """.formatted(formatForReadability(plainCode), otpTtlMinutes));
        mailSender.send(msg);
    }

    private static String formatForReadability(String code) {
        // "482193" -> "482 193"
        if (code.length() == 6) return code.substring(0,3) + " " + code.substring(3);
        return code;
    }
}
