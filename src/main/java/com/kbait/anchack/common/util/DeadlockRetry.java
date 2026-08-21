package com.kbait.anchack.common.util;

import org.springframework.dao.DeadlockLoserDataAccessException;

import java.util.function.Supplier;

/**
 * MySQL 데드락(DeadlockLoserDataAccessException) 전용 재시도 유틸리티.
 *
 * InnoDB는 데드락을 감지하면 희생 트랜잭션 전체를 롤백한다 - 트랜잭션 중간의 SQL문 하나만
 * 다시 실행해서는 복구되지 않고, @Transactional 메서드 호출 자체를 처음부터 다시 시작해야
 * 새 트랜잭션으로 재시도된다. Spring의 @Transactional은 AOP 프록시로 동작하므로 같은 빈
 * 안에서 this.method()로 자기 자신을 호출하면 프록시를 안 거쳐 트랜잭션이 새로 시작되지
 * 않는다(self-invocation 문제) - 그래서 이 재시도는 @Transactional 메서드를 "호출하는 쪽"
 * (Controller)에서 걸어야 매 시도마다 실제로 새 트랜잭션이 열린다.
 *
 * condition/recommendation 생성·재계산이 실제 사용처다: k6 write_heavy 부하테스트에서
 * VU 20대부터 recommendations INSERT가 admin_dongs를 참조하는 FK 때문에(소수의 상위권
 * 행정동에 여러 트랜잭션의 참조가 몰림) 동시 요청끼리 데드락이 걸려 create 요청의 4.37%가
 * 실패하는 걸 확인했다.
 */
public final class DeadlockRetry {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 50;

    private DeadlockRetry() {
    }

    public static <T> T execute(Supplier<T> action) {
        int attempt = 0;

        while (true) {
            try {
                return action.get();
            } catch (DeadlockLoserDataAccessException e) {
                attempt++;

                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }

                sleepBeforeRetry(attempt);
            }
        }
    }

    /**
     * 재시도들이 같은 타이밍에 다시 몰려 데드락을 재생산하지 않도록, 시도 횟수에 비례해
     * 대기시간을 늘리고 약간의 지터를 섞는다.
     */
    private static void sleepBeforeRetry(int attempt) {
        long jitterMs = (long) (Math.random() * BASE_BACKOFF_MS);

        try {
            Thread.sleep(BASE_BACKOFF_MS * attempt + jitterMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("데드락 재시도 대기 중 인터럽트됨", e);
        }
    }
}
