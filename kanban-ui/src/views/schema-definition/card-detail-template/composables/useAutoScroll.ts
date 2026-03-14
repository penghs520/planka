import { onMounted, onUnmounted } from 'vue'

export function useAutoScroll(containerRef: import('vue').Ref<HTMLElement | null>) {
    let autoScrollFrameId: number | null = null
    let autoScrollSpeed = 0

    function processAutoScroll() {
        if (autoScrollSpeed !== 0 && containerRef.value) {
            containerRef.value.scrollTop += autoScrollSpeed
            autoScrollFrameId = requestAnimationFrame(processAutoScroll)
        } else {
            autoScrollFrameId = null
        }
    }

    function handleAutoScroll(event: DragEvent) {
        if (!containerRef.value) return

        const rect = containerRef.value.getBoundingClientRect()
        const threshold = 80 // 距离边缘多少像素触发
        const maxSpeed = 15 // 最大滚动速度

        // 计算相对于视口的位置
        const clientY = event.clientY

        if (clientY < rect.top + threshold) {
            // 向上滚动
            const ratio = 1 - Math.max(0, (clientY - rect.top) / threshold)
            autoScrollSpeed = -maxSpeed * ratio
        } else if (clientY > rect.bottom - threshold) {
            // 向下滚动
            const ratio = 1 - Math.max(0, (rect.bottom - clientY) / threshold)
            autoScrollSpeed = maxSpeed * ratio
        } else {
            autoScrollSpeed = 0
        }

        if (autoScrollSpeed !== 0 && !autoScrollFrameId) {
            processAutoScroll()
        }
    }

    function stopAutoScroll() {
        autoScrollSpeed = 0
    }

    onMounted(() => {
        document.addEventListener('dragover', handleAutoScroll, true)
    })

    onUnmounted(() => {
        document.removeEventListener('dragover', handleAutoScroll, true)
        if (autoScrollFrameId) {
            cancelAnimationFrame(autoScrollFrameId)
        }
    })

    return {
        stopAutoScroll
    }
}
