import { useEffect, useRef } from 'react'

const VERTEX = `
attribute vec2 aPosition;
void main() {
  gl_Position = vec4(aPosition, 0.0, 1.0);
}
`

const FRAGMENT = `
precision highp float;
uniform vec2 uResolution;
uniform float uTime;

float prism(vec3 point) {
  vec3 folded = abs(point);
  float triangle = max(folded.z * 0.866025 + point.y * 0.5, -point.y) - 0.38;
  return max(folded.x - 3.2, triangle);
}

mat2 spin(float angle) {
  float c = cos(angle);
  float s = sin(angle);
  return mat2(c, -s, s, c);
}

vec3 spectrum(float t) {
  return 0.55 + 0.45 * cos(6.28318 * (vec3(0.0, 0.33, 0.67) + t));
}

void main() {
  vec2 uv = (gl_FragCoord.xy - 0.5 * uResolution.xy) / uResolution.y;
  vec3 origin = vec3(0.0, 0.42, 3.15);
  vec3 ray = normalize(vec3(uv, -1.15));
  float clock = uTime * 0.18;
  float traveled = 0.0;
  float nearest = 8.0;
  float hit = -1.0;

  for (int step = 0; step < 72; step++) {
    vec3 point = origin + ray * traveled;
    point.xy *= spin(clock);
    point.yz *= spin(0.42);
    float distanceToPrism = prism(point);
    nearest = min(nearest, distanceToPrism);
    if (distanceToPrism < 0.004) {
      hit = traveled;
      break;
    }
    traveled += distanceToPrism;
    if (traveled > 14.0) {
      break;
    }
  }

  vec3 background = vec3(0.04, 0.035, 0.08);
  float aura = exp(-max(nearest, 0.0) * 1.05);
  vec3 color = background + spectrum(uv.x * 0.55 + uv.y * 0.25 + clock * 0.35) * aura;
  float beam = exp(-pow(uv.y * 2.6, 2.0));
  color += vec3(0.2, 0.62, 1.0) * beam * (0.28 + aura * 0.85);

  if (hit > 0.0) {
    vec3 point = origin + ray * hit;
    vec3 view = ray;
    point.xy *= spin(clock);
    point.yz *= spin(0.42);
    view.xy *= spin(clock);
    view.yz *= spin(0.42);
    vec2 edge = vec2(0.003, 0.0);
    vec3 normal = normalize(vec3(
      prism(point + edge.xyy) - prism(point - edge.xyy),
      prism(point + edge.yxy) - prism(point - edge.yxy),
      prism(point + edge.yyx) - prism(point - edge.yyx)
    ));
    float fresnel = pow(1.0 - max(dot(normal, -view), 0.0), 1.6);
    float bands = 0.5 + 0.5 * sin(point.y * 4.0 + point.z * 3.0 + uTime * 0.5);
    vec3 glass = spectrum(normal.y * 0.35 + bands * 0.28 + clock);
    color = mix(color, glass * (0.55 + fresnel * 0.8), 0.5);
  }

  float grain = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
  color += (grain - 0.5) * 0.035;
  gl_FragColor = vec4(color, 1.0);
}
`

export function PrismLights() {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    const gl = canvas.getContext('webgl', { alpha: false, antialias: false })
    if (!gl) {
      return undefined
    }

    const program = linkProgram(gl, VERTEX, FRAGMENT)
    if (!program) {
      return undefined
    }

    const buffer = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW)
    const position = gl.getAttribLocation(program, 'aPosition')
    const resolution = gl.getUniformLocation(program, 'uResolution')
    const time = gl.getUniformLocation(program, 'uTime')
    gl.useProgram(program)
    gl.enableVertexAttribArray(position)
    gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0)

    const motion = window.matchMedia('(prefers-reduced-motion: reduce)')
    let frameId = 0

    function resize() {
      const ratio = Math.min(window.devicePixelRatio || 1, 2)
      const width = Math.max(1, Math.floor(canvas.clientWidth * ratio))
      const height = Math.max(1, Math.floor(canvas.clientHeight * ratio))
      if (canvas.width !== width || canvas.height !== height) {
        canvas.width = width
        canvas.height = height
      }
      gl.viewport(0, 0, canvas.width, canvas.height)
    }

    function paint(now) {
      resize()
      gl.uniform2f(resolution, canvas.width, canvas.height)
      gl.uniform1f(time, motion.matches ? 1.4 : now / 1000)
      gl.drawArrays(gl.TRIANGLES, 0, 3)
      if (!motion.matches) {
        frameId = requestAnimationFrame(paint)
      }
    }

    function onResize() {
      if (motion.matches) {
        paint(0)
      }
    }

    paint(performance.now())
    window.addEventListener('resize', onResize)
    return () => {
      cancelAnimationFrame(frameId)
      window.removeEventListener('resize', onResize)
      gl.deleteBuffer(buffer)
      gl.deleteProgram(program)
    }
  }, [])

  return <canvas ref={canvasRef} className="prism-lights" aria-hidden="true" />
}

function linkProgram(gl, vertexSource, fragmentSource) {
  const vertex = compile(gl, gl.VERTEX_SHADER, vertexSource)
  const fragment = compile(gl, gl.FRAGMENT_SHADER, fragmentSource)
  if (!vertex || !fragment) {
    return null
  }
  const program = gl.createProgram()
  gl.attachShader(program, vertex)
  gl.attachShader(program, fragment)
  gl.linkProgram(program)
  gl.deleteShader(vertex)
  gl.deleteShader(fragment)
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    gl.deleteProgram(program)
    return null
  }
  return program
}

function compile(gl, type, source) {
  const shader = gl.createShader(type)
  gl.shaderSource(shader, source)
  gl.compileShader(shader)
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    gl.deleteShader(shader)
    return null
  }
  return shader
}
