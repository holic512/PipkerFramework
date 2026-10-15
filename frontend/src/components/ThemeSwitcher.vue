<!--
  @file ThemeSwitcher.vue
  @project Pipker Framework
  @module Frontend Theme Switcher
  @description Renders the shared full or icon-only selector for the active design language and light or dark color scheme.
  @logic Teleports a viewport-positioned panel, shares palette options with the store, anchors reveals to the selected control, and closes on outside pointer, focus exit, window blur or Escape with listener cleanup.
  @dependencies Vue, Pinia theme store
  @index_tags component, theme-switcher, claude, element-plus, dark-mode
  @author holic512
-->
<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, useId } from 'vue'
import { DESIGN_THEMES, useThemeStore, type ColorScheme, type DesignTheme } from '../stores/theme'

withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false,
})

const themeStore = useThemeStore()

const switcher = ref<HTMLDetailsElement>()
const panelId = useId()
const panel = ref<HTMLDivElement>()
const isOpen = ref(false)
const panelStyle = ref<Record<string, string>>({})

function contains(target: EventTarget | null): boolean {
  return target instanceof Node && Boolean(switcher.value?.contains(target) || panel.value?.contains(target))
}

function onTriggerKeydown(event: KeyboardEvent): void {
  if (isOpen.value && event.key === 'Tab' && !event.shiftKey) {
    event.preventDefault()
    panel.value?.querySelector('button')?.focus()
  }
}

function onPanelKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Tab') return
  if (event.shiftKey && event.target === panel.value?.querySelector('button')) {
    event.preventDefault()
    switcher.value?.querySelector('summary')?.focus()
  } else if (!event.shiftKey && event.target === panel.value?.querySelector('.theme-switcher__group:last-child button:last-child')) {
    event.preventDefault()
    const controls = Array.from(document.querySelectorAll<HTMLElement>(
      'a[href], button, input, select, textarea, summary, [tabindex]',
    )).filter(element => element.tabIndex >= 0 && !element.matches(':disabled')
      && element.getClientRects().length > 0 && !panel.value?.contains(element))
    const summary = switcher.value?.querySelector('summary')
    const next = controls[controls.indexOf(summary as HTMLElement) + 1]
    close()
    const target = next ?? summary
    target?.focus()
  }
}

function onToggle(): void {
  isOpen.value = Boolean(switcher.value?.open)
  if (!isOpen.value || !switcher.value) return
  const bounds = switcher.value.getBoundingClientRect()
  const width = Math.min(272, window.innerWidth - 24)
  const below = window.innerHeight - bounds.bottom - 20
  const above = bounds.top - 20
  const upwards = below < 270 && above > below
  panelStyle.value = {
    width: `${width}px`,
    left: `${Math.max(12, Math.min(bounds.right - width, window.innerWidth - width - 12))}px`,
    ...(upwards ? { bottom: `${window.innerHeight - bounds.top + 8}px` } : { top: `${bounds.bottom + 8}px` }),
    maxHeight: `${Math.max(80, upwards ? above : below)}px`,
  }
}
const designOptions = DESIGN_THEMES

function close(restoreFocus = false): void {
  if (!switcher.value?.open) return
  switcher.value.open = false
  isOpen.value = false
  if (restoreFocus) switcher.value.querySelector('summary')?.focus()
}

function onPointerDown(event: PointerEvent): void {
  if (!contains(event.target)) close()
}

function onFocusOut(event: FocusEvent): void {
  if (!contains(event.relatedTarget)) close()
}

function onWindowBlur(): void { close() }
function onScroll(event: Event): void {
  if (!contains(event.target)) close()
}

function selectTheme(event: MouseEvent, option: DesignTheme | ColorScheme): void {
  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const origin = event.detail === 0
    ? { x: bounds.left + bounds.width / 2, y: bounds.top + bounds.height / 2 }
    : { x: event.clientX, y: event.clientY }
  close(true)
  if (option === 'light' || option === 'dark') void themeStore.setColorScheme(option, origin)
  else void themeStore.setDesignTheme(option, origin)
}

onMounted(() => {
  document.addEventListener('pointerdown', onPointerDown, true)
  window.addEventListener('blur', onWindowBlur)
  window.addEventListener('resize', onWindowBlur)
  document.addEventListener('scroll', onScroll, true)
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown, true)
  window.removeEventListener('blur', onWindowBlur)
  window.removeEventListener('resize', onWindowBlur)
  document.removeEventListener('scroll', onScroll, true)
})

const colorOptions: Array<{ value: ColorScheme; label: string }> = [
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
]
</script>

<template>
  <details ref="switcher" class="theme-switcher" :class="{ 'theme-switcher--compact': compact }"
    @toggle="onToggle" @focusout="onFocusOut" @keydown.esc.stop.prevent="close(true)">
    <summary @keydown="onTriggerKeydown" :aria-controls="panelId" :aria-label="`切换界面主题，当前为 ${themeStore.themeLabel}`">
      <span class="theme-switcher__mark" aria-hidden="true">◐</span>
      <span v-if="!compact" class="theme-switcher__label">{{ themeStore.themeLabel }}</span>
    </summary>

    <Teleport to="body">
      <div v-if="isOpen" :id="panelId" ref="panel" class="theme-switcher__panel" :style="panelStyle"
        @focusout="onFocusOut" @keydown="onPanelKeydown" @keydown.esc.stop.prevent="close(true)">
        <div class="theme-switcher__group" role="group" aria-label="主题配色">
          <span class="theme-switcher__caption">主题配色</span>
          <div class="theme-switcher__options">
            <button
              v-for="option in designOptions"
              :key="option.value"
              :aria-pressed="themeStore.designTheme === option.value"
              type="button"
              @click="selectTheme($event, option.value)"
            >
              <span class="theme-switcher__swatch" :style="{ background: option.swatch }" aria-hidden="true" />
              {{ option.label }}
            </button>
          </div>
        </div>

        <div class="theme-switcher__group" role="group" aria-label="色彩模式">
          <span class="theme-switcher__caption">色彩模式</span>
          <div class="theme-switcher__options">
            <button
              v-for="option in colorOptions"
              :key="option.value"
              :aria-pressed="themeStore.colorScheme === option.value"
              type="button"
              @click="selectTheme($event, option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </details>
</template>
