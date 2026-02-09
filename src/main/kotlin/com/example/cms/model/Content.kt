package com.example.cms.model

import com.fasterxml.jackson.annotation.JsonCreator
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "contents")
data class Content(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false)
    val title: String,

    @Column(unique = true, nullable = false)
    val slug: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    val author: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    val category: Category? = null,

    @Column(columnDefinition = "TEXT")
    val excerpt: String? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    val body: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "featured_image_id")
    val featuredImage: Media? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val status: ContentStatus = ContentStatus.DRAFT,

    val publishedAt: LocalDateTime? = null,

    val viewCount: Int = 0,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @ManyToMany
    @JoinTable(
        name = "content_tags",
        joinColumns = [JoinColumn(name = "content_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    val tags: List<Tag> = listOf(),

    @OneToMany(mappedBy = "content", cascade = [CascadeType.ALL])
    val comments: List<Comment> = listOf()
)

enum class ContentStatus {
    DRAFT, PUBLISHED, ARCHIVED;

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(value: String): ContentStatus {
            return valueOf(value.uppercase())
        }
    }
}
