package com.asohCloak.asohCloak.entity.courseMedia;
import com.asohCloak.asohCloak.entity.course.Course;
import com.asohCloak.asohCloak.enums.MediaKind;
import com.asohCloak.asohCloak.enums.MediaStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_media")
@Getter @Setter @NoArgsConstructor
public class CourseMedia {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaKind kind;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(length = 64)
    private String sha256;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "upload_id", length = 512)
    private String uploadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaStatus status = MediaStatus.UPLOADING;

    @Column(name = "part_size_bytes", nullable = false)
    private long partSizeBytes;

    @Column(name = "part_count", nullable = false)
    private int partCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}