package com.blogplatform.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.ArticleLike;
import com.blogplatform.domain.ArticleStatus;
import com.blogplatform.domain.Category;
import com.blogplatform.domain.Comment;
import com.blogplatform.domain.Subscription;
import com.blogplatform.domain.Tag;
import com.blogplatform.domain.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

/**
 * Checks that the schema really behaves the way it was designed: relations
 * resolve, the "one like per user" and "one subscription per pair" constraints
 * are enforced by the database, and auditing fills in the timestamps.
 */
@DataJpaTest
class DomainMappingTests {

	@Autowired
	private UserRepository users;
	@Autowired
	private ArticleRepository articles;
	@Autowired
	private CategoryRepository categories;
	@Autowired
	private TagRepository tags;
	@Autowired
	private CommentRepository comments;
	@Autowired
	private ArticleLikeRepository likes;
	@Autowired
	private SubscriptionRepository subscriptions;

	private User author;
	private User reader;

	@BeforeEach
	void createUsers() {
		author = users.save(newUser("nora", "nora@example.com"));
		reader = users.save(newUser("mira", "mira@example.com"));
	}

	@Test
	void savesAnArticleWithItsCategoryAndTags() {
		Category category = categories.save(newCategory("Programming", "programming"));
		Tag java = tags.save(newTag("Java", "java"));
		Tag spring = tags.save(newTag("Spring", "spring"));

		Article article = newArticle("Hello world", "hello-world");
		article.setCategory(category);
		article.getTags().add(java);
		article.getTags().add(spring);
		articles.saveAndFlush(article);

		Optional<Article> found = articles.findBySlug("hello-world");

		assertThat(found).isPresent();
		assertThat(found.get().getAuthor().getUsername()).isEqualTo("nora");
		assertThat(found.get().getCategory().getName()).isEqualTo("Programming");
		assertThat(found.get().getTags()).extracting(Tag::getSlug)
				.containsExactlyInAnyOrder("java", "spring");
		assertThat(found.get().isPublished()).isTrue();
	}

	@Test
	void fillsAuditingTimestamps() {
		Article article = articles.saveAndFlush(newArticle("Timestamps", "timestamps"));

		assertThat(article.getCreatedAt()).isNotNull();
		assertThat(article.getUpdatedAt()).isNotNull();
	}

	@Test
	void countsCommentsPerArticle() {
		Article article = articles.saveAndFlush(newArticle("Comments", "comments"));
		comments.save(newComment(article, reader, "Nice one"));
		comments.saveAndFlush(newComment(article, author, "Thanks!"));

		assertThat(comments.countByArticleId(article.getId())).isEqualTo(2);
		assertThat(comments.findByArticleId(article.getId(), PageRequest.of(0, 10)))
				.hasSize(2);
	}

	@Test
	void rejectsTheSameUserLikingAnArticleTwice() {
		Article article = articles.saveAndFlush(newArticle("Likes", "likes"));
		likes.saveAndFlush(newLike(article, reader));

		assertThat(likes.existsByArticleIdAndUserId(article.getId(), reader.getId())).isTrue();
		assertThat(likes.countByArticleId(article.getId())).isEqualTo(1);
		assertThatThrownBy(() -> likes.saveAndFlush(newLike(article, reader)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsFollowingTheSameAuthorTwice() {
		subscriptions.saveAndFlush(newSubscription(reader, author));

		assertThat(subscriptions.countByAuthorId(author.getId())).isEqualTo(1);
		assertThat(subscriptions.countByFollowerId(reader.getId())).isEqualTo(1);
		assertThat(subscriptions.findFollowedAuthorIds(reader.getId()))
				.containsExactly(author.getId());
		assertThatThrownBy(() -> subscriptions.saveAndFlush(newSubscription(reader, author)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findsUsersByPartOfTheirName() {
		assertThat(users.search("nor", PageRequest.of(0, 10)))
				.extracting(User::getUsername).containsExactly("nora");
		assertThat(users.existsByEmailIgnoreCase("NORA@example.com")).isTrue();
	}

	// --- fixtures -------------------------------------------------------

	private User newUser(String username, String email) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPasswordHash("not-a-real-hash");
		return user;
	}

	private Category newCategory(String name, String slug) {
		Category category = new Category();
		category.setName(name);
		category.setSlug(slug);
		return category;
	}

	private Tag newTag(String name, String slug) {
		Tag tag = new Tag();
		tag.setName(name);
		tag.setSlug(slug);
		return tag;
	}

	private Article newArticle(String title, String slug) {
		Article article = new Article();
		article.setTitle(title);
		article.setSlug(slug);
		article.setContent("Some body text.");
		article.setAuthor(author);
		article.setStatus(ArticleStatus.PUBLISHED);
		article.setPublishedAt(Instant.now());
		return article;
	}

	private Comment newComment(Article article, User commentAuthor, String content) {
		Comment comment = new Comment();
		comment.setArticle(article);
		comment.setAuthor(commentAuthor);
		comment.setContent(content);
		return comment;
	}

	private ArticleLike newLike(Article article, User user) {
		ArticleLike like = new ArticleLike();
		like.setArticle(article);
		like.setUser(user);
		return like;
	}

	private Subscription newSubscription(User follower, User followed) {
		Subscription subscription = new Subscription();
		subscription.setFollower(follower);
		subscription.setAuthor(followed);
		return subscription;
	}
}
