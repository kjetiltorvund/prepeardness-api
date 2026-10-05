package no.kjetil.preparednessapi.features.articleitem.service;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateGroceryResponse;

import java.util.Date;
import java.util.List;

public interface ItemService {
    ArticleItem createArticleItem(ArticleItem articleItem);

    ArticleItem readArticleItemById(long id);

    ArticleItem updateArticleItem(ArticleItem articleItem);

    void deleteArticleItemById(long id);

    List<ArticleItem> findAllByDatePassedExpirationDate(Date date);

    ArticleItem findById(Long id);

    ArticleItem findByBarcode(String barCode);

    ArticleItem findByQrCode(String qrCode);

    ArticleItem findByExpirationDate(Date expirationDate);

    ArticleItem findByBarcodeAndExpirationDate(String barcode, Date expirationDate);

    List<ArticleItem> findAll();

    CreateGroceryResponse save(ArticleItem articleItem);

    List<ArticleItem> saveAll(List<ArticleItem> articleItems);

    List<ArticleItem> findAllByExpirationDateAndReplaced(Date expirationDate, Boolean replaced);
}
