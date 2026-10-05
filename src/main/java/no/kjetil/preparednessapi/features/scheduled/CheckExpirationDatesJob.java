package no.kjetil.preparednessapi.features.scheduled;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.service.ItemService;
import no.kjetil.preparednessapi.features.email.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CheckExpirationDatesJob {
    private final ItemService itemService;
    private final EmailService emailService;

    public CheckExpirationDatesJob(ItemService itemService, EmailService emailService) {
        this.itemService = itemService;
        this.emailService = emailService;
    }
    
    @Scheduled(cron = "0 0 * * * *")
    public void checkIfAnythingIsExpired() {
        List<ArticleItem> expiredArticles = itemService.findAllByDatePassedExpirationDate(new Date(LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond()));
        
        List<String> expiredItems = new ArrayList<>();
        
        expiredArticles.forEach(expiredItem -> expiredItems.add(String.format("%s %s has expired, please replace item located in %s\n", expiredItem.getExpirationDate(), expiredItem.getArticleName(), expiredItem.getPlacement())));
        
        if(!expiredItems.isEmpty()) {
            emailService.sendEmail("kjetiltorvund@gmail.com", "Expired goods", expiredItems.stream().map(Object::toString).collect(Collectors.joining()));
        }
    }
    
    @Scheduled(cron = "0 5 * * * *")
    public void checkIfAnythingWillExpireInTheFuture() {
        int amountOfDaysToExpiration = 3;
        Date inTheFuture = Date.from(LocalDateTime.now().plusDays(amountOfDaysToExpiration).atZone(ZoneId.systemDefault()).toInstant());

        List<ArticleItem> soonToBeExpiredArticles = itemService.findAllByDatePassedExpirationDate(inTheFuture);
        
        List<String> expiredItems = new ArrayList<>();
        
        soonToBeExpiredArticles.forEach(soonToExpireItem -> expiredItems.add(String.format("%s %s will expire soon, item located in %s\n", soonToExpireItem.getExpirationDate(), soonToExpireItem.getArticleName(), soonToExpireItem.getPlacement())));

        if(!expiredItems.isEmpty()) {
            emailService.sendEmail("kjetiltorvund@gmail.com", "Goods expiring soon", expiredItems.stream().map(Object::toString).collect(Collectors.joining()));
        }
    }
}
