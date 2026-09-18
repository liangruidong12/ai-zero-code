import { ref, type Ref } from 'vue'

/**
 * 可视化编辑 composable
 * 通过同域直接操作 iframe.contentDocument 实现元素选中
 */

export interface SelectedElement {
  tagName: string
  id: string
  className: string
  text: string
  cssSelector: string
  rect: { x: number; y: number; width: number; height: number }
  positionDesc: string
}

export function useVisualEditor(previewUrl: Ref<string>) {
  const editMode = ref(false)
  const selectedElement = ref<SelectedElement | null>(null)
  const iframeRef = ref<HTMLIFrameElement | null>(null)

  let hoverHandler: ((e: MouseEvent) => void) | null = null
  let clickHandler: ((e: MouseEvent) => void) | null = null
  let lastHovered: Element | null = null

  function isContainer(el: Element): boolean {
    const tag = el.tagName.toLowerCase()
    if (['html', 'body', 'head', 'document'].includes(tag)) return true
    const rect = el.getBoundingClientRect()
    const vw = window.innerWidth
    const vh = window.innerHeight
    if (rect.width > vw * 0.8 && rect.height > vh * 0.8) return true
    return false
  }

  function buildCssSelector(el: Element): string {
    const parts: string[] = []
    let current: Element | null = el
    let depth = 0
    while (current && current.tagName && depth < 5) {
      const tag = current.tagName.toLowerCase()
      const id = current.id ? `#${current.id}` : ''
      const cls = current.className && typeof current.className === 'string'
        ? `.${current.className.trim().split(/\s+/).slice(0, 2).join('.')}`
        : ''
      parts.unshift(`${tag}${id}${cls}`)
      current = current.parentElement
      depth++
    }
    return parts.join(' > ')
  }

  function describePosition(el: Element): string {
    const rect = el.getBoundingClientRect()
    const x = Math.round(rect.x)
    const y = Math.round(rect.y)
    const w = Math.round(rect.width)
    const h = Math.round(rect.height)

    let vPos = '页面中部'
    if (y < 200) vPos = '页面顶部'
    else if (y > window.innerHeight * 0.7) vPos = '页面底部'

    let hPos = '居中'
    if (x < window.innerWidth * 0.3) hPos = '左侧'
    else if (x > window.innerWidth * 0.7) hPos = '右侧'

    return `${vPos} ${hPos} (约 ${x}, ${y}, 宽 ${w}px, 高 ${h}px)`
  }

  function extractElementInfo(el: Element): SelectedElement {
    const text = (el.textContent || '').trim().slice(0, 50)
    const rect = el.getBoundingClientRect()
    return {
      tagName: el.tagName.toLowerCase(),
      id: el.id || '',
      className: typeof el.className === 'string' ? el.className.trim().slice(0, 80) : '',
      text,
      cssSelector: buildCssSelector(el),
      rect: {
        x: Math.round(rect.x),
        y: Math.round(rect.y),
        width: Math.round(rect.width),
        height: Math.round(rect.height),
      },
      positionDesc: describePosition(el),
    }
  }

  function onMouseMove(e: MouseEvent) {
    const target = e.target as Element
    if (!target || isContainer(target)) {
      if (lastHovered) {
        (lastHovered as HTMLElement).style.outline = ''
        lastHovered = null
      }
      return
    }
    if (lastHovered && lastHovered !== target) {
      (lastHovered as HTMLElement).style.outline = ''
    }
    (target as HTMLElement).style.outline = '2px solid #4096ff'
    lastHovered = target
  }

  function onClick(e: MouseEvent) {
    e.preventDefault()
    e.stopPropagation()
    const target = e.target as Element
    if (!target || isContainer(target)) return

    // 清除之前的选中样式
    if (lastHovered) {
      (lastHovered as HTMLElement).style.outline = ''
    }
    // 固定选中样式
    (target as HTMLElement).style.outline = '3px solid #0958d9'
    lastHovered = target

    selectedElement.value = extractElementInfo(target)
    console.log('[useVisualEditor] 选中元素:', selectedElement.value)
  }

  function attachEditor(iframe: HTMLIFrameElement) {
    const doc = iframe.contentDocument
    if (!doc) {
      console.error('[useVisualEditor] 无法访问 iframe contentDocument（可能跨域）')
      return false
    }

    // 注入高亮样式
    const style = doc.createElement('style')
    style.id = '__visual_editor_style__'
    style.textContent = `
      * { cursor: crosshair !important; }
      body[data-edit-mode] * { transition: outline 0.1s ease !important; }
    `
    doc.head.appendChild(style)
    doc.body.setAttribute('data-edit-mode', 'true')

    hoverHandler = onMouseMove
    clickHandler = onClick
    doc.addEventListener('mousemove', hoverHandler)
    doc.addEventListener('click', clickHandler, true)
    console.log('[useVisualEditor] 编辑器已附加到 iframe')
    return true
  }

  function detachEditor(iframe: HTMLIFrameElement) {
    const doc = iframe.contentDocument
    if (!doc) return

    if (hoverHandler) {
      doc.removeEventListener('mousemove', hoverHandler)
      hoverHandler = null
    }
    if (clickHandler) {
      doc.removeEventListener('click', clickHandler, true)
      clickHandler = null
    }

    // 清除所有 outline
    const styled = doc.querySelectorAll('[style*="outline"]')
    styled.forEach(el => {
      const htmlEl = el as HTMLElement
      if (htmlEl.style.outline.includes('4096ff') || htmlEl.style.outline.includes('0958d9')) {
        htmlEl.style.outline = ''
      }
    })

    const style = doc.getElementById('__visual_editor_style__')
    if (style) style.remove()
    doc.body?.removeAttribute('data-edit-mode')
    lastHovered = null
    console.log('[useVisualEditor] 编辑器已从 iframe 分离')
  }

  function enterEditMode() {
    if (!iframeRef.value) {
      console.error('[useVisualEditor] iframe 未就绪')
      return false
    }
    editMode.value = true
    // 等 Vue 更新后附加
    setTimeout(() => {
      if (iframeRef.value) {
        attachEditor(iframeRef.value)
      }
    }, 100)
    return true
  }

  function exitEditMode() {
    editMode.value = false
    selectedElement.value = null
    if (iframeRef.value) {
      detachEditor(iframeRef.value)
    }
  }

  function clearSelection() {
    if (lastHovered) {
      (lastHovered as HTMLElement).style.outline = ''
      lastHovered = null
    }
    selectedElement.value = null
  }

  function buildElementContext(): string {
    const el = selectedElement.value
    if (!el) return ''
    return `\n\n--- 用户选中的页面元素信息 ---
标签: <${el.tagName}>
ID: ${el.id || '无'}
CSS类: ${el.className || '无'}
CSS选择器: ${el.cssSelector}
位置: ${el.positionDesc}
文本内容: ${el.text || '无'}
--- 请根据以上信息修改对应元素 ---`
  }

  return {
    editMode,
    selectedElement,
    iframeRef,
    enterEditMode,
    exitEditMode,
    clearSelection,
    buildElementContext,
    attachEditor,
    detachEditor,
  }
}
