package com.kbait.anchack.place.mapper;

import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.domain.PlaceCategory;
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

class PlaceMapperXmlTest {

    private static final String MAPPER_RESOURCE = "mappers/place/PlaceMapper.xml";
    private static final String MAPPER_NAMESPACE = PlaceMapper.class.getName();
    private static final String UPSERT_BATCH_STATEMENT = MAPPER_NAMESPACE + ".upsertBatch";

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
    void mapperXmlLoadsMappedStatement() {
        assertThat(configuration.hasStatement(UPSERT_BATCH_STATEMENT)).isTrue();
    }

    @Test
    void upsertBatchInsertsPlaceColumnsWithoutDataDate() {
        String sql = getNormalizedUpsertSql();

        assertThat(sql)
                .contains("INSERT INTO places")
                .contains("external_id")
                .contains("admin_dong_id")
                .contains("category")
                .contains("name")
                .contains("latitude")
                .contains("longitude")
                .contains("data_source_id")
                .doesNotContain("data_date");
    }

    @Test
    void upsertBatchUpdatesMutableColumnsOnDuplicateKey() {
        String sql = getNormalizedUpsertSql();

        assertThat(sql)
                .contains("ON DUPLICATE KEY UPDATE")
                .contains("admin_dong_id = VALUES(admin_dong_id)")
                .contains("category = VALUES(category)")
                .contains("name = VALUES(name)")
                .contains("latitude = VALUES(latitude)")
                .contains("longitude = VALUES(longitude)")
                .contains("updated_at = CURRENT_TIMESTAMP");
    }

    @Test
    void upsertBatchBindsPlacePropertiesInInsertOrder() {
        List<ParameterMapping> parameterMappings = getUpsertMappedStatement()
                .getBoundSql(upsertParameters())
                .getParameterMappings();

        assertThat(parameterMappings)
                .extracting(ParameterMapping::getProperty)
                .containsExactly(
                        "__frch_place_0.externalId",
                        "__frch_place_0.adminDongId",
                        "__frch_place_0.category",
                        "__frch_place_0.name",
                        "__frch_place_0.latitude",
                        "__frch_place_0.longitude",
                        "__frch_place_0.dataSourceId"
                );
    }

    private MappedStatement getUpsertMappedStatement() {
        return configuration.getMappedStatement(UPSERT_BATCH_STATEMENT);
    }

    private String getNormalizedUpsertSql() {
        return getUpsertMappedStatement()
                .getBoundSql(upsertParameters())
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<String, Object> upsertParameters() {
        return Map.of("places", List.of(createPlace()));
    }

    private Place createPlace() {
        return Place.builder()
                .externalId("kakao-123")
                .adminDongId(1L)
                .category(PlaceCategory.PHARMACY)
                .name("test pharmacy")
                .latitude(new BigDecimal("37.478154"))
                .longitude(new BigDecimal("126.951484"))
                .dataSourceId(13L)
                .build();
    }
}
