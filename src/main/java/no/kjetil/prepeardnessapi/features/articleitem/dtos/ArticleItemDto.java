package no.kjetil.prepeardnessapi.features.articleitem.dtos;

import java.time.Instant;

public record ArticleItemDto(String articleName,
                             Instant expirationDate,
                             String barcode,
                             String QrCode) {
}
