<!--
  @file ThemeSwitcher.vue
  @project Pipker Framework
  @module Frontend Theme Switcher
  @description Renders a shared compact selector for the active design language and light or dark color scheme.
  @logic Reads and updates the global Pinia theme state so all visible application surfaces switch together.
  @dependencies Vue, Pinia theme store
  @index_tags component, theme-switcher, claude, element-plus, dark-mode
  @author holic512
-->
<script setup lang="ts">
import { useThemeStore, type ColorScheme, type DesignTheme } from '../stores/theme'

const themeStore = useThemeStore()

const designOptions: Array<{ value: DesignTheme; label: string }> = [
  { value: 'claude', label: 'Claude' },
  { value: 'element', label: 'Element' },
]

const colorOptions: Array<{ value: ColorScheme; label: string }> = [
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
]
</script>

<template>
  <details class="theme-switcher">
    <summary :aria-label="`切换界面主题，当前为 ${themeStore.themeLabel}`">
      <span class="theme-switcher__mark" aria-hidden="true">◐</span>
      <span class="theme-switcher__label">{{ themeStore.themeLabel }}</span>
    </summary>

    <div class="theme-switcher__panel">
      <div class="theme-switcher__group" aria-label="设计语言">
        <span class="theme-switcher__caption">设计语言</span>
        <div class="theme-switcher__options">
          <button
            v-for="option in designOptions"
            :key="option.value"
            :aria-pressed="themeStore.designTheme === option.value"
            type="button"
            @click="themeStore.setDesignTheme(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <div class="theme-switcher__group" aria-label="色彩模式">
        <span class="theme-switcher__caption">色彩模式</span>
        <div class="theme-switcher__options">
          <button
            v-for="option in colorOptions"
            :key="option.value"
            :aria-pressed="themeStore.colorScheme === option.value"
            type="button"
            @click="themeStore.setColorScheme(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </div>
  </details>
</template>
