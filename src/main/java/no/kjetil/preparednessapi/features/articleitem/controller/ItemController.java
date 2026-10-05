package no.kjetil.preparednessapi.features.articleitem.controller;

import jakarta.validation.Valid;
import no.kjetil.preparednessapi.config.exceptionhandlers.ResourceNotFoundException;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.dtos.CreateGroceryResponse;
import no.kjetil.preparednessapi.features.articleitem.dtos.ItemDto;
import no.kjetil.preparednessapi.features.articleitem.service.ItemService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/articles", headers = "API-Version=v1")
public class ItemController {

    private final ItemService itemService;

    private final ModelMapper modelMapper;


    public ItemController(ItemService itemService, ModelMapper modelMapper) {
        this.itemService = itemService;
        this.modelMapper = modelMapper;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<ItemDto> getArticleById(
            @PathVariable @NonNull Long id) {
        ArticleItem byId = itemService.findById(id);

        if (byId == null) {
            throw new ResourceNotFoundException("Article with " + id + " not found");
        }

        ItemDto itemDto = convertToDto(byId);
        return ResponseEntity.ok(itemDto);
    }

    @GetMapping(path = "/barcode/{barcode}")
    public ResponseEntity<ItemDto> getArticleByBarcode(@PathVariable String barcode, @RequestParam Date expirationDate) {
        ItemDto itemDto = convertToDto(itemService.findByBarcodeAndExpirationDate(barcode, expirationDate));
        return ResponseEntity.ok(itemDto);
    }

    @GetMapping
    public ResponseEntity<List<ItemDto>> getAll(
            @RequestParam(required = false) Date expirationDate,
            @RequestParam(required = false, defaultValue = "false") Boolean replaced) {
        List<ArticleItem> items = itemService.findAllByExpirationDateAndReplaced(expirationDate, replaced);

        List<ItemDto> itemDtos = items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(itemDtos);
    }

    @PostMapping
    public ResponseEntity<CreateGroceryResponse> createArticleItem(@Valid @RequestBody ItemDto requestBody) {
        ArticleItem articleItem = modelMapper.map(requestBody, ArticleItem.class);

        if (articleItem == null) {
            return ResponseEntity.badRequest().build();
        }

        CreateGroceryResponse response = itemService.save(articleItem);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/batch")
    public ResponseEntity<List<ItemDto>> createArticleItemsBatch(@RequestBody List<ItemDto> requestBody) {
        List<ArticleItem> articleItems = requestBody.stream()
                .map(dto -> modelMapper.map(dto, ArticleItem.class))
                .toList();

        if (articleItems.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<ArticleItem> savedItems = itemService.saveAll(articleItems);

        List<ItemDto> savedDtos = savedItems.stream()
                .map(item -> modelMapper.map(item, ItemDto.class))
                .toList();

        return ResponseEntity.ok(savedDtos);
    }

    @PutMapping
    public ResponseEntity<ItemDto> updateArticleItem(@Valid @RequestBody ItemDto requestBody) {
        ArticleItem articleItem = modelMapper.map(requestBody, ArticleItem.class);

        if (articleItem == null) {
            return ResponseEntity.badRequest().build();
        }

        itemService.updateArticleItem(articleItem);

        ItemDto updated = modelMapper.map(itemService.findById(articleItem.getId()), ItemDto.class);

        return ResponseEntity.ok(modelMapper.map(updated, ItemDto.class));
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteArticleById(@PathVariable @NonNull Long id) {
        ArticleItem articleItem = itemService.findById(id);

        if (articleItem == null) {
            return ResponseEntity.notFound().build();
        }

        itemService.deleteById(id);

        return ResponseEntity.noContent().build();
    }


    private ItemDto convertToDto(ArticleItem articleItem) {
        return modelMapper.map(articleItem, ItemDto.class);
    }
}
