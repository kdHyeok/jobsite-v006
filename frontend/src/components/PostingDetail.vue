<script setup lang="ts">
import type { ApplicationStage, JobPosting } from '../types/posting'
import { employmentTypeLabels, formatDeadline, stageLabels } from '../types/posting'

// 수정·보관·삭제 버튼은 드로어 하단이 가진다. 지원단계만 여기서 바로 바꾼다 — 가장 자주 하는 조작이라.
defineProps<{ posting: JobPosting }>()
const emit = defineEmits<{ changeStage: [ApplicationStage] }>()
</script>

<template>
  <div>
    <section class="detail-section">
      <label class="field">
        <span>지원단계</span>
        <select
          :value="posting.stage"
          @change="emit('changeStage', ($event.target as HTMLSelectElement).value as ApplicationStage)"
        >
          <option v-for="(label, value) in stageLabels" :key="value" :value="value">{{ label }}</option>
        </select>
      </label>
    </section>

    <section class="detail-section">
      <dl class="detail-facts">
        <div>
          <dt>서류마감</dt>
          <dd>{{ formatDeadline(posting.deadlineAt) }}</dd>
        </div>
        <div>
          <dt>고용형태</dt>
          <dd>{{ employmentTypeLabels[posting.employmentType] }}</dd>
        </div>
        <div>
          <dt>모집인원</dt>
          <dd>{{ posting.headcount || '—' }}</dd>
        </div>
        <div>
          <dt>근무지역</dt>
          <dd>{{ posting.workLocation || '—' }}</dd>
        </div>
        <div>
          <dt>공고링크</dt>
          <dd>
            <a v-if="posting.postingUrl" :href="posting.postingUrl" target="_blank" rel="noreferrer">열기 ↗</a>
            <span v-else>—</span>
          </dd>
        </div>
      </dl>
    </section>

    <section class="detail-section">
      <p class="label">지원자격</p>
      <p class="body-copy" :class="{ empty: !posting.qualifications }">
        {{ posting.qualifications || '미입력' }}
      </p>
    </section>

    <section class="detail-section">
      <p class="label">담당업무</p>
      <p class="body-copy" :class="{ empty: !posting.responsibilities }">
        {{ posting.responsibilities || '미입력' }}
      </p>
    </section>

    <section class="detail-section">
      <p class="label">요구역량</p>
      <p class="body-copy" :class="{ empty: !posting.requiredSkills }">
        {{ posting.requiredSkills || '미입력' }}
      </p>
    </section>
  </div>
</template>
