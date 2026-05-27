package edu.ucsb.cs156.dining.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import edu.ucsb.cs156.dining.repositories.projections.ItemRatingProjection;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StatisticsController.class)
@Import(TestConfig.class)
public class StatisticsControllerTests extends ControllerTestCase {

  @Autowired private MockMvc mockMvc;

  @MockBean ReviewRepository reviewRepository;

  @Test
  public void api_statistics_logged_out_returns_403() throws Exception {
    mockMvc.perform(get("/api/statistics")).andExpect(status().isForbidden());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void api_statistics_user_logged_in_returns_expected_shape() throws Exception {
    LocalDateTime lastUpdated = LocalDateTime.parse("2026-05-26T18:00:00");
    when(reviewRepository.countByStatus(ModerationStatus.APPROVED)).thenReturn(10L);
    when(reviewRepository.countDistinctItemsByStatus(ModerationStatus.APPROVED)).thenReturn(7L);
    when(reviewRepository.countDistinctCommonsByStatus(ModerationStatus.APPROVED)).thenReturn(4L);
    when(reviewRepository.findMaxDateEditedByStatus(ModerationStatus.APPROVED))
        .thenReturn(lastUpdated);

    mockMvc
        .perform(get("/api/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalApprovedReviews").value(10))
        .andExpect(jsonPath("$.totalMenuItemsReviewed").value(7))
        .andExpect(jsonPath("$.totalCommonsCovered").value(4))
        .andExpect(jsonPath("$.lastUpdated").value("2026-05-26T18:00:00"));

    verify(reviewRepository, times(1)).countByStatus(ModerationStatus.APPROVED);
    verify(reviewRepository, times(1)).countDistinctItemsByStatus(ModerationStatus.APPROVED);
    verify(reviewRepository, times(1)).countDistinctCommonsByStatus(ModerationStatus.APPROVED);
    verify(reviewRepository, times(1)).findMaxDateEditedByStatus(ModerationStatus.APPROVED);
  }

  @WithMockUser(roles = {"USER", "ADMIN"})
  @Test
  public void api_statistics_admin_logged_in_returns_200() throws Exception {
    when(reviewRepository.countByStatus(ModerationStatus.APPROVED)).thenReturn(0L);
    when(reviewRepository.countDistinctItemsByStatus(ModerationStatus.APPROVED)).thenReturn(0L);
    when(reviewRepository.countDistinctCommonsByStatus(ModerationStatus.APPROVED)).thenReturn(0L);
    when(reviewRepository.findMaxDateEditedByStatus(ModerationStatus.APPROVED)).thenReturn(null);

    mockMvc.perform(get("/api/statistics")).andExpect(status().isOk());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void api_statistics_null_lastUpdated_serializes_as_null() throws Exception {
    when(reviewRepository.countByStatus(ModerationStatus.APPROVED)).thenReturn(0L);
    when(reviewRepository.countDistinctItemsByStatus(ModerationStatus.APPROVED)).thenReturn(0L);
    when(reviewRepository.countDistinctCommonsByStatus(ModerationStatus.APPROVED)).thenReturn(0L);
    when(reviewRepository.findMaxDateEditedByStatus(ModerationStatus.APPROVED)).thenReturn(null);

    mockMvc
        .perform(get("/api/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalApprovedReviews").value(0))
        .andExpect(jsonPath("$.totalMenuItemsReviewed").value(0))
        .andExpect(jsonPath("$.totalCommonsCovered").value(0))
        .andExpect(jsonPath("$.lastUpdated").doesNotExist());
  }

  @Test
  public void items_best_logged_out_returns_403() throws Exception {
    mockMvc.perform(get("/api/statistics/items/best")).andExpect(status().isForbidden());
  }

  @Test
  public void items_worst_logged_out_returns_403() throws Exception {
    mockMvc.perform(get("/api/statistics/items/worst")).andExpect(status().isForbidden());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_best_default_params_uses_ALL_window_limit_10_minReviews_3() throws Exception {
    ItemRatingProjection projection =
        mockItemRatingProjection(1L, "Pizza", "ortega", "lunch", 5.0, 3L);
    when(reviewRepository.findTopRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(List.of(projection));

    LocalDateTime expectedSince = StatsWindow.ALL.since(LocalDateTime.now());

    mockMvc
        .perform(get("/api/statistics/items/best"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].itemId").value(1))
        .andExpect(jsonPath("$[0].name").value("Pizza"))
        .andExpect(jsonPath("$[0].diningCommonsCode").value("ortega"))
        .andExpect(jsonPath("$[0].mealCode").value("lunch"))
        .andExpect(jsonPath("$[0].avgStars").value(5.0))
        .andExpect(jsonPath("$[0].reviewCount").value(3));

    ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<Long> minReviewsCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    verify(reviewRepository, times(1))
        .findTopRatedItems(
            eq(ModerationStatus.APPROVED),
            sinceCaptor.capture(),
            minReviewsCaptor.capture(),
            pageableCaptor.capture());

    // ALL is stable epoch, so we can assert exact equality.
    org.junit.jupiter.api.Assertions.assertEquals(expectedSince, sinceCaptor.getValue());
    org.junit.jupiter.api.Assertions.assertEquals(3L, minReviewsCaptor.getValue().longValue());
    org.junit.jupiter.api.Assertions.assertEquals(0, pageableCaptor.getValue().getPageNumber());
    org.junit.jupiter.api.Assertions.assertEquals(10, pageableCaptor.getValue().getPageSize());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_best_window_SIX_MONTHS_passes_correct_since() throws Exception {
    when(reviewRepository.findTopRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(Collections.emptyList());

    LocalDateTime testNow = LocalDateTime.now();
    LocalDateTime expectedSince = StatsWindow.SIX_MONTHS.since(testNow);

    mockMvc.perform(get("/api/statistics/items/best?window=SIX_MONTHS")).andExpect(status().isOk());

    ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(reviewRepository, times(1))
        .findTopRatedItems(
            eq(ModerationStatus.APPROVED), sinceCaptor.capture(), anyLong(), any(Pageable.class));

    LocalDateTime actualSince = sinceCaptor.getValue();
    boolean withinTolerance =
        !actualSince.isBefore(expectedSince.minusSeconds(2))
            && !actualSince.isAfter(expectedSince.plusSeconds(2));
    org.junit.jupiter.api.Assertions.assertTrue(withinTolerance);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_best_limit_above_max_is_clamped_to_50() throws Exception {
    when(reviewRepository.findTopRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/statistics/items/best?limit=100")).andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    verify(reviewRepository, times(1))
        .findTopRatedItems(eq(ModerationStatus.APPROVED), any(), eq(3L), pageableCaptor.capture());

    org.junit.jupiter.api.Assertions.assertEquals(50, pageableCaptor.getValue().getPageSize());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_best_limit_zero_clamped_to_1() throws Exception {
    when(reviewRepository.findTopRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/statistics/items/best?limit=0")).andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    verify(reviewRepository, times(1))
        .findTopRatedItems(eq(ModerationStatus.APPROVED), any(), eq(3L), pageableCaptor.capture());

    org.junit.jupiter.api.Assertions.assertEquals(1, pageableCaptor.getValue().getPageSize());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_best_minReviews_param_propagates() throws Exception {
    when(reviewRepository.findTopRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/statistics/items/best?minReviews=7")).andExpect(status().isOk());

    ArgumentCaptor<Long> minReviewsCaptor = ArgumentCaptor.forClass(Long.class);

    verify(reviewRepository, times(1))
        .findTopRatedItems(
            eq(ModerationStatus.APPROVED), any(), minReviewsCaptor.capture(), any(Pageable.class));

    org.junit.jupiter.api.Assertions.assertEquals(7L, minReviewsCaptor.getValue().longValue());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_best_minReviews_zero_clamped_to_1() throws Exception {
    when(reviewRepository.findTopRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/statistics/items/best?minReviews=0")).andExpect(status().isOk());

    ArgumentCaptor<Long> minReviewsCaptor = ArgumentCaptor.forClass(Long.class);

    verify(reviewRepository, times(1))
        .findTopRatedItems(
            eq(ModerationStatus.APPROVED), any(), minReviewsCaptor.capture(), any(Pageable.class));

    org.junit.jupiter.api.Assertions.assertEquals(1L, minReviewsCaptor.getValue().longValue());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_best_invalid_window_returns_400() throws Exception {
    mockMvc
        .perform(get("/api/statistics/items/best?window=BOGUS"))
        .andExpect(status().isBadRequest());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_best_empty_result_returns_empty_array() throws Exception {
    when(reviewRepository.findTopRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/statistics/items/best"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_worst_default_params_calls_findBottomRatedItems_and_returns_shape()
      throws Exception {
    ItemRatingProjection projection =
        mockItemRatingProjection(9L, "Salad", "carrillo", "dinner", 1.0, 4L);
    when(reviewRepository.findBottomRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(List.of(projection));

    LocalDateTime expectedSince = StatsWindow.ALL.since(LocalDateTime.now());

    mockMvc
        .perform(get("/api/statistics/items/worst"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].itemId").value(9))
        .andExpect(jsonPath("$[0].name").value("Salad"))
        .andExpect(jsonPath("$[0].diningCommonsCode").value("carrillo"))
        .andExpect(jsonPath("$[0].mealCode").value("dinner"))
        .andExpect(jsonPath("$[0].avgStars").value(1.0))
        .andExpect(jsonPath("$[0].reviewCount").value(4));

    ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(reviewRepository, times(1))
        .findBottomRatedItems(
            eq(ModerationStatus.APPROVED), sinceCaptor.capture(), eq(3L), any(Pageable.class));

    org.junit.jupiter.api.Assertions.assertEquals(expectedSince, sinceCaptor.getValue());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void items_worst_window_ONE_WEEK_passes_correct_since() throws Exception {
    when(reviewRepository.findBottomRatedItems(any(), any(), anyLong(), any(Pageable.class)))
        .thenReturn(Collections.emptyList());

    LocalDateTime testNow = LocalDateTime.now();
    LocalDateTime expectedSince = StatsWindow.ONE_WEEK.since(testNow);

    mockMvc.perform(get("/api/statistics/items/worst?window=ONE_WEEK")).andExpect(status().isOk());

    ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(reviewRepository, times(1))
        .findBottomRatedItems(
            eq(ModerationStatus.APPROVED), sinceCaptor.capture(), anyLong(), any(Pageable.class));

    LocalDateTime actualSince = sinceCaptor.getValue();
    boolean withinTolerance =
        !actualSince.isBefore(expectedSince.minusSeconds(2))
            && !actualSince.isAfter(expectedSince.plusSeconds(2));
    org.junit.jupiter.api.Assertions.assertTrue(withinTolerance);
  }

  private ItemRatingProjection mockItemRatingProjection(
      long itemId,
      String name,
      String diningCommonsCode,
      String mealCode,
      Double avgStars,
      Long reviewCount) {
    ItemRatingProjection projection = mock(ItemRatingProjection.class);
    when(projection.getItemId()).thenReturn(itemId);
    when(projection.getName()).thenReturn(name);
    when(projection.getDiningCommonsCode()).thenReturn(diningCommonsCode);
    when(projection.getMealCode()).thenReturn(mealCode);
    when(projection.getAvgStars()).thenReturn(avgStars);
    when(projection.getReviewCount()).thenReturn(reviewCount);
    return projection;
  }
}
