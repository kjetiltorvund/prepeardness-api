package no.kjetil.prepeardnessapi.features.articleitem.repositories;

import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ArticleItemRepository extends JpaRepository<ArticleItem, Long> {
    //@Query("select * from ArticleItem as ai where ai.expirationDate < :date")
    List<ArticleItem> findAllByExpirationDateAfter(Date date);
}
