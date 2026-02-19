import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export interface CardTab {
    /** 卡片 ID */
    cardId: string
    /** 卡片标题 */
    title: string
    /** 卡片编号 */
    code?: string
    /** 卡片状态 */
    cardStyle?: 'ACTIVE' | 'ARCHIVED' | 'DISCARDED'
    /** 是否正在加载 */
    loading?: boolean
}

export const useCardTabsStore = defineStore('cardTabs', () => {
    // 打开的卡片标签页列表
    const tabs = ref<CardTab[]>([])

    // 当前激活的标签页 ID
    const activeTabId = ref<string | null>(null)

    // 当前激活的标签页
    const activeTab = computed(() => {
        if (!activeTabId.value) return null
        return tabs.value.find((tab) => tab.cardId === activeTabId.value) || null
    })

    // 是否有打开的标签页
    const hasTabs = computed(() => tabs.value.length > 0)

    /**
     * 打开卡片详情标签页
     * 如果卡片已打开，则激活该标签页
     * 如果卡片未打开，则创建新标签页
     */
    function openTab(cardId: string, title?: string, code?: string) {
        const existingTab = tabs.value.find((tab) => tab.cardId === cardId)
        if (existingTab) {
            // 已存在，激活该标签页
            activeTabId.value = cardId
            // 更新标题（可能标题有变化）
            if (title) existingTab.title = title
            if (code) existingTab.code = code
        } else {
            // 创建新标签页
            tabs.value.push({
                cardId,
                title: title || '加载中...',
                code,
                loading: true,
            })
            activeTabId.value = cardId
        }
    }

    /**
     * 关闭标签页
     */
    function closeTab(cardId: string) {
        const index = tabs.value.findIndex((tab) => tab.cardId === cardId)
        if (index === -1) return

        tabs.value.splice(index, 1)

        // 如果关闭的是当前激活的标签页，切换到相邻标签页
        if (activeTabId.value === cardId) {
            if (tabs.value.length > 0) {
                // 优先切换到右侧标签页，否则切换到左侧
                const newIndex = Math.min(index, tabs.value.length - 1)
                activeTabId.value = tabs.value[newIndex]?.cardId || null
            } else {
                activeTabId.value = null
            }
        }
    }

    /**
     * 关闭所有标签页
     */
    function closeAllTabs() {
        tabs.value = []
        activeTabId.value = null
    }

    /**
     * 关闭其他标签页
     */
    function closeOtherTabs(cardId: string) {
        const tab = tabs.value.find((t) => t.cardId === cardId)
        if (tab) {
            tabs.value = [tab]
            activeTabId.value = cardId
        }
    }

    /**
     * 激活标签页
     */
    function activateTab(cardId: string) {
        if (tabs.value.some((tab) => tab.cardId === cardId)) {
            activeTabId.value = cardId
        }
    }

    /**
     * 更新标签页信息
     */
    function updateTab(cardId: string, updates: Partial<Omit<CardTab, 'cardId'>>) {
        const tab = tabs.value.find((t) => t.cardId === cardId)
        if (tab) {
            Object.assign(tab, updates)
        }
    }

    /**
     * 取消激活标签页（返回视图）
     */
    function deactivateTab() {
        activeTabId.value = null
    }

    return {
        tabs,
        activeTabId,
        activeTab,
        hasTabs,
        openTab,
        closeTab,
        closeAllTabs,
        closeOtherTabs,
        activateTab,
        updateTab,
        deactivateTab,
    }
})
