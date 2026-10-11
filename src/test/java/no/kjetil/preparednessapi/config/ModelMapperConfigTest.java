package no.kjetil.preparednessapi.config;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateItemDto;
import no.kjetil.preparednessapi.features.articleitem.dtos.UpdateItemDto;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ModelMapperConfigTest {

    private final ModelMapper modelMapper = new ModelMapperConfig().modelMapper();

    @Test
    void createItemDtoKeepsCreatedAtAndCopiesTheOtherFields() {
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
        ArticleItem item = ArticleItem.builder()
                .articleName("Rice")
                .createdAt(Date.from(createdAt))
                .barcode("123")
                .quantity(2)
                .inventory(7)
                .build();

        CreateItemDto dto = modelMapper.map(item, CreateItemDto.class);

        assertEquals(createdAt, dto.getCreatedAt().toInstant());
        assertEquals("Rice", dto.getArticleName());
        assertEquals("123", dto.getBarcode());
        assertEquals(2, dto.getQuantity());
        assertEquals(7, dto.getInventory());
    }

    @Test
    void createItemDtoDefaultsCreatedAtToNow() {
        CreateItemDto dto = modelMapper.map(ArticleItem.builder().articleName("Rice").build(), CreateItemDto.class);

        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void updateItemDtoLeavesMissingCreatedAtEmpty() {
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");

        UpdateItemDto withDate = modelMapper.map(
                ArticleItem.builder().id(5).createdAt(Date.from(createdAt)).build(), UpdateItemDto.class);
        UpdateItemDto withoutDate = modelMapper.map(ArticleItem.builder().id(5).build(), UpdateItemDto.class);

        assertEquals(5L, withDate.getId());
        assertEquals(createdAt, withDate.getCreatedAt().toInstant());
        assertNull(withoutDate.getCreatedAt());
    }
}
