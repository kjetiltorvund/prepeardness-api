package no.kjetil.prepeardnessapi.features.articleitem.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@Entity
@Table(name = "article_items")
@NoArgsConstructor
@AllArgsConstructor
public class ArticleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String articleName;

    private Instant expirationDate;

    private boolean expired = false;

    private String barcode;

    private String qrCode;

    private boolean active = true;

    private String placement;
}
