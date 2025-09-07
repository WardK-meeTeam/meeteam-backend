package com.wardk.meeteam_backend.domain.llm;

import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

@Component
public class LlmConcurrencyLimiter {
    private final Semaphore semaphore = new Semaphore(5); // 3개→5개로 증가하여 더 많은 병렬 처리

    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }

    public void release() {
        semaphore.release();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
