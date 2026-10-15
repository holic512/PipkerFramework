/**
 * @file theme.ts
 * @project Pipker Framework
 * @module Frontend Theme State
 * @description Owns the selected design language and color scheme for the entire browser application.
 * @logic Restores one persisted preference object, coordinates interruptible transitions for all theme changes, and keeps HTML attributes and Element Plus dark variables aligned.
 * @dependencies Pinia, Vue Composition API, browser localStorage, HTML document
 * @index_tags pinia, theme, claude, element-plus, dark-mode
 * @author holic512
 */

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { transitionTheme, type ThemeOrigin } from '../utils/theme-transition'

export const DESIGN_THEMES = [
  { value: 'claude', label: 'Claude', swatch: '#595852' },
  { value: 'element', label: 'Element', swatch: '#1d6fc4' },
  { value: 'teal', label: '青瓷', swatch: '#0f766e' },
  { value: 'forest', label: '森林', swatch: '#2f7145' },
  { value: 'iris', label: '鸢尾', swatch: '#6d4bc3' },
  { value: 'rose', label: '玫瑰', swatch: '#b43f65' },
] as const

export type DesignTheme = typeof DESIGN_THEMES[number]['value']
export type ColorScheme = 'light' | 'dark'

interface ThemePreference {
  designTheme: DesignTheme
  colorScheme: ColorScheme
}

const STORAGE_KEY = 'pipker.ui.theme-preference'
const DEFAULT_PREFERENCE: ThemePreference = {
  designTheme: 'claude',
  colorScheme: 'light',
}

export const useThemeStore = defineStore('theme', () => {
  const designTheme = ref<DesignTheme>(DEFAULT_PREFERENCE.designTheme)
  const colorScheme = ref<ColorScheme>(DEFAULT_PREFERENCE.colorScheme)
  let requestedPreference = { ...DEFAULT_PREFERENCE }
  const themeLabel = computed(() => `${DESIGN_THEMES.find(option => option.value === designTheme.value)?.label} · ${colorScheme.value === 'light' ? '浅色' : '深色'}`)

  function initializeTheme(): void {
    const preference = readPreference()
    requestedPreference = preference
    designTheme.value = preference.designTheme
    colorScheme.value = preference.colorScheme
    applyTheme(preference)
  }

  function setDesignTheme(nextDesignTheme: DesignTheme, origin?: ThemeOrigin): Promise<void> {
    return updatePreference({ ...requestedPreference, designTheme: nextDesignTheme }, origin)
  }

  function setColorScheme(nextColorScheme: ColorScheme, origin?: ThemeOrigin): Promise<void> {
    return updatePreference({ ...requestedPreference, colorScheme: nextColorScheme }, origin)
  }

  function toggleColorScheme(origin?: ThemeOrigin): Promise<void> {
    return setColorScheme(requestedPreference.colorScheme === 'light' ? 'dark' : 'light', origin)
  }

  async function updatePreference(nextPreference: ThemePreference, origin?: ThemeOrigin): Promise<void> {
    if (nextPreference.designTheme === requestedPreference.designTheme
      && nextPreference.colorScheme === requestedPreference.colorScheme) return
    requestedPreference = nextPreference
    await transitionTheme(() => {
      designTheme.value = nextPreference.designTheme
      colorScheme.value = nextPreference.colorScheme
      persistPreference(nextPreference)
      applyTheme(nextPreference)
    }, origin)
  }

  return {
    designTheme,
    colorScheme,
    themeLabel,
    initializeTheme,
    setDesignTheme,
    setColorScheme,
    toggleColorScheme,
  }
})

function readPreference(): ThemePreference {
  try {
    const storedValue = window.localStorage.getItem(STORAGE_KEY)
    if (!storedValue) {
      return DEFAULT_PREFERENCE
    }
    const parsedValue = JSON.parse(storedValue) as Partial<ThemePreference>
    if (isDesignTheme(parsedValue.designTheme) && isColorScheme(parsedValue.colorScheme)) {
      return {
        designTheme: parsedValue.designTheme,
        colorScheme: parsedValue.colorScheme,
      }
    }
  } catch {
    // A blocked or malformed local preference should never prevent the application from starting.
  }
  return DEFAULT_PREFERENCE
}

function persistPreference(preference: ThemePreference): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(preference))
  } catch {
    // Keep the active in-memory preference when browser storage is unavailable.
  }
}

function applyTheme(preference: ThemePreference): void {
  const documentElement = document.documentElement
  documentElement.dataset.designTheme = preference.designTheme
  documentElement.dataset.colorScheme = preference.colorScheme
  documentElement.classList.toggle('dark', preference.colorScheme === 'dark')
}

function isDesignTheme(value: unknown): value is DesignTheme {
  return DESIGN_THEMES.some(option => option.value === value)
}

function isColorScheme(value: unknown): value is ColorScheme {
  return value === 'light' || value === 'dark'
}
