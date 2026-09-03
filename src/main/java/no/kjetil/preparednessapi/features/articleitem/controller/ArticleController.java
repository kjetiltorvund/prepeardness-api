package no.kjetil.preparednessapi.features.articleitem.controller;

import jakarta.validation.Valid;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.ArticleItemDto;
import no.kjetil.preparednessapi.features.articleitem.service.ArticleItemService;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/articles", headers = "API-Version=v1")
public class ArticleController {

    private final ArticleItemService articleItemService;

    private final ModelMapper modelMapper;


    public ArticleController(ArticleItemService articleItemService, ModelMapper modelMapper) {
        this.articleItemService = articleItemService;
        this.modelMapper = modelMapper;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<ArticleItemDto> getArticleById(@PathVariable @NonNull Long id) {
        ArticleItemDto itemDto = convertToDto(articleItemService.findById(id).orElse(new ArticleItem()));
        return ResponseEntity.ok(itemDto);
    }

    @GetMapping
    public ResponseEntity<List<ArticleItemDto>> getAll() {
        List<ArticleItem> items = articleItemService.findAll();

        List<ArticleItemDto> articleItemDtos = items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(articleItemDtos);
    }

    @PostMapping
    public ResponseEntity<HttpStatus> createArticleItem(@Valid @RequestBody ArticleItemDto requestBody) {
        ArticleItem articleItem = modelMapper.map(requestBody, ArticleItem.class);

        if(articleItem == null) {
            return ResponseEntity.badRequest().build();
        }
        articleItemService.save(articleItem);

        return ResponseEntity.ok(HttpStatus.CREATED);
    }

    @PostMapping(path = "/batch")
    public ResponseEntity<List<ArticleItemDto>> createArticleItemsBatch(@RequestBody List<ArticleItemDto> requestBody) {
        List<ArticleItem> articleItems = requestBody.stream()
                .map(dto -> modelMapper.map(dto, ArticleItem.class))
                .toList();

        if(articleItems == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ArticleItem> savedItems = articleItemService.saveAll(articleItems);

        List<ArticleItemDto> savedDtos = savedItems.stream()
                .map(item -> modelMapper.map(item, ArticleItemDto.class))
                .toList();

        return ResponseEntity.ok(savedDtos);
    }

    @PutMapping
    public ResponseEntity<ArticleItemDto> updateArticleItem(@Valid @RequestBody ArticleItemDto requestBody) {
        ArticleItem articleItem = modelMapper.map(requestBody, ArticleItem.class);

        if(articleItem == null) {
            return ResponseEntity.badRequest().build();
        }

        articleItemService.save(articleItem);

        ArticleItemDto updated = modelMapper.map(articleItemService.findById(articleItem.getId()), ArticleItemDto.class);

        return ResponseEntity.ok(modelMapper.map(updated, ArticleItemDto.class));
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteArticleById(@PathVariable @NonNull Long id) {
        Optional<ArticleItem> articleItemOptional = articleItemService.findById(id);

        if (articleItemOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        articleItemService.deleteById(id);

        return ResponseEntity.noContent().build();
    }


    private ArticleItemDto convertToDto(ArticleItem articleItem) {
        return modelMapper.map(articleItem, ArticleItemDto.class);
    }
}
