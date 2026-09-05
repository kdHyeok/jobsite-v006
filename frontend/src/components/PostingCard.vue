<script setup lang="ts">
import type { ApplicationStage, JobPosting } from '../types/posting'
import { ddayLabel, ddayTone, stageLabels } from '../types/posting'

defineProps<{ posting: JobPosting; selected?: boolean }>()
const emit = defineEmits<{ select: [] }>()

/** 진행 정도에 맞춘 칩 색. 관심은 중립, 지원 이후는 파랑, 결과 대기는 초록. */
const stageTone: Record<ApplicationStage, string> = {
  INTERESTED: 'neutral',
  DRAFTING: 'warning',
  SUBMITTED: 'primary',
  CODING_TEST: 'primary',
  INTERVIEW: 'primary',
  AWAITING_RESULT: 'positive',
}
</script>

<template>
  <button type="button" class="card" :class="{ selected }" @click="emit('select')">
    <span class="card__top">
      <span class="card__title">{{ posting.position }}</span>
      <span class="dday" :data-tone="ddayTone(posting.deadlineAt)">{{ ddayLabel(posting.deadlineAt) }}</span>
    </span>
    <span class="card__sub">{{ posting.companyName || '회사 미입력' }}</span>
    <span class="card__chips">
      <span class="chip" :data-tone="stageTone[posting.stage]">{{ stageLabels[posting.stage] }}</span>
    </span>
  </button>
</template>
