package com.kbait.anchack.rental.registry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeoulLawdCodeRegistryTest {

    private final SeoulLawdCodeRegistry registry = new SeoulLawdCodeRegistry();

    @Test
    void 서울_25개_법정_시군구_코드와_구_이름을_제공한다() {
        assertThat(SeoulLawdCodeRegistry.seoulLawdCodes())
                .hasSize(25)
                .startsWith("11110", "11140", "11170")
                .endsWith("11680", "11710", "11740");
        assertThat(registry.requireGuName("11110")).isEqualTo("종로구");
        assertThat(registry.requireGuName("11620")).isEqualTo("관악구");
        assertThat(registry.requireGuName("11740")).isEqualTo("강동구");
    }

    @Test
    void 지원하지_않는_코드는_명시적으로_실패한다() {
        assertThatThrownBy(() -> registry.requireGuName("26440"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는");
    }
}
