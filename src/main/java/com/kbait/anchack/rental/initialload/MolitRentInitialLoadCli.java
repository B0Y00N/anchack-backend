package com.kbait.anchack.rental.initialload;

import com.kbait.anchack.common.config.RootConfig;
import com.kbait.anchack.rental.config.MolitRentApiProperties;
import com.kbait.anchack.rental.initialload.MolitRentInitialLoadRunner.FailedTask;
import com.kbait.anchack.rental.initialload.MolitRentInitialLoadRunner.InitialLoadRequest;
import com.kbait.anchack.rental.initialload.MolitRentInitialLoadRunner.InitialLoadResult;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.time.Clock;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gradle JavaExec로만 명시적으로 시작하는 일회성 초기 적재 진입점이다.
 */
public final class MolitRentInitialLoadCli {

    private static final Logger log = LoggerFactory.getLogger(MolitRentInitialLoadCli.class);

    private static final String ALL_OPTION = "--all";
    private static final String YEAR_MONTH_OPTION_PREFIX = "--year-month=";
    private static final String LAWD_CODE_OPTION_PREFIX = "--lawd-code=";
    private static final String RETRY_COMMAND_FORMAT =
            "./gradlew molitRentInitialLoad --args='--year-month=%s --lawd-code=%s'";
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ContextFactory contextFactory;

    public MolitRentInitialLoadCli() {
        this(MolitRentInitialLoadCli::createRootContext);
    }

    MolitRentInitialLoadCli(ContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public static void main(String[] args) {
        System.exit(new MolitRentInitialLoadCli().run(args));
    }

    int run(String[] args) {
        final InitialLoadRequest request;
        try {
            request = parseRequest(args, Clock.system(SEOUL_ZONE_ID));
        } catch (IllegalArgumentException exception) {
            log.error("국토부 전월세 초기 적재 입력 오류: exceptionType={}", exception.getClass().getName());
            return 2;
        }

        try (ConfigurableApplicationContext context = contextFactory.create()) {
            validateMolitApiKey(context.getBean(MolitRentApiProperties.class));
            MolitRentIngestionService ingestionService = context.getBean(MolitRentIngestionService.class);
            Clock clock = context.getBean("molitRentClock", Clock.class);
            MolitRentInitialLoadRunner runner = new MolitRentInitialLoadRunner(ingestionService, clock);
            InitialLoadResult result = runner.run(request);

            log.info(
                    "국토부 전월세 초기 적재 완료: successfulTaskCount={}, failedTaskCount={}",
                    result.getSuccessfulTaskCount(),
                    result.getFailedTaskCount()
            );
            logFailedTasks(result);

            return result.hasFailures() ? 1 : 0;
        } catch (RuntimeException exception) {
            log.error("국토부 전월세 초기 적재 초기화 또는 실행 실패: exceptionType={}", exception.getClass().getName());
            return 2;
        }
    }

    private void validateMolitApiKey(MolitRentApiProperties properties) {
        if (properties.getServiceKey().isBlank()) {
            throw new IllegalStateException("국토부 API 서비스 키가 설정되지 않았습니다.");
        }
    }

    private void logFailedTasks(InitialLoadResult result) {
        for (FailedTask failedTask : result.getFailedTasks()) {
            String retryCommand = RETRY_COMMAND_FORMAT.formatted(
                    failedTask.getYearMonth(),
                    failedTask.getLawdCode()
            );
            log.warn(
                    "국토부 전월세 초기 적재 실패 대상: lawdCode={}, yearMonth={}, exceptionType={}, retryCommand={}",
                    failedTask.getLawdCode(),
                    failedTask.getYearMonth(),
                    failedTask.getExceptionType(),
                    retryCommand
            );
        }
    }

    static InitialLoadRequest parseRequest(String[] args, Clock clock) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("--all 또는 부분 실행 옵션이 필요합니다.");
        }

        boolean all = false;
        String yearMonthValue = null;
        String lawdCode = null;

        for (String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("초기 적재 옵션은 null일 수 없습니다.");
            }
            if (ALL_OPTION.equals(arg)) {
                if (all) {
                    throw new IllegalArgumentException("--all 옵션은 한 번만 지정할 수 있습니다.");
                }
                all = true;
                continue;
            }
            if (arg.startsWith(YEAR_MONTH_OPTION_PREFIX)) {
                if (yearMonthValue != null) {
                    throw new IllegalArgumentException("--year-month 옵션은 한 번만 지정할 수 있습니다.");
                }
                yearMonthValue = arg.substring(YEAR_MONTH_OPTION_PREFIX.length());
                continue;
            }
            if (arg.startsWith(LAWD_CODE_OPTION_PREFIX)) {
                if (lawdCode != null) {
                    throw new IllegalArgumentException("--lawd-code 옵션은 한 번만 지정할 수 있습니다.");
                }
                lawdCode = arg.substring(LAWD_CODE_OPTION_PREFIX.length());
                continue;
            }

            throw new IllegalArgumentException("지원하지 않는 초기 적재 옵션입니다.");
        }

        if (all) {
            if (yearMonthValue != null || lawdCode != null) {
                throw new IllegalArgumentException("--all과 부분 실행 옵션을 함께 지정할 수 없습니다.");
            }
            return InitialLoadRequest.all();
        }

        if (yearMonthValue == null || lawdCode == null) {
            throw new IllegalArgumentException("부분 실행에는 --year-month와 --lawd-code를 모두 지정해야 합니다.");
        }
        if (!lawdCode.matches("[0-9]{5}")) {
            throw new IllegalArgumentException("--lawd-code는 숫자 5자리여야 합니다.");
        }
        try {
            YearMonth yearMonth = YearMonth.parse(yearMonthValue);
            validatePartialRequest(yearMonth, lawdCode, clock);
            return InitialLoadRequest.partial(yearMonth, lawdCode);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("--year-month는 YYYY-MM 형식이어야 합니다.", exception);
        }
    }

    private static void validatePartialRequest(YearMonth yearMonth, String lawdCode, Clock clock) {
        if (!MolitRentInitialLoadRunner.SEOUL_LAWD_CODES.contains(lawdCode)) {
            throw new IllegalArgumentException("초기 적재 대상은 서울 25개 구 법정 시군구 코드여야 합니다.");
        }

        YearMonth currentYearMonth = YearMonth.now(clock);
        if (yearMonth.isBefore(currentYearMonth.minusMonths(11)) || yearMonth.isAfter(currentYearMonth)) {
            throw new IllegalArgumentException(
                    "초기 적재 대상 월은 KST 기준 현재월과 직전 11개월 범위여야 합니다."
            );
        }
    }

    private static ConfigurableApplicationContext createRootContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("molitRentInitialLoadSchedulerOverrides", schedulerOverrides())
        );
        context.register(RootConfig.class);
        context.refresh();
        return context;
    }

    static Map<String, Object> schedulerOverrides() {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("molit.rent.scheduler.cron", "-");
        overrides.put("kakao.place.scheduler.cron", "-");
        overrides.put("cctv.place.scheduler.cron", "-");
        return Map.copyOf(overrides);
    }

    @FunctionalInterface
    interface ContextFactory {

        ConfigurableApplicationContext create();
    }
}
