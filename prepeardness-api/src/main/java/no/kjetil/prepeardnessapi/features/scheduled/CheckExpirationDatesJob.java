package no.kjetil.prepeardnessapi.features.scheduled;

import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.prepeardnessapi.features.articleitem.repositories.ArticleItemRepository;
import no.kjetil.prepeardnessapi.features.email.service.EmailService;
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
    private ArticleItemRepository articleItemRepository;
    private EmailService emailService;
    
    public CheckExpirationDatesJob(ArticleItemRepository articleItemRepository, EmailService emailService) {
        this.articleItemRepository = articleItemRepository;
        this.emailService = emailService;
    }
    
    @Scheduled(cron = "0 0 * * * *")
    public void checkIfAnythingIsExpired() {
        List<ArticleItem> expiredArticles = articleItemRepository.findAllByExpirationDateAfter(new Date(LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond()));
        
        List<String> expiredItems = new ArrayList<>();
        
        expiredArticles.stream().forEach(expiredItem -> {
            expiredItems.add(String.format("%s %s has expired, please replace item located in %s\n", expiredItem.getExpirationDate(), expiredItem.getArticleName(), expiredItem.getPlacement()));
        });
        
        if(expiredItems.size() > 0) {
            emailService.sendEmail("kjetiltorvund@gmail.com", "Expired goods", expiredItems.stream().map(Object::toString).collect(Collectors.joining()));
        }
    }
    
    @Scheduled(cron = "0 5 * * * *")
    public void checkIfAnythingWillExpireInTheFuture() {
        int amountOfDaysToExpiration = 3;
        Date inTheFuture = Date.from(LocalDateTime.now().plusDays(amountOfDaysToExpiration).atZone(ZoneId.systemDefault()).toInstant());
        
        List<ArticleItem> soonToBeExpiredArticles = articleItemRepository.findAllByExpirationDateAfter(inTheFuture);
        
        List<String> expiredItems = new ArrayList<>();
        
        soonToBeExpiredArticles.stream().forEach(soonToExpireItem -> {
            expiredItems.add(String.format("%s %s will expire soon, item located in %s\n", soonToExpireItem.getExpirationDate(), soonToExpireItem.getArticleName(), soonToExpireItem.getPlacement()));
        });

        if(expiredItems.size() > 0) {
            emailService.sendEmail("kjetiltorvund@gmail.com", "Goods expiring soon", expiredItems.stream().map(Object::toString).collect(Collectors.joining()));
        }
    }
}
