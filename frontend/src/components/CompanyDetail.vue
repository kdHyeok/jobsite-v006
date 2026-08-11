<script setup lang="ts">
import type { Company } from '../types/company'
import { statusLabels } from '../types/company'

defineProps<{ company: Company }>()
const emit = defineEmits<{ edit: []; delete: [] }>()

const formatDateTime = (value: string) =>
  new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
</script>

<template>
  <article class="detail-card">
    <div class="detail-card__head">
      <div>
        <p class="eyebrow">기업 프로필</p>
        <h2>{{ company.name }}</h2>
        <div class="detail-tags">
          <span class="status-pill" :data-status="company.status">{{ statusLabels[company.status] }}</span>
          <span>{{ company.industry || '산업 미입력' }}</span>
          <span>{{ company.location || '지역 미입력' }}</span>
        </div>
      </div>
      <div class="detail-actions">
        <button type="button" class="button secondary" @click="emit('edit')">수정</button>
        <button type="button" class="button danger" @click="emit('delete')">삭제</button>
      </div>
    </div>

    <div class="detail-grid">
      <section>
        <p class="label">핵심 요약</p>
        <p class="body-copy">{{ company.summary || '아직 기업 요약이 없습니다.' }}</p>
      </section>
      <section>
        <p class="label">지원 메모</p>
        <p class="body-copy preserve">{{ company.memo || '아직 개인 메모가 없습니다.' }}</p>
      </section>
      <section class="detail-meta">
        <div>
          <p class="label">웹사이트</p>
          <a v-if="company.websiteUrl" :href="company.websiteUrl" target="_blank" rel="noreferrer">링크 열기 ↗</a>
          <span v-else>미입력</span>
        </div>
        <div>
          <p class="label">최근 수정</p>
          <span>{{ formatDateTime(company.updatedAt) }}</span>
        </div>
      </section>
    </div>
  </article>
</template>
