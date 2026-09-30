package no.kjetil.preparednessapi.features.articleitem.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatusCode;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;

public interface ArticleItemService {
    ArticleItem createArticleItem(ArticleItem articleItem);

    ArticleItem readArticleItemById(long id);

    ArticleItem updateArticleItem(ArticleItem articleItem);

    void deleteArticleItemById(long id);

    List<ArticleItem> findAllByDatePassedExpirationDate(Date date);

    ArticleItem findById(Long id);

    List<ArticleItem> findAll();

    HttpStatusCode save(ArticleItem articleItem);

    List<ArticleItem> saveAll(List<ArticleItem> articleItems);

    void deleteById(Long id);
}
