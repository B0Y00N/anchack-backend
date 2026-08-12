//package com.kbait.anchack.review.service;
//
//import com.kbait.anchack.review.domain.Review;
//import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
//import com.kbait.anchack.review.exception.ReviewAccessDeniedException;
//import com.kbait.anchack.review.exception.ReviewNotFoundException;
//import com.kbait.anchack.review.mapper.ReviewMapper;
//import com.kbait.anchack.review.mapper.ReviewReactionMapper;
//import com.kbait.anchack.review.service.impl.ReviewServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.catchThrowable;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class ReviewServiceImplTest {
//
//    private static final Long REVIEW_ID = 1L;
//    private static final Long WRITER_ID = 100L;
//    private static final Long OTHER_USER_ID = 200L;
//
//    @Mock
//    private ReviewMapper reviewMapper;
//
//    @Mock
//    private ReviewReactionMapper reviewReactionMapper;
//
//    private ReviewService reviewService;
//
//    @BeforeEach
//    void setUp() {
//        reviewService = new ReviewServiceImpl(reviewMapper, reviewReactionMapper);
//    }
//
//    @Test
//    void 본인이_작성하지_않은_리뷰를_수정하면_예외가_발생한다() {
//        Review review = createActiveReview(WRITER_ID);
//        when(reviewMapper.findById(REVIEW_ID)).thenReturn(review);
//
//        Throwable thrown = catchThrowable(
//            () -> reviewService.updateReview(OTHER_USER_ID, REVIEW_ID, createUpdateRequest())
//        );
//
//        assertThat(thrown).isInstanceOf(ReviewAccessDeniedException.class);
//    }
//
//    @Test
//    void 본인이_작성하지_않은_리뷰를_삭제하면_예외가_발생한다() {
//        Review review = createActiveReview(WRITER_ID);
//        when(reviewMapper.findById(REVIEW_ID)).thenReturn(review);
//
//        Throwable thrown = catchThrowable(
//            () -> reviewService.deleteReview(OTHER_USER_ID, REVIEW_ID)
//        );
//
//        assertThat(thrown).isInstanceOf(ReviewAccessDeniedException.class);
//    }
//
//    @Test
//    void 존재하지_않는_리뷰를_조회하면_예외가_발생한다() {
//        when(reviewMapper.findById(REVIEW_ID)).thenReturn(null);
//
//        Throwable thrown = catchThrowable(() -> reviewService.getReview(REVIEW_ID, null));
//
//        assertThat(thrown).isInstanceOf(ReviewNotFoundException.class);
//    }
//
//    private Review createActiveReview(Long writerId) {
//        Review review = new Review();
//        review.setReviewId(REVIEW_ID);
//        review.setUserId(writerId);
//        review.setStatus(Review.STATUS_ACTIVE);
//        return review;
//    }
//
//    private ReviewUpdateRequest createUpdateRequest() {
//        ReviewUpdateRequest request = new ReviewUpdateRequest();
//        request.setOverallRating(4);
//        request.setContent("이 동네는 조용하고 살기 좋은 편이라 만족스럽습니다.");
//
//        Map<String, Integer> categoryScores = new HashMap<>();
//        categoryScores.put("NOISE", 4);
//        request.setCategoryScores(categoryScores);
//
//        return request;
//    }
//}
