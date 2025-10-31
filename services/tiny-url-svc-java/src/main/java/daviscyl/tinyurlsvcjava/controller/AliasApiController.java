package daviscyl.tinyurlsvcjava.controller;

import daviscyl.tinyurlsvcjava.api.AliasApi;
import daviscyl.tinyurlsvcjava.service.RedirectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AliasApiController implements AliasApi {

    private final RedirectService redirectService;

    @Override
    public ResponseEntity<Void> resolveAlias(String alias) {
        return redirectService.resolveAlias(alias)
            .map(destinationUrl -> {
                // Track the redirect asynchronously (in a real app, use @Async)
                try {
                    redirectService.trackRedirect(alias);
                } catch (Exception e) {
                    log.error("Failed to track redirect for alias: {}", alias, e);
                }

                // Return 302 redirect
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(destinationUrl)).<Void>build();
            })
            .orElseGet(() -> ResponseEntity.<Void>status(HttpStatus.NOT_FOUND).build());
    }
}
