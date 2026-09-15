export interface HighlightSegment { text: string; matched: boolean }

const escapePattern = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

export function highlightMatches(text: string | null, query: string): HighlightSegment[] {
  const terms = [...new Set(query.toLowerCase().match(/[\p{L}\p{N}]+/gu) ?? [])]
    .sort((left, right) => right.length - left.length)
  if (!text || terms.length === 0) return [{ text: text ?? '', matched: false }]

  const pattern = new RegExp(terms.map(escapePattern).join('|'), 'giu')
  const segments: HighlightSegment[] = []
  let cursor = 0
  for (const match of text.matchAll(pattern)) {
    const index = match.index ?? 0
    if (index > cursor) segments.push({ text: text.slice(cursor, index), matched: false })
    segments.push({ text: match[0], matched: true })
    cursor = index + match[0].length
  }
  if (cursor < text.length) segments.push({ text: text.slice(cursor), matched: false })
  return segments.length ? segments : [{ text, matched: false }]
}
