package com.wardk.meeteam_backend.domain.llm;

import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

@Component
public class LlmConcurrencyLimiter {
    private final Semaphore semaphore = new Semaphore(2);
    
    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }
    public void release() {
        semaphore.release();
    }
}
