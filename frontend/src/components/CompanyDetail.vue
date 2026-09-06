<script setup lang="ts">
import type { Company } from '../types/company'
import { companySizeLabels, formatFoundedOn, formatRevenue } from '../types/company'
import { ddayLabel, ddayTone, statusLabels } from '../types/posting'

// 수정·삭제 버튼은 드로어 헤더가 가진다. 여기는 읽기 전용 본문.
defineProps<{ company: Company }>()

/** 채용정보 행을 더블클릭하면 채용공고 화면에서 그 공고를 연다. */
const emit = defineEmits<{ openPosting: [postingId: string] }>()
</script>

<template>
  <div>
    <section v-if="company.industries.length || company.companySize" class="detail-section detail-chips">
      <span v-if="company.companySize" class="chip" data-tone="primary">
        {{ companySizeLabels[company.companySize] }}
      </span>
      <span v-for="industry in company.industries" :key="industry" class="chip">{{ industry }}</span>
    </section>

    <section class="detail-section">
      <p class="label">간략 소개</p>
      <p class="body-copy" :class="{ empty: !company.summary }">
        {{ company.summary || '아직 기업 소개가 없습니다.' }}
      </p>
    </section>

    <section class="detail-section">
      <p class="label">기업 정보</p>
      <dl class="detail-facts">
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
      </dl>
    </section>

    <section class="detail-section">
      <p class="label">채용정보 · 마감 전 {{ company.openPostings.length }}건 <small>더블클릭하면 공고가 열립니다</small></p>
      <p v-if="company.openPostings.length === 0" class="body-copy empty">진행 중인 공고가 없습니다.</p>
      <div v-else class="mini-list">
        <button
          v-for="posting in company.openPostings"
          :key="posting.id"
          type="button"
          class="mini-row"
          title="더블클릭하면 채용공고 화면에서 열립니다"
          @dblclick="emit('openPosting', posting.id)"
          @keydown.enter.prevent="emit('openPosting', posting.id)"
        >
          <span class="mini-row__text">
            <strong>{{ posting.title }}</strong>
            <span>{{ statusLabels[posting.status] }}<template v-if="posting.positions.length > 1"> · 직무 {{ posting.positions.length }}</template></span>
          </span>
          <span class="dday" :data-tone="ddayTone(posting.deadlineAt)">{{ ddayLabel(posting.deadlineAt) }}</span>
        </button>
      </div>
    </section>

    <section class="detail-section">
      <p class="label">지원 메모</p>
      <p class="body-copy" :class="{ empty: !company.memo }">
        {{ company.memo || '아직 개인 메모가 없습니다.' }}
      </p>
    </section>
  </div>
</template>
