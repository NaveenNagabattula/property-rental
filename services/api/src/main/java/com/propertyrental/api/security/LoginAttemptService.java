package com.propertyrental.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPT = 5;
    private static final long LOCK_TIME_MS = 15 * 60 * 1000; // 15 minutes

    private final ConcurrentHashMap<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lockCache = new ConcurrentHashMap<>();

    public void loginFailed(String ip) {
        if (isBlocked(ip)) {
            log.warn("Login attempt rejected for ip={}: already locked", ip);
            return;
        }
        int attempts = attemptsCache.getOrDefault(ip, 0) + 1;
        attemptsCache.put(ip, attempts);
        if (attempts >= MAX_ATTEMPT) {
            lockCache.put(ip, Instant.now().plusMillis(LOCK_TIME_MS));
            log.warn("Login locked for ip={} after {} failed attempts", ip, MAX_ATTEMPT);
        } else {
            log.warn("Failed login attempt {}/{} for ip={}", attempts, MAX_ATTEMPT, ip);
        }
    }

    public void loginSucceeded(String ip) {
        boolean hadFailedAttempts = attemptsCache.containsKey(ip) || lockCache.containsKey(ip);
        attemptsCache.remove(ip);
        lockCache.remove(ip);
        if (hadFailedAttempts) {
            log.info("Login lock cleared for ip={} after successful authentication", ip);
        }
    }

    public boolean isBlocked(String ip) {
        Instant lockExpiry = lockCache.get(ip);
        if (lockExpiry == null) {
            return false;
        }
        if (Instant.now().isAfter(lockExpiry)) {
            lockCache.remove(ip);
            attemptsCache.remove(ip);
            return false;
        }
        return true;
    }
}
