import { useCallback, useEffect, useRef, useState } from 'react'

type Metrics = {
  visible: boolean
  thumbHeight: number
  thumbTop: number
}

const TRACK_INSET = 6
const MIN_THUMB = 40

function readMetrics(): Metrics {
  const doc = document.documentElement
  const scrollHeight = doc.scrollHeight
  const clientHeight = doc.clientHeight
  const maxScroll = scrollHeight - clientHeight

  if (maxScroll <= 1) {
    return { visible: false, thumbHeight: 0, thumbTop: 0 }
  }

  const trackHeight = clientHeight - TRACK_INSET * 2
  const thumbHeight = Math.max(MIN_THUMB, (clientHeight / scrollHeight) * trackHeight)
  const scrollRatio = window.scrollY / maxScroll
  const thumbTop = TRACK_INSET + scrollRatio * (trackHeight - thumbHeight)

  return { visible: true, thumbHeight, thumbTop }
}

export function CustomScrollbar() {
  const trackRef = useRef<HTMLDivElement>(null)
  const [metrics, setMetrics] = useState<Metrics>(readMetrics)
  const dragRef = useRef<{ startY: number; startScroll: number } | null>(null)

  const update = useCallback(() => {
    setMetrics(readMetrics())
  }, [])

  useEffect(() => {
    document.documentElement.classList.add('native-scrollbar-hidden')

    update()
    window.addEventListener('scroll', update, { passive: true })
    window.addEventListener('resize', update, { passive: true })

    const observer = new ResizeObserver(update)
    observer.observe(document.documentElement)
    observer.observe(document.body)

    return () => {
      document.documentElement.classList.remove('native-scrollbar-hidden')
      window.removeEventListener('scroll', update)
      window.removeEventListener('resize', update)
      observer.disconnect()
    }
  }, [update])

  useEffect(() => {
    function onPointerMove(e: PointerEvent) {
      if (!dragRef.current) return
      const doc = document.documentElement
      const maxScroll = doc.scrollHeight - doc.clientHeight
      const trackHeight = doc.clientHeight - TRACK_INSET * 2 - metrics.thumbHeight
      if (trackHeight <= 0) return
      const deltaY = e.clientY - dragRef.current.startY
      const scrollDelta = (deltaY / trackHeight) * maxScroll
      window.scrollTo({ top: dragRef.current.startScroll + scrollDelta })
    }

    function onPointerUp() {
      dragRef.current = null
    }

    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', onPointerUp)
    window.addEventListener('pointercancel', onPointerUp)
    return () => {
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', onPointerUp)
      window.removeEventListener('pointercancel', onPointerUp)
    }
  }, [metrics.thumbHeight])

  function onThumbPointerDown(e: React.PointerEvent<HTMLDivElement>) {
    e.preventDefault()
    dragRef.current = { startY: e.clientY, startScroll: window.scrollY }
    e.currentTarget.setPointerCapture(e.pointerId)
  }

  function onTrackPointerDown(e: React.PointerEvent<HTMLDivElement>) {
    if (e.target !== trackRef.current) return
    const doc = document.documentElement
    const maxScroll = doc.scrollHeight - doc.clientHeight
    const trackHeight = doc.clientHeight - TRACK_INSET * 2 - metrics.thumbHeight
    const clickY = e.clientY - TRACK_INSET - metrics.thumbHeight / 2
    const ratio = Math.min(1, Math.max(0, clickY / trackHeight))
    window.scrollTo({ top: ratio * maxScroll, behavior: 'smooth' })
  }

  if (!metrics.visible) return null

  return (
    <div
      ref={trackRef}
      className="viewport-scrollbar"
      aria-hidden
      onPointerDown={onTrackPointerDown}
    >
      <div
        className="viewport-scrollbar-thumb"
        style={{ height: metrics.thumbHeight, transform: `translateY(${metrics.thumbTop}px)` }}
        onPointerDown={onThumbPointerDown}
      />
    </div>
  )
}
