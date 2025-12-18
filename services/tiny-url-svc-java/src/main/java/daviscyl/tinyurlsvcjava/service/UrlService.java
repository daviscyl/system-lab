package daviscyl.tinyurlsvcjava.service;

import daviscyl.tinyurlsvcjava.entity.UrlEntity;
import daviscyl.tinyurlsvcjava.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private static final String ALIAS_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int DEFAULT_ALIAS_LENGTH = 7;
    private static final Random RANDOM = new Random();

    @Transactional
    public UrlEntity createUrl(String destinationUrl, String customAlias, OffsetDateTime expiresAt, String userId) {
        String alias = customAlias != null ? customAlias : generateUniqueAlias();

        if (urlRepository.existsByAlias(alias)) {
            throw new IllegalArgumentException("Alias already exists: " + alias);
        }

        UrlEntity url = UrlEntity.builder()
            .alias(alias)
            .destinationUrl(destinationUrl)
            .userId(userId)
            .expiresAt(expiresAt)
            .isActive(true)
            .redirectCount(0L)
            .build();

        return urlRepository.save(url);
    }

    @Transactional(readOnly = true)
    public Optional<UrlEntity> findByAlias(String alias) {
        return urlRepository.findByAlias(alias);
    }

    @Transactional(readOnly = true)
    public Page<UrlEntity> listUrls(String userId, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return urlRepository.findByUserIdWithSearch(userId, search, pageable);
        }
        return urlRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public UrlEntity updateUrl(String alias, String userId, String newDestinationUrl, OffsetDateTime newExpiresAt) {
        UrlEntity url = urlRepository.findByAlias(alias)
            .orElseThrow(() -> new IllegalArgumentException("URL not found: " + alias));

        if (!url.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this URL");
        }

        if (newDestinationUrl != null) {
            url.setDestinationUrl(newDestinationUrl);
        }

        if (newExpiresAt != null) {
            url.setExpiresAt(newExpiresAt);
        }

        return urlRepository.save(url);
    }

    @Transactional
    public void deleteUrl(String alias, String userId) {
        UrlEntity url = urlRepository.findByAlias(alias)
            .orElseThrow(() -> new IllegalArgumentException("URL not found: " + alias));

        if (!url.getUserId().equals(userId)) {
            throw new SecurityException("User does not own this URL");
        }

        if (!url.getIsActive()) {
            throw new IllegalStateException("URL is already inactive");
        }

        url.setIsActive(false);
        urlRepository.save(url);
    }

    private String generateUniqueAlias() {
        String alias;
        int attempts = 0;
        do {
            alias = generateRandomAlias();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Unable to generate unique alias after 10 attempts");
            }
        } while (urlRepository.existsByAlias(alias));
        return alias;
    }

    private String generateRandomAlias() {
        StringBuilder sb = new StringBuilder(DEFAULT_ALIAS_LENGTH);
        for (int i = 0; i < DEFAULT_ALIAS_LENGTH; i++) {
            sb.append(ALIAS_CHARACTERS.charAt(RANDOM.nextInt(ALIAS_CHARACTERS.length())));
        }
        return sb.toString();
    }
}
