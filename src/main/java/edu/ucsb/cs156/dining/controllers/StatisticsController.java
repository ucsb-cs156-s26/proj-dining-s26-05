package edu.ucsb.cs156.dining.controllers;

import edu.ucsb.cs156.dining.repositories.ReviewRepository;
import edu.ucsb.cs156.dining.repositories.projections.ItemRatingProjection;
import edu.ucsb.cs156.dining.statuses.ModerationStatus;
import edu.ucsb.cs156.dining.util.StatsWindow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Statistics")
@RequestMapping("/api/statistics")
@RestController
@Slf4j
public class StatisticsController extends ApiController {

  private static final int DEFAULT_LIMIT = 10;
  private static final int MAX_LIMIT = 50;
  private static final long DEFAULT_MIN_REVIEWS = 3;

  @Autowired ReviewRepository reviewRepository;

  public record StatisticsSummary(
      long totalApprovedReviews,
      long totalMenuItemsReviewed,
      long totalCommonsCovered,
      LocalDateTime lastUpdated) {}

  public record RatedItem(
      Long itemId,
      String name,
      String diningCommonsCode,
      String mealCode,
      Double avgStars,
      Long reviewCount) {}

  @Operation(summary = "Get a summary of review statistics")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping(value = "", produces = "application/json")
  public StatisticsSummary getSummary() {
    return new StatisticsSummary(
        reviewRepository.countByStatus(ModerationStatus.APPROVED),
        reviewRepository.countDistinctItemsByStatus(ModerationStatus.APPROVED),
        reviewRepository.countDistinctCommonsByStatus(ModerationStatus.APPROVED),
        reviewRepository.findMaxDateEditedByStatus(ModerationStatus.APPROVED));
  }

  LocalDateTime sinceFor(StatsWindow window) {
    return window.since(LocalDateTime.now());
  }

  private static int clampLimit(int limit) {
    if (limit < 1) {
      return 1;
    }
    return Math.min(limit, MAX_LIMIT);
  }

  private static long clampMinReviews(long minReviews) {
    if (minReviews < 1) {
      return 1;
    }
    return minReviews;
  }

  private RatedItem toRatedItem(ItemRatingProjection p) {
    return new RatedItem(
        p.getItemId(),
        p.getName(),
        p.getDiningCommonsCode(),
        p.getMealCode(),
        p.getAvgStars(),
        p.getReviewCount());
  }

  @Operation(summary = "Get best rated menu items by average stars")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping(value = "/items/best", produces = "application/json")
  public List<RatedItem> bestRatedItems(
      @RequestParam(defaultValue = "ALL") StatsWindow window,
      @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit,
      @RequestParam(name = "minReviews", defaultValue = "" + DEFAULT_MIN_REVIEWS) long minReviews) {
    int clampedLimit = clampLimit(limit);
    long clampedMinReviews = clampMinReviews(minReviews);

    LocalDateTime since = sinceFor(window);
    List<ItemRatingProjection> projections =
        reviewRepository.findTopRatedItems(
            ModerationStatus.APPROVED, since, clampedMinReviews, PageRequest.of(0, clampedLimit));

    return projections.stream().map(this::toRatedItem).toList();
  }

  @Operation(summary = "Get worst rated menu items by average stars")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping(value = "/items/worst", produces = "application/json")
  public List<RatedItem> worstRatedItems(
      @RequestParam(defaultValue = "ALL") StatsWindow window,
      @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit,
      @RequestParam(name = "minReviews", defaultValue = "" + DEFAULT_MIN_REVIEWS) long minReviews) {
    int clampedLimit = clampLimit(limit);
    long clampedMinReviews = clampMinReviews(minReviews);

    LocalDateTime since = sinceFor(window);
    List<ItemRatingProjection> projections =
        reviewRepository.findBottomRatedItems(
            ModerationStatus.APPROVED, since, clampedMinReviews, PageRequest.of(0, clampedLimit));

    return projections.stream().map(this::toRatedItem).toList();
  }
}
