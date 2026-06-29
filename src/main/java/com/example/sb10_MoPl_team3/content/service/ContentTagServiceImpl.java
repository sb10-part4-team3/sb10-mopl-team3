package com.example.sb10_MoPl_team3.content.service;

import com.example.sb10_MoPl_team3.content.entity.ContentTag;
import com.example.sb10_MoPl_team3.content.entity.Tag;
import com.example.sb10_MoPl_team3.content.repository.ContentTagRepository;
import com.example.sb10_MoPl_team3.content.repository.TagRepository;
import java.util.ArrayList;
import java.util.List;
import com.example.sb10_MoPl_team3.content.entity.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentTagServiceImpl implements ContentTagService{

  private final TagRepository tagRepository;
  private final ContentTagRepository  contentTagRepository;

  @Override
  public List<String> syncTags(Content content, List<String> tagNames) {
    List<String> safeTagNames = (tagNames != null)
        ? tagNames.stream().distinct().toList()
        : List.of();


    contentTagRepository.deleteAllByContentId(content.getId());
    List<Tag> tags = new ArrayList<>();

    for(String name : safeTagNames){
      Tag tag = tagRepository.findByName(name)
          .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
      tags.add(tag);
    }

    List<ContentTag> contentTags = new ArrayList<>();
    for (Tag tag : tags) {
      contentTags.add(new ContentTag(content, tag));
    }
    contentTagRepository.saveAll(contentTags);

    return tags.stream().map(Tag::getName).toList();
  }
}
