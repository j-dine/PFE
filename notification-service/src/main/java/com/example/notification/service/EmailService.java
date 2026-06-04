package com.example.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    public EmailService() {
    }

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        if (mailSender == null) {
            logMail(to, subject, body);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true active le rendu HTML

            mailSender.send(mimeMessage);
            System.out.println("E-mail envoyé de manière asynchrone à " + to);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'e-mail à " + to + ": " + e.getMessage());
        }
    }

    private void logMail(String to, String subject, String body) {
        System.out.println("--- SIMULATION E-MAIL (JavaMailSender non configuré) ---");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
        System.out.println("---------------------------------------------------------");
    }
}