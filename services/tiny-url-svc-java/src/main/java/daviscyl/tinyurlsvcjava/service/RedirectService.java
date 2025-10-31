package daviscyl.tinyurlsvcjava.service;

import daviscyl.tinyurlsvcjava.entity.UrlEntity;
import daviscyl.tinyurlsvcjava.entity.UrlStatsEntity;
import daviscyl.tinyurlsvcjava.repository.UrlRepository;
import daviscyl.tinyurlsvcjava.repository.UrlStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {

    private final UrlRepository urlRepository;
    private final UrlStatsRepository urlStatsRepository;

    @Cacheable(value = "url-cache", key = "#alias")
    @Transactional(readOnly = true)
    public Optional<String> resolveAlias(String alias) {
        Optional<UrlEntity> urlOpt = urlRepository.findByAliasAndIsActiveTrue(alias);

        if (urlOpt.isEmpty()) {
            log.debug("Alias not found or inactive: {}", alias);
            return Optional.empty();
        }

        UrlEntity url = urlOpt.get();

        if (url.isExpired()) {
            log.debug("Alias expired: {}", alias);
            return Optional.empty();
        }

        return Optional.of(url.getDestinationUrl());
    }

    @Transactional
    public void trackRedirect(String alias) {
        urlRepository.findByAliasAndIsActiveTrue(alias).ifPresent(url -> {
            // Update URL redirect count and last redirect time
            url.incrementRedirectCount();
            urlRepository.save(url);

            // Update daily stats
            LocalDate today = LocalDate.now();
            UrlStatsEntity stats = urlStatsRepository
                .findByUrlIdAndStatDate(url.getId(), today)
                .orElse(UrlStatsEntity.builder()
                    .urlId(url.getId())
                    .statDate(today)
                    .redirectCount(0L)
                    .uniqueVisitors(0L)
                    .build());

            stats.incrementRedirectCount();
            urlStatsRepository.save(stats);

            log.debug("Tracked redirect for alias: {}", alias);
        });
    }
}
