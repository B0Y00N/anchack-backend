package com.kbait.anchack.admindong.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDongMapperXmlTest {

    @Test
    void 행정동_코드_ID_일괄_조회는_필요한_세_컬럼만_조회하고_조건절이_없다() throws Exception {
        String resource = "mappers/admindong/AdminDongMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            ).parse();
        }

        MappedStatement statement = configuration.getMappedStatement(
                AdminDongMapper.class.getName() + ".findAllCodeMappings"
        );
        String sql = statement.getBoundSql(null).getSql().replaceAll("\\s+", " ").trim();

        assertThat(sql)
                .isEqualTo("SELECT admin_dong_id, gu_code, dong_code FROM admin_dongs")
                .doesNotContainIgnoringCase("WHERE", "LIMIT", "JOIN");
    }
}
