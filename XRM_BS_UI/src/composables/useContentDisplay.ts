import { useRouter } from 'vue-router'

export function useContentDisplay() {
  const router = useRouter()

  const handleContentClick = (e: MouseEvent) => {
    const target = e.target as HTMLElement
    if (target.tagName === 'A') {
      const href = target.getAttribute('href')
      if (!href) return


      if (href.startsWith('http://') || href.startsWith('https://')) {
        window.open(href, '_blank')
        e.preventDefault()
        return
      }

      if (/^https?:\/\//i.test(href)) {
        window.open(href, '_blank', 'noopener')
        e.preventDefault()
      } else if (/^([a-z0-9-]+\.)+[a-z]{2,}(\/|$)/i.test(href) || href.includes('.')) {
        const fullUrl = href.startsWith('//') ? 'https:' + href : 'https://' + href
        window.open(fullUrl, '_blank', 'noopener')
        e.preventDefault()
      } else if (href.startsWith('/')) {
        router.push(href)
        e.preventDefault()
      }
    }
  }



  const disableBilibiliAutoplay = () => {
    document.querySelectorAll('.editor-content iframe').forEach((el: Element) => {
      const iframe = el as HTMLIFrameElement
      const src = iframe.src
      if (!src.includes('player.bilibili.com')) return
      try {
        const url = new URL(src)
        url.searchParams.set('autoplay', '0')
        iframe.src = url.toString()
      } catch {
        iframe.src += (src.includes('?') ? '&' : '?') + 'autoplay=0'
      }
    })
  }

  return { handleContentClick, disableBilibiliAutoplay }
}

