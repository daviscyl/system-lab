package daviscyl.tinyurlsvcjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "url_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id", nullable = false)
    private Long urlId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "redirect_count", nullable = false)
    @Builder.Default
    private Long redirectCount = 0L;

    @Column(name = "unique_visitors", nullable = false)
    @Builder.Default
    private Long uniqueVisitors = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void incrementRedirectCount() {
        this.redirectCount++;
    }
}
