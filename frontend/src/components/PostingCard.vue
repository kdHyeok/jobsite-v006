<script setup lang="ts">
import type { JobPosting } from '../types/posting'
import { ddayLabel, ddayTone, employmentTypeLabels, formatDeadline, stageLabels } from '../types/posting'

defineProps<{ posting: JobPosting; compact?: boolean }>()
</script>

<template>
  <article class="posting-card" :class="{ compact }" :data-tone="ddayTone(posting.deadlineAt)">
    <div class="posting-card__head">
      <div class="posting-card__title">
        <strong>{{ posting.position }}</strong>
        <span class="posting-card__company">{{ posting.companyName || '회사 미입력' }}</span>
      </div>
      <span class="dday" :data-tone="ddayTone(posting.deadlineAt)">{{ ddayLabel(posting.deadlineAt) }}</span>
    </div>

    <div class="posting-card__tags">
      <span class="status-pill" :data-stage="posting.stage">{{ stageLabels[posting.stage] }}</span>
      <span class="tag">{{ employmentTypeLabels[posting.employmentType] }}</span>
      <span v-if="posting.workLocation" class="tag">{{ posting.workLocation }}</span>
      <span v-if="posting.headcount" class="tag">{{ posting.headcount }}</span>
    </div>

    <p class="posting-card__deadline">서류마감 · {{ formatDeadline(posting.deadlineAt) }}</p>

    <template v-if="!compact">
      <dl class="posting-card__facts">
        <div v-if="posting.qualifications">
          <dt>지원자격</dt>
          <dd>{{ posting.qualifications }}</dd>
        </div>
        <div v-if="posting.responsibilities">
          <dt>담당업무</dt>
          <dd>{{ posting.responsibilities }}</dd>
        </div>
        <div v-if="posting.requiredSkills">
          <dt>요구역량</dt>
          <dd>{{ posting.requiredSkills }}</dd>
        </div>
      </dl>
      <a v-if="posting.postingUrl" class="posting-card__link" :href="posting.postingUrl" target="_blank" rel="noreferrer">
        공고 열기 ↗
      </a>
    </template>

    <div v-if="$slots.actions" class="posting-card__actions">
      <slot name="actions" />
    </div>
  </article>
</template>
