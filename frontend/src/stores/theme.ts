/**
 * @file theme.ts
 * @project Pipker Framework
 * @module Frontend Theme State
 * @description Owns the selected design language and color scheme for the entire browser application.
 * @logic Restores one persisted preference object, reflects it on the HTML element, and keeps Element Plus dark variables aligned through the dark class.
 * @dependencies Pinia, Vue Composition API, browser localStorage, HTML document
 * @index_tags pinia, theme, claude, element-plus, dark-mode
 * @author holic512
 */

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type DesignTheme = 'claude' | 'element'
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
  const themeLabel = computed(() => `${designTheme.value === 'claude' ? 'Claude' : 'Element'} · ${colorScheme.value === 'light' ? '浅色' : '深色'}`)

  function initializeTheme(): void {
    const preference = readPreference()
    designTheme.value = preference.designTheme
    colorScheme.value = preference.colorScheme
    applyTheme(preference)
  }

  function setDesignTheme(nextDesignTheme: DesignTheme): void {
    updatePreference({
      designTheme: nextDesignTheme,
      colorScheme: colorScheme.value,
    })
  }

  function setColorScheme(nextColorScheme: ColorScheme): void {
    updatePreference({
      designTheme: designTheme.value,
      colorScheme: nextColorScheme,
    })
  }

  function toggleColorScheme(): void {
    setColorScheme(colorScheme.value === 'light' ? 'dark' : 'light')
  }

  function updatePreference(nextPreference: ThemePreference): void {
    designTheme.value = nextPreference.designTheme
    colorScheme.value = nextPreference.colorScheme
    persistPreference(nextPreference)
    applyTheme(nextPreference)
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
  return value === 'claude' || value === 'element'
}

function isColorScheme(value: unknown): value is ColorScheme {
  return value === 'light' || value === 'dark'
}
