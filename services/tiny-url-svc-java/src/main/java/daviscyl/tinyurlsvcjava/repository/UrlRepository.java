package daviscyl.tinyurlsvcjava.repository;

import daviscyl.tinyurlsvcjava.entity.UrlEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByAlias(String alias);

    Optional<UrlEntity> findByAliasAndIsActiveTrue(String alias);

    boolean existsByAlias(String alias);

    Page<UrlEntity> findByUserId(String userId, Pageable pageable);

    @Query("SELECT u FROM UrlEntity u WHERE u.userId = :userId " +
           "AND (:search IS NULL OR u.alias LIKE %:search% OR u.destinationUrl LIKE %:search%)")
    Page<UrlEntity> findByUserIdWithSearch(
        @Param("userId") String userId,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT COUNT(u) FROM UrlEntity u WHERE u.userId = :userId " +
           "AND (:search IS NULL OR u.alias LIKE %:search% OR u.destinationUrl LIKE %:search%)")
    long countByUserIdWithSearch(
        @Param("userId") String userId,
        @Param("search") String search
    );
}
