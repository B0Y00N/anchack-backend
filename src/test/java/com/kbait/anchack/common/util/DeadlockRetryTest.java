package com.kbait.anchack.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DeadlockLoserDataAccessException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeadlockRetryTest {

    @Test
    void 데드락_없이_바로_성공하면_한_번만_실행한다() {
        AtomicInteger attempts = new AtomicInteger();

        String result = DeadlockRetry.execute(() -> {
            attempts.incrementAndGet();
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void 데드락이_두_번_나도_세_번째_시도에서_성공하면_결과를_반환한다() {
        AtomicInteger attempts = new AtomicInteger();

        String result = DeadlockRetry.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new DeadlockLoserDataAccessException("deadlock", null);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void 최대_시도_횟수를_넘기면_데드락_예외를_그대로_던진다() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> DeadlockRetry.execute(() -> {
            attempts.incrementAndGet();
            throw new DeadlockLoserDataAccessException("deadlock", null);
        })).isInstanceOf(DeadlockLoserDataAccessException.class);

        assertThat(attempts.get()).isEqualTo(3);
    }

    /**
     * Thread.sleep()은 잠들기 직전에 인터럽트 플래그가 이미 켜져 있으면 그 자리에서 바로
     * InterruptedException을 던지는 규약이 있다 - 그래서 별도 스레드로 타이밍을 맞출 필요
     * 없이, 테스트 스레드 자신에 미리 인터럽트를 걸어두면 execute()의 재시도 대기
     * (sleepBeforeRetry) 진입 즉시 이 분기를 탈 수 있다.
     */
    @Test
    void 재시도_대기_중_인터럽트되면_IllegalStateException을_던지고_인터럽트_상태를_복원한다() {
        Thread.currentThread().interrupt();

        try {
            assertThatThrownBy(() -> DeadlockRetry.execute(() -> {
                throw new DeadlockLoserDataAccessException("deadlock", null);
            }))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(InterruptedException.class);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // 인터럽트 상태를 정리해 이후 테스트에 영향을 주지 않는다.
        }
    }
}
