package no.kjetil.preparednessapi.features.email.controllers;

import no.kjetil.preparednessapi.features.email.dtos.SendMailRequest;
import no.kjetil.preparednessapi.features.email.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/email", headers = "API-Version=v1")
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendEmail(@RequestBody SendMailRequest requestBody) {
        emailService.sendEmail(requestBody.to(), requestBody.subject(), requestBody.body());

        return ResponseEntity.noContent().build();
    }
}
