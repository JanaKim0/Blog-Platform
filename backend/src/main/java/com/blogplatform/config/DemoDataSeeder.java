package com.blogplatform.config;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.ArticleLike;
import com.blogplatform.domain.ArticleStatus;
import com.blogplatform.domain.Comment;
import com.blogplatform.domain.Role;
import com.blogplatform.domain.Subscription;
import com.blogplatform.domain.User;
import com.blogplatform.repository.ArticleLikeRepository;
import com.blogplatform.repository.ArticleRepository;
import com.blogplatform.repository.CategoryRepository;
import com.blogplatform.repository.CommentRepository;
import com.blogplatform.repository.SubscriptionRepository;
import com.blogplatform.repository.UserRepository;
import com.blogplatform.service.TagService;
import com.blogplatform.util.Slugs;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with a few authors and articles, so that a freshly
 * cloned copy shows a working blog instead of an empty page.
 * <p>
 * Off unless {@code app.demo-data=true}, and it refuses to touch a database that
 * already has users - nobody wants invented accounts appearing next to real
 * ones.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	/** Written in the README so the demo accounts can actually be signed into. */
	private static final String DEMO_PASSWORD = "demo-password";

	private final UserRepository users;
	private final ArticleRepository articles;
	private final CategoryRepository categories;
	private final CommentRepository comments;
	private final ArticleLikeRepository likes;
	private final SubscriptionRepository subscriptions;
	private final TagService tagService;
	private final PasswordEncoder passwordEncoder;

	public DemoDataSeeder(UserRepository users, ArticleRepository articles,
			CategoryRepository categories, CommentRepository comments, ArticleLikeRepository likes,
			SubscriptionRepository subscriptions, TagService tagService,
			PasswordEncoder passwordEncoder) {
		this.users = users;
		this.articles = articles;
		this.categories = categories;
		this.comments = comments;
		this.likes = likes;
		this.subscriptions = subscriptions;
		this.tagService = tagService;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (users.count() > 0) {
			log.info("Demo data was asked for, but the database already has accounts - skipping");
			return;
		}

		User nora = createUser("nora", "Nora Kim", "Learning backends the long way round.");
		User mira = createUser("mira", "Mira Sol", "Designer. Opinions about contrast.");
		User lena = createUser("lena", "Lena Park", "Cooks in batches, writes about it afterwards.");

		Article spring = publish(nora, "How I learned Spring Boot",
				"Six weeks, one messenger and a lot of stack traces.",
				"""
				I started with a single controller and no idea what a bean was.

				By the end I had a working API, tests I trusted, and a much better sense of \
				when to stop reading documentation and start writing code.""",
				"programming", List.of("Java", "Spring Boot", "Learning"), 6);

		Article database = publish(nora, "Designing a database you will not regret",
				"Constraints are cheaper than bug reports.",
				"""
				Every unique constraint I added saved me an argument with myself later.

				The database is the last place that can say no, and it never gets tired.""",
				"programming", List.of("Databases", "PostgreSQL"), 4);

		Article pink = publish(mira, "Powder pink, and why contrast matters",
				"A soft palette does not have to be unreadable.",
				"""
				Pastels are lovely until you put white text on them.

				The fix is usually not a darker background - it is a darker label.""",
				"design", List.of("Colour", "Accessibility"), 3);

		publish(lena, "A week of cooking from one shopping list",
				"Seven dinners, one trip, no waste.",
				"""
				The trick turned out to be buying fewer things, not more.

				Everything was built around three base recipes, and nothing went in the bin.""",
				"food", List.of("Cooking"), 2);

		publish(lena, "Notes on switching careers at thirty",
				"Nobody tells you how long the middle takes.",
				"""
				The first month is exciting. The fourth is just work.

				That is the part that counts, and the part nobody writes about.""",
				"career", List.of("Career", "Learning"), 1);

		draft(nora, "Half-written thoughts on testing",
				"""
				Something about the difference between tests that describe behaviour and \
				tests that describe implementation. Not finished.""");

		like(spring, mira);
		like(spring, lena);
		like(pink, nora);
		like(database, lena);

		comment(spring, mira, "The stack traces part is painfully familiar.");
		comment(spring, lena, "Which chapter did you give up on documentation?");
		comment(spring, nora, "Roughly the one about auto-configuration.");
		comment(pink, lena, "The darker label idea is going straight into my project.");

		follow(mira, nora);
		follow(lena, nora);
		follow(lena, mira);

		log.info("Seeded demo data: 3 accounts, 6 articles. Password for all of them: {}",
				DEMO_PASSWORD);
	}

	private User createUser(String username, String displayName, String bio) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(username + "@example.com");
		user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
		user.setDisplayName(displayName);
		user.setBio(bio);
		user.setRole(Role.USER);
		user.setEnabled(true);
		return users.save(user);
	}

	private Article publish(User author, String title, String summary, String content,
			String categorySlug, List<String> tags, int daysAgo) {
		Article article = article(author, title, content);
		article.setSummary(summary);
		article.setCategory(categories.findBySlug(categorySlug).orElse(null));
		article.getTags().addAll(tagService.resolve(tags));
		article.setStatus(ArticleStatus.PUBLISHED);
		// Spread over the past week, so the feed has an order worth looking at.
		article.setPublishedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
		return articles.save(article);
	}

	private Article draft(User author, String title, String content) {
		Article article = article(author, title, content);
		article.setStatus(ArticleStatus.DRAFT);
		return articles.save(article);
	}

	private Article article(User author, String title, String content) {
		Article article = new Article();
		article.setAuthor(author);
		article.setTitle(title);
		article.setSlug(Slugs.slugify(title, "article", 200));
		article.setContent(content);
		return article;
	}

	private void like(Article article, User user) {
		ArticleLike like = new ArticleLike();
		like.setArticle(article);
		like.setUser(user);
		likes.save(like);
	}

	private void comment(Article article, User author, String content) {
		Comment comment = new Comment();
		comment.setArticle(article);
		comment.setAuthor(author);
		comment.setContent(content);
		comments.save(comment);
	}

	private void follow(User follower, User author) {
		Subscription subscription = new Subscription();
		subscription.setFollower(follower);
		subscription.setAuthor(author);
		subscriptions.save(subscription);
	}
}
