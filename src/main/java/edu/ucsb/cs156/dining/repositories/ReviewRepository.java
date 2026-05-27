package edu.ucsb.cs156.dining.repositories;

import edu.ucsb.cs156.dining.entities.MenuItem;
import edu.ucsb.cs156.dining.entities.Review;
import edu.ucsb.cs156.dining.entities.User;
import edu.ucsb.cs156.dining.repositories.projections.CommonsRatingProjection;
import edu.ucsb.cs156.dining.repositories.projections.CommonsReviewRow;
import edu.ucsb.cs156.dining.repositories.projections.ItemRatingProjection;
import edu.ucsb.cs156.dining.statuses.ModerationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** The ReviewRepository is a repository for Review entities */
@Repository
public interface ReviewRepository extends CrudRepository<Review, Long> {

  /**
   * @param user
   * @return all reviews that have come from a single reviewer, ex say this user has made a few
   *     reviews over the past year well then this method will return only the reviews that this
   *     user has sent
   */
  Iterable<Review> findByReviewer(User user);

  Iterable<Review> findByStatus(ModerationStatus moderationStatus);

  Iterable<Review> findByItemAndStatus(MenuItem item, ModerationStatus approved);

  @Query("SELECT COUNT(r) FROM reviews r WHERE r.status = :status")
  long countByStatus(@Param("status") ModerationStatus status);

  @Query("SELECT COUNT(DISTINCT r.item.id) FROM reviews r WHERE r.status = :status")
  long countDistinctItemsByStatus(@Param("status") ModerationStatus status);

  @Query("SELECT COUNT(DISTINCT r.item.diningCommonsCode) FROM reviews r WHERE r.status = :status")
  long countDistinctCommonsByStatus(@Param("status") ModerationStatus status);

  @Query("SELECT MAX(r.dateEdited) FROM reviews r WHERE r.status = :status")
  LocalDateTime findMaxDateEditedByStatus(@Param("status") ModerationStatus status);

  @Query(
      """
      SELECT r.item.id AS itemId,
             r.item.name AS name,
             r.item.diningCommonsCode AS diningCommonsCode,
             r.item.mealCode AS mealCode,
             AVG(r.itemsStars) AS avgStars,
             COUNT(r) AS reviewCount
      FROM reviews r
      WHERE r.status = :status
        AND r.dateItemServed >= :since
        AND r.itemsStars IS NOT NULL
      GROUP BY r.item.id, r.item.name, r.item.diningCommonsCode, r.item.mealCode
      HAVING COUNT(r) >= :minReviews
      ORDER BY AVG(r.itemsStars) DESC, COUNT(r) DESC
      """)
  List<ItemRatingProjection> findTopRatedItems(
      @Param("status") ModerationStatus status,
      @Param("since") LocalDateTime since,
      @Param("minReviews") long minReviews,
      Pageable pageable);

  @Query(
      """
      SELECT r.item.id AS itemId,
             r.item.name AS name,
             r.item.diningCommonsCode AS diningCommonsCode,
             r.item.mealCode AS mealCode,
             AVG(r.itemsStars) AS avgStars,
             COUNT(r) AS reviewCount
      FROM reviews r
      WHERE r.status = :status
        AND r.dateItemServed >= :since
        AND r.itemsStars IS NOT NULL
      GROUP BY r.item.id, r.item.name, r.item.diningCommonsCode, r.item.mealCode
      HAVING COUNT(r) >= :minReviews
      ORDER BY AVG(r.itemsStars) ASC, COUNT(r) DESC
      """)
  List<ItemRatingProjection> findBottomRatedItems(
      @Param("status") ModerationStatus status,
      @Param("since") LocalDateTime since,
      @Param("minReviews") long minReviews,
      Pageable pageable);

  @Query(
      """
      SELECT r.item.diningCommonsCode AS diningCommonsCode,
             AVG(r.itemsStars) AS avgStars,
             COUNT(r) AS reviewCount
      FROM reviews r
      WHERE r.status = :status
        AND r.dateItemServed >= :since
        AND r.itemsStars IS NOT NULL
      GROUP BY r.item.diningCommonsCode
      ORDER BY AVG(r.itemsStars) DESC, COUNT(r) DESC
      """)
  List<CommonsRatingProjection> findCommonsAverages(
      @Param("status") ModerationStatus status, @Param("since") LocalDateTime since);

  @Query(
      """
      SELECT r.item.diningCommonsCode AS diningCommonsCode,
             r.dateItemServed AS dateItemServed,
             r.itemsStars AS itemsStars
      FROM reviews r
      WHERE r.status = :status
        AND r.dateItemServed >= :since
        AND r.itemsStars IS NOT NULL
      """)
  List<CommonsReviewRow> findCommonsReviewRows(
      @Param("status") ModerationStatus status, @Param("since") LocalDateTime since);
}
