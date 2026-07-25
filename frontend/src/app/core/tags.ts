/**
 * Tags are typed as one comma-separated line, which is far less fiddly than a
 * chip widget on a phone. The server matches tags by slug, so "Spring Boot" and
 * "spring boot" end up as the same tag - but sending the same spelling twice is
 * still worth avoiding here.
 */

const MAX_TAGS = 10;

export function parseTagInput(input: string): string[] {
  const seen = new Set<string>();
  const tags: string[] = [];

  for (const piece of input.split(',')) {
    const tag = piece.trim().replace(/^#+/, '').trim();
    if (tag === '') {
      continue;
    }
    const key = tag.toLowerCase();
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    tags.push(tag);
    if (tags.length === MAX_TAGS) {
      break;
    }
  }
  return tags;
}

/** Turns the tags of a loaded article back into a line the author can edit. */
export function formatTagInput(names: readonly string[]): string {
  return names.join(', ');
}
