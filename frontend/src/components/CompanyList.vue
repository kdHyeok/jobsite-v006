<script setup lang="ts">
import type { Company } from '../types/company'
import { statusLabels } from '../types/company'

defineProps<{
  companies: Company[]
  selectedId: string | null
}>()

const emit = defineEmits<{
  select: [company: Company]
}>()

const formatDate = (value: string) =>
  new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric' }).format(new Date(value))
</script>

<template>
  <section class="company-list-panel" aria-label="기업 목록">
    <div v-if="companies.length === 0" class="empty-state compact">
      <strong>등록된 기업이 없습니다.</strong>
      <span>오른쪽 위 ‘기업 추가’로 첫 기업을 정리해 보세요.</span>
    </div>
    <button
      v-for="company in companies"
      :key="company.id"
      type="button"
      class="company-row"
      :class="{ active: selectedId === company.id }"
      :aria-pressed="selectedId === company.id"
      @click="emit('select', company)"
    >
      <span class="company-row__top">
        <strong>{{ company.name }}</strong>
        <span class="status-pill" :data-status="company.status">{{ statusLabels[company.status] }}</span>
      </span>
      <span class="company-row__meta">{{ company.industry || '산업 미입력' }} · {{ company.location || '지역 미입력' }}</span>
      <span class="company-row__date">최근 수정 {{ formatDate(company.updatedAt) }}</span>
    </button>
  </section>
</template>
