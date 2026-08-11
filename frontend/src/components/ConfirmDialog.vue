<script setup lang="ts">
defineProps<{ companyName: string; deleting: boolean }>()
const emit = defineEmits<{ confirm: []; cancel: [] }>()
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('cancel')">
    <section class="confirm-card" role="alertdialog" aria-modal="true" aria-label="기업 삭제 확인">
      <span class="danger-mark">!</span>
      <h2>기업 정보를 삭제할까요?</h2>
      <p><strong>{{ companyName }}</strong>의 기록이 PostgreSQL에서 영구 삭제됩니다.</p>
      <div class="modal-actions">
        <button type="button" class="button secondary" :disabled="deleting" @click="emit('cancel')">취소</button>
        <button type="button" class="button danger solid" :disabled="deleting" @click="emit('confirm')">
          {{ deleting ? '삭제 중…' : '삭제' }}
        </button>
      </div>
    </section>
  </div>
</template>
