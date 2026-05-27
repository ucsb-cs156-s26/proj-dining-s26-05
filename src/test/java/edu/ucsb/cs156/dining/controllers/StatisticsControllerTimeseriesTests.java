package edu.ucsb.cs156.dining.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.dining.ControllerTestCase;
import edu.ucsb.cs156.dining.repositories.ReviewRepository;
import edu.ucsb.cs156.dining.repositories.projections.CommonsReviewRow;
import edu.ucsb.cs156.dining.statuses.ModerationStatus;
import edu.ucsb.cs156.dining.testconfig.TestConfig;
import edu.ucsb.cs156.dining.util.StatsWindow;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StatisticsController.class)
@Import(TestConfig.class)
public class StatisticsControllerTimeseriesTests extends ControllerTestCase {

  @Autowired private MockMvc mockMvc;

  @MockBean ReviewRepository reviewRepository;

  @Test
  public void commons_timeseries_logged_out_returns_403() throws Exception {
    mockMvc.perform(get("/api/statistics/commons/timeseries")).andExpect(status().isForbidden());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_default_bucket_DAY_window_ALL_returns_expected_shape()
      throws Exception {
    CommonsReviewRow row = mockCommonsReviewRow("ortega", LocalDateTime.of(2026, 5, 26, 12, 0), 4);
    when(reviewRepository.findCommonsReviewRows(any(), any())).thenReturn(List.of(row));

    LocalDateTime expectedSince = StatsWindow.ALL.since(LocalDateTime.now());

    mockMvc
        .perform(get("/api/statistics/commons/timeseries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].diningCommonsCode").value("ortega"))
        .andExpect(jsonPath("$[0].bucketStart").value("2026-05-26"))
        .andExpect(jsonPath("$[0].avgStars").value(4.0))
        .andExpect(jsonPath("$[0].reviewCount").value(1));

    ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(reviewRepository, times(1))
        .findCommonsReviewRows(eq(ModerationStatus.APPROVED), sinceCaptor.capture());

    org.junit.jupiter.api.Assertions.assertEquals(expectedSince, sinceCaptor.getValue());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_bucket_DAY_groups_same_calendar_day() throws Exception {
    CommonsReviewRow morning =
        mockCommonsReviewRow("ortega", LocalDateTime.of(2026, 5, 26, 8, 0), 4);
    CommonsReviewRow evening =
        mockCommonsReviewRow("ortega", LocalDateTime.of(2026, 5, 26, 20, 0), 5);
    when(reviewRepository.findCommonsReviewRows(any(), any()))
        .thenReturn(List.of(morning, evening));

    mockMvc
        .perform(get("/api/statistics/commons/timeseries?bucket=DAY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].bucketStart").value("2026-05-26"))
        .andExpect(jsonPath("$[0].avgStars").value(4.5))
        .andExpect(jsonPath("$[0].reviewCount").value(2));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_bucket_WEEK_groups_iso_week() throws Exception {
    CommonsReviewRow wednesday =
        mockCommonsReviewRow("ortega", LocalDateTime.of(2026, 5, 28, 12, 0), 4);
    CommonsReviewRow sunday =
        mockCommonsReviewRow("ortega", LocalDateTime.of(2026, 5, 31, 9, 0), 5);
    when(reviewRepository.findCommonsReviewRows(any(), any()))
        .thenReturn(List.of(wednesday, sunday));

    mockMvc
        .perform(get("/api/statistics/commons/timeseries?bucket=WEEK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].bucketStart").value("2026-05-25"))
        .andExpect(jsonPath("$[0].avgStars").value(4.5))
        .andExpect(jsonPath("$[0].reviewCount").value(2));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_bucket_MONTH_groups_same_month() throws Exception {
    CommonsReviewRow midMonth =
        mockCommonsReviewRow("ortega", LocalDateTime.of(2026, 5, 15, 12, 0), 4);
    CommonsReviewRow lateMonth =
        mockCommonsReviewRow("ortega", LocalDateTime.of(2026, 5, 28, 12, 0), 5);
    when(reviewRepository.findCommonsReviewRows(any(), any()))
        .thenReturn(List.of(midMonth, lateMonth));

    mockMvc
        .perform(get("/api/statistics/commons/timeseries?bucket=MONTH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].bucketStart").value("2026-05-01"))
        .andExpect(jsonPath("$[0].avgStars").value(4.5))
        .andExpect(jsonPath("$[0].reviewCount").value(2));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_window_SIX_MONTHS_passes_correct_since() throws Exception {
    when(reviewRepository.findCommonsReviewRows(any(), any())).thenReturn(Collections.emptyList());

    LocalDateTime testNow = LocalDateTime.now();
    LocalDateTime expectedSince = StatsWindow.SIX_MONTHS.since(testNow);

    mockMvc
        .perform(get("/api/statistics/commons/timeseries?window=SIX_MONTHS"))
        .andExpect(status().isOk());

    ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(reviewRepository, times(1))
        .findCommonsReviewRows(eq(ModerationStatus.APPROVED), sinceCaptor.capture());

    LocalDateTime actualSince = sinceCaptor.getValue();
    boolean withinTolerance =
        !actualSince.isBefore(expectedSince.minusSeconds(2))
            && !actualSince.isAfter(expectedSince.plusSeconds(2));
    org.junit.jupiter.api.Assertions.assertTrue(withinTolerance);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_window_ONE_MONTH_passes_correct_since() throws Exception {
    when(reviewRepository.findCommonsReviewRows(any(), any())).thenReturn(Collections.emptyList());

    LocalDateTime testNow = LocalDateTime.now();
    LocalDateTime expectedSince = StatsWindow.ONE_MONTH.since(testNow);

    mockMvc
        .perform(get("/api/statistics/commons/timeseries?window=ONE_MONTH"))
        .andExpect(status().isOk());

    ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(reviewRepository, times(1))
        .findCommonsReviewRows(eq(ModerationStatus.APPROVED), sinceCaptor.capture());

    LocalDateTime actualSince = sinceCaptor.getValue();
    boolean withinTolerance =
        !actualSince.isBefore(expectedSince.minusSeconds(2))
            && !actualSince.isAfter(expectedSince.plusSeconds(2));
    org.junit.jupiter.api.Assertions.assertTrue(withinTolerance);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_window_ONE_WEEK_passes_correct_since() throws Exception {
    when(reviewRepository.findCommonsReviewRows(any(), any())).thenReturn(Collections.emptyList());

    LocalDateTime testNow = LocalDateTime.now();
    LocalDateTime expectedSince = StatsWindow.ONE_WEEK.since(testNow);

    mockMvc
        .perform(get("/api/statistics/commons/timeseries?window=ONE_WEEK"))
        .andExpect(status().isOk());

    ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(reviewRepository, times(1))
        .findCommonsReviewRows(eq(ModerationStatus.APPROVED), sinceCaptor.capture());

    LocalDateTime actualSince = sinceCaptor.getValue();
    boolean withinTolerance =
        !actualSince.isBefore(expectedSince.minusSeconds(2))
            && !actualSince.isAfter(expectedSince.plusSeconds(2));
    org.junit.jupiter.api.Assertions.assertTrue(withinTolerance);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_empty_result_returns_empty_array() throws Exception {
    when(reviewRepository.findCommonsReviewRows(any(), any())).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/statistics/commons/timeseries"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_results_sorted_by_commons_then_bucketStart() throws Exception {
    CommonsReviewRow carrilloLater =
        mockCommonsReviewRow("carrillo", LocalDateTime.of(2026, 5, 2, 12, 0), 3);
    CommonsReviewRow ortegaEarly =
        mockCommonsReviewRow("ortega", LocalDateTime.of(2026, 5, 1, 12, 0), 5);
    CommonsReviewRow carrilloEarly =
        mockCommonsReviewRow("carrillo", LocalDateTime.of(2026, 5, 1, 12, 0), 4);
    when(reviewRepository.findCommonsReviewRows(any(), any()))
        .thenReturn(List.of(carrilloLater, ortegaEarly, carrilloEarly));

    mockMvc
        .perform(get("/api/statistics/commons/timeseries?bucket=DAY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].diningCommonsCode").value("carrillo"))
        .andExpect(jsonPath("$[0].bucketStart").value("2026-05-01"))
        .andExpect(jsonPath("$[1].diningCommonsCode").value("carrillo"))
        .andExpect(jsonPath("$[1].bucketStart").value("2026-05-02"))
        .andExpect(jsonPath("$[2].diningCommonsCode").value("ortega"))
        .andExpect(jsonPath("$[2].bucketStart").value("2026-05-01"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_invalid_bucket_returns_400() throws Exception {
    mockMvc
        .perform(get("/api/statistics/commons/timeseries?bucket=BOGUS"))
        .andExpect(status().isBadRequest());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void commons_timeseries_invalid_window_returns_400() throws Exception {
    mockMvc
        .perform(get("/api/statistics/commons/timeseries?window=BOGUS"))
        .andExpect(status().isBadRequest());
  }

  private CommonsReviewRow mockCommonsReviewRow(
      String diningCommonsCode, LocalDateTime dateItemServed, int itemsStars) {
    CommonsReviewRow row = mock(CommonsReviewRow.class);
    when(row.getDiningCommonsCode()).thenReturn(diningCommonsCode);
    when(row.getDateItemServed()).thenReturn(dateItemServed);
    when(row.getItemsStars()).thenReturn(itemsStars);
    return row;
  }
}
