package edu.ucsb.cs156.dining.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.dining.repositories.projections.CommonsReviewRow;
import edu.ucsb.cs156.dining.util.TimeBucket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StatisticsControllerComputeTimeseriesTests {

  @Test
  public void computeTimeseries_empty_rows_returns_empty_list() {
    assertEquals(
        Collections.emptyList(),
        StatisticsController.computeTimeseries(Collections.emptyList(), TimeBucket.DAY));
  }

  @Test
  public void computeTimeseries_single_row_returns_average_and_count() {
    CommonsReviewRow row = mockRow("ortega", LocalDateTime.of(2026, 5, 26, 12, 0), 4);

    List<StatisticsController.CommonsTimeseriesPoint> result =
        StatisticsController.computeTimeseries(List.of(row), TimeBucket.DAY);

    assertEquals(1, result.size());
    assertEquals("ortega", result.get(0).diningCommonsCode());
    assertEquals(LocalDate.of(2026, 5, 26), result.get(0).bucketStart());
    assertEquals(4.0, result.get(0).avgStars());
    assertEquals(1L, result.get(0).reviewCount());
  }

  @Test
  public void computeTimeseries_day_bucket_averages_multiple_reviews_same_day() {
    CommonsReviewRow first = mockRow("ortega", LocalDateTime.of(2026, 5, 26, 8, 0), 4);
    CommonsReviewRow second = mockRow("ortega", LocalDateTime.of(2026, 5, 26, 20, 0), 5);

    List<StatisticsController.CommonsTimeseriesPoint> result =
        StatisticsController.computeTimeseries(List.of(first, second), TimeBucket.DAY);

    assertEquals(1, result.size());
    assertEquals(4.5, result.get(0).avgStars());
    assertEquals(2L, result.get(0).reviewCount());
  }

  @Test
  public void computeTimeseries_week_bucket_groups_cross_day_same_iso_week() {
    CommonsReviewRow wednesday = mockRow("ortega", LocalDateTime.of(2026, 5, 28, 12, 0), 4);
    CommonsReviewRow sunday = mockRow("ortega", LocalDateTime.of(2026, 5, 31, 9, 0), 5);

    List<StatisticsController.CommonsTimeseriesPoint> result =
        StatisticsController.computeTimeseries(List.of(wednesday, sunday), TimeBucket.WEEK);

    assertEquals(1, result.size());
    assertEquals(LocalDate.of(2026, 5, 25), result.get(0).bucketStart());
    assertEquals(4.5, result.get(0).avgStars());
    assertEquals(2L, result.get(0).reviewCount());
  }

  @Test
  public void computeTimeseries_month_bucket_groups_same_month() {
    CommonsReviewRow midMonth = mockRow("ortega", LocalDateTime.of(2026, 5, 15, 12, 0), 4);
    CommonsReviewRow lateMonth = mockRow("ortega", LocalDateTime.of(2026, 5, 28, 12, 0), 5);

    List<StatisticsController.CommonsTimeseriesPoint> result =
        StatisticsController.computeTimeseries(List.of(midMonth, lateMonth), TimeBucket.MONTH);

    assertEquals(1, result.size());
    assertEquals(LocalDate.of(2026, 5, 1), result.get(0).bucketStart());
    assertEquals(4.5, result.get(0).avgStars());
    assertEquals(2L, result.get(0).reviewCount());
  }

  @Test
  public void computeTimeseries_sorts_by_commons_then_bucketStart() {
    CommonsReviewRow carrilloLater = mockRow("carrillo", LocalDateTime.of(2026, 5, 2, 12, 0), 3);
    CommonsReviewRow ortegaEarly = mockRow("ortega", LocalDateTime.of(2026, 5, 1, 12, 0), 5);
    CommonsReviewRow carrilloEarly = mockRow("carrillo", LocalDateTime.of(2026, 5, 1, 12, 0), 4);

    List<StatisticsController.CommonsTimeseriesPoint> result =
        StatisticsController.computeTimeseries(
            List.of(carrilloLater, ortegaEarly, carrilloEarly), TimeBucket.DAY);

    assertEquals(3, result.size());
    assertEquals("carrillo", result.get(0).diningCommonsCode());
    assertEquals(LocalDate.of(2026, 5, 1), result.get(0).bucketStart());
    assertEquals("carrillo", result.get(1).diningCommonsCode());
    assertEquals(LocalDate.of(2026, 5, 2), result.get(1).bucketStart());
    assertEquals("ortega", result.get(2).diningCommonsCode());
    assertEquals(LocalDate.of(2026, 5, 1), result.get(2).bucketStart());
  }

  private CommonsReviewRow mockRow(
      String diningCommonsCode, LocalDateTime dateItemServed, int itemsStars) {
    CommonsReviewRow row = mock(CommonsReviewRow.class);
    when(row.getDiningCommonsCode()).thenReturn(diningCommonsCode);
    when(row.getDateItemServed()).thenReturn(dateItemServed);
    when(row.getItemsStars()).thenReturn(itemsStars);
    return row;
  }
}
