<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Position, ReferenceItem, ReferenceKind, ReferencePayload } from '../types/position'
import { referenceKindLabels } from '../types/position'
import { ddayLabel, ddayTone, formatDeadlineParts, statusLabels } from '../types/posting'

/**
 * 직무 상세 + 참고 정보 스트립. 추가는 "기존에서 검색" 또는 "새로 만들기".
 * API 호출은 부모(PositionBoard)가 한다 — 여기는 어떤 id 를 붙이고 뗄지만 말한다.
 */
const props = defineProps<{
  position: Position
  allReferences: ReferenceItem[]
  allPositions: Position[]
  busy: boolean
}>()

const emit = defineEmits<{
  attach: [referenceId: string]
  /** 이 직무에서만 뗀다. 참고 정보 자체는 남는다. */
  detach: [referenceId: string]
  create: [payload: ReferencePayload]
  update: [referenceId: string, payload: ReferencePayload]
  /** 참고 정보를 아예 지운다 — 붙어 있는 모든 직무에서 사라진다. 확인은 부모가 받는다. */
  remove: [item: ReferenceItem]
  /** 참고 직무 카드에서 그 직무로 건너뛴다. 같은 화면이라 라우팅은 없다. */
  openPosition: [positionId: string]
  /** 역방향 — 이 직무가 속한 채용공고로 건너뛴다. */
  openPosting: [postingId: string]
}>()

/** 만들기와 수정이 같은 폼을 쓴다. editingId 가 있으면 수정. */
const picker = ref<'closed' | 'search' | 'form'>('closed')
const editingId = ref<string | null>(null)
const query = ref('')
const emptyDraft = (): ReferencePayload => ({ kind: 'ARTICLE', title: '', url: '', memo: '', relatedPositionId: null })
const draft = ref<ReferencePayload>(emptyDraft())

const attachedIds = computed(() => new Set(props.position.references.map((r) => r.id)))

/** 이미 붙은 건 빼고, 검색어로 제목·메모를 거른다. */
const candidates = computed(() => {
  const needle = query.value.trim().toLowerCase()
  return props.allReferences.filter((r) => !attachedIds.value.has(r.id)
    && (!needle || r.title.toLowerCase().includes(needle) || (r.memo ?? '').toLowerCase().includes(needle)))
})

/** 참고 직무 후보: 나 자신은 빼고, "회사 · 직무" 로 보여준다. */
const otherPositions = computed(() => props.allPositions.filter((p) => p.id !== props.position.id))

function openNew() {
  editingId.value = null
  draft.value = emptyDraft()
  picker.value = 'form'
}

function openEdit(item: ReferenceItem) {
  editingId.value = item.id
  draft.value = {
    kind: item.kind,
    title: item.title,
    url: item.url ?? '',
    memo: item.memo ?? '',
    relatedPositionId: item.relatedPositionId,
  }
  picker.value = 'form'
}

function onRelatedChange(id: string) {
  draft.value.relatedPositionId = id || null
  const target = otherPositions.value.find((p) => p.id === id)
  // 제목을 비워 뒀으면 "회사 · 직무" 로 채운다. 사용자가 고치면 그대로 둔다.
  if (target && !draft.value.title.trim()) {
    draft.value.title = `${target.companyName ?? '회사 미입력'} · ${target.name}`
  }
}

function submitForm() {
  if (!draft.value.title.trim()) return
  const payload = { ...draft.value, title: draft.value.title.trim() }
  if (editingId.value) emit('update', editingId.value, payload)
  else emit('create', payload)
  picker.value = 'closed'
}

const sections: Array<[keyof Position, string]> = [
  ['responsibilities', '담당업무'],
  ['impact', '업무의 영향력'],
  ['requiredSkills', '요구 역량'],
  ['preferredSkills', '우대 역량'],
  ['growth', '성장 방향'],
  ['experience', '취득 경험'],
]
</script>

<template>
  <div>
    <section class="detail-section detail-chips">
      <span class="dday" :data-tone="ddayTone(position.deadlineAt)">{{ ddayLabel(position.deadlineAt) }}</span>
      <span v-if="position.status" class="chip" data-tone="primary">{{ statusLabels[position.status] }}</span>
      <span v-for="tech in position.techStack" :key="tech" class="chip">{{ tech }}</span>
    </section>

    <section class="detail-section">
      <dl class="detail-facts">
        <div>
          <dt>공고</dt>
          <dd>
            <button
              v-if="position.postingTitle"
              type="button"
              class="entity-link"
              @click="emit('openPosting', position.postingId)"
            >
              <span class="entity-icon" aria-hidden="true">▤</span>
              <span>({{ position.companyName ?? '회사 미입력' }}) {{ position.postingTitle }}</span>
              <span aria-hidden="true">↗</span>
            </button>
            <span v-else>—</span>
          </dd>
        </div>
        <div><dt>서류마감</dt><dd class="deadline-text"><span>{{ formatDeadlineParts(position.deadlineAt).year }}</span><strong>{{ formatDeadlineParts(position.deadlineAt).emphasized }}</strong></dd></div>
        <div><dt>소속 팀</dt><dd>{{ position.team || '—' }}</dd></div>
        <div><dt>담당 역할</dt><dd>{{ position.role || '—' }}</dd></div>
        <div><dt>모집인원</dt><dd>{{ position.headcount || '—' }}</dd></div>
        <div><dt>근무지역</dt><dd>{{ position.workLocation || '—' }}</dd></div>
      </dl>
    </section>

    <!-- 참고 정보: 가로 스크롤 카드 -->
    <section class="detail-section">
      <div class="strip-head">
        <p class="label">참고 정보 · {{ position.references.length }}</p>
        <div class="strip-tools">
          <button type="button" class="button ghost compact" :disabled="busy" @click="picker = picker === 'search' ? 'closed' : 'search'">기존에서 검색</button>
          <button type="button" class="button ghost compact" :disabled="busy" @click="picker === 'form' ? (picker = 'closed') : openNew()">새로 만들기</button>
        </div>
      </div>

      <div v-if="picker === 'search'" class="picker">
        <input v-model="query" class="search" placeholder="제목·메모로 검색" autofocus />
        <p v-if="candidates.length === 0" class="body-copy empty">붙일 수 있는 참고 정보가 없습니다.</p>
        <div v-else class="mini-list">
          <button v-for="item in candidates.slice(0, 8)" :key="item.id" type="button" class="mini-row" :disabled="busy" @click="emit('attach', item.id)">
            <span class="mini-row__text">
              <strong>{{ item.title }}</strong>
              <span>{{ referenceKindLabels[item.kind] }}</span>
            </span>
            <span class="chip" data-tone="primary">붙이기</span>
          </button>
        </div>
      </div>

      <form v-if="picker === 'form'" class="picker form-grid" @submit.prevent="submitForm">
        <label class="field">
          <span>종류</span>
          <select v-model="draft.kind">
            <option v-for="(label, value) in referenceKindLabels" :key="value" :value="value as ReferenceKind">{{ label }}</option>
          </select>
        </label>
        <label v-if="draft.kind === 'RELATED_POSITION'" class="field">
          <span>참고할 직무</span>
          <select :value="draft.relatedPositionId ?? ''" @change="onRelatedChange(($event.target as HTMLSelectElement).value)">
            <option value="">선택</option>
            <option v-for="p in otherPositions" :key="p.id" :value="p.id">{{ p.companyName ?? '회사 미입력' }} · {{ p.name }}</option>
          </select>
        </label>
        <label class="field full">
          <span>제목 <b>*</b></span>
          <input v-model="draft.title" maxlength="200" placeholder="예: 검색팀 3년차 인터뷰" />
        </label>
        <label class="field full">
          <span>링크</span>
          <input v-model="draft.url" maxlength="500" placeholder="https://" />
        </label>
        <label class="field full">
          <span>메모</span>
          <textarea v-model="draft.memo" rows="2" />
        </label>
        <div class="full picker__actions">
          <button type="button" class="button secondary compact" @click="picker = 'closed'">취소</button>
          <button type="submit" class="button primary compact" :disabled="busy || !draft.title.trim()">
            {{ editingId ? '저장' : '만들고 붙이기' }}
          </button>
        </div>
      </form>

      <p v-if="position.references.length === 0 && picker === 'closed'" class="body-copy empty">아직 붙인 참고 정보가 없습니다.</p>
      <div v-else-if="position.references.length" class="strip">
        <article v-for="item in position.references" :key="item.id" class="strip-card">
          <span class="chip">{{ referenceKindLabels[item.kind] }}</span>
          <strong>{{ item.title }}</strong>
          <p v-if="item.memo" class="strip-card__memo">{{ item.memo }}</p>
          <div class="strip-card__foot">
            <a v-if="item.url" :href="item.url" target="_blank" rel="noreferrer">열기 ↗</a>
            <button
              v-else-if="item.relatedPositionId"
              type="button"
              class="button ghost compact"
              @click="emit('openPosition', item.relatedPositionId)"
            >
              직무 열기
            </button>
            <span v-else />
            <span class="strip-card__tools">
              <button type="button" :disabled="busy" title="참고 정보를 고칩니다" @click="openEdit(item)">수정</button>
              <button type="button" :disabled="busy" title="이 직무에서만 뗍니다. 참고 정보는 남습니다" @click="emit('detach', item.id)">떼기</button>
              <button type="button" class="danger" :disabled="busy" title="붙어 있는 모든 직무에서 사라집니다" @click="emit('remove', item)">삭제</button>
            </span>
          </div>
        </article>
      </div>
    </section>

    <section v-for="[key, label] in sections" :key="key" class="detail-section">
      <p class="label">{{ label }}</p>
      <p class="body-copy" :class="{ empty: !position[key] }">{{ position[key] || '미입력' }}</p>
    </section>
  </div>
</template>
