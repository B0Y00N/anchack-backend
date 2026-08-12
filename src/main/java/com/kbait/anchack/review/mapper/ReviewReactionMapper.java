package com.kbait.anchack.review.mapper;

import com.kbait.anchack.review.domain.ReviewReaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewReactionMapper {

    /**
     * 특정 사용자가 리뷰에 남긴 반응을 조회한다.
     */
    ReviewReaction findByReviewAndUser(
        @Param("reviewId") Long reviewId,
        @Param("userId") Long userId
    );

    /**
     * 리뷰 반응을 등록한다.
     * 등록 후 생성된 reactionId가 reaction.reactionId에 저장된다.
     */
    int insertReaction(ReviewReaction reviewReaction);

    /**
     * 등록된 반응의 종류를 변경한다.
     */
    int updateReactionType(
        @Param("reactionId") Long reactionId,
        @Param("reactionType") String reactionType
    );

    /**
     * 리뷰 반응을 삭제한다.
     */
    int deleteReaction(
        @Param("reactionId") Long reactionId
    );

    /**
     * 리뷰 하나의 좋아요·싫어요 개수를 조회한다.
     *
     * 반환 값:
     * reviewId, likeCount, dislikeCount
     */
    Map<String, Object> countByReviewId(
        @Param("reviewId") Long reviewId
    );

    /**
     * 여러 리뷰의 좋아요·싫어요 개수를 조회한다.
     */
    List<Map<String, Object>> countByReviewIds(
        @Param("reviewIds") List<Long> reviewIds
    );

    /**
     * 여러 리뷰에 대해 특정 사용자가 남긴 반응을 조회한다.
     */
    List<ReviewReaction> findByReviewIdsAndUser(
        @Param("reviewIds") List<Long> reviewIds,
        @Param("userId") Long userId
    );
}
