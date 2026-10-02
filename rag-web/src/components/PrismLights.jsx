import { useEffect, useRef } from 'react'

const PRISMS = [
  { y: 0.16, scale: 1.2, speed: 0.042, hue: 268, delay: 0.02 },
  { y: 0.38, scale: 0.7, speed: 0.068, hue: 196, delay: 0.38 },
  { y: 0.58, scale: 1.05, speed: 0.05, hue: 326, delay: 0.18 },
  { y: 0.78, scale: 0.62, speed: 0.08, hue: 46, delay: 0.62 },
  { y: 0.28, scale: 0.48, speed: 0.095, hue: 164, delay: 0.74 },
]

export function PrismLights() {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    const context = canvas.getContext('2d')
    if (!context) {
      return undefined
    }

    const motion = window.matchMedia('(prefers-reduced-motion: reduce)')
    let frameId = 0

    function resize() {
      const ratio = Math.min(window.devicePixelRatio || 1, 2)
      canvas.width = Math.floor(canvas.clientWidth * ratio)
      canvas.height = Math.floor(canvas.clientHeight * ratio)
      context.setTransform(ratio, 0, 0, ratio, 0, 0)
    }

    function paint(now) {
      const width = canvas.clientWidth
      const height = canvas.clientHeight
      context.clearRect(0, 0, width, height)
      const wash = context.createLinearGradient(0, 0, width, height)
      wash.addColorStop(0, '#140a24')
      wash.addColorStop(0.48, '#07060f')
      wash.addColorStop(1, '#07141c')
      context.fillStyle = wash
      context.fillRect(0, 0, width, height)

      context.globalCompositeOperation = 'lighter'
      for (const prism of PRISMS) {
        const travel = motion.matches ? prism.delay : ((now / 1000) * prism.speed + prism.delay) % 1
        const span = width + 560 * prism.scale
        const x = -280 * prism.scale + travel * span
        const glide = motion.matches ? 0.42 : (now / 900 + prism.delay) % 1
        paintPrism(context, x, height * prism.y, 480 * prism.scale, 96 * prism.scale, prism.hue, glide)
      }
      context.globalCompositeOperation = 'source-over'

      if (!motion.matches) {
        frameId = requestAnimationFrame(paint)
      }
    }

    function onResize() {
      resize()
      if (motion.matches) {
        paint(0)
      }
    }

    resize()
    paint(performance.now())
    window.addEventListener('resize', onResize)
    return () => {
      cancelAnimationFrame(frameId)
      window.removeEventListener('resize', onResize)
    }
  }, [])

  return <canvas ref={canvasRef} className="prism-lights" aria-hidden="true" />
}

function paintPrism(context, x, y, length, height, hue, glide) {
  const skew = height * 0.7
  const rise = height * 0.22
  const front = [
    [x, y + height],
    [x + skew, y],
    [x + skew * 2, y + height],
  ]
  const back = front.map(([px, py]) => [px + length, py - rise])

  fillFace(context, [front[0], front[2], back[2], back[0]], hsla(hue, 70, 42, 0.16))
  fillFace(context, [front[0], front[1], back[1], back[0]], hsla(hue, 92, 64, 0.34))
  fillFace(context, [front[1], front[2], back[2], back[1]], hsla((hue + 36) % 360, 96, 72, 0.28))
  fillFace(context, front, hsla(hue, 100, 78, 0.5))
  strokeRidge(context, [front[1], back[1]], hsla(hue, 100, 88, 0.8))

  context.save()
  trace(context, [front[0], front[1], back[1], back[2], front[2]])
  context.clip()
  const band = x + glide * (length + skew)
  const light = context.createLinearGradient(band - 36, y, band + 84, y + height)
  light.addColorStop(0, 'rgba(255,255,255,0)')
  light.addColorStop(0.42, hsla(hue, 100, 80, 0.2))
  light.addColorStop(0.5, 'rgba(255,255,255,0.95)')
  light.addColorStop(0.58, hsla((hue + 48) % 360, 100, 70, 0.45))
  light.addColorStop(1, 'rgba(255,255,255,0)')
  context.fillStyle = light
  context.fillRect(band - 48, y - 30, 140, height + rise + 60)
  context.restore()
}

function fillFace(context, points, color) {
  trace(context, points)
  context.fillStyle = color
  context.fill()
}

function strokeRidge(context, points, color) {
  trace(context, points)
  context.strokeStyle = color
  context.lineWidth = 2
  context.stroke()
}

function trace(context, points) {
  context.beginPath()
  context.moveTo(points[0][0], points[0][1])
  for (const [px, py] of points.slice(1)) {
    context.lineTo(px, py)
  }
  context.closePath()
}

function hsla(hue, saturation, lightness, alpha) {
  return `hsla(${hue}, ${saturation}%, ${lightness}%, ${alpha})`
}
