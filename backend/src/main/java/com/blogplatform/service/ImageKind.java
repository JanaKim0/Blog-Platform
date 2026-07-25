package com.blogplatform.service;

/**
 * The two places images are used, and how large each is allowed to stay. An
 * avatar shown at 48px does not need to be a 4000px photo, so uploads are
 * scaled down to these bounds before they are stored.
 */
public enum ImageKind {

	AVATAR(512),
	COVER(1600);

	private final int maxDimension;

	ImageKind(int maxDimension) {
		this.maxDimension = maxDimension;
	}

	/** Longest allowed side, in pixels. */
	public int maxDimension() {
		return maxDimension;
	}
}
