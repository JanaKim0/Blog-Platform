package com.blogplatform.service;

import com.blogplatform.config.StorageProperties;
import com.blogplatform.exception.BadRequestException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded pictures on disk and hands back the URL they are served
 * under.
 * <p>
 * Every upload is decoded and re-encoded rather than copied through. That does
 * three useful things at once: a file that only claims to be an image is
 * rejected because it cannot be decoded, oversized photos are scaled down, and
 * whatever metadata the original carried (camera model, GPS coordinates) is
 * dropped on the way out.
 */
@Service
public class ImageStorageService {

	private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

	/** Formats {@code ImageIO} can decode without extra plugins. */
	private static final Set<String> ALLOWED_CONTENT_TYPES =
			Set.of("image/jpeg", "image/pjpeg", "image/png", "image/gif");

	private static final String PUBLIC_PREFIX = "/uploads/";

	private final Path root;

	public ImageStorageService(StorageProperties properties) {
		this.root = Paths.get(properties.location()).toAbsolutePath().normalize();
		try {
			Files.createDirectories(root);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot create the upload directory " + root, ex);
		}
	}

	/**
	 * Validates, shrinks and saves {@code file}.
	 *
	 * @return the public URL of the stored image, e.g. {@code /uploads/a1/a1b2....jpg}
	 */
	public String store(MultipartFile file, ImageKind kind) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("No file was uploaded");
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new BadRequestException("Only JPEG, PNG and GIF images are supported");
		}

		BufferedImage original = decode(file);
		BufferedImage prepared = flattenAndScale(original, kind.maxDimension());

		String fileName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
		String folder = fileName.substring(0, 2);
		Path directory = root.resolve(folder);
		try {
			Files.createDirectories(directory);
			try (OutputStream out = Files.newOutputStream(directory.resolve(fileName))) {
				if (!ImageIO.write(prepared, "jpg", out)) {
					throw new IllegalStateException("No JPEG writer is available");
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not save the uploaded image", ex);
		}
		return PUBLIC_PREFIX + folder + "/" + fileName;
	}

	/**
	 * Deletes a previously stored image, given the URL that {@link #store} returned.
	 * Missing files and URLs pointing outside the upload directory are ignored -
	 * a stale reference should not break the request that is replacing it.
	 */
	public void delete(String publicUrl) {
		if (!StringUtils.hasText(publicUrl) || !publicUrl.startsWith(PUBLIC_PREFIX)) {
			return;
		}
		Path target = root.resolve(publicUrl.substring(PUBLIC_PREFIX.length())).normalize();
		if (!target.startsWith(root)) {
			log.warn("Refusing to delete {} - it is outside the upload directory", publicUrl);
			return;
		}
		try {
			Files.deleteIfExists(target);
		}
		catch (IOException ex) {
			log.warn("Could not delete the old image {}", publicUrl, ex);
		}
	}

	private BufferedImage decode(MultipartFile file) {
		try (InputStream in = file.getInputStream()) {
			BufferedImage image = ImageIO.read(in);
			if (image == null) {
				throw new BadRequestException("The uploaded file is not a readable image");
			}
			return image;
		}
		catch (IOException ex) {
			throw new BadRequestException("The uploaded file could not be read");
		}
	}

	/**
	 * Scales the image to fit within {@code maxDimension} (never up) and draws it
	 * on an opaque white background, because JPEG has no transparency and a
	 * transparent PNG would otherwise come out with black patches.
	 */
	private BufferedImage flattenAndScale(BufferedImage source, int maxDimension) {
		int width = source.getWidth();
		int height = source.getHeight();
		double scale = Math.min(1.0, (double) maxDimension / Math.max(width, height));
		int targetWidth = Math.max(1, (int) Math.round(width * scale));
		int targetHeight = Math.max(1, (int) Math.round(height * scale));

		BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = target.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, targetWidth, targetHeight);
			graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
		}
		finally {
			graphics.dispose();
		}
		return target;
	}
}
