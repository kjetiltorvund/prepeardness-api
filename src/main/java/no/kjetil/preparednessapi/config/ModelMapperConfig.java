package no.kjetil.preparednessapi.config;

import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateItemDto;
import no.kjetil.preparednessapi.features.articleitem.dtos.UpdateItemDto;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {

        // The database columns are timestamptz, so the Date is sent as an OffsetDateTime in UTC.
        Converter<Date, OffsetDateTime> toUtc = context ->
                context.getSource() != null ? context.getSource().toInstant().atOffset(ZoneOffset.UTC) : null;

        // New items get the current time as created_at, unless the caller has set one.
        Converter<Date, OffsetDateTime> toUtcOrNow = context ->
                (context.getSource() != null ? context.getSource().toInstant() : Instant.now())
                        .atOffset(ZoneOffset.UTC);

        ModelMapper modelMapper = new ModelMapper();

        modelMapper.typeMap(ArticleItem.class, CreateItemDto.class)
                .addMappings(mapper ->
                        mapper.using(toUtcOrNow)
                                .map(ArticleItem::getCreatedAt, CreateItemDto::setCreatedAt));

        modelMapper.typeMap(ArticleItem.class, UpdateItemDto.class)
                .addMappings(mapper ->
                        mapper.using(toUtc)
                                .map(ArticleItem::getCreatedAt, UpdateItemDto::setCreatedAt));

        return modelMapper;
    }
}
