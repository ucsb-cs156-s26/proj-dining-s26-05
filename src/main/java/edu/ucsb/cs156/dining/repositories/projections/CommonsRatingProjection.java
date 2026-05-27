package edu.ucsb.cs156.dining.repositories.projections;

/** Projection for per-dining-commons average star ratings. */
public interface CommonsRatingProjection {
  String getDiningCommonsCode();

  Double getAvgStars();

  Long getReviewCount();
}
