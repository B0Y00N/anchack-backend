package com.kbait.anchack.recommendation.client.impl;

import com.kbait.anchack.recommendation.client.OpenAiProxyClient;
import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.dto.CategoryScoreBreakdown;
import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.RecommendationReasonContext;
import com.kbait.anchack.recommendation.exception.OpenAiApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * OpenAI 프록시로 recommendation_reason/caution을 생성한다. 한 번의 호출로 두 필드를 모두
 * 받기 위해 "추천 이유: .../ 주의사항: ..." 두 줄 형식으로만 답하도록 프롬프트에서 강제하고,
 * 응답 문자열을 그 형식대로 파싱한다. 호출 실패나 형식이 어긋나 파싱이 안 되는 경우 예외를
 * 전파하지 않고 플레이스홀더로 폴백한다 - 이미 하드필터·소프트스코어링을 통과해 상위 5개로
 * 선정된 추천 결과 자체를 reason 생성 실패 때문에 날리지 않기 위함
 * (recommendation.service.CommuteFilter가 카카오 API 실패를 흡수하는 것과 같은 원칙).
 */
@RequiredArgsConstructor
public class OpenAiRecommendationReasonClient implements RecommendationReasonClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiRecommendationReasonClient.class);

    private static final String PLACEHOLDER = "추후 openai api 호출";
    private static final String REASON_PREFIX = "추천 이유:";
    private static final String CAUTION_PREFIX = "주의사항:";

    private static final Map<String, String> CATEGORY_LABELS = Map.ofEntries(
            Map.entry("CULTURE", "문화"),
            Map.entry("TRANSIT", "교통"),
            Map.entry("SPORTS", "스포츠"),
            Map.entry("NATURE", "자연"),
            Map.entry("CONVENIENCE", "편의시설"),
            Map.entry("HEALTHCARE", "의료"),
            Map.entry("FOOD", "식음료"),
            Map.entry("SAFETY", "안전"),
            Map.entry("SILENCE", "조용함")
    );

    private final OpenAiProxyClient openAiProxyClient;

    @Override
    public GeneratedReason generate(RecommendationReasonContext context) {
        try {
            String content = openAiProxyClient.chat(buildPrompt(context));

            return parse(content);
        } catch (OpenAiApiException e) {
            log.warn("OpenAI 프록시 호출 실패, 플레이스홀더로 대체: adminDongId={}, message={}",
                    context.getAdminDongId(), e.getMessage());
            return placeholder();
        } catch (IllegalStateException e) {
            log.warn("OpenAI 응답 형식이 예상과 달라 플레이스홀더로 대체: adminDongId={}, message={}",
                    context.getAdminDongId(), e.getMessage());
            return placeholder();
        }
    }

    private String buildPrompt(RecommendationReasonContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("아래 행정동을 사용자에게 추천하는 이유와 주의사항을 작성해줘.\n");
        prompt.append("행정동: ").append(context.getAdminDongName()).append('\n');
        prompt.append("종합 점수(0~100): ").append(context.getTotalScore()).append('\n');

        if (context.getCommuteTime() != null) {
            prompt.append("통근 시간: ").append(context.getCommuteTime()).append("분, 환승 ")
                    .append(context.getTransferCount()).append("회\n");
        }

        prompt.append(buildCategorySection(context.getCategoryBreakdowns()));

        prompt.append("응답은 반드시 아래 두 줄 형식만 사용해:\n");
        prompt.append(REASON_PREFIX).append(" <장점 1~3개, 쉼표로 구분된 짧은 문구>\n");
        prompt.append(CAUTION_PREFIX).append(" <단점/우려사항 1~3개, 쉼표로 구분된 짧은 문구. ")
                .append("긍정적인 내용은 절대 쓰지 마>\n");
        prompt.append("모든 문구는 반드시 '~해요/~있어요'체로 통일해서 써(예: '~함', '~음', '~임' 같은 ")
                .append("문어체는 절대 섞지 마).\n");
        prompt.append("언급 가능한 주제는 오직 위에 나열된 카테고리(").append(categoryLabelsText(context))
                .append(")뿐이야. 그 외 주제(예: 교통/위치/접근성, 조명, 자연경관, 상권 등 위에 없는 것)는 ")
                .append("절대 언급하지 마. 위에 주어진 점수/정보로 확인되지 않는 구체적인 사실(특정 시설 이름, ")
                .append("소음의 종류 등)도 지어내지 말고, 주어진 카테고리와 점수 수준에 기반한 내용만 써.\n");
        prompt.append("예(카테고리와 점수 수준만으로 표현한 것 - 특정 시설·경관 등 구체적 사실은 ")
                .append("언급하지 않음): ").append(REASON_PREFIX)
                .append(" 안전 점수가 높아요, 편의시설이 풍부해요, 조용한 편이에요");

        return prompt.toString();
    }

    private String categoryLabelsText(RecommendationReasonContext context) {
        List<CategoryScoreBreakdown> breakdowns = context.getCategoryBreakdowns();

        if (breakdowns == null || breakdowns.isEmpty()) {
            return "없음";
        }

        return breakdowns.stream()
                .map(breakdown -> CATEGORY_LABELS.getOrDefault(breakdown.getCategory(), breakdown.getCategory()))
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String buildCategorySection(List<CategoryScoreBreakdown> breakdowns) {
        if (breakdowns == null || breakdowns.isEmpty()) {
            return "";
        }

        StringBuilder section = new StringBuilder("카테고리별 점수(0~100, 사용자 우선순위 가중치 높은 순):\n");

        breakdowns.stream()
                .sorted(Comparator.comparing(CategoryScoreBreakdown::getWeight).reversed())
                .forEach(breakdown -> section.append("- ")
                        .append(CATEGORY_LABELS.getOrDefault(breakdown.getCategory(), breakdown.getCategory()))
                        .append(": ").append(breakdown.getRawScore()).append('\n'));

        return section.toString();
    }

    private GeneratedReason parse(String content) {
        Objects.requireNonNull(content, "OpenAI 응답 content가 null입니다.");

        String reason = extractLine(content, REASON_PREFIX);
        String caution = extractLine(content, CAUTION_PREFIX);

        if (reason == null || caution == null) {
            throw new IllegalStateException("OpenAI 응답이 예상한 형식이 아닙니다: " + content);
        }

        return GeneratedReason.builder()
                .recommendationReason(reason)
                .caution(caution)
                .build();
    }

    private String extractLine(String content, String prefix) {
        return content.lines()
                .map(String::trim)
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim())
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElse(null);
    }

    private GeneratedReason placeholder() {
        return GeneratedReason.builder()
                .recommendationReason(PLACEHOLDER)
                .caution(PLACEHOLDER)
                .build();
    }
}
