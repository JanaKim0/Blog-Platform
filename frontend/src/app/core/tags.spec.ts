import { formatTagInput, parseTagInput } from './tags';

describe('parseTagInput', () => {
  it('splits on commas and trims the pieces', () => {
    expect(parseTagInput('Java, Spring Boot ,  Testing')).toEqual([
      'Java',
      'Spring Boot',
      'Testing',
    ]);
  });

  it('ignores empty pieces so a trailing comma is harmless', () => {
    expect(parseTagInput('Java,,Spring,')).toEqual(['Java', 'Spring']);
    expect(parseTagInput('   ')).toEqual([]);
    expect(parseTagInput('')).toEqual([]);
  });

  it('strips a leading hash, because people type tags that way', () => {
    expect(parseTagInput('#Java, ##Spring')).toEqual(['Java', 'Spring']);
  });

  it('drops repeats regardless of case', () => {
    expect(parseTagInput('Java, java, JAVA')).toEqual(['Java']);
  });

  it('stops at ten tags, which is the server limit', () => {
    const many = Array.from({ length: 15 }, (_, i) => `tag${i}`).join(', ');
    expect(parseTagInput(many)).toHaveLength(10);
  });

  it('round-trips through the editable line', () => {
    const tags = ['Java', 'Spring Boot'];
    expect(parseTagInput(formatTagInput(tags))).toEqual(tags);
  });
});
