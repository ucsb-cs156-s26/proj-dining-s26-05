package edu.ucsb.cs156.dining.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucsb.cs156.dining.entities.MenuItem;
import edu.ucsb.cs156.dining.entities.Review;
import edu.ucsb.cs156.dining.entities.User;
import edu.ucsb.cs156.dining.repositories.projections.ItemRatingProjection;
import edu.ucsb.cs156.dining.statuses.ModerationStatus;
import edu.ucsb.cs156.dining.util.StatsWindow;
import edu.ucsb.cs156.jpa.JpaSliceTestApplication;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@ContextConfiguration(classes = JpaSliceTestApplication.class)
@TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    })
public class ReviewRepositoryTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 26, 12, 0, 0);
  private static final LocalDateTime EPOCH = StatsWindow.ALL.since(NOW);

  @Autowired private TestEntityManager entityManager;

  @Autowired private ReviewRepository reviewRepository;

  private User reviewer;

  @BeforeEach
  public void setUp() {
    reviewer =
        entityManager.persist(
            User.builder().email("test@ucsb.edu").googleSub("sub-1").fullName("Test User").build());
    entityManager.flush();
  }

  @Test
  public void findTopRatedItems_orders_by_avg_stars_desc_then_review_count() {
    MenuItem bestItem = persistMenuItem("Pizza", "ortega", "lunch");
    MenuItem secondItem = persistMenuItem("Salad", "carrillo", "dinner");
    MenuItem tieAvgFewerReviews = persistMenuItem("Soup", "de-la-guerra", "breakfast");

    addApprovedReview(bestItem, 5L, NOW);
    addApprovedReview(bestItem, 5L, NOW);
    addApprovedReview(bestItem, 5L, NOW);

    addApprovedReview(secondItem, 4L, NOW);
    addApprovedReview(secondItem, 4L, NOW);
    addApprovedReview(secondItem, 4L, NOW);

    addApprovedReview(tieAvgFewerReviews, 5L, NOW);
    addApprovedReview(tieAvgFewerReviews, 5L, NOW);

    List<ItemRatingProjection> results =
        reviewRepository.findTopRatedItems(
            ModerationStatus.APPROVED, EPOCH, 3, PageRequest.of(0, 10));

    assertEquals(2, results.size());
    assertEquals(bestItem.getId(), results.get(0).getItemId());
    assertEquals(5.0, results.get(0).getAvgStars());
    assertEquals(3L, results.get(0).getReviewCount());
    assertEquals(secondItem.getId(), results.get(1).getItemId());
    assertEquals(4.0, results.get(1).getAvgStars());
  }

  @Test
  public void findTopRatedItems_excludes_non_approved_reviews() {
    MenuItem item = persistMenuItem("Burger", "portola", "lunch");

    addReview(item, 5L, ModerationStatus.AWAITING_REVIEW, NOW);
    addReview(item, 5L, ModerationStatus.REJECTED, NOW);
    addApprovedReview(item, 2L, NOW);

    List<ItemRatingProjection> results =
        reviewRepository.findTopRatedItems(
            ModerationStatus.APPROVED, EPOCH, 1, PageRequest.of(0, 10));

    assertEquals(1, results.size());
    assertEquals(2.0, results.get(0).getAvgStars());
    assertEquals(1L, results.get(0).getReviewCount());
  }

  @Test
  public void findTopRatedItems_excludes_reviews_before_since() {
    MenuItem item = persistMenuItem("Tacos", "ortega", "dinner");
    LocalDateTime since = StatsWindow.ONE_WEEK.since(NOW);

    addApprovedReview(item, 5L, NOW.minusWeeks(2));
    addApprovedReview(item, 4L, NOW.minusDays(2));

    List<ItemRatingProjection> results =
        reviewRepository.findTopRatedItems(
            ModerationStatus.APPROVED, since, 1, PageRequest.of(0, 10));

    assertEquals(1, results.size());
    assertEquals(4.0, results.get(0).getAvgStars());
    assertEquals(1L, results.get(0).getReviewCount());
  }

  @Test
  public void findTopRatedItems_excludes_items_below_min_reviews() {
    MenuItem popularItem = persistMenuItem("Pasta", "carrillo", "lunch");
    MenuItem loneReviewItem = persistMenuItem("Rice", "ortega", "dinner");

    addApprovedReview(popularItem, 4L, NOW);
    addApprovedReview(popularItem, 4L, NOW);
    addApprovedReview(popularItem, 4L, NOW);

    addApprovedReview(loneReviewItem, 5L, NOW);
    addApprovedReview(loneReviewItem, 5L, NOW);

    List<ItemRatingProjection> results =
        reviewRepository.findTopRatedItems(
            ModerationStatus.APPROVED, EPOCH, 3, PageRequest.of(0, 10));

    assertEquals(1, results.size());
    assertEquals(popularItem.getId(), results.get(0).getItemId());
  }

  @Test
  public void findTopRatedItems_excludes_null_items_stars() {
    MenuItem item = persistMenuItem("Toast", "portola", "breakfast");

    addApprovedReview(item, null, NOW);
    addApprovedReview(item, 4L, NOW);
    addApprovedReview(item, 4L, NOW);
    addApprovedReview(item, 4L, NOW);

    List<ItemRatingProjection> results =
        reviewRepository.findTopRatedItems(
            ModerationStatus.APPROVED, EPOCH, 3, PageRequest.of(0, 10));

    assertEquals(1, results.size());
    assertEquals(4.0, results.get(0).getAvgStars());
    assertEquals(3L, results.get(0).getReviewCount());
  }

  @Test
  public void findTopRatedItems_respects_pageable_limit() {
    for (int i = 0; i < 4; i++) {
      MenuItem item = persistMenuItem("Item " + i, "ortega", "lunch");
      addApprovedReview(item, (long) (i + 1), NOW);
      addApprovedReview(item, (long) (i + 1), NOW);
      addApprovedReview(item, (long) (i + 1), NOW);
    }

    List<ItemRatingProjection> results =
        reviewRepository.findTopRatedItems(
            ModerationStatus.APPROVED, EPOCH, 3, PageRequest.of(0, 2));

    assertEquals(2, results.size());
  }

  @Test
  public void findBottomRatedItems_orders_by_avg_stars_asc() {
    MenuItem worstItem = persistMenuItem("Mystery Meat", "ortega", "dinner");
    MenuItem betterItem = persistMenuItem("Grilled Chicken", "carrillo", "lunch");

    addApprovedReview(worstItem, 1L, NOW);
    addApprovedReview(worstItem, 2L, NOW);
    addApprovedReview(worstItem, 2L, NOW);

    addApprovedReview(betterItem, 4L, NOW);
    addApprovedReview(betterItem, 5L, NOW);
    addApprovedReview(betterItem, 5L, NOW);

    List<ItemRatingProjection> results =
        reviewRepository.findBottomRatedItems(
            ModerationStatus.APPROVED, EPOCH, 3, PageRequest.of(0, 10));

    assertEquals(2, results.size());
    assertEquals(worstItem.getId(), results.get(0).getItemId());
    assertTrue(results.get(0).getAvgStars() < results.get(1).getAvgStars());
    assertEquals(betterItem.getId(), results.get(1).getItemId());
  }

  @Test
  public void findTopRatedItems_returns_empty_when_no_matching_reviews() {
    List<ItemRatingProjection> results =
        reviewRepository.findTopRatedItems(
            ModerationStatus.APPROVED, EPOCH, 1, PageRequest.of(0, 10));

    assertTrue(results.isEmpty());
  }

  private MenuItem persistMenuItem(String name, String commonsCode, String mealCode) {
    MenuItem item =
        entityManager.persist(
            MenuItem.builder()
                .name(name)
                .diningCommonsCode(commonsCode)
                .mealCode(mealCode)
                .station("grill")
                .build());
    entityManager.flush();
    return item;
  }

  private void addApprovedReview(MenuItem item, Long stars, LocalDateTime dateItemServed) {
    addReview(item, stars, ModerationStatus.APPROVED, dateItemServed);
  }

  private void addReview(
      MenuItem item, Long stars, ModerationStatus status, LocalDateTime dateItemServed) {
    entityManager.persist(
        Review.builder()
            .item(item)
            .reviewer(reviewer)
            .itemsStars(stars)
            .status(status)
            .dateItemServed(dateItemServed)
            .build());
    entityManager.flush();
  }
}
