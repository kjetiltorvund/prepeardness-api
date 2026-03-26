package no.kjetil.preparednessapi.features.articleitem.controller;

import jakarta.validation.Valid;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.ArticleItemDto;
import no.kjetil.preparednessapi.features.articleitem.repositories.ArticleItemRepository;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/articles", headers = "API-Version=v1")
public class ArticleController {

    private final ArticleItemRepository articleItemRepository;

    private final ModelMapper modelMapper;


    public ArticleController(ArticleItemRepository articleItemRepository, ModelMapper modelMapper) {
        this.articleItemRepository = articleItemRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<ArticleItemDto> getArticleById(@PathVariable @NonNull Long id) {
        ArticleItemDto itemDto = convertToDto(articleItemRepository.findById(id).orElse(new ArticleItem()));
        return ResponseEntity.ok(itemDto);
    }

    @GetMapping
    public ResponseEntity<List<ArticleItemDto>> getAll() {
        List<ArticleItem> items = articleItemRepository.findAll();

        List<ArticleItemDto> articleItemDtos = items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(articleItemDtos);
    }

    @PostMapping
    public ResponseEntity<ArticleItemDto> createArticleItem(@Valid @RequestBody ArticleItemDto requestBody) {
        ArticleItem articleItem = modelMapper.map(requestBody, ArticleItem.class);

        if(articleItem == null) {
            return ResponseEntity.badRequest().build();
        }
        ArticleItem saved = articleItemRepository.save(articleItem);

        return ResponseEntity.ok(modelMapper.map(saved, ArticleItemDto.class));
    }

    @PostMapping(path = "/batch")
    public ResponseEntity<List<ArticleItemDto>> createArticleItemsBatch(@RequestBody List<ArticleItemDto> requestBody) {
        List<ArticleItem> articleItems = requestBody.stream()
                .map(dto -> modelMapper.map(dto, ArticleItem.class))
                .toList();

        if(articleItems == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ArticleItem> savedItems = articleItemRepository.saveAll(articleItems);

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

        ArticleItem updated = articleItemRepository.save(articleItem);

        return ResponseEntity.ok(modelMapper.map(updated, ArticleItemDto.class));
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteArticleById(@PathVariable @NonNull Long id) {
        Optional<ArticleItem> articleItemOptional = articleItemRepository.findById(id);

        if (articleItemOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        articleItemRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }


    private ArticleItemDto convertToDto(ArticleItem articleItem) {
        return modelMapper.map(articleItem, ArticleItemDto.class);
    }
}
