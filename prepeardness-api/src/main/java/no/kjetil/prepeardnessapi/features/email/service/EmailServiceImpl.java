package no.kjetil.prepeardnessapi.features.email.service;

import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

@Service
public class EmailServiceImpl implements EmailService {

    private SendGrid sendGrid;

    public EmailServiceImpl(SendGrid sendGrid) {
        this.sendGrid = sendGrid;
    }

    @Override
    public int sendEmail(String to, String topic, String msg) {
        Email from = new Email("test@example.com");
        Email toEmail =  new Email("test@example.com");
        Content content = new Content("text/plain", msg);
        Mail mail = new Mail(from, topic, toEmail, content);

        Request request = new Request();

        try {
           request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);

            return response.getStatusCode();
        } catch (Exception ex) {
            System.out.println("Error sending email: " + ex.getMessage());
        }

        return 500;
    }
}
