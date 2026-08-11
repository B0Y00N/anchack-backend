package com.kbait.anchack.place.mapper;

import org.apache.ibatis.annotations.Param;

public interface PlaceAdminDongMapper {

    Long findIdByGuNameAndDongName(
            @Param("guName") String guName,
            @Param("dongName") String dongName
    );

    Long findIdByGuCodeAndDongCode(
            @Param("guCode") String guCode,
            @Param("dongCode") String dongCode
    );
}
