package no.kjetil.preparednessapi.features.articleitem.repositories;

import no.kjetil.preparednessapi.PostgreSqlIntegrationSetup;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.scheduled.ArticleItemTestData;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ArticleItemRepositoryTest extends PostgreSqlIntegrationSetup {

    @Autowired
    private ArticleItemRepository repository;

    @BeforeEach
    void setup() {
    }

    @Test
    public void shouldPersistArticleItem() {
        ArticleItem articleItem = new ArticleItem();
        articleItem.setArticleName("Leverpostei");

        ArticleItem saved = repository.save(articleItem);

        assertThat(saved.getId(), notNullValue());
    }

    @Test
    public void shouldReturnAllArticleItemsWithExpiredDate() {
        List<ArticleItem> expiredArticles = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            expiredArticles.add(ArticleItemTestData.randomExpiredBetweenDaysAgo(3, 10));
        }

        List<ArticleItem> expected = repository.saveAll(expiredArticles);

        Date date = Instant.now().toDate();
        List<ArticleItem> actual = repository.findAllByDatePassedExpirationDate(date);

        assertThat(actual.size(), is(expected.size()));
    }

    @Test
    public void shouldOnlyReturnExpiredArticles() {
        // Arrange
        List<ArticleItem> expiredArticles = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            expiredArticles.add(ArticleItemTestData.randomExpiredBetweenDaysAgo(3, 10));
        }

        List<ArticleItem> persistedExpiredArticles = repository.saveAll(expiredArticles);

        ArticleItem articleItem = ArticleItem.builder()
                .articleName("Leverpostei")
                .expired(false)
                .expirationDate(Instant.now().toDateTime(DateTimeZone.UTC).plusDays(5).toDate())
                .placement("Kjøleskapet")
                .build();

        repository.save(articleItem);

        // Act
        Date date = Instant.now().toDate();
        List<ArticleItem> actual = repository.findAllByDatePassedExpirationDate(date);

        List<ArticleItem> all = repository.findAll();

        // Assert
        assertThat(actual, hasSize(persistedExpiredArticles.size()));
        assertThat(all.size(), greaterThan(actual.size()));
    }
}