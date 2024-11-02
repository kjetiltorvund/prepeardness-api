package no.kjetil.prepeardnessapi.features.scheduled;

import no.kjetil.prepeardnessapi.features.articleitem.repositories.ArticleItemRepository;
import org.springframework.stereotype.Component;

@Component
public class CheckExpirationDatesJob {
    private ArticleItemRepository articleItemRepository;

    public CheckExpirationDatesJob(ArticleItemRepository articleItemRepository) {
        this.articleItemRepository = articleItemRepository;
    }
}
