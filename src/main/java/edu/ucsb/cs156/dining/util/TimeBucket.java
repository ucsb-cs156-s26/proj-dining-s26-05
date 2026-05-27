package edu.ucsb.cs156.dining.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/** Buckets for grouping review statistics over time. */
public enum TimeBucket {
  DAY,
  WEEK,
  MONTH;

  /**
   * @param dateTime a review's {@code dateItemServed}
   * @return the start date of the bucket containing {@code dateTime}
   */
  public LocalDate floor(LocalDateTime dateTime) {
    LocalDate date = dateTime.toLocalDate();
    return switch (this) {
      case DAY -> date;
      case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case MONTH -> date.withDayOfMonth(1);
    };
  }
}
