package com.blogplatform.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTests {

	@Test
	void lowercasesAndJoinsWordsWithHyphens() {
		assertThat(Slugs.slugify("Hello World")).isEqualTo("hello-world");
		assertThat(Slugs.slugify("  Trailing and leading  ")).isEqualTo("trailing-and-leading");
	}

	@Test
	void collapsesPunctuationInsteadOfLeavingEmptySegments() {
		assertThat(Slugs.slugify("C++ vs. Java: a comparison"))
				.isEqualTo("c-vs-java-a-comparison");
		assertThat(Slugs.slugify("!!! wow !!!")).isEqualTo("wow");
	}

	@Test
	void transliteratesCyrillicRatherThanDroppingIt() {
		assertThat(Slugs.slugify("Многоуважаемый шкаф")).isEqualTo("mnogouvazhaemyy-shkaf");
		assertThat(Slugs.slugify("Ещё один щенок")).isEqualTo("eshche-odin-shchenok");
	}

	@Test
	void stripsAccentsWithoutLosingTheLetter() {
		assertThat(Slugs.slugify("Café déjà vu")).isEqualTo("cafe-deja-vu");
	}

	@Test
	void returnsEmptyWhenThereIsNothingUsable() {
		assertThat(Slugs.slugify("🎉🎉🎉")).isEmpty();
		assertThat(Slugs.slugify("")).isEmpty();
		assertThat(Slugs.slugify(null)).isEmpty();
	}

	@Test
	void fallsBackAndTruncatesWithoutLeavingATrailingHyphen() {
		assertThat(Slugs.slugify("🎉", "article", 200)).isEqualTo("article");
		assertThat(Slugs.slugify("aaa bbb ccc", "article", 8)).isEqualTo("aaa-bbb");
		assertThat(Slugs.slugify("aaa bbb ccc", "article", 7)).isEqualTo("aaa-bbb");
	}
}
