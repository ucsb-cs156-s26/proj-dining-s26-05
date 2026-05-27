package edu.ucsb.cs156.dining.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class TimeBucketTests {

  @Test
  public void day_floor_returns_same_calendar_date() {
    LocalDateTime served = LocalDateTime.of(2026, 5, 26, 18, 30);
    assertEquals(LocalDate.of(2026, 5, 26), TimeBucket.DAY.floor(served));
  }

  @Test
  public void week_floor_wednesday_returns_monday_of_that_week() {
    LocalDateTime served = LocalDateTime.of(2026, 5, 28, 12, 0);
    assertEquals(LocalDate.of(2026, 5, 25), TimeBucket.WEEK.floor(served));
  }

  @Test
  public void week_floor_sunday_returns_previous_monday() {
    LocalDateTime served = LocalDateTime.of(2026, 5, 31, 9, 0);
    assertEquals(LocalDate.of(2026, 5, 25), TimeBucket.WEEK.floor(served));
  }

  @Test
  public void week_floor_monday_returns_same_day() {
    LocalDateTime served = LocalDateTime.of(2026, 6, 1, 9, 0);
    assertEquals(LocalDate.of(2026, 6, 1), TimeBucket.WEEK.floor(served));
  }

  @Test
  public void month_floor_returns_first_of_month() {
    LocalDateTime served = LocalDateTime.of(2026, 5, 15, 12, 0);
    assertEquals(LocalDate.of(2026, 5, 1), TimeBucket.MONTH.floor(served));
  }

  @Test
  public void month_floor_leap_year_february_returns_feb_first() {
    LocalDateTime served = LocalDateTime.of(2024, 2, 29, 8, 0);
    assertEquals(LocalDate.of(2024, 2, 1), TimeBucket.MONTH.floor(served));
  }
}
