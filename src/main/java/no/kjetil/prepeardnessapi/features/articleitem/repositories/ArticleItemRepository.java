package no.kjetil.prepeardnessapi.features.articleitem.repositories;

import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository("articleItemRepository")
public interface ArticleItemRepository extends JpaRepository<ArticleItem, Long> {
    @Query("select ai from ArticleItem ai where ai.expirationDate < :date")
    List<ArticleItem> findAllByDatePassedExpirationDate(Date date);
}
