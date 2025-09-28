package com.wardk.meeteam_backend.domain.llm;

import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

@Component
public class LlmConcurrencyLimiter {
    // 동시 실행 수를 3개로 제한 - LLM API 부하 방지 및 안정성 확보
    private final Semaphore semaphore = new Semaphore(5);

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
