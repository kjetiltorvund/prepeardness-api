package no.kjetil.preparednessapi.features.email.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final String username;
    private final String password;

    public EmailServiceImpl(EmailProperties properties) {
        this.username = properties.getUsername();
        this.password = properties.getPassword();
    }

    @Override
    public int sendEmail(String to, String topic, String msg) {

        logger.info("Trying to send email...");

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "465");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.socketFactory.port", "465");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        logger.info("Given username is: {}", username);

        Session session = Session.getInstance(prop,
                new jakarta.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("prepardness-no-reply@gmail.com"));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );
            message.setSubject(topic);
            message.setText(msg);

            Transport.send(message);

            logger.info("Done sending emails.");

        } catch (MessagingException e) {
            logger.error("Failed to send email.");
            logger.error(e.getMessage());
        }

        return 1;
    }
}
