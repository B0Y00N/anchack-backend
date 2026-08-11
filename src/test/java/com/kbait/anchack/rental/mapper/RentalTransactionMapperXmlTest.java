package com.kbait.anchack.rental.mapper;

import com.kbait.anchack.rental.domain.RentalTransactionCategoryCounts;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RentalTransactionMapperXmlTest {

    private static final String MAPPER_RESOURCE = "mappers/ingestion/RentalTransactionMapper.xml";
    private static final String MAPPER_NAMESPACE = RentalTransactionMapper.class.getName();
    private static final String FIND_COUNTS_STATEMENT =
            MAPPER_NAMESPACE + ".findCategoryCountsByGuCodeAndTransactionDateRange";

    private Configuration configuration;

    @BeforeEach
    void setUp() throws IOException {
        configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(MAPPER_RESOURCE)) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    MAPPER_RESOURCE,
                    configuration.getSqlFragments()
            );
            mapperBuilder.parse();
        }
    }

    @Test
    void Mapper_XML과_세_Mapped_Statement를_오류_없이_로드한다() {
        assertThat(configuration.hasStatement(FIND_COUNTS_STATEMENT)).isTrue();
        assertThat(configuration.hasStatement(MAPPER_NAMESPACE + ".deleteByGuCodeAndTransactionDateRange"))
                .isTrue();
        assertThat(configuration.hasStatement(MAPPER_NAMESPACE + ".insertBatch")).isTrue();
    }

    @Test
    void 건수_조회는_불변_값_객체를_constructor_resultMap으로_매핑한다() {
        MappedStatement mappedStatement = configuration.getMappedStatement(FIND_COUNTS_STATEMENT);
        ResultMap resultMap = mappedStatement.getResultMaps().get(0);

        assertThat(resultMap.getType()).isEqualTo(RentalTransactionCategoryCounts.class);
        assertThat(resultMap.getConstructorResultMappings())
                .extracting(ResultMapping::getColumn)
                .containsExactly("officetel_count", "row_house_count", "single_house_count");
        assertThat(resultMap.getConstructorResultMappings())
                .extracting(ResultMapping::getJavaType)
                .containsExactly(long.class, long.class, long.class);
    }

    @Test
    void 건수_조회는_구와_반개방_거래일_범위를_사용한다() {
        String sql = getNormalizedFindCountsSql();

        assertThat(sql)
                .contains("gu_code = ?")
                .contains("transaction_date >= ?")
                .contains("transaction_date < ?");
        assertThat(getFindCountsMappedStatement().getBoundSql(findCountsParameters()).getParameterMappings())
                .extracting(ParameterMapping::getProperty)
                .containsExactly("guCode", "startDate", "endDateExclusive");
    }

    @Test
    void 건수_조회는_여섯_house_type만_API_유형별로_집계한다() {
        String sql = getNormalizedFindCountsSql();

        assertThat(sql)
                .contains("house_type = '오피스텔'")
                .contains("house_type IN ('연립', '다세대', '연립다세대')")
                .contains("house_type IN ('단독', '다가구')");
    }

    private MappedStatement getFindCountsMappedStatement() {
        return configuration.getMappedStatement(FIND_COUNTS_STATEMENT);
    }

    private String getNormalizedFindCountsSql() {
        return getFindCountsMappedStatement()
                .getBoundSql(findCountsParameters())
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<String, Object> findCountsParameters() {
        return Map.of(
                "guCode", "11680",
                "startDate", LocalDate.of(2026, 6, 1),
                "endDateExclusive", LocalDate.of(2026, 7, 1)
        );
    }
}
