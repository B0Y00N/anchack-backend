package com.kbait.anchack.route.service.impl;

import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.service.RouteService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 통근 경로 계산 뼈대. 카카오 길찾기·ODSay 등 공급자가 아직 미확정이라 실제 호출은
 * 붙이지 않았음 - 공급자 확정 시 route.client에 클라이언트를 추가하고 이 클래스가
 * 그 클라이언트를 호출하도록 채워 넣으면 됨. destAddress가 없는 조건에서는 호출되지
 * 않으므로(recommendation.service.CommuteFilter 참고) 앱 부팅에는 영향 없음.
 */
@Service
public class RouteServiceImpl implements RouteService {

    @Override
    public CommuteResult calculateCommute(
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            String destAddress,
            String commuteType
    ) {
        throw new UnsupportedOperationException("통근 경로 계산은 아직 구현되지 않았습니다.");
    }
}
