package no.kjetil.preparednessapi.features.articleitem.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@Entity
@Table(name = "article_items")
@NoArgsConstructor
@AllArgsConstructor
public class ArticleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String articleName;

    private Date expirationDate;

    @Builder.Default
    private boolean expired = false;

    private String barcode;

    private String qrCode;

    @Builder.Default
    private boolean active = true;

    private String placement;
}
