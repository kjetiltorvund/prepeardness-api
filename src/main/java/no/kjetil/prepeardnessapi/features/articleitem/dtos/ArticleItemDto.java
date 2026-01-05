package no.kjetil.prepeardnessapi.features.articleitem.dtos;

import lombok.*;

import java.util.Date;

@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Data
public final class ArticleItemDto {
    private String articleName;
    private Date expirationDate;
    private String barcode;
    private String qrCode;
}
