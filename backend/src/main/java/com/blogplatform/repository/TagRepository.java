package com.blogplatform.repository;

import com.blogplatform.domain.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

	Optional<Tag> findBySlug(String slug);

	Optional<Tag> findByNameIgnoreCase(String name);

	List<Tag> findBySlugIn(Collection<String> slugs);

	List<Tag> findAllByOrderByNameAsc();
}
