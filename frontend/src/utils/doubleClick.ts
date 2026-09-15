const interactive = 'a, button, input, select, textarea, label, [role="tab"]'

export function opensEditor(event: MouseEvent) {
  return event.target instanceof Element && !event.target.closest(interactive)
}
