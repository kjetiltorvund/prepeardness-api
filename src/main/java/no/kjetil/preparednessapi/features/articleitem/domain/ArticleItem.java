package no.kjetil.preparednessapi.features.articleitem.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleItem {
    private long id;

    private String articleName;

    private Date expirationDate;

    private int daysUntilExpired;

    @Builder.Default
    private boolean expired = false;

    private String barcode;

    private String qrCode;

    @Builder.Default
    private boolean active = true;

    private String placement;
}
