package no.kjetil.prepeardnessapi.features.scheduled;

import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.prepeardnessapi.features.articleitem.repositories.ArticleItemRepository;
import no.kjetil.prepeardnessapi.features.email.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Component
public class CheckExpirationDatesJob {
    private ArticleItemRepository articleItemRepository;
    private EmailService emailService;

    public CheckExpirationDatesJob(ArticleItemRepository articleItemRepository, EmailService emailService) {
        this.articleItemRepository = articleItemRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkIfAnythingIsExpired() {
        List<ArticleItem> expiredArticles = articleItemRepository.findAllByDatePassedExpirationDate(new Date(LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond()));

        if (expiredArticles.isEmpty()) {
            System.out.println("No expired articles");
            return;
        }
        StringBuilder emailBody = new StringBuilder("The following articles are expired:\n");
        for (ArticleItem articleItem : expiredArticles) {
            emailBody.append(articleItem.getArticleName()).append(" - ").append(articleItem.getExpirationDate()).append("\n");
        }
        emailBody.append("Please check your inventory and remove expired items.");
        emailService.sendEmail("kjetiltorvund@gmail.com", emailBody
                .toString());
    }
}
