import { marked } from 'marked'

export function renderMarkdown(text: string): string {
  try {
    return marked.parse(text || '') as string
  } catch {
    return text
  }
}
