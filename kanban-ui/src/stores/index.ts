import { createPinia } from 'pinia'

const pinia = createPinia()

export default pinia

export { useUserStore } from './user'
export { useOrgStore } from './org'
export { useAppStore } from './app'
