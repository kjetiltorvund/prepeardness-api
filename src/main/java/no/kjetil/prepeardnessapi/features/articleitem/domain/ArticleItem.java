package no.kjetil.prepeardnessapi.features.articleitem.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "article_items")
public class ArticleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String articleName;

    private Date expirationDate;

    private boolean expired = false;

    private String barcode;

    private String qrCode;

    private boolean active = true;

    private String placement;
}
