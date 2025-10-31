package daviscyl.tinyurlsvcjava.service;

import daviscyl.tinyurlsvcjava.entity.UrlEntity;
import daviscyl.tinyurlsvcjava.entity.UrlStatsEntity;
import daviscyl.tinyurlsvcjava.repository.UrlRepository;
import daviscyl.tinyurlsvcjava.repository.UrlStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final UrlRepository urlRepository;
    private final UrlStatsRepository urlStatsRepository;

    @Transactional(readOnly = true)
    public StatsResult getUrlStats(String alias, String userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        UrlEntity url = urlRepository.findByAlias(alias)
            .orElseThrow(() -> new IllegalArgumentException("URL not found: " + alias));

        if (!url.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this URL");
        }

        LocalDate start = startDate != null ? startDate.toLocalDate() : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate.toLocalDate() : LocalDate.now();

        List<UrlStatsEntity> dailyStats = urlStatsRepository.findByUrlIdAndStatDateBetween(url.getId(), start, end);
        Long totalRedirects = urlStatsRepository.sumRedirectCountByUrlIdAndDateRange(url.getId(), start, end);

        return new StatsResult(
            url.getRedirectCount(),
            totalRedirects != null ? totalRedirects : 0L,
            url.getLastRedirectAt(),
            dailyStats
        );
    }

    public record StatsResult(
        Long totalRedirectCount,
        Long periodRedirectCount,
        OffsetDateTime lastRedirectAt,
        List<UrlStatsEntity> dailyStats
    ) {}
}
