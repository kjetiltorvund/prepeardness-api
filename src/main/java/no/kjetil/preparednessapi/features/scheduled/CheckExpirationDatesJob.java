package no.kjetil.preparednessapi.features.scheduled;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.service.ItemService;
import no.kjetil.preparednessapi.features.email.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CheckExpirationDatesJob {
    private static final int DAYS_TO_EXPIRATION = 3;

    private final ItemService itemService;
    private final EmailService emailService;

    public CheckExpirationDatesJob(ItemService itemService, EmailService emailService) {
        this.itemService = itemService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Oslo")
    public void checkIfAnythingIsExpired() {
        List<ArticleItem> expiredArticles = itemService.findAllByDatePassedExpirationDate(new Date());

        String expiredItems = expiredArticles.stream()
                .map(expiredItem -> String.format("%s %s has expired, please replace item located in %s\n", expiredItem.getExpirationDate(), expiredItem.getArticleName(), expiredItem.getPlacement()))
                .collect(Collectors.joining());

        if(!expiredItems.isEmpty()) {
            emailService.sendEmail("kjetiltorvund@gmail.com", "Expired goods", expiredItems);
        }
    }

    @Scheduled(cron = "0 5 7 * * *", zone = "Europe/Oslo")
    public void checkIfAnythingWillExpireInTheFuture() {
        Date now = new Date();
        Date inTheFuture = Date.from(now.toInstant().plus(Duration.ofDays(DAYS_TO_EXPIRATION)));

        // Items that have already expired are covered by checkIfAnythingIsExpired.
        String soonToExpireItems = itemService.findAllByDatePassedExpirationDate(inTheFuture).stream()
                .filter(item -> item.getExpirationDate() != null && !item.getExpirationDate().before(now))
                .map(soonToExpireItem -> String.format("%s %s will expire soon, item located in %s\n", soonToExpireItem.getExpirationDate(), soonToExpireItem.getArticleName(), soonToExpireItem.getPlacement()))
                .collect(Collectors.joining());

        if(!soonToExpireItems.isEmpty()) {
            emailService.sendEmail("kjetiltorvund@gmail.com", "Goods expiring soon", soonToExpireItems);
        }
    }
}
