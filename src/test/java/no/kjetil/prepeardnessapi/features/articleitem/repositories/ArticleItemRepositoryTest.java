package no.kjetil.prepeardnessapi.features.articleitem.repositories;

import no.kjetil.prepeardnessapi.App;
import no.kjetil.prepeardnessapi.PostgreSqlIntegrationSetup;
import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(classes = App.class)
class ArticleItemRepositoryTest extends PostgreSqlIntegrationSetup {

    @Autowired
    private ArticleItemRepository repository;

    @BeforeEach
    void setup() {
    }

    @Test
    public void shouldPersistArticleItem() {
        LocalDate now = LocalDate.now();
        ArticleItem articleItem = new ArticleItem();
        articleItem.setArticleName("Leverpostei");
                //.active(true)
                //.expired(false)
 //               .expirationDate(now.plusDays(2).atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC))
                //.build();

        ArticleItem saved = repository.save(articleItem);

        assertThat(saved.getId(), notNullValue());
    }
}