package com.santhosh.fixture.urlshortener;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ShortUrlService {
    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final AtomicLong sequence = new AtomicLong(100_000);
    private final Map<String, URI> urls = new ConcurrentHashMap<>();

    public String shorten(URI target) {
        if (target == null || target.getScheme() == null ||
                !(target.getScheme().equals("http") || target.getScheme().equals("https"))) {
            throw new IllegalArgumentException("Only absolute HTTP(S) URLs are accepted");
        }
        String code = encode(sequence.incrementAndGet());
        urls.put(code, target);
        return code;
    }

    public Optional<URI> resolve(String code) {
        return Optional.ofNullable(urls.get(code));
    }

    private String encode(long value) {
        StringBuilder result = new StringBuilder();
        do {
            result.append(ALPHABET[(int) (value % ALPHABET.length)]);
            value /= ALPHABET.length;
        } while (value > 0);
        return result.reverse().toString();
    }
}
