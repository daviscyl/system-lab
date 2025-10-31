package daviscyl.tinyurlsvcjava.controller;

import daviscyl.tinyurlsvcjava.api.UrlsApi;
import daviscyl.tinyurlsvcjava.entity.UrlEntity;
import daviscyl.tinyurlsvcjava.model.*;
import daviscyl.tinyurlsvcjava.service.StatsService;
import daviscyl.tinyurlsvcjava.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UrlsApiController implements UrlsApi {

    private final UrlService urlService;
    private final StatsService statsService;

    @Override
    public ResponseEntity<UrlResource> createUrl(CreateUrlRequest createUrlRequest) {
        String userId = getCurrentUserId();

        UrlEntity entity = urlService.createUrl(
            createUrlRequest.getLongUrl().toString(),
            createUrlRequest.getCustomAlias(),
            createUrlRequest.getExpiresAt(),
            userId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toUrlResource(entity));
    }

    @Override
    public ResponseEntity<Void> deleteUrl(String alias) {
        String userId = getCurrentUserId();
        urlService.deleteUrl(alias, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UrlResource> getUrl(String alias) {
        String userId = getCurrentUserId();

        UrlEntity entity = urlService.findByAlias(alias)
            .orElseThrow(() -> new IllegalArgumentException("URL not found"));

        if (!entity.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this URL");
        }

        return ResponseEntity.ok(toUrlResource(entity));
    }

    @Override
    public ResponseEntity<UrlStats> getUrlStats(String alias, OffsetDateTime rangeStart, OffsetDateTime rangeEnd) {
        String userId = getCurrentUserId();

        StatsService.StatsResult stats = statsService.getUrlStats(alias, userId, rangeStart, rangeEnd);

        List<DailyRedirectMetrics> dailyMetrics = stats.dailyStats().stream()
            .map(s -> new DailyRedirectMetrics()
                .date(s.getStatDate())
                .redirectCount(s.getRedirectCount().intValue())
                .uniqueVisitors(s.getUniqueVisitors().intValue()))
            .collect(Collectors.toList());

        UrlStats urlStats = new UrlStats()
            .alias(alias)
            .totalRedirects(stats.totalRedirectCount().intValue())
            .uniqueVisitors(0)
            .timeSeries(dailyMetrics);

        if (stats.lastRedirectAt() != null) {
            urlStats.setLastRedirectAt(JsonNullable.of(stats.lastRedirectAt()));
        }

        return ResponseEntity.ok(urlStats);
    }

    @Override
    public ResponseEntity<ListUrlsResponse> listUrls(Integer page, Integer pageSize, String search) {
        String userId = getCurrentUserId();

        int pageNum = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UrlEntity> urlPage = urlService.listUrls(userId, search, pageable);

        List<UrlResource> urls = urlPage.getContent().stream()
            .map(this::toUrlResource)
            .collect(Collectors.toList());

        int totalPages = urlPage.getTotalPages();
        ListUrlsResponse response = new ListUrlsResponse()
            .data(urls)
            .page(pageNum)
            .pageSize(size)
            .totalItems((int) urlPage.getTotalElements())
            .totalPages(totalPages);

        if (urlPage.hasNext()) {
            response.setNextPage(JsonNullable.of(pageNum + 1));
        }

        return ResponseEntity.ok()
            .header("X-Total-Count", String.valueOf(urlPage.getTotalElements()))
            .body(response);
    }

    @Override
    public ResponseEntity<UrlResource> updateUrl(String alias, UpdateUrlRequest updateUrlRequest) {
        String userId = getCurrentUserId();

        String newDestUrl = updateUrlRequest.getLongUrl() != null ?
            updateUrlRequest.getLongUrl().toString() : null;

        OffsetDateTime newExpiry = null;
        if (updateUrlRequest.getExpiresAt() != null && updateUrlRequest.getExpiresAt().isPresent()) {
            newExpiry = updateUrlRequest.getExpiresAt().get();
        }

        UrlEntity entity = urlService.updateUrl(
            alias,
            userId,
            newDestUrl,
            newExpiry
        );

        return ResponseEntity.ok(toUrlResource(entity));
    }

    private UrlResource toUrlResource(UrlEntity entity) {
        UrlResource resource = new UrlResource()
            .id(UUID.randomUUID())
            .alias(entity.getAlias())
            .longUrl(URI.create(entity.getDestinationUrl()))
            .shortUrl(URI.create("https://tiny.url/" + entity.getAlias()))
            .createdAt(entity.getCreatedAt())
            .active(entity.getIsActive());

        if (entity.getUpdatedAt() != null) {
            resource.setUpdatedAt(entity.getUpdatedAt());
        }

        if (entity.getExpiresAt() != null) {
            resource.setExpiresAt(JsonNullable.of(entity.getExpiresAt()));
        }

        return resource;
    }

    private String getCurrentUserId() {
        // TODO: Extract from Spring Security Authentication
        // For now, return a placeholder
        return "user-123";
    }
}
