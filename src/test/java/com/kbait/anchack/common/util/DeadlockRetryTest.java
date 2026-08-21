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
}
