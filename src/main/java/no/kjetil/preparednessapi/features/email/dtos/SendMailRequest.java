package no.kjetil.preparednessapi.features.email.dtos;

public record SendMailRequest(String to, String subject, String body) {
}