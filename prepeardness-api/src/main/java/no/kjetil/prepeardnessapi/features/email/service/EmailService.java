package no.kjetil.prepeardnessapi.features.email.service;

public interface EmailService {
    public int sendEmail(String to, String topic, String msg);
}
