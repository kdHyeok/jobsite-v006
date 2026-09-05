<script setup lang="ts">
import type { Company } from '../types/company'
import { companySizeLabels } from '../types/company'

defineProps<{
  companies: Company[]
  selectedId: string | null
}>()

const emit = defineEmits<{
  select: [company: Company]
}>()

/** 카드에는 업종을 두 개까지만. 나머지는 개수로 접는다 — docs/design-system.md 규칙 3. */
const VISIBLE_INDUSTRIES = 2
</script>

<template>
  <section class="card-grid" aria-label="기업 목록">
    <button
      v-for="company in companies"
      :key="company.id"
      type="button"
      class="card"
      :class="{ selected: selectedId === company.id }"
      @click="emit('select', company)"
    >
      <span class="card__top">
        <span class="card__title">{{ company.name }}</span>
        <span v-if="company.companySize" class="chip" data-tone="primary">
          {{ companySizeLabels[company.companySize] }}
        </span>
      </span>
      <span v-if="company.industries.length" class="card__chips">
        <span v-for="industry in company.industries.slice(0, VISIBLE_INDUSTRIES)" :key="industry" class="chip">
          {{ industry }}
        </span>
        <span v-if="company.industries.length > VISIBLE_INDUSTRIES" class="chip">
          +{{ company.industries.length - VISIBLE_INDUSTRIES }}
        </span>
      </span>
      <span v-else class="card__sub">업종 미입력</span>
    </button>
  </section>
</template>
