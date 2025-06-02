package ir.hamqadam.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "posts")
public class Post {

    @Id
    private String postId;

    // --- Basic Information ---
    @Field("post_type")
    private String postType; // e.g., "general_blog", "idea_proposal", "project_description", "team_intro" etc.

    @Field("title")
    private Map<String, String> title; // i18n

    @Field("author_info")
    private AuthorInfo authorInfo;

    @Field("version")
    private Integer version; // Optional for versioning posts

    @Field("status")
    private PostStatus status; // Phase 1: Enum for fixed workflow stages

    @Field("visibility")
    private PostVisibility visibility;

    // --- Content ---
    @Field("content_body_type")
    private ContentBodyType contentBodyType; // Enum: MARKDOWN, HTML, RICH_TEXT_JSON

    @Field("content_body")
    private Map<String, String> contentBody; // i18n, actual content based on content_body_type

    @Field("excerpt")
    private Map<String, String> excerpt; // i18n, summary

    @Field("featured_image_url")
    private String featuredImageUrl;

    @Field("media_attachments")
    private List<MediaAttachment> mediaAttachments;

    // --- Metadata & Categorization ---
    @Field("tags")
    private List<String> tags; // Could be List<Map<String, String>> if tags are i18n objects

    @Field("category_ids") // Assuming categories are managed elsewhere or simple strings
    private List<String> categoryIds;

    @Field("language") // Default language of the post, though fields are i18n
    private String language; // e.g., "fa", "en"

    // --- Publication & Interaction ---
    @Field("telegram_publication_info")
    private TelegramPublicationInfo telegramPublicationInfo; // Optional

    @Field("allow_comments")
    private boolean allowComments;

    @Field("comment_count")
    private long commentCount;

    @Field("view_count")
    private long viewCount;

    @Field("like_count")
    private long likeCount;

    @Field("reaction_counts")
    private Map<String, Long> reactionCounts; // e.g., {"👍": 10, "❤️": 5}

    // --- Links to Other Entities ---
    @Field("linked_entity_info")
    private LinkedEntityInfo linkedEntityInfo; // If this post describes/belongs to another entity

    @Field("parent_post_id")
    private String parentPostId; // If this is a reply or part of a series

    @Field("related_post_ids")
    private List<String> relatedPostIds;

    // --- Timestamps ---
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("published_at")
    private LocalDateTime publishedAt;

    @Field("scheduled_for_publication_at")
    private LocalDateTime scheduledForPublicationAt;


    // --- Inner classes ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthorInfo {
        @Field("author_type")
        private AuthorType authorType; // Enum: USER, TEAM
        @Field("author_id")
        private String authorId; // UserId or TeamId
        @Field("acting_user_id") // User who actually performed the creation/edit
        private String actingUserId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaAttachment {
        @Field("media_type")
        private MediaType mediaType; // Enum: IMAGE, VIDEO, AUDIO, DOCUMENT
        private String url;
        private Map<String, String> caption; // i18n
        @Field("alt_text")
        private Map<String, String> altText; // i18n, for images
        @Field("file_name")
        private String fileName;
        private Long size; // in bytes
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TelegramPublicationInfo {
        @Field("telegram_message_id")
        private String telegramMessageId;
        @Field("telegram_channel_or_group_id")
        private String telegramChannelOrGroupId;
        @Field("last_synced_at")
        private LocalDateTime lastSyncedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LinkedEntityInfo {
        @Field("entity_type") // E.g., "Project", "Team", "Routine"
        private String entityType;
        @Field("entity_id")
        private String entityId;
    }

    // --- Enums ---
    public enum AuthorType { USER, TEAM }
    public enum PostStatus { // Phase 1 Fixed Workflow Stages
        DRAFT, PENDING_APPROVAL, PUBLISHED, SCHEDULED, ARCHIVED, REJECTED
    }
    public enum PostVisibility {
        PUBLIC, UNLISTED, PRIVATE_TO_AUTHOR, TEAM_ONLY, PROJECT_MEMBERS_ONLY
    }
    public enum ContentBodyType { MARKDOWN, HTML, RICH_TEXT_JSON }
    public enum MediaType { IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER }
}