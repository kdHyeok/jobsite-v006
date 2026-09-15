export type InlineToken =
  | { kind: 'text'; text: string }
  | { kind: 'link'; text: string; href: string }

export type MarkdownBlock =
  | { kind: 'heading'; level: number; text: string }
  | { kind: 'paragraph'; text: string }
  | { kind: 'ul' | 'ol'; items: Array<{ text: string; indent: number; value?: number }> }

const inlinePattern = /\[([^\]\n]+)]\((https?:\/\/[^)\s]+)\)|(https?:\/\/[^\s<]+)/gi

export function inlineTokens(text: string): InlineToken[] {
  const tokens: InlineToken[] = []
  let cursor = 0

  for (const match of text.matchAll(inlinePattern)) {
    const index = match.index ?? 0
    if (index > cursor) tokens.push({ kind: 'text', text: text.slice(cursor, index) })

    const isNamed = Boolean(match[1])
    let href = isNamed ? match[2] : match[3]
    let trailing = ''
    if (!isNamed) {
      const trailingMatch = href.match(/[.,!?;:\])}]+$/)
      if (trailingMatch) {
        trailing = trailingMatch[0]
        href = href.slice(0, -trailing.length)
      }
    }

    tokens.push({ kind: 'link', text: isNamed ? match[1] : href, href })
    if (trailing) tokens.push({ kind: 'text', text: trailing })
    cursor = index + match[0].length
  }

  if (cursor < text.length) tokens.push({ kind: 'text', text: text.slice(cursor) })
  return tokens.length ? tokens : [{ kind: 'text', text }]
}

function indentLevel(whitespace: string): number {
  return Math.floor(whitespace.replace(/\t/g, '  ').length / 2)
}

export function parseMarkdown(source: string): MarkdownBlock[] {
  const blocks: MarkdownBlock[] = []
  let paragraph: string[] = []
  let listKind: 'ul' | 'ol' | null = null
  let listItems: Array<{ text: string; indent: number; value?: number }> = []

  const flushParagraph = () => {
    if (paragraph.length) blocks.push({ kind: 'paragraph', text: paragraph.join('\n') })
    paragraph = []
  }
  const flushList = () => {
    if (listKind) blocks.push({ kind: listKind, items: listItems })
    listKind = null
    listItems = []
  }

  for (const line of source.replace(/\r\n?/g, '\n').split('\n')) {
    if (!line.trim()) {
      flushParagraph()
      flushList()
      continue
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      flushParagraph()
      flushList()
      blocks.push({ kind: 'heading', level: heading[1].length, text: heading[2] })
      continue
    }

    const unordered = line.match(/^([ \t]*)-\s+(.+)$/)
    const ordered = line.match(/^([ \t]*)(\d+)\.\s+(.+)$/)
    if (unordered || ordered) {
      flushParagraph()
      const kind = unordered ? 'ul' : 'ol'
      if (listKind !== kind) {
        flushList()
        listKind = kind
      }
      const match = unordered ?? ordered!
      listItems.push({
        text: unordered ? match[2] : match[3],
        indent: indentLevel(match[1]),
        ...(ordered ? { value: Number(match[2]) } : {}),
      })
      continue
    }

    flushList()
    paragraph.push(line)
  }

  flushParagraph()
  flushList()
  return blocks
}

export interface TextEdit { value: string; start: number; end: number }

export function indentSelection(value: string, start: number, end: number, reverse: boolean): TextEdit {
  const lineStart = value.lastIndexOf('\n', start - 1) + 1
  const effectiveEnd = end > start && value[end - 1] === '\n' ? end - 1 : end
  const lineEndIndex = value.indexOf('\n', effectiveEnd)
  const lineEnd = lineEndIndex === -1 ? value.length : lineEndIndex
  const selected = value.slice(lineStart, lineEnd)

  if (!reverse && start === end) {
    return {
      value: value.slice(0, start) + '  ' + value.slice(end),
      start: start + 2,
      end: start + 2,
    }
  }

  const lines = selected.split('\n')
  let removedBeforeStart = 0
  let delta = 0
  const edited = lines.map((line, index) => {
    if (!reverse) {
      delta += 2
      return `  ${line}`
    }
    const prefix = line.match(/^(?:\t| {1,2})/)?.[0] ?? ''
    delta -= prefix.length
    if (index === 0) removedBeforeStart = Math.min(prefix.length, start - lineStart)
    return line.slice(prefix.length)
  }).join('\n')

  return {
    value: value.slice(0, lineStart) + edited + value.slice(lineEnd),
    start: reverse ? Math.max(lineStart, start - removedBeforeStart) : start + 2,
    end: Math.max(lineStart, end + delta),
  }
}
