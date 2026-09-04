<script setup lang="ts">
import PostingCard from './PostingCard.vue'
import type { Company } from '../types/company'
import { companySizeLabels, formatFoundedOn, formatRevenue } from '../types/company'

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
          <span v-if="company.companySize" class="status-pill">{{ companySizeLabels[company.companySize] }}</span>
          <span v-for="industry in company.industries" :key="industry" class="tag">{{ industry }}</span>
          <span v-if="company.industries.length === 0">업종 미입력</span>
        </div>
      </div>
      <div class="detail-actions">
        <button type="button" class="button secondary" @click="emit('edit')">수정</button>
        <button type="button" class="button danger" @click="emit('delete')">삭제</button>
      </div>
    </div>

    <div class="detail-grid">
      <section>
        <p class="label">간략 소개</p>
        <p class="body-copy">{{ company.summary || '아직 기업 소개가 없습니다.' }}</p>
      </section>
      <section>
        <p class="label">지원 메모</p>
        <p class="body-copy preserve">{{ company.memo || '아직 개인 메모가 없습니다.' }}</p>
      </section>

      <section class="fact-grid">
        <p class="label">기업 정보</p>
        <dl>
          <div>
            <dt>매출액</dt>
            <dd>{{ formatRevenue(company.annualRevenue) }}</dd>
          </div>
          <div>
            <dt>사원수</dt>
            <dd>{{ company.employeeCount === null ? '—' : `${company.employeeCount.toLocaleString('ko-KR')}명` }}</dd>
          </div>
          <div>
            <dt>설립연월</dt>
            <dd>{{ formatFoundedOn(company.foundedOn) }}</dd>
          </div>
          <div>
            <dt>주소</dt>
            <dd>{{ company.address || '—' }}</dd>
          </div>
          <div>
            <dt>홈페이지</dt>
            <dd>
              <a v-if="company.websiteUrl" :href="company.websiteUrl" target="_blank" rel="noreferrer">링크 열기 ↗</a>
              <span v-else>—</span>
            </dd>
          </div>
          <div>
            <dt>최근 수정</dt>
            <dd>{{ formatDateTime(company.updatedAt) }}</dd>
          </div>
        </dl>
      </section>

      <section class="posting-section">
        <p class="label">채용정보 <span class="count">{{ company.openPostings.length }}</span></p>
        <p v-if="company.openPostings.length === 0" class="body-copy">진행 중인 공고가 없습니다.</p>
        <div v-else class="posting-section__list">
          <PostingCard
            v-for="posting in company.openPostings"
            :key="posting.id"
            :posting="posting"
            compact
          />
        </div>
      </section>
    </div>
  </article>
</template>
