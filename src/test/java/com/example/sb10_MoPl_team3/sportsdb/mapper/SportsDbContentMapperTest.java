package com.example.sb10_MoPl_team3.sportsdb.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb10_MoPl_team3.content.ContentType;
import com.example.sb10_MoPl_team3.content.entity.Content;
import com.example.sb10_MoPl_team3.sportsdb.dto.SportsDbEventsResponse.SportsDbEvent;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SportsDbContentMapperTest {

    private final SportsDbContentMapper mapper = new SportsDbContentMapper();

    @Test
    @DisplayName("경기 응답을 Content로 변환하면 필드가 올바르게 매핑된다")
    void toContent_정상_변환() {
        SportsDbEvent event = new SportsDbEvent(
            "1001",
            "Arsenal vs Chelsea",
            "2026-08-01",
            "English Premier League",
            "Emirates Stadium",
            "https://example.com/thumb.jpg"
        );

        Content content = mapper.toContent(event);

        assertThat(content.getType()).isEqualTo(ContentType.SPORT);
        assertThat(content.getTitle()).isEqualTo("Arsenal vs Chelsea");
        assertThat(content.getDescription()).isEqualTo("English Premier League 2026-08-01");
        assertThat(content.getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(content.getExternalId()).isEqualTo("EVENT-1001");
        assertThat(content.getSource()).isEqualTo("SPORTS_DB");
        assertThat(content.getEventDate())
            .isEqualTo(LocalDate.of(2026, 8, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Test
    @DisplayName("날짜 형식이 올바르지 않으면 eventDate는 null이다")
    void toContent_날짜_파싱_실패_시_eventDate_null() {
        SportsDbEvent event = new SportsDbEvent(
            "1002", "제목", "알수없음", "리그", "경기장", "https://example.com/thumb.jpg"
        );

        Content content = mapper.toContent(event);

        assertThat(content.getEventDate()).isNull();
    }

    @Test
    @DisplayName("날짜가 없으면 eventDate는 null이다")
    void toContent_날짜_없으면_eventDate_null() {
        SportsDbEvent event = new SportsDbEvent(
            "1003", "제목", null, "리그", "경기장", "https://example.com/thumb.jpg"
        );

        Content content = mapper.toContent(event);

        assertThat(content.getEventDate()).isNull();
    }
}
