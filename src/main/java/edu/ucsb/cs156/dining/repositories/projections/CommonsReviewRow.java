package edu.ucsb.cs156.dining.repositories.projections;

import java.time.LocalDateTime;

/** Minimal row for commons time-series bucketing in application code. */
public interface CommonsReviewRow {
  String getDiningCommonsCode();

  LocalDateTime getDateItemServed();

  Integer getItemsStars();
}
