package edu.ucsb.cs156.dining.util;

import java.time.LocalDateTime;

/** Time windows for filtering review statistics by {@code dateItemServed}. */
public enum StatsWindow {
  ALL,
  SIX_MONTHS,
  ONE_MONTH,
  ONE_WEEK;

  /**
   * @param now reference time (typically {@code LocalDateTime.now()})
   * @return earliest {@code dateItemServed} to include for this window
   */
  public LocalDateTime since(LocalDateTime now) {
    return switch (this) {
      case ALL -> LocalDateTime.of(1970, 1, 1, 0, 0);
      case SIX_MONTHS -> now.minusMonths(6);
      case ONE_MONTH -> now.minusMonths(1);
      case ONE_WEEK -> now.minusWeeks(1);
    };
  }
}
