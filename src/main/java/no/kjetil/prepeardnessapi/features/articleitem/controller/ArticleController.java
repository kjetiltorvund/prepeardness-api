package no.kjetil.prepeardnessapi.features.articleitem.controller;

import no.kjetil.prepeardnessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.prepeardnessapi.features.articleitem.dtos.ArticleItemDto;
import no.kjetil.prepeardnessapi.features.articleitem.repositories.ArticleItemRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/v1/articles")
public class ArticleController {

    private ArticleItemRepository articleItemRepository;

    private ModelMapper modelMapper;

    @Autowired
    public ArticleController(ArticleItemRepository articleItemRepository, ModelMapper modelMapper) {
        this.articleItemRepository = articleItemRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping(path = "/{id}")
    public ArticleItemDto getArticleById(@PathVariable("id") String id) {
        return convertToDto(articleItemRepository.findById(id).orElse(new ArticleItem()));
    }

    @GetMapping
    public List<ArticleItemDto> getAll() {
        List<ArticleItem> items = (List<ArticleItem>) articleItemRepository.findAll(); // This is a workaround for the DynamoDB repository
                
        return items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ArticleItemDto createArticleItem(@RequestBody ArticleItemDto requestBody) {
        ArticleItem articleItem = modelMapper.map(requestBody, ArticleItem.class);

        ArticleItem saved = articleItemRepository.save(articleItem);

        return modelMapper.map(saved, ArticleItemDto.class);
    }

    private ArticleItemDto convertToDto(ArticleItem articleItem) {
        return modelMapper.map(articleItem, ArticleItemDto.class);
    }
}
