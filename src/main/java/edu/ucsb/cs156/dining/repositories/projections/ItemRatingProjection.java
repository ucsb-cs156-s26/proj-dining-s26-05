package edu.ucsb.cs156.dining.repositories.projections;

/** Projection for per-menu-item average star ratings. */
public interface ItemRatingProjection {
  Long getItemId();

  String getName();

  String getDiningCommonsCode();

  String getMealCode();

  Double getAvgStars();

  Long getReviewCount();
}
