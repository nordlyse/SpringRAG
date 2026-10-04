import { useRef } from 'react'

export function GlassCard({ title, children }) {
  const cardRef = useRef(null)

  function onPointerMove(event) {
    const card = cardRef.current
    const bounds = card.getBoundingClientRect()
    card.style.setProperty('--spot-x', `${event.clientX - bounds.left}px`)
    card.style.setProperty('--spot-y', `${event.clientY - bounds.top}px`)
  }

  return (
    <section ref={cardRef} className="glass" onPointerMove={onPointerMove}>
      <h2>{title}</h2>
      {children}
    </section>
  )
}
