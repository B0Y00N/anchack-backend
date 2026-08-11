package com.kbait.anchack.review.service.impl;

import com.kbait.anchack.common.exception.ForbiddenException;
import com.kbait.anchack.common.exception.UnauthorizedException;
import com.kbait.anchack.review.domain.Review;
import com.kbait.anchack.review.domain.ReviewReaction;
import com.kbait.anchack.review.domain.ReviewScore;
import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewReactionResponse;
import com.kbait.anchack.review.dto.response.ReviewResponse;
import com.kbait.anchack.review.exception.ReviewAccessDeniedException;
import com.kbait.anchack.review.exception.ReviewNotFoundException;
import com.kbait.anchack.review.mapper.ReviewMapper;
import com.kbait.anchack.review.mapper.ReviewReactionMapper;
import com.kbait.anchack.review.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MIN_CONTENT_LENGTH = 20;
    private static final String ADMIN_ROLE = "ADMIN";

    private final ReviewMapper reviewMapper;
    private final ReviewReactionMapper reviewReactionMapper;

    public ReviewServiceImpl(
        ReviewMapper reviewMapper,
        ReviewReactionMapper reviewReactionMapper
    ) {
        this.reviewMapper = reviewMapper;
        this.reviewReactionMapper = reviewReactionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByAdminDong(Long adminDongId, Long viewerId) {
        if (adminDongId == null) {
            throw new IllegalArgumentException("행정동 ID가 필요합니다.");
        }

        if (reviewMapper.existsAdminDong(adminDongId) == 0) {
            throw new IllegalArgumentException("존재하지 않는 행정동입니다.");
        }

        List<Review> reviews = reviewMapper.findActiveByAdminDongId(adminDongId);

        attachCategoryScores(reviews);
        attachReactions(reviews, viewerId);

        return reviews.stream()
            .map(ReviewResponse::from)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId, Long viewerId) {
        Review review = findReview(reviewId);

        if (!review.isActive()) {
            throw new IllegalStateException("현재 조회할 수 없는 리뷰입니다.");
        }

        review.setCategoryScores(getCategoryScores(reviewId));
        attachReactions(Collections.singletonList(review), viewerId);

        return ReviewResponse.from(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다.");
        }

        List<Review> reviews = reviewMapper.findByUserId(userId);

        attachCategoryScores(reviews);
        attachReactions(reviews, userId);

        return reviews.stream()
            .map(ReviewResponse::fromForOwner)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다.");
        }

        validateReviewFields(request);

        if (reviewMapper.existsAdminDong(request.getAdminDongId()) == 0) {
            throw new IllegalArgumentException("존재하지 않는 행정동입니다.");
        }

        Review review = new Review();
        review.setAdminDongId(request.getAdminDongId());
        review.setUserId(userId);
        review.setOverallRating(request.getOverallRating());
        review.setContent(request.getContent().trim());
        review.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));

        int insertedCount = reviewMapper.insertReview(review);

        if (insertedCount != 1 || review.getReviewId() == null) {
            throw new IllegalStateException("리뷰 저장에 실패했습니다.");
        }

        insertCategoryScores(review.getReviewId(), request.getCategoryScores());

        Review savedReview = findReview(review.getReviewId());
        savedReview.setCategoryScores(getCategoryScores(savedReview.getReviewId()));

        return ReviewResponse.fromForOwner(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = findReview(reviewId);

        validateOwner(userId, review);
        validateReviewFields(request);

        if (!review.isActive()) {
            throw new IllegalStateException("활성 상태의 리뷰만 수정할 수 있습니다.");
        }

        review.setOverallRating(request.getOverallRating());
        review.setContent(request.getContent().trim());
        review.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));

        int updatedCount = reviewMapper.updateReview(review);

        if (updatedCount != 1) {
            throw new IllegalStateException("리뷰 수정에 실패했습니다.");
        }

        reviewMapper.deleteReviewScores(reviewId);
        insertCategoryScores(reviewId, request.getCategoryScores());

        Review updatedReview = findReview(reviewId);
        updatedReview.setCategoryScores(getCategoryScores(reviewId));

        return ReviewResponse.fromForOwner(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = findReview(reviewId);

        validateOwner(userId, review);

        if (!review.isActive()) {
            throw new IllegalStateException("이미 삭제되었거나 숨김 처리된 리뷰입니다.");
        }

        int updatedCount = reviewMapper.updateReviewStatus(reviewId, Review.STATUS_DELETED);

        if (updatedCount != 1) {
            throw new IllegalStateException("리뷰 삭제에 실패했습니다.");
        }
    }

    /**
     * 같은 반응을 다시 누르면 취소(삭제)되고, 반대 반응을 누르면 바뀐다.
     * 자기 자신이 쓴 리뷰에도 반응은 남길 수 있게 허용한다(굳이 막을 이유가 없음).
     */
    @Override
    @Transactional
    public ReviewReactionResponse reactToReview(Long userId, Long reviewId, String reactionType) {
        if (userId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        Review review = findReview(reviewId);

        if (!review.isActive()) {
            throw new IllegalStateException("현재 반응을 남길 수 없는 리뷰입니다.");
        }

        String normalizedType = normalizeReactionType(reactionType);
        String myReaction = applyReaction(reviewId, userId, normalizedType);

        Map<String, Object> counts = reviewReactionMapper.countByReviewId(reviewId);

        return new ReviewReactionResponse(
            reviewId,
            toLong(counts == null ? null : counts.get("likeCount")),
            toLong(counts == null ? null : counts.get("dislikeCount")),
            myReaction
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForAdmin(Long adminId, String status) {
        validateAdmin(adminId);

        String normalizedStatus = normalizeStatus(status);
        List<Review> reviews = reviewMapper.findAllForAdmin(normalizedStatus);

        attachCategoryScores(reviews);

        return reviews.stream()
            .map(ReviewResponse::fromForOwner)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewResponse updateReviewStatusByAdmin(
        Long adminId,
        Long reviewId,
        AdminReviewStatusRequest request
    ) {
        validateAdmin(adminId);

        if (request == null) {
            throw new IllegalArgumentException("변경할 상태 정보가 필요합니다.");
        }

        findReview(reviewId);

        String status = normalizeRequiredStatus(request.getStatus());

        int updatedCount = reviewMapper.updateReviewStatus(reviewId, status);

        if (updatedCount != 1) {
            throw new IllegalStateException("리뷰 상태 변경에 실패했습니다.");
        }

        reviewMapper.insertAdminAction(reviewId, status, request.getReason());

        Review updatedReview = findReview(reviewId);
        updatedReview.setCategoryScores(getCategoryScores(reviewId));

        return ReviewResponse.fromForOwner(updatedReview);
    }

    private Review findReview(Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("리뷰 ID가 필요합니다.");
        }

        Review review = reviewMapper.findById(reviewId);

        if (review == null) {
            throw new ReviewNotFoundException(reviewId);
        }

        return review;
    }

    /**
     * 기존 반응이 없으면 새로 등록하고, 같은 반응이면 취소, 다르면 반응 종류를 바꾼다.
     * 반환값은 처리 후 현재 사용자의 반응("LIKE"/"DISLIKE"/null)이다.
     */
    private String applyReaction(Long reviewId, Long userId, String normalizedType) {
        ReviewReaction existing = reviewReactionMapper.findByReviewAndUser(reviewId, userId);

        if (existing == null) {
            ReviewReaction reaction = new ReviewReaction();
            reaction.setReviewId(reviewId);
            reaction.setUserId(userId);
            reaction.setReactionType(normalizedType);

            int insertedCount = reviewReactionMapper.insertReaction(reaction);

            if (insertedCount != 1) {
                throw new IllegalStateException("리뷰 반응 저장에 실패했습니다.");
            }

            return normalizedType;
        }

        if (existing.isSameType(normalizedType)) {
            reviewReactionMapper.deleteReaction(existing.getReactionId());
            return null;
        }

        reviewReactionMapper.updateReactionType(existing.getReactionId(), normalizedType);
        return normalizedType;
    }

    private void insertCategoryScores(Long reviewId, Map<String, Integer> categoryScores) {
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            int insertedCount =
                reviewMapper.insertReviewScore(reviewId, entry.getKey(), entry.getValue());

            if (insertedCount != 1) {
                throw new IllegalArgumentException("존재하지 않는 리뷰 평가 항목입니다: " + entry.getKey());
            }
        }
    }

    /**
     * 리뷰 목록의 항목별 별점을 채운다. 리뷰 개수만큼 조회하지 않도록
     * findScoresByReviewIds로 한 번에 가져와서 리뷰별로 나눠 담는다.
     */
    private void attachCategoryScores(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return;
        }

        List<Long> reviewIds = reviews.stream()
            .map(Review::getReviewId)
            .collect(Collectors.toList());

        List<ReviewScore> scores = reviewMapper.findScoresByReviewIds(reviewIds);

        Map<Long, Map<String, Integer>> scoresByReviewId = new LinkedHashMap<>();

        for (ReviewScore score : scores) {
            scoresByReviewId
                .computeIfAbsent(score.getReviewId(), key -> new LinkedHashMap<>())
                .put(score.getCategoryCode(), score.getScore());
        }

        for (Review review : reviews) {
            review.setCategoryScores(
                scoresByReviewId.getOrDefault(review.getReviewId(), new LinkedHashMap<>())
            );
        }
    }

    /**
     * 리뷰 목록에 좋아요/싫어요 개수와, viewerId가 남긴 반응을 채워준다.
     * viewerId가 null이면(비로그인) 개수만 채우고 myReaction은 비워둔다.
     */
    private void attachReactions(List<Review> reviews, Long viewerId) {
        if (reviews.isEmpty()) {
            return;
        }

        List<Long> reviewIds = reviews.stream()
            .map(Review::getReviewId)
            .collect(Collectors.toList());

        Map<Long, Map<String, Object>> countsByReviewId = countReactionsByReviewId(reviewIds);
        Map<Long, String> myReactionByReviewId = findMyReactionsByReviewId(reviewIds, viewerId);

        for (Review review : reviews) {
            Map<String, Object> row = countsByReviewId.get(review.getReviewId());

            review.setLikeCount(toLong(row == null ? null : row.get("likeCount")));
            review.setDislikeCount(toLong(row == null ? null : row.get("dislikeCount")));
            review.setMyReaction(myReactionByReviewId.get(review.getReviewId()));
        }
    }

    private Map<Long, Map<String, Object>> countReactionsByReviewId(List<Long> reviewIds) {
        List<Map<String, Object>> counts = reviewReactionMapper.countByReviewIds(reviewIds);
        Map<Long, Map<String, Object>> countsByReviewId = new LinkedHashMap<>();

        for (Map<String, Object> row : counts) {
            countsByReviewId.put(toLong(row.get("reviewId")), row);
        }

        return countsByReviewId;
    }

    private Map<Long, String> findMyReactionsByReviewId(List<Long> reviewIds, Long viewerId) {
        if (viewerId == null) {
            return Collections.emptyMap();
        }

        Map<Long, String> myReactionByReviewId = new LinkedHashMap<>();

        for (ReviewReaction reaction : reviewReactionMapper.findByReviewIdsAndUser(reviewIds, viewerId)) {
            myReactionByReviewId.put(reaction.getReviewId(), reaction.getReactionType());
        }

        return myReactionByReviewId;
    }

    private String normalizeReactionType(String reactionType) {
        if (reactionType == null || reactionType.trim().isEmpty()) {
            throw new IllegalArgumentException("반응 종류(reactionType)가 필요합니다.");
        }

        String normalized = reactionType.trim().toUpperCase();

        if (!ReviewReaction.TYPE_LIKE.equals(normalized) && !ReviewReaction.TYPE_DISLIKE.equals(normalized)) {
            throw new IllegalArgumentException("반응 종류는 LIKE 또는 DISLIKE여야 합니다.");
        }

        return normalized;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private Map<String, Integer> getCategoryScores(Long reviewId) {
        Map<String, Integer> categoryScores = new LinkedHashMap<>();

        for (ReviewScore score : reviewMapper.findScoresByReviewId(reviewId)) {
            categoryScores.put(score.getCategoryCode(), score.getScore());
        }

        return categoryScores;
    }

    private void validateReviewFields(ReviewCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("리뷰 정보가 필요합니다.");
        }

        if (request.getAdminDongId() == null) {
            throw new IllegalArgumentException("행정동 정보가 필요합니다.");
        }

        validateReviewFields(request.getOverallRating(), request.getContent(), request.getCategoryScores());
    }

    private void validateReviewFields(ReviewUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("수정할 리뷰 정보가 필요합니다.");
        }

        validateReviewFields(request.getOverallRating(), request.getContent(), request.getCategoryScores());
    }

    private void validateReviewFields(
        Integer overallRating,
        String content,
        Map<String, Integer> categoryScores
    ) {
        if (overallRating == null || overallRating < MIN_RATING || overallRating > MAX_RATING) {
            throw new IllegalArgumentException("종합 별점은 1점부터 5점까지 입력해야 합니다.");
        }

        if (content == null || content.trim().length() < MIN_CONTENT_LENGTH) {
            throw new IllegalArgumentException("리뷰 내용은 최소 20자 이상 입력해야 합니다.");
        }

        if (categoryScores == null || categoryScores.isEmpty()) {
            throw new IllegalArgumentException("항목별 별점을 입력해주세요.");
        }

        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            validateCategoryScore(entry.getKey(), entry.getValue());
        }
    }

    private void validateCategoryScore(String categoryCode, Integer score) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("평가 항목 코드가 필요합니다.");
        }

        if (score == null || score < MIN_RATING || score > MAX_RATING) {
            throw new IllegalArgumentException("항목별 별점은 1점부터 5점까지 입력해야 합니다.");
        }
    }

    private void validateOwner(Long userId, Review review) {
        if (!review.isWrittenBy(userId)) {
            throw new ReviewAccessDeniedException(review.getReviewId());
        }
    }

    private void validateAdmin(Long adminId) {
        if (adminId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        String role = reviewMapper.findUserRole(adminId);

        if (!ADMIN_ROLE.equals(role)) {
            throw new ForbiddenException("관리자 권한이 필요합니다.");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        return normalizeRequiredStatus(status);
    }

    private String normalizeRequiredStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("리뷰 상태가 필요합니다.");
        }

        String normalized = status.trim().toUpperCase();

        boolean isValidStatus = Review.STATUS_ACTIVE.equals(normalized)
            || Review.STATUS_HIDDEN.equals(normalized)
            || Review.STATUS_DELETED.equals(normalized);

        if (!isValidStatus) {
            throw new IllegalArgumentException("리뷰 상태는 ACTIVE, HIDDEN, DELETED 중 하나여야 합니다.");
        }

        return normalized;
    }
}
