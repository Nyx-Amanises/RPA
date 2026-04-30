import { readonly, reactive } from 'vue'

export const DEFAULT_PAGE_FORBIDDEN_MESSAGE = '\u65e0\u6743\u9650\u8bbf\u95ee\u5f53\u524d\u9875\u9762'

const state = reactive({
  deniedPath: '',
  message: '',
  version: 0
})

export const pageAccessState = readonly(state)

export function setPageForbidden(path, message = '') {
  state.deniedPath = path || ''
  state.message = message || DEFAULT_PAGE_FORBIDDEN_MESSAGE
}

export function clearPageForbidden(path = '') {
  if (!path || state.deniedPath === path) {
    state.deniedPath = ''
    state.message = ''
  }

  state.version += 1
}
