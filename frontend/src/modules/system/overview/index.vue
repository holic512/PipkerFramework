<!--
  @file index.vue
  @project Pipker Framework
  @module System Overview
  @description Provides the componentKey target for the initial system/overview/index database menu.
  @logic Keeps the database component key stable while composing the existing overview surface as the first dynamically registered page.
  @dependencies OverviewPage
  @index_tags system-overview, dynamic-route, component-key
  @author holic512
-->
<script setup lang="ts">
import OverviewPage from '../../overview/pages/OverviewPage.vue'
</script>

<template>
  <OverviewPage />
</template>
