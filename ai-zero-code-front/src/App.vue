<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

import { sayHello } from '@/api/test.ts'

sayHello().then((res) => {
  console.log(res)
})
</script>

<template>
  <BasicLayout />
</template>
