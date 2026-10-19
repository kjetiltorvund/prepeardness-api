package no.kjetil.preparednessapi.features.email.service;

public interface EmailService {
    /**
     * @param toAddressList - Comma separated list of email addresses to send to.
     * @param topic         - Email topic or subject
     * @param msg           - The message to send.
     * @return 1 for success, else exception
     */
    int sendEmail(String toAddressList, String topic, String msg);
}
