package com.kbait.anchack.review.service.impl;

import com.kbait.anchack.common.exception.ForbiddenException;
import com.kbait.anchack.common.exception.UnauthorizedException;
import com.kbait.anchack.review.domain.Review;
//import com.kbait.anchack.review.domain.ReviewReaction;
import com.kbait.anchack.review.domain.ReviewScore;
import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
//import com.kbait.anchack.review.dto.response.ReviewReactionResponse;
import com.kbait.anchack.review.dto.response.ReviewResponse;
import com.kbait.anchack.review.exception.ReviewAccessDeniedException;
import com.kbait.anchack.review.exception.ReviewNotFoundException;
import com.kbait.anchack.review.mapper.ReviewMapper;
//import com.kbait.anchack.review.mapper.ReviewReactionMapper;
import com.kbait.anchack.review.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//import java.util.Collections;
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
//    private final ReviewReactionMapper reviewReactionMapper;

    public ReviewServiceImpl(
        ReviewMapper reviewMapper
//        ,ReviewReactionMapper reviewReactionMapper
    ) {
        this.reviewMapper = reviewMapper;
//        this.reviewReactionMapper = reviewReactionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByAdminDong(Long adminDongId, Long viewerId) {
        if (adminDongId == null) {
            throw new IllegalArgumentException("�됱젙�� ID媛� �꾩슂�⑸땲��.");
        }

        if (reviewMapper.existsAdminDong(adminDongId) == 0) {
            throw new IllegalArgumentException("議댁옱�섏� �딅뒗 �됱젙�숈엯�덈떎.");
        }

        List<Review> reviews = reviewMapper.findActiveByAdminDongId(adminDongId);

        attachCategoryScores(reviews);
//        attachReactions(reviews, viewerId);

        return reviews.stream()
            .map(ReviewResponse::from)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId, Long viewerId) {
        Review review = findReview(reviewId);

        if (!review.isActive()) {
            throw new IllegalStateException("�꾩옱 議고쉶�� �� �녿뒗 由щ럭�낅땲��.");
        }

        review.setCategoryScores(getCategoryScores(reviewId));
//        attachReactions(Collections.singletonList(review), viewerId);

        return ReviewResponse.from(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("�ъ슜�� ID媛� �꾩슂�⑸땲��.");
        }

        List<Review> reviews = reviewMapper.findByUserId(userId);

        attachCategoryScores(reviews);
//        attachReactions(reviews, userId);

        return reviews.stream()
            .map(ReviewResponse::fromForOwner)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("�ъ슜�� ID媛� �꾩슂�⑸땲��.");
        }

        validateReviewFields(request);

        if (reviewMapper.existsAdminDong(request.getAdminDongId()) == 0) {
            throw new IllegalArgumentException("議댁옱�섏� �딅뒗 �됱젙�숈엯�덈떎.");
        }

        Review review = new Review();
        review.setAdminDongId(request.getAdminDongId());
        review.setUserId(userId);
        review.setOverallRating(request.getOverallRating());
        review.setContent(request.getContent().trim());
        review.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));

        int insertedCount = reviewMapper.insertReview(review);

        if (insertedCount != 1 || review.getReviewId() == null) {
            throw new IllegalStateException("由щ럭 ���μ뿉 �ㅽ뙣�덉뒿�덈떎.");
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
            throw new IllegalStateException("�쒖꽦 �곹깭�� 由щ럭留� �섏젙�� �� �덉뒿�덈떎.");
        }

        review.setOverallRating(request.getOverallRating());
        review.setContent(request.getContent().trim());
        review.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));

        int updatedCount = reviewMapper.updateReview(review);

        if (updatedCount != 1) {
            throw new IllegalStateException("由щ럭 �섏젙�� �ㅽ뙣�덉뒿�덈떎.");
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
            throw new IllegalStateException("�대� ��젣�섏뿀嫄곕굹 �④� 泥섎━�� 由щ럭�낅땲��.");
        }

        int updatedCount = reviewMapper.updateReviewStatus(reviewId, Review.STATUS_DELETED);

        if (updatedCount != 1) {
            throw new IllegalStateException("由щ럭 ��젣�� �ㅽ뙣�덉뒿�덈떎.");
        }
    }

    /**
     * 媛숈� 諛섏쓳�� �ㅼ떆 �꾨Ⅴ硫� 痍⑥냼(��젣)�섍퀬, 諛섎� 諛섏쓳�� �꾨Ⅴ硫� 諛붾�먮떎.
     * �먭린 �먯떊�� �� 由щ럭�먮룄 諛섏쓳�� �④만 �� �덇쾶 �덉슜�쒕떎(援녹씠 留됱쓣 �댁쑀媛� �놁쓬).
     */
//    @Override
//    @Transactional
//    public ReviewReactionResponse reactToReview(Long userId, Long reviewId, String reactionType) {
//        if (userId == null) {
//            throw new UnauthorizedException("濡쒓렇�몄씠 �꾩슂�⑸땲��.");
//        }
//
//        Review review = findReview(reviewId);
//
//        if (!review.isActive()) {
//            throw new IllegalStateException("�꾩옱 諛섏쓳�� �④만 �� �녿뒗 由щ럭�낅땲��.");
//        }
//
//        String normalizedType = normalizeReactionType(reactionType);
//        String myReaction = applyReaction(reviewId, userId, normalizedType);
//
//        Map<String, Object> counts = reviewReactionMapper.countByReviewId(reviewId);
//
//        return new ReviewReactionResponse(
//            reviewId,
//            toLong(counts == null ? null : counts.get("likeCount")),
//            toLong(counts == null ? null : counts.get("dislikeCount")),
//            myReaction
//        );
//    }

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
            throw new IllegalArgumentException("蹂�寃쏀븷 �곹깭 �뺣낫媛� �꾩슂�⑸땲��.");
        }

        findReview(reviewId);

        String status = normalizeRequiredStatus(request.getStatus());

        int updatedCount = reviewMapper.updateReviewStatus(reviewId, status);

        if (updatedCount != 1) {
            throw new IllegalStateException("由щ럭 �곹깭 蹂�寃쎌뿉 �ㅽ뙣�덉뒿�덈떎.");
        }

        reviewMapper.insertAdminAction(reviewId, status, request.getReason());

        Review updatedReview = findReview(reviewId);
        updatedReview.setCategoryScores(getCategoryScores(reviewId));

        return ReviewResponse.fromForOwner(updatedReview);
    }

    private Review findReview(Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("由щ럭 ID媛� �꾩슂�⑸땲��.");
        }

        Review review = reviewMapper.findById(reviewId);

        if (review == null) {
            throw new ReviewNotFoundException(reviewId);
        }

        return review;
    }

    /**
     * 湲곗〈 諛섏쓳�� �놁쑝硫� �덈줈 �깅줉�섍퀬, 媛숈� 諛섏쓳�대㈃ 痍⑥냼, �ㅻⅤ硫� 諛섏쓳 醫낅쪟瑜� 諛붽씔��.
     * 諛섑솚媛믪� 泥섎━ �� �꾩옱 �ъ슜�먯쓽 諛섏쓳("LIKE"/"DISLIKE"/null)�대떎.
     */
//    private String applyReaction(Long reviewId, Long userId, String normalizedType) {
//        ReviewReaction existing = reviewReactionMapper.findByReviewAndUser(reviewId, userId);
//
//        if (existing == null) {
//            ReviewReaction reaction = new ReviewReaction();
//            reaction.setReviewId(reviewId);
//            reaction.setUserId(userId);
//            reaction.setReactionType(normalizedType);
//
//            int insertedCount = reviewReactionMapper.insertReaction(reaction);
//
//            if (insertedCount != 1) {
//                throw new IllegalStateException("由щ럭 諛섏쓳 ���μ뿉 �ㅽ뙣�덉뒿�덈떎.");
//            }
//
//            return normalizedType;
//        }
//
//        if (existing.isSameType(normalizedType)) {
//            reviewReactionMapper.deleteReaction(existing.getReactionId());
//            return null;
//        }
//
//        reviewReactionMapper.updateReactionType(existing.getReactionId(), normalizedType);
//        return normalizedType;
//    }

    private void insertCategoryScores(Long reviewId, Map<String, Integer> categoryScores) {
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            int insertedCount =
                reviewMapper.insertReviewScore(reviewId, entry.getKey(), entry.getValue());

            if (insertedCount != 1) {
                throw new IllegalArgumentException("議댁옱�섏� �딅뒗 由щ럭 �됯� ��ぉ�낅땲��: " + entry.getKey());
            }
        }
    }

    /**
     * 由щ럭 紐⑸줉�� ��ぉ蹂� 蹂꾩젏�� 梨꾩슫��. 由щ럭 媛쒖닔留뚰겮 議고쉶�섏� �딅룄濡�
     * findScoresByReviewIds濡� �� 踰덉뿉 媛��몄��� 由щ럭蹂꾨줈 �섎닠 �대뒗��.
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
     * 由щ럭 紐⑸줉�� 醫뗭븘��/�レ뼱�� 媛쒖닔��, viewerId媛� �④릿 諛섏쓳�� 梨꾩썙以���.
     * viewerId媛� null�대㈃(鍮꾨줈洹몄씤) 媛쒖닔留� 梨꾩슦怨� myReaction�� 鍮꾩썙�붾떎.
     */
//    private void attachReactions(List<Review> reviews, Long viewerId) {
//        if (reviews.isEmpty()) {
//            return;
//        }
//
//        List<Long> reviewIds = reviews.stream()
//            .map(Review::getReviewId)
//            .collect(Collectors.toList());
//
//        Map<Long, Map<String, Object>> countsByReviewId = countReactionsByReviewId(reviewIds);
//        Map<Long, String> myReactionByReviewId = findMyReactionsByReviewId(reviewIds, viewerId);
//
//        for (Review review : reviews) {
//            Map<String, Object> row = countsByReviewId.get(review.getReviewId());
//
//            review.setLikeCount(toLong(row == null ? null : row.get("likeCount")));
//            review.setDislikeCount(toLong(row == null ? null : row.get("dislikeCount")));
//            review.setMyReaction(myReactionByReviewId.get(review.getReviewId()));
//        }
//    }

//    private Map<Long, Map<String, Object>> countReactionsByReviewId(List<Long> reviewIds) {
//        List<Map<String, Object>> counts = reviewReactionMapper.countByReviewIds(reviewIds);
//        Map<Long, Map<String, Object>> countsByReviewId = new LinkedHashMap<>();
//
//        for (Map<String, Object> row : counts) {
//            countsByReviewId.put(toLong(row.get("reviewId")), row);
//        }
//
//        return countsByReviewId;
//    }

//    private Map<Long, String> findMyReactionsByReviewId(List<Long> reviewIds, Long viewerId) {
//        if (viewerId == null) {
//            return Collections.emptyMap();
//        }
//
//        Map<Long, String> myReactionByReviewId = new LinkedHashMap<>();
//
//        for (ReviewReaction reaction : reviewReactionMapper.findByReviewIdsAndUser(reviewIds, viewerId)) {
//            myReactionByReviewId.put(reaction.getReviewId(), reaction.getReactionType());
//        }
//
//        return myReactionByReviewId;
//    }
//
//    private String normalizeReactionType(String reactionType) {
//        if (reactionType == null || reactionType.trim().isEmpty()) {
//            throw new IllegalArgumentException("諛섏쓳 醫낅쪟(reactionType)媛� �꾩슂�⑸땲��.");
//        }
//
//        String normalized = reactionType.trim().toUpperCase();
//
//        if (!ReviewReaction.TYPE_LIKE.equals(normalized) && !ReviewReaction.TYPE_DISLIKE.equals(normalized)) {
//            throw new IllegalArgumentException("諛섏쓳 醫낅쪟�� LIKE �먮뒗 DISLIKE�ъ빞 �⑸땲��.");
//        }
//
//        return normalized;
//    }
//
//    private long toLong(Object value) {
//        if (value == null) {
//            return 0L;
//        }
//
//        if (value instanceof Number) {
//            return ((Number) value).longValue();
//        }
//
//        try {
//            return Long.parseLong(value.toString());
//        } catch (NumberFormatException exception) {
//            return 0L;
//        }
//    }

    private Map<String, Integer> getCategoryScores(Long reviewId) {
        Map<String, Integer> categoryScores = new LinkedHashMap<>();

        for (ReviewScore score : reviewMapper.findScoresByReviewId(reviewId)) {
            categoryScores.put(score.getCategoryCode(), score.getScore());
        }

        return categoryScores;
    }

    private void validateReviewFields(ReviewCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("由щ럭 �뺣낫媛� �꾩슂�⑸땲��.");
        }

        if (request.getAdminDongId() == null) {
            throw new IllegalArgumentException("�됱젙�� �뺣낫媛� �꾩슂�⑸땲��.");
        }

        validateReviewFields(request.getOverallRating(), request.getContent(), request.getCategoryScores());
    }

    private void validateReviewFields(ReviewUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("�섏젙�� 由щ럭 �뺣낫媛� �꾩슂�⑸땲��.");
        }

        validateReviewFields(request.getOverallRating(), request.getContent(), request.getCategoryScores());
    }

    private void validateReviewFields(
        Integer overallRating,
        String content,
        Map<String, Integer> categoryScores
    ) {
        if (overallRating == null || overallRating < MIN_RATING || overallRating > MAX_RATING) {
            throw new IllegalArgumentException("醫낇빀 蹂꾩젏�� 1�먮��� 5�먭퉴吏� �낅젰�댁빞 �⑸땲��.");
        }

        if (content == null || content.trim().length() < MIN_CONTENT_LENGTH) {
            throw new IllegalArgumentException("由щ럭 �댁슜�� 理쒖냼 20�� �댁긽 �낅젰�댁빞 �⑸땲��.");
        }

        if (categoryScores == null || categoryScores.isEmpty()) {
            throw new IllegalArgumentException("��ぉ蹂� 蹂꾩젏�� �낅젰�댁＜�몄슂.");
        }

        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            validateCategoryScore(entry.getKey(), entry.getValue());
        }
    }

    private void validateCategoryScore(String categoryCode, Integer score) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("�됯� ��ぉ 肄붾뱶媛� �꾩슂�⑸땲��.");
        }

        if (score == null || score < MIN_RATING || score > MAX_RATING) {
            throw new IllegalArgumentException("��ぉ蹂� 蹂꾩젏�� 1�먮��� 5�먭퉴吏� �낅젰�댁빞 �⑸땲��.");
        }
    }

    private void validateOwner(Long userId, Review review) {
        if (!review.isWrittenBy(userId)) {
            throw new ReviewAccessDeniedException(review.getReviewId());
        }
    }

    private void validateAdmin(Long adminId) {
        if (adminId == null) {
            throw new UnauthorizedException("濡쒓렇�몄씠 �꾩슂�⑸땲��.");
        }

        String role = reviewMapper.findUserRole(adminId);

        if (!ADMIN_ROLE.equals(role)) {
            throw new ForbiddenException("愿�由ъ옄 沅뚰븳�� �꾩슂�⑸땲��.");
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
            throw new IllegalArgumentException("由щ럭 �곹깭媛� �꾩슂�⑸땲��.");
        }

        String normalized = status.trim().toUpperCase();

        boolean isValidStatus = Review.STATUS_ACTIVE.equals(normalized)
            || Review.STATUS_HIDDEN.equals(normalized)
            || Review.STATUS_DELETED.equals(normalized);

        if (!isValidStatus) {
            throw new IllegalArgumentException("由щ럭 �곹깭�� ACTIVE, HIDDEN, DELETED 以� �섎굹�ъ빞 �⑸땲��.");
        }

        return normalized;
    }
}
