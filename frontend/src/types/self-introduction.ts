export interface SelfIntroduction {
  id: string
  resumeId: string
  question: string
  answer: string | null
  createdAt: string
  updatedAt: string
}

export interface SelfIntroductionPayload {
  resumeId: string
  question: string
  answer: string
}
