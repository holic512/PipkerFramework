/**
 * @file theme-transition.ts
 * @project Pipker Framework
 * @module Frontend Theme Transition
 * @description Coordinates interruptible, accessible full-screen theme reveals.
 * @logic Cancels superseded snapshots, commits only the latest request, and falls back to an immediate update when motion is unavailable.
 * @dependencies Vue nextTick, browser View Transitions and Web Animations APIs
 * @index_tags theme, animation, accessibility
 * @author holic512
 */
import { nextTick } from 'vue'

export interface ThemeOrigin { x: number; y: number }

let revision = 0
let activeTransition: ViewTransition | undefined

export async function transitionTheme(update: () => void, origin?: ThemeOrigin): Promise<void> {
  const request = ++revision
  activeTransition?.skipTransition()
  activeTransition = undefined
  const root = document.documentElement
  root.classList.remove('theme-transitioning')
  let animation: Animation | undefined
  let committed = false
  const commit = async () => {
    if (request !== revision || committed) return
    committed = true
    update()
    await nextTick()
  }

  if (!document.startViewTransition || window.matchMedia('(prefers-reduced-motion: reduce)').matches
    || document.visibilityState === 'hidden') {
    await commit()
    return
  }

  const x = Math.min(window.innerWidth, Math.max(0, origin?.x ?? window.innerWidth / 2))
  const y = Math.min(window.innerHeight, Math.max(0, origin?.y ?? window.innerHeight / 2))
  const radius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))
  root.classList.add('theme-transitioning')
  try {
    const transition = document.startViewTransition(commit)
    activeTransition = transition
    // Attach rejection handlers immediately, including when a newer request skips the snapshot.
    const finished = transition.finished.catch(() => undefined)
    await transition.ready
    if (request !== revision) return
    animation = root.animate(
      { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
      { duration: 480, easing: 'cubic-bezier(0.22, 1, 0.36, 1)', pseudoElement: '::view-transition-new(root)', fill: 'both' },
    )
    await animation.finished
    await finished
  } catch {
    if (request === revision) {
      activeTransition?.skipTransition()
      await commit()
    }
  } finally {
    animation?.cancel()
    if (request === revision) {
      activeTransition = undefined
      root.classList.remove('theme-transitioning')
    }
  }
}
