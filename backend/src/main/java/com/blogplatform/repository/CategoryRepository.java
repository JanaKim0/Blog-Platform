package com.blogplatform.repository;

import com.blogplatform.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findBySlug(String slug);

	Optional<Category> findByNameIgnoreCase(String name);

	boolean existsBySlug(String slug);

	List<Category> findAllByOrderByNameAsc();
}
