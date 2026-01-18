package no.kjetil.preparednessapi.features.email.service;

public interface EmailService {
    public int sendEmail(String to, String topic, String msg);
}
