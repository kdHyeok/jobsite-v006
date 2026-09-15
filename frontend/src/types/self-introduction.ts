export interface SelfIntroduction {
  id: string
  resumeIds: string[]
  question: string
  answer: string | null
  createdAt: string
  updatedAt: string
}

export interface SelfIntroductionPayload {
  resumeIds: string[]
  question: string
  answer: string
}
