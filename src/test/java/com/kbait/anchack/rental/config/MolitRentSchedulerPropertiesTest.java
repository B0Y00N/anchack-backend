package com.kbait.anchack.rental.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MolitRentSchedulerPropertiesTest {

    private static final String ACTIVE_CRON = "0 0 3 * * MON";

    @Test
    void 비활성_cron과_빈_문자열이면_빈_코드_목록을_허용한다() {
        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties("-", "");

        assertThat(properties.getLawdCodes()).isEmpty();
    }

    @Test
    void 비활성_cron과_공백_문자열이면_빈_코드_목록을_허용한다() {
        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties("-", "   ");

        assertThat(properties.getLawdCodes()).isEmpty();
    }

    @Test
    void 유효한_활성_cron과_코드_목록을_허용한다() {
        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties(
                ACTIVE_CRON,
                "11110,11140"
        );

        assertThat(properties.getLawdCodes()).containsExactly("11110", "11140");
    }

    @Test
    void 코드의_앞뒤_공백을_제거한다() {
        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties(
                ACTIVE_CRON,
                " 11110 , 11140 "
        );

        assertThat(properties.getLawdCodes()).containsExactly("11110", "11140");
    }

    @Test
    void 입력된_코드_순서를_보존한다() {
        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties(
                ACTIVE_CRON,
                "11740,11110,11680"
        );

        assertThat(properties.getLawdCodes()).containsExactly("11740", "11110", "11680");
    }

    @Test
    void cron_원문을_그대로_보존한다() {
        String cron = " 0 0 3 * * MON ";

        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties(cron, "11110");

        assertThat(properties.getCron()).isEqualTo(cron);
    }

    @Test
    void cron_문법은_검증하지_않는다() {
        String cron = "not-a-cron";

        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties(cron, "11110");

        assertThat(properties.getCron()).isEqualTo(cron);
    }

    @Test
    void 법정_시군구_코드를_변환하지_않고_그대로_보존한다() {
        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties(
                ACTIVE_CRON,
                "01110,11140"
        );

        assertThat(properties.getLawdCodes()).containsExactly("01110", "11140");
    }

    @Test
    void 반환된_코드_목록은_수정할_수_없다() {
        MolitRentSchedulerProperties properties = new MolitRentSchedulerProperties(ACTIVE_CRON, "11110");
        List<String> lawdCodes = properties.getLawdCodes();

        assertThatThrownBy(() -> lawdCodes.add("11140"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void null_cron은_거부한다() {
        assertThatThrownBy(() -> new MolitRentSchedulerProperties(null, "11110"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cron");
    }

    @Test
    void null_코드_문자열은_거부한다() {
        assertThatThrownBy(() -> new MolitRentSchedulerProperties(ACTIVE_CRON, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("법정 시군구 코드");
    }

    @Test
    void 활성_cron과_빈_문자열이면_거부한다() {
        assertThatThrownBy(() -> new MolitRentSchedulerProperties(ACTIVE_CRON, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("활성 cron");
    }

    @Test
    void 활성_cron과_공백_문자열이면_거부한다() {
        assertThatThrownBy(() -> new MolitRentSchedulerProperties(ACTIVE_CRON, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("활성 cron");
    }

    @Test
    void 공백이_포함된_비활성_표시는_활성_cron으로_판단한다() {
        assertThatThrownBy(() -> new MolitRentSchedulerProperties(" - ", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("활성 cron");
    }

    @Test
    void 네_자리_코드는_거부한다() {
        assertInvalidLawdCode("1111");
    }

    @Test
    void 여섯_자리_코드는_거부한다() {
        assertInvalidLawdCode("111100");
    }

    @Test
    void 문자가_포함된_코드는_거부한다() {
        assertInvalidLawdCode("1111A");
    }

    @Test
    void 앞쪽_빈_항목은_거부한다() {
        assertEmptyLawdCodeItem(",11110");
    }

    @Test
    void 중간_빈_항목은_거부한다() {
        assertEmptyLawdCodeItem("11110,,11140");
    }

    @Test
    void 뒤쪽_빈_항목은_거부한다() {
        assertEmptyLawdCodeItem("11110,");
    }

    @Test
    void 공백뿐인_중간_항목은_거부한다() {
        assertEmptyLawdCodeItem("11110,   ,11140");
    }

    @Test
    void 동일한_코드가_중복되면_거부한다() {
        assertDuplicateLawdCode("11110,11110");
    }

    @Test
    void 공백_제거_후_코드가_중복되면_거부한다() {
        assertDuplicateLawdCode("11110, 11110");
    }

    private void assertInvalidLawdCode(String lawdCodes) {
        assertThatThrownBy(() -> new MolitRentSchedulerProperties(ACTIVE_CRON, lawdCodes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ASCII 숫자 5자리");
    }

    private void assertEmptyLawdCodeItem(String lawdCodes) {
        assertThatThrownBy(() -> new MolitRentSchedulerProperties(ACTIVE_CRON, lawdCodes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("빈 항목");
    }

    private void assertDuplicateLawdCode(String lawdCodes) {
        assertThatThrownBy(() -> new MolitRentSchedulerProperties(ACTIVE_CRON, lawdCodes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복");
    }
}
