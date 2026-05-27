package edu.ucsb.cs156.jpa;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Isolated Spring Boot config for {@code @DataJpaTest} slices. Lives outside {@code
 * edu.ucsb.cs156.dining} so {@code ExampleApplication} component scanning does not load it.
 */
@SpringBootApplication
@EntityScan(basePackages = "edu.ucsb.cs156.dining.entities")
@EnableJpaRepositories(basePackages = "edu.ucsb.cs156.dining.repositories")
public class JpaSliceTestApplication {}
