package com.blogplatform.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Turns human titles into URL-friendly slugs.
 * <p>
 * Cyrillic is transliterated rather than dropped, so a Russian title still ends
 * up with a readable address instead of an empty slug.
 */
public final class Slugs {

	private static final Map<Character, String> TRANSLITERATION = Map.ofEntries(
			Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
			Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"),
			Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
			Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
			Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
			Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "kh"), Map.entry('ц', "ts"),
			Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "shch"), Map.entry('ъ', ""),
			Map.entry('ы', "y"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
			Map.entry('я', "ya"));

	private Slugs() {
	}

	/**
	 * @return a lowercase {@code a-z0-9-} slug, or an empty string if the input
	 *         had nothing usable in it (an emoji-only title, say)
	 */
	public static String slugify(String input) {
		if (input == null || input.isBlank()) {
			return "";
		}

		String lowercase = input.toLowerCase(Locale.ROOT);
		StringBuilder transliterated = new StringBuilder(lowercase.length());
		for (char character : lowercase.toCharArray()) {
			String replacement = TRANSLITERATION.get(character);
			transliterated.append(replacement != null ? replacement : character);
		}

		// Splitting accented letters into "letter + accent" makes the accents
		// easy to strip, turning "café" into "cafe" rather than "caf".
		String withoutAccents = Normalizer.normalize(transliterated, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");

		return withoutAccents
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+", "")
				.replaceAll("-+$", "");
	}

	/** Same, but never empty and never longer than {@code maxLength}. */
	public static String slugify(String input, String fallback, int maxLength) {
		String slug = slugify(input);
		if (slug.isEmpty()) {
			slug = fallback;
		}
		if (slug.length() > maxLength) {
			slug = slug.substring(0, maxLength).replaceAll("-+$", "");
		}
		return slug;
	}
}
