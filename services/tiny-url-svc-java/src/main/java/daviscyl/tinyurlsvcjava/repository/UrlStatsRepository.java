package daviscyl.tinyurlsvcjava.repository;

import daviscyl.tinyurlsvcjava.entity.UrlStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlStatsRepository extends JpaRepository<UrlStatsEntity, Long> {

    Optional<UrlStatsEntity> findByUrlIdAndStatDate(Long urlId, LocalDate statDate);

    List<UrlStatsEntity> findByUrlIdAndStatDateBetween(
        Long urlId,
        LocalDate startDate,
        LocalDate endDate
    );

    @Query("SELECT SUM(s.redirectCount) FROM UrlStatsEntity s " +
           "WHERE s.urlId = :urlId AND s.statDate BETWEEN :startDate AND :endDate")
    Long sumRedirectCountByUrlIdAndDateRange(
        @Param("urlId") Long urlId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
