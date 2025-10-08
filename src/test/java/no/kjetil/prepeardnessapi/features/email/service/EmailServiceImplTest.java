package no.kjetil.prepeardnessapi.features.email.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailServiceImplTest {

    @Test
    public void shouldSendEmailToRecipient() {

        EmailService emailService = new EmailServiceImpl("", "");

        emailService.sendEmail("kjetiltorvund@gmail.com", "Testing");
    }
}