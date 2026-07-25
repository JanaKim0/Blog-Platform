package com.blogplatform.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * A page of results in a shape the API controls. Spring's {@code Page} would
 * serialise its whole internal structure, which is explicitly not a stable
 * contract, so every paginated endpoint returns this instead.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements,
		int totalPages, boolean first, boolean last) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
	}

	/** Same, but converting each entity into its DTO on the way. */
	public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
				page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
				page.isFirst(), page.isLast());
	}
}
