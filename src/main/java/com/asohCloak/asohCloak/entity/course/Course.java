package com.asohCloak.asohCloak.entity.course;

import com.asohCloak.asohCloak.entity.user.User;
import com.asohCloak.asohCloak.enums.CourseLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String shortDescription;

    @ElementCollection
    @CollectionTable(name = "course_upload_videos", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "video_url")
    private List<String> uploadVideos;

    @ElementCollection
    @CollectionTable(name = "course_upload_documents", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "document_url")
    private List<String> uploadDocuments;

    @Column
    private String thumbnailUrl;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false, length = 3)
    private String currency = "XAF";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseLevel level = CourseLevel.BEGINNER;

    @Column(length = 100)
    private String category;

    @ElementCollection
    @CollectionTable(name = "course_tags", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "tag")
    private List<String> tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private User instructor;

    @Column(nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private int durationInMinutes;

    @Column(nullable = false)
    private int enrolledCount = 0;

    @Column(nullable = false)
    private boolean published = false;

    @Column
    private Instant publishedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}