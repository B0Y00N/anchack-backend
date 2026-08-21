package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.RecommendationRow;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RecommendationRow에는 guName/dongName/latitude/longitude처럼 응답 조립 전용
 * (non-persisted) 필드가 섞여 있다(route/transportType/lineNum/vehicleType/walkMin/
 * transitMin은 V8부터 recommendations 컬럼으로 실제 저장된다). insertBatch가 컬럼을
 * 명시적으로 나열하지 않고 자동 매핑으로 바뀌면 guName 등도 그대로 INSERT 파라미터에
 * 섞여 들어가 컬럼 수 불일치로 터지므로, 바인딩되는 파라미터가 실제 recommendations
 * 테이블 컬럼에 대응하는 속성으로만 한정되는지 고정한다.
 */
class RecommendationMapperXmlTest {

    private static final String MAPPER_RESOURCE = "mappers/recommendation/RecommendationMapper.xml";
    private static final String MAPPER_NAMESPACE = RecommendationMapper.class.getName();
    private static final String INSERT_BATCH_STATEMENT = MAPPER_NAMESPACE + ".insertBatch";

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
    void mapperXmlLoadsInsertBatchStatement() {
        assertThat(configuration.hasStatement(INSERT_BATCH_STATEMENT)).isTrue();
    }

    @Test
    void insertBatch은_recommendations_테이블_컬럼만_INSERT한다() {
        String sql = getNormalizedInsertSql();

        assertThat(sql)
                .contains("INSERT INTO recommendations")
                .contains("condition_id")
                .contains("admin_dong_id")
                .contains("total_score")
                .contains("data_coverage_rate")
                .contains("commute_time")
                .contains("transfer_count")
                .contains("route")
                .contains("transport_type")
                .contains("line_num")
                .contains("vehicle_type")
                .contains("walk_min")
                .contains("transit_min")
                .contains("recommendation_reason")
                .contains("caution");
    }

    @Test
    void insertBatch은_응답_조립_전용_transient_필드는_바인딩하지_않는다() {
        List<ParameterMapping> parameterMappings = getInsertBatchMappedStatement()
                .getBoundSql(insertParameters())
                .getParameterMappings();

        assertThat(parameterMappings)
                .extracting(ParameterMapping::getProperty)
                .containsExactly(
                        "__frch_row_0.conditionId",
                        "__frch_row_0.adminDongId",
                        "__frch_row_0.totalScore",
                        "__frch_row_0.dataCoverageRate",
                        "__frch_row_0.commuteTime",
                        "__frch_row_0.transferCount",
                        "__frch_row_0.route",
                        "__frch_row_0.transportType",
                        "__frch_row_0.lineNum",
                        "__frch_row_0.vehicleType",
                        "__frch_row_0.walkMin",
                        "__frch_row_0.transitMin",
                        "__frch_row_0.rank",
                        "__frch_row_0.recommendationReason",
                        "__frch_row_0.caution"
                );
    }

    private MappedStatement getInsertBatchMappedStatement() {
        return configuration.getMappedStatement(INSERT_BATCH_STATEMENT);
    }

    private String getNormalizedInsertSql() {
        return getInsertBatchMappedStatement()
                .getBoundSql(insertParameters())
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<String, Object> insertParameters() {
        return Map.of("rows", List.of(createRow()));
    }

    /** guName/dongName/latitude/longitude까지 전부 채운 행을 만들어, 이 필드들이 있어도
     * 바인딩 파라미터 목록에 섞이지 않는지 확인한다(route 등은 실제로 저장되므로 반대로
     * 바인딩 목록에 포함되는지를 위 테스트에서 확인한다). */
    private RecommendationRow createRow() {
        return RecommendationRow.builder()
                .conditionId(1L)
                .adminDongId(2L)
                .totalScore(new BigDecimal("70.00"))
                .dataCoverageRate(new BigDecimal("100.00"))
                .commuteTime(30)
                .transferCount(1)
                .rank(1)
                .recommendationReason("reason")
                .caution("caution")
                .guName("은평구")
                .dongName("증산동")
                .latitude(new BigDecimal("37.5871"))
                .longitude(new BigDecimal("126.9095"))
                .route("6호선 증산 → 디지털미디어시티")
                .transportType("SUBWAY")
                .lineNum("6호선")
                .vehicleType("일반")
                .walkMin(5)
                .transitMin(15)
                .build();
    }
}
