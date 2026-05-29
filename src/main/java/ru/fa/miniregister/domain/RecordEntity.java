package ru.fa.miniregister.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * JPA-сущность «Бронь переговорной» (B-record).
 * Хранит заголовок, описание, статус и дату создания.
 */
@Schema(description = "Бронь переговорной комнаты")
@Entity
@Table(name = "records")
public class RecordEntity {

    @Schema(description = "Уникальный идентификатор", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Название брони", example = "Бронь переговорной B-101")
    @NotBlank
    @Column(nullable = false)
    private String title;

    @Schema(description = "Комментарий к брони", example = "Совещание отдела, 3 этаж")
    @Column(length = 2000)
    private String description;

    @Schema(description = "Статус брони", example = "NEW")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status = RecordStatus.NEW;

    @Schema(description = "Дата и время создания", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RecordStatus.NEW;
        }
    }

    public RecordEntity() {}

    public RecordEntity(String title, String description, RecordStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public RecordStatus getStatus() { return status; }
    public void setStatus(RecordStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
