package com.example.sb10_MoPl_team3.content.controller;

import com.example.sb10_MoPl_team3.content.dto.ContentCreateRequest;
import com.example.sb10_MoPl_team3.content.dto.ContentDto;
import com.example.sb10_MoPl_team3.content.dto.ContentUpdateRequest;
import com.example.sb10_MoPl_team3.content.service.ContentService;
import com.example.sb10_MoPl_team3.global.cursor.CursorPageRequest;
import com.example.sb10_MoPl_team3.global.cursor.CursorResponse;
import com.example.sb10_MoPl_team3.global.openapi.ApiErrorResponses;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/contents")
@RestController
@RequiredArgsConstructor
@Tag(name = "콘텐츠 관리", description = "콘텐츠 조회 및 관리자용 콘텐츠 관리 API")
@SecurityRequirement(name = "BearerAuth")
public class ContentController {

  private final ContentService contentService;

  @GetMapping("/{contentId}")
  @Operation(summary = "콘텐츠 단건 조회")
  @ApiErrorResponses.Common
  public ResponseEntity<ContentDto> find (@PathVariable UUID contentId){
    ContentDto dto = contentService.getContent(contentId);
    return ResponseEntity.ok(dto);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "[어드민] 콘텐츠 생성")
  @ApiErrorResponses.Forbidden
  @ApiResponse(responseCode = "201", description = "생성 성공", content = @Content(schema = @Schema(implementation = ContentDto.class)))
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ContentDto> create(
      @Valid @RequestPart("request") ContentCreateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
  ){
    ContentDto dto = contentService.create(request, thumbnail);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @PatchMapping(value = "/{contentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "[어드민] 콘텐츠 수정")
  @ApiErrorResponses.Forbidden
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ContentDto> update(@PathVariable UUID contentId,
      @Valid @RequestPart("request") ContentUpdateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail){
    ContentDto dto = contentService.updateContent(contentId, request, thumbnail);
    return ResponseEntity.ok(dto);
  }

  @DeleteMapping("/{contentId}")
  @Operation(summary = "[어드민] 콘텐츠 삭제")
  @ApiErrorResponses.Forbidden
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable UUID contentId) {
    contentService.deleteContent(contentId);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  @Operation(summary = "콘텐츠 목록 조회 (커서 페이지네이션)")
  @ApiErrorResponses.Common
  public ResponseEntity<CursorResponse<ContentDto>> findContents(
      @RequestParam(required = false) String typeEqual,
      @RequestParam(required = false) String keywordLike,
      @RequestParam(required = false) List<String> tagsIn,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam String sortBy,
      @RequestParam String sortDirection
  ) {
    CursorPageRequest pageRequest = new CursorPageRequest(cursor, idAfter, limit, sortBy, sortDirection);
    CursorResponse<ContentDto> response = contentService.getContents(pageRequest, typeEqual, keywordLike, tagsIn);
    return ResponseEntity.ok(response);
  }


}
