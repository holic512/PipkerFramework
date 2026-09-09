<!--
  @file LoginPulseVisual.vue
  @project Pipker Framework
  @module Frontend Authentication
  @description Renders the decorative ECharts projected globe shown beside the system login form.
  @logic Projects meridians and simplified continent dots into a decorative hemisphere using GraphChart and Canvas, keeps a blue palette on the dark visual panel, respects reduced motion, resizes with its host, and disposes browser resources on unmount.
  @dependencies Vue, Pinia theme store, Apache ECharts core, GraphChart, CanvasRenderer
  @index_tags authentication, login, echarts, globe, animation, accessibility, responsive, theme
  @author holic512
-->
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import type { ECharts } from 'echarts/core'
import { GraphChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { useThemeStore } from '../../../stores/theme'

echarts.use([GraphChart, CanvasRenderer])

const themeStore = useThemeStore()
const chartHost = ref<HTMLDivElement | null>(null)
const reducedMotion = ref(false)

let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null
let motionQuery: MediaQueryList | null = null

// Orthographic projection: the visible hemisphere faces Europe, Africa and Asia.
const networkNodes: { x: number; y: number; size: number }[] = []
const networkLinks: { source: number; target: number }[] = []
function project(longitude: number, latitude: number) {
  const lon = (longitude - 45) * Math.PI / 180
  const lat = latitude * Math.PI / 180
  return { x: 100 * Math.cos(lat) * Math.sin(lon), y: -100 * Math.sin(lat), size: 0 }
}
for (let latitude = -75; latitude <= 75; latitude += 15) {
  let previous = -1
  for (let longitude = -45; longitude <= 135; longitude += 3) {
    const index = networkNodes.push(project(longitude, latitude)) - 1
    if (previous >= 0) networkLinks.push({ source: previous, target: index })
    previous = index
  }
}
for (let longitude = -45; longitude <= 135; longitude += 15) {
  let previous = -1
  for (let latitude = -90; latitude <= 90; latitude += 3) {
    const index = networkNodes.push(project(longitude, latitude)) - 1
    if (previous >= 0) networkLinks.push({ source: previous, target: index })
    previous = index
  }
}
// Simplified decorative continental silhouettes; no external map requests.
const continents = [
  [[-17, 35], [10, 37], [34, 30], [43, 12], [51, 11], [40, -12], [32, -34], [18, -35], [9, -10], [-10, 5], [-17, 20]],
  [[-10, 36], [-10, 58], [7, 62], [20, 71], [38, 68], [45, 54], [30, 40], [15, 36]],
  [[30, 40], [45, 70], [100, 76], [135, 60], [135, 35], [120, 20], [106, 5], [94, 23], [78, 8], [68, 25], [45, 12], [35, 30]],
  [[113, -22], [130, -12], [135, -15], [135, -36], [116, -35]],
  [[47, -13], [51, -17], [47, -26], [44, -24]],
]
function inside(x: number, y: number, polygon: number[][]): boolean {
  let result = false
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const a = polygon[i]!, b = polygon[j]!
    if ((a[1]! > y) !== (b[1]! > y) && x < (b[0]! - a[0]!) * (y - a[1]!) / (b[1]! - a[1]!) + a[0]!) result = !result
  }
  return result
}
for (let lat = -54; lat <= 78; lat += 2.5) {
  for (let lon = -42; lon <= 133; lon += 2.5) {
    if (continents.some(polygon => inside(lon, lat, polygon))) {
      networkNodes.push({ ...project(lon, lat), size: 2.4 })
    }
  }
}

watch(
  [() => themeStore.designTheme, () => themeStore.colorScheme],
  () => renderChart(),
)

onMounted(() => {
  if (!chartHost.value) {
    return
  }

  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  reducedMotion.value = motionQuery.matches
  motionQuery.addEventListener('change', handleMotionPreferenceChange)

  chart = echarts.init(chartHost.value, undefined, {
    renderer: 'canvas',
    devicePixelRatio: Math.min(window.devicePixelRatio, 2),
  })
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartHost.value)
  renderChart()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  motionQuery?.removeEventListener('change', handleMotionPreferenceChange)
  chart?.dispose()
  chart = null
})

function handleMotionPreferenceChange(event: MediaQueryListEvent): void {
  reducedMotion.value = event.matches
  renderChart()
}

function renderChart(): void {
  if (!chart) {
    return
  }

  const palette = readPalette()
  chart.setOption({
    animation: !reducedMotion.value,
    animationDuration: 1_350,
    animationEasing: 'cubicOut',
    animationDelay: 0,
    series: [
      {
        type: 'graph',
        layout: 'none',
        left: '7%', right: '7%', top: '7%', bottom: '7%',
        roam: false,
        silent: true,
        cursor: 'default',
        symbol: 'circle',
        data: networkNodes.map((node) => ({
          ...node,
          name: '',
          symbolSize: node.size,
          itemStyle: {
            color: palette.node,
            shadowBlur: 0,
            shadowColor: palette.glow,
          },
        })),
        links: networkLinks,
        lineStyle: {
          color: palette.line,
          width: 1,
          opacity: 0.36,
          curveness: 0,
        },
        label: {
          show: false,
        },
        emphasis: {
          disabled: true,
        },
      },
    ],
  }, true)
}

function readPalette(): Record<'accent' | 'glow' | 'line' | 'node', string> {
  return {
    accent: '#75dcff',
    glow: 'rgba(74, 179, 230, 0.25)',
    line: '#4788b0',
    node: '#73c9ed',
  }
}
</script>

<template>
  <div class="login-pulse" aria-hidden="true">
    <div class="login-pulse__atmosphere"></div>
    <div ref="chartHost" class="login-pulse__chart"></div>
    <span class="login-pulse__orbit"></span>
    <span class="login-pulse__satellite"></span>
  </div>
</template>

<style scoped lang="scss">
.login-pulse { position: relative; aspect-ratio: 1; pointer-events: none; }
.login-pulse__atmosphere {
  position: absolute; inset: 7%; border-radius: 50%;
  background: radial-gradient(circle at 32% 30%, #16466b66, #0a203844 58%, #030b1766);
  border: 1px solid #6fc9ec55;
  box-shadow: inset -18px -12px 45px #020a1b88, inset 2px 2px 24px #53caff22, 0 0 65px #3aa1d21c;
}
.login-pulse__chart { position: absolute; inset: 0; }
.login-pulse__orbit { position: absolute; inset: 0; border: 1px solid #74c9ef33; border-radius: 50%; transform: rotate(-28deg) scaleY(.34); }
.login-pulse__satellite { position: absolute; top: 67%; left: 7%; width: 5px; height: 5px; background: #bbecff; border-radius: 50%; box-shadow: 0 0 14px 4px #57caff77; }
</style>
