package no.kjetil.preparednessapi.features.articleitem.service;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;

import java.util.Date;
import java.util.List;

public interface ArticleItemService {
    ArticleItem createArticleItem(ArticleItem articleItem);

    ArticleItem readArticleItemById(long id);

    ArticleItem updateArticleItem(ArticleItem articleItem);

    void deleteArticleItemById(long id);

    List<ArticleItem> findAllByDatePassedExpirationDate(Date date);

    ArticleItem findById(Long id);

    List<ArticleItem> findAll();

    ArticleItem save(ArticleItem articleItem);

    List<ArticleItem> saveAll(List<ArticleItem> articleItems);

    void deleteById(Long id);
}
