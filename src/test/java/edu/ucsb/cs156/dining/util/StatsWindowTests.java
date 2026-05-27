package edu.ucsb.cs156.dining.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class StatsWindowTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 26, 12, 0, 0);

  @Test
  public void all_since_returns_epoch() {
    assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0), StatsWindow.ALL.since(NOW));
  }

  @Test
  public void six_months_since_subtracts_six_months() {
    assertEquals(NOW.minusMonths(6), StatsWindow.SIX_MONTHS.since(NOW));
  }

  @Test
  public void one_month_since_subtracts_one_month() {
    assertEquals(NOW.minusMonths(1), StatsWindow.ONE_MONTH.since(NOW));
  }

  @Test
  public void one_week_since_subtracts_one_week() {
    assertEquals(NOW.minusWeeks(1), StatsWindow.ONE_WEEK.since(NOW));
  }
}
