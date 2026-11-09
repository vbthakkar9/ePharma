package com.pharmacy.saas.security.service;

import com.pharmacy.saas.common.exception.BusinessException;
import com.pharmacy.saas.common.constants.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RateLimitingService {

    private static class RateLimitBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private Instant windowStart = Instant.now();
        private final int maxRequests;
        private final Duration window;

        public RateLimitBucket(int maxRequests, Duration window) {
            this.maxRequests = maxRequests;
            this.window = window;
        }

        public synchronized boolean tryConsume() {
            Instant now = Instant.now();
            if (Duration.between(windowStart, now).compareTo(window) >= 0) {
                // Window expired, reset
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= maxRequests;
        }
    }

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public void checkLimit(String key, int maxRequests, Duration window) {
        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket(maxRequests, window));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new BusinessException(ErrorCodes.RATE_LIMIT_EXCEEDED,
                    "Too many requests. Please try again later.");
        }
    }

    public void checkLoginLimit(String ipAddress) {
        checkLimit("login:" + ipAddress, 5, Duration.ofMinutes(1));
    }

    public void checkForgotPasswordLimit(String ipAddress) {
        checkLimit("forgot_password:" + ipAddress, 5, Duration.ofMinutes(1));
    }

    public void clearBucket(String key) {
        buckets.remove(key);
    }
}