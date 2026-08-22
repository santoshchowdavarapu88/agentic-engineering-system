package com.santhosh.fixture.urlshortener;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping
public class ShortUrlController {
    private final ShortUrlService service;

    public ShortUrlController(ShortUrlService service) { this.service = service; }

    @PostMapping("/api/v1/urls")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateShortUrlResponse create(@Valid @RequestBody CreateShortUrlRequest request) {
        return new CreateShortUrlResponse(service.shorten(request.url()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return service.resolve(code).map(target -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(target).<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record CreateShortUrlRequest(@NotNull URI url) { }
    public record CreateShortUrlResponse(String code) { }
}
