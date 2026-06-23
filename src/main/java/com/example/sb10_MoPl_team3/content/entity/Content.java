package com.example.sb10_MoPl_team3.content.entity;

import com.example.sb10_MoPl_team3.content.ContentType;
import java.time.Instant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.example.sb10_MoPl_team3.global.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "content",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_content_external_id_source",
		columnNames = {"external_id", "source"}
	)
)
@SQLDelete(sql = "UPDATE content SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private ContentType type;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "thumbnail_url")
	private String thumbnailUrl;

	@Column(name = "external_id", nullable = false)
	private String externalId;

	@Column(name = "source", nullable = false)
	private String source;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public Content(ContentType type, String title, String description, String thumbnailUrl,
		String externalId, String source) {
		this.type = type;
		this.title = title;
		this.description = description;
		this.thumbnailUrl = thumbnailUrl;
		this.externalId = externalId;
		this.source = source;
	}
}
