package no.kjetil.prepeardnessapi.features.articleitem.repositories;

import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleItemRepository extends JpaRepository<ArticleItem, Long> {
}
