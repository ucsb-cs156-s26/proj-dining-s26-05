package edu.ucsb.cs156.dining.controllers;

import edu.ucsb.cs156.dining.repositories.ReviewRepository;
import edu.ucsb.cs156.dining.repositories.projections.CommonsRatingProjection;
import edu.ucsb.cs156.dining.repositories.projections.CommonsReviewRow;
import edu.ucsb.cs156.dining.repositories.projections.ItemRatingProjection;
import edu.ucsb.cs156.dining.statuses.ModerationStatus;
import edu.ucsb.cs156.dining.util.StatsWindow;
import edu.ucsb.cs156.dining.util.TimeBucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public record CommonsAverage(String diningCommonsCode, Double avgStars, Long reviewCount) {}

  public record CommonsTimeseriesPoint(
      String diningCommonsCode, LocalDate bucketStart, Double avgStars, Long reviewCount) {}

  private record TimeseriesKey(String diningCommonsCode, LocalDate bucketStart) {}

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
    return Math.min(Math.max(limit, 1), MAX_LIMIT);
  }

  private static long clampMinReviews(long minReviews) {
    return Math.max(minReviews, 1L);
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

  private CommonsAverage toCommonsAverage(CommonsRatingProjection p) {
    return new CommonsAverage(p.getDiningCommonsCode(), p.getAvgStars(), p.getReviewCount());
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

  @Operation(summary = "Get average star ratings grouped by dining commons")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping(value = "/commons/averages", produces = "application/json")
  public List<CommonsAverage> commonsAverages(
      @RequestParam(defaultValue = "ALL") StatsWindow window) {
    LocalDateTime since = sinceFor(window);
    List<CommonsRatingProjection> projections =
        reviewRepository.findCommonsAverages(ModerationStatus.APPROVED, since);

    return projections.stream().map(this::toCommonsAverage).toList();
  }

  static List<CommonsTimeseriesPoint> computeTimeseries(
      List<CommonsReviewRow> rows, TimeBucket bucket) {
    Map<TimeseriesKey, List<Integer>> starsByKey = new HashMap<>();

    for (CommonsReviewRow row : rows) {
      LocalDate bucketStart = bucket.floor(row.getDateItemServed());
      TimeseriesKey key = new TimeseriesKey(row.getDiningCommonsCode(), bucketStart);
      starsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row.getItemsStars());
    }

    return starsByKey.entrySet().stream()
        .map(
            entry -> {
              List<Integer> stars = entry.getValue();
              double avgStars = stars.stream().mapToInt(Integer::intValue).average().orElse(0.0);
              return new CommonsTimeseriesPoint(
                  entry.getKey().diningCommonsCode(),
                  entry.getKey().bucketStart(),
                  avgStars,
                  (long) stars.size());
            })
        .sorted(
            Comparator.comparing(CommonsTimeseriesPoint::diningCommonsCode)
                .thenComparing(CommonsTimeseriesPoint::bucketStart))
        .toList();
  }

  @Operation(summary = "Get per-commons average ratings over time buckets")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping(value = "/commons/timeseries", produces = "application/json")
  public List<CommonsTimeseriesPoint> commonsTimeseries(
      @RequestParam(defaultValue = "DAY") TimeBucket bucket,
      @RequestParam(defaultValue = "ALL") StatsWindow window) {
    LocalDateTime since = sinceFor(window);
    List<CommonsReviewRow> rows =
        reviewRepository.findCommonsReviewRows(ModerationStatus.APPROVED, since);

    return computeTimeseries(rows, bucket);
  }
}
