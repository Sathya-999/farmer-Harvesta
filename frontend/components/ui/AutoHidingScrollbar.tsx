'use client'

import React, { useState, useEffect, useRef, useCallback } from 'react'

interface Props {
  /** Target scroll container ref. If not provided or null, attaches to window/document. */
  containerRef?: React.RefObject<HTMLElement | null>
  /** Width of the scrollbar thumb in pixels (default 6px) */
  width?: number
  /** Color of the scrollbar thumb */
  thumbColor?: string
  /** Color when thumb is hovered or active */
  thumbActiveColor?: string
  /** Right offset in pixels (default 3px) */
  rightOffset?: number
  /** Optional zIndex (default 9999) */
  zIndex?: number
}

export default function AutoHidingScrollbar({
  containerRef,
  width = 6,
  thumbColor = 'rgba(36, 68, 42, 0.45)',
  thumbActiveColor = '#24442A',
  rightOffset = 3,
  zIndex = 9999,
}: Props) {
  const [thumbTop, setThumbTop] = useState(0)
  const [thumbHeight, setThumbHeight] = useState(0)
  const [isScrollable, setIsScrollable] = useState(false)
  const [visible, setVisible] = useState(false)
  const [isHovered, setIsHovered] = useState(false)
  const [isDragging, setIsDragging] = useState(false)

  const hideTimerRef = useRef<NodeJS.Timeout | null>(null)
  const isHoveringThumbOrTrackRef = useRef(false)
  const isDraggingRef = useRef(false)
  const dragStartYRef = useRef(0)
  const dragStartScrollTopRef = useRef(0)
  const trackRef = useRef<HTMLDivElement | null>(null)
  const thumbRef = useRef<HTMLDivElement | null>(null)

  // Clear hide timer
  const clearHideTimer = () => {
    if (hideTimerRef.current) {
      clearTimeout(hideTimerRef.current)
      hideTimerRef.current = null
    }
  }

  // Show scrollbar and auto-hide after 1200ms unless hovered or dragging
  const triggerActivity = useCallback((duration = 1200) => {
    setVisible(true)
    clearHideTimer()

    hideTimerRef.current = setTimeout(() => {
      if (!isHoveringThumbOrTrackRef.current && !isDraggingRef.current) {
        setVisible(false)
      }
    }, duration)
  }, [])

  // Recalculate metrics based on content height vs container height
  const updateMetrics = useCallback(() => {
    const isWin = !containerRef || !containerRef.current
    let scrollHeight = 0
    let clientHeight = 0
    let scrollTop = 0

    if (isWin) {
      scrollHeight = Math.max(
        document.documentElement.scrollHeight,
        document.body.scrollHeight
      )
      clientHeight = window.innerHeight
      scrollTop = window.scrollY || document.documentElement.scrollTop
    } else {
      const el = containerRef.current!
      scrollHeight = el.scrollHeight
      clientHeight = el.clientHeight
      scrollTop = el.scrollTop
    }

    if (scrollHeight <= clientHeight + 4 || clientHeight <= 0) {
      setIsScrollable(false)
      return
    }

    setIsScrollable(true)
    const minThumbHeight = 32
    const calculatedHeight = Math.max(
      minThumbHeight,
      (clientHeight / scrollHeight) * clientHeight
    )
    const maxTop = clientHeight - calculatedHeight
    const currentTop =
      scrollHeight > clientHeight
        ? (scrollTop / (scrollHeight - clientHeight)) * maxTop
        : 0

    setThumbHeight(calculatedHeight)
    setThumbTop(Math.min(maxTop, Math.max(0, currentTop)))
  }, [containerRef])

  // Setup observers and event listeners
  useEffect(() => {
    const isWin = !containerRef || !containerRef.current
    const targetElement = isWin ? null : containerRef.current!

    const handleScroll = () => {
      updateMetrics()
      triggerActivity(1100)
    }

    const handleKeyDown = (e: KeyboardEvent) => {
      const scrollKeys = [
        'ArrowDown',
        'ArrowUp',
        'PageDown',
        'PageUp',
        'Home',
        'End',
        ' ',
      ]
      if (scrollKeys.includes(e.key)) {
        triggerActivity(1400)
      }
    }

    const handlePointerMoveNearby = (e: MouseEvent) => {
      // If pointer is within 48px of right edge of window/container, reveal scrollbar
      if (isWin) {
        const distFromRight = window.innerWidth - e.clientX
        if (distFromRight <= 48 && distFromRight >= 0) {
          triggerActivity(1200)
        }
      } else if (targetElement) {
        const rect = targetElement.getBoundingClientRect()
        if (
          e.clientX >= rect.right - 36 &&
          e.clientX <= rect.right &&
          e.clientY >= rect.top &&
          e.clientY <= rect.bottom
        ) {
          triggerActivity(1200)
        }
      }
    }

    updateMetrics()

    // Scroll listeners
    if (isWin) {
      window.addEventListener('scroll', handleScroll, { passive: true })
      window.addEventListener('keydown', handleKeyDown, { passive: true })
      window.addEventListener('mousemove', handlePointerMoveNearby, { passive: true })
    } else if (targetElement) {
      targetElement.addEventListener('scroll', handleScroll, { passive: true })
      targetElement.addEventListener('mousemove', handlePointerMoveNearby, { passive: true })
    }

    // Window resize
    window.addEventListener('resize', updateMetrics)

    // ResizeObserver for dynamic content height changes
    let ro: ResizeObserver | null = null
    if (typeof ResizeObserver !== 'undefined') {
      ro = new ResizeObserver(() => {
        updateMetrics()
      })
      if (isWin) {
        ro.observe(document.body)
        ro.observe(document.documentElement)
      } else if (targetElement) {
        ro.observe(targetElement)
      }
    }

    // MutationObserver to catch DOM additions (cards loaded, API data rendered)
    let mo: MutationObserver | null = null
    if (typeof MutationObserver !== 'undefined') {
      mo = new MutationObserver(() => {
        updateMetrics()
      })
      const obsTarget = isWin ? document.body : targetElement
      if (obsTarget) {
        mo.observe(obsTarget, { childList: true, subtree: true, attributes: true })
      }
    }

    return () => {
      clearHideTimer()
      if (isWin) {
        window.removeEventListener('scroll', handleScroll)
        window.removeEventListener('keydown', handleKeyDown)
        window.removeEventListener('mousemove', handlePointerMoveNearby)
      } else if (targetElement) {
        targetElement.removeEventListener('scroll', handleScroll)
        targetElement.removeEventListener('mousemove', handlePointerMoveNearby)
      }
      window.removeEventListener('resize', updateMetrics)
      if (ro) ro.disconnect()
      if (mo) mo.disconnect()
    }
  }, [containerRef, updateMetrics, triggerActivity])

  // Drag handling with pointer capture
  const handleThumbPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    e.stopPropagation()
    e.preventDefault()

    const isWin = !containerRef || !containerRef.current
    const currentScroll = isWin
      ? window.scrollY || document.documentElement.scrollTop
      : containerRef.current!.scrollTop

    isDraggingRef.current = true
    setIsDragging(true)
    setVisible(true)
    clearHideTimer()

    dragStartYRef.current = e.clientY
    dragStartScrollTopRef.current = currentScroll

    if (e.currentTarget.setPointerCapture) {
      try {
        e.currentTarget.setPointerCapture(e.pointerId)
      } catch (err) {
        // ignore capture errors
      }
    }
  }

  const handleThumbPointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDraggingRef.current) return
    e.preventDefault()

    const isWin = !containerRef || !containerRef.current
    let clientHeight = 0
    let scrollHeight = 0

    if (isWin) {
      clientHeight = window.innerHeight
      scrollHeight = Math.max(
        document.documentElement.scrollHeight,
        document.body.scrollHeight
      )
    } else {
      clientHeight = containerRef.current!.clientHeight
      scrollHeight = containerRef.current!.scrollHeight
    }

    const availableScrollRange = scrollHeight - clientHeight
    const availableTrackRange = clientHeight - thumbHeight
    if (availableTrackRange <= 0) return

    const deltaY = e.clientY - dragStartYRef.current
    const scrollDelta = (deltaY / availableTrackRange) * availableScrollRange
    const targetScroll = Math.max(
      0,
      Math.min(availableScrollRange, dragStartScrollTopRef.current + scrollDelta)
    )

    if (isWin) {
      window.scrollTo({ top: targetScroll, behavior: 'instant' as ScrollBehavior })
    } else {
      containerRef.current!.scrollTop = targetScroll
    }
    updateMetrics()
  }

  const handleThumbPointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDraggingRef.current) return
    isDraggingRef.current = false
    setIsDragging(false)

    if (e.currentTarget.releasePointerCapture) {
      try {
        e.currentTarget.releasePointerCapture(e.pointerId)
      } catch (err) {
        // ignore
      }
    }

    if (!isHoveringThumbOrTrackRef.current) {
      triggerActivity(1000)
    }
  }

  // Track click jumps or pages smoothly to clicked location
  const handleTrackPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (e.target === thumbRef.current) return
    e.stopPropagation()

    const isWin = !containerRef || !containerRef.current
    const trackRect = e.currentTarget.getBoundingClientRect()
    const clickYRelative = e.clientY - trackRect.top

    let clientHeight = 0
    let scrollHeight = 0

    if (isWin) {
      clientHeight = window.innerHeight
      scrollHeight = Math.max(
        document.documentElement.scrollHeight,
        document.body.scrollHeight
      )
    } else {
      clientHeight = containerRef.current!.clientHeight
      scrollHeight = containerRef.current!.scrollHeight
    }

    const availableScrollRange = scrollHeight - clientHeight
    const targetScroll =
      ((clickYRelative - thumbHeight / 2) / (clientHeight - thumbHeight)) *
      availableScrollRange

    const finalScroll = Math.max(0, Math.min(availableScrollRange, targetScroll))

    if (isWin) {
      window.scrollTo({ top: finalScroll, behavior: 'smooth' })
    } else {
      containerRef.current!.scrollTo({ top: finalScroll, behavior: 'smooth' })
    }
    triggerActivity(1200)
  }

  if (!isScrollable) return null

  const isWin = !containerRef || !containerRef.current
  const activeWidth = isDragging || isHovered ? width + 3 : width
  const effectiveOpacity = visible || isDragging || isHovered ? 1 : 0

  return (
    <div
      ref={trackRef}
      role="scrollbar"
      aria-orientation="vertical"
      aria-valuenow={Math.round(thumbTop)}
      aria-label="Custom auto-hiding scrollbar"
      onPointerDown={handleTrackPointerDown}
      onMouseEnter={() => {
        isHoveringThumbOrTrackRef.current = true
        setIsHovered(true)
        clearHideTimer()
        setVisible(true)
      }}
      onMouseLeave={() => {
        isHoveringThumbOrTrackRef.current = false
        setIsHovered(false)
        if (!isDraggingRef.current) {
          triggerActivity(600)
        }
      }}
      style={{
        position: isWin ? 'fixed' : 'absolute',
        top: 0,
        right: rightOffset,
        bottom: 0,
        width: Math.max(width + 10, 16),
        zIndex,
        pointerEvents: visible || isDragging || isHovered ? 'auto' : 'none',
        display: 'flex',
        justifyContent: 'center',
        background:
          isHovered || isDragging ? 'rgba(36, 68, 42, 0.04)' : 'transparent',
        borderRadius: 999,
        transition: 'background 0.2s ease',
        cursor: 'pointer',
      }}
    >
      <div
        ref={thumbRef}
        onPointerDown={handleThumbPointerDown}
        onPointerMove={handleThumbPointerMove}
        onPointerUp={handleThumbPointerUp}
        onPointerCancel={handleThumbPointerUp}
        style={{
          position: 'absolute',
          top: thumbTop,
          width: activeWidth,
          height: thumbHeight,
          borderRadius: 999,
          background: isDragging
            ? thumbActiveColor
            : isHovered
            ? thumbActiveColor
            : thumbColor,
          opacity: effectiveOpacity,
          transition: isDragging
            ? 'background 0.15s, width 0.15s'
            : 'opacity 0.35s cubic-bezier(0.2, 0.8, 0.2, 1), background 0.15s, width 0.15s',
          cursor: isDragging ? 'grabbing' : 'grab',
          pointerEvents: 'auto',
          boxShadow:
            effectiveOpacity > 0
              ? '0 1px 4px rgba(0, 0, 0, 0.18)'
              : 'none',
        }}
      />
    </div>
  )
}
