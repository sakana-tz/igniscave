// 浴场引擎编排：三层 canvas（焦散/水面/雾气）+ 漂浮卡片物理 + 拖拽交互
// 用法：const bath = useBathhouse({ poolRef, waterCanvas, causticsCanvas, mistCanvas })
//       bath.initPool() / bath.disposePool() / bath.rebuildBodies()
//       模板绑定 @pointerdown="bath.onDown($event, post)"，点击导航前用 bath.isJustDragged() 抑制
import { nextTick } from 'vue'
import { TAU, clamp, rand } from './physics'
import { satCollide, halfExtents, wallContact } from './physics'
import { WaterSurface } from './waterSurface'
import { createMistLayer } from './mist'
import { computeCardLayout } from './layout'

const MAX_SPEED = 900, SUBSTEP_PX = 6, MAX_SUBSTEPS = 6
const SOLVER_ITERS = 8, SOLVER_SLOP = 0.12, SOLVER_PERCENT = 0.95

// 浓金色焦散光斑精灵
function buildGlowSprite() {
  const size = 128
  const cv = document.createElement('canvas')
  cv.width = cv.height = size
  const g = cv.getContext('2d')
  const grad = g.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
  grad.addColorStop(0, 'rgba(255, 215, 120, 1)')
  grad.addColorStop(0.30, 'rgba(255, 195, 80, 0.55)')
  grad.addColorStop(0.60, 'rgba(240, 170, 50, 0.18)')
  grad.addColorStop(1, 'rgba(220, 150, 30, 0)')
  g.fillStyle = grad
  g.fillRect(0, 0, size, size)
  return cv
}

export function useBathhouse({ poolRef, waterCanvas, causticsCanvas, mistCanvas, onLayoutChange }) {
  let wCtx, cCtx, mCtx, dpr
  let W = 0, H = 0
  let boundBottom = 0
  let water = null
  let rafId = 0
  let time = 0
  let glowSprite = null
  let waveSources = []
  const bodies = []
  const bodyMap = new Map()
  let resizeObs = null
  let dragBody = null
  let dragPointerId = null
  let zIdx = 20
  let justDragged = false
  let initialized = false
  let curLayout = null        // 最近一次响应式布局结果（cols/gap/widths）
  let gridSig = ''            // 最近一次网格排版的布局签名（列数+xl 宽度）
  let rebuilding = false      // rebuildBodies 内抑制 handleResize 的重复调度
  let relayoutTimer = 0       // 尺寸变化后重排版防抖
  const mist = createMistLayer()

  // 按浴池内容区宽度计算卡片尺寸，并以 CSS 变量（--cw-s/m/l/xl）下发给样式层
  function applyLayout(pool) {
    const cs = getComputedStyle(pool)
    const padX = (parseFloat(cs.paddingLeft) || 0) + (parseFloat(cs.paddingRight) || 0)
    const avail = Math.max(200, pool.clientWidth - padX)
    const layout = computeCardLayout(avail)
    pool.style.setProperty('--cw-s', layout.widths.s + 'px')
    pool.style.setProperty('--cw-m', layout.widths.m + 'px')
    pool.style.setProperty('--cw-l', layout.widths.l + 'px')
    pool.style.setProperty('--cw-xl', layout.widths.xl + 'px')
    curLayout = layout
    return layout
  }

  function initWaveSources() {
    waveSources.length = 0
    for (let i = 0; i < 3; i++) {
      waveSources.push({
        x: W * ((i + 0.5) / 3) + rand(-60, 60), y: rand(H * 0.25, H * 0.75),
        vx: rand(-18, 18), vy: rand(-10, 10), r: rand(65, 100),
        freq: rand(0.55, 0.95), phase: rand(0, TAU), amp: rand(0.5, 1.05), k: rand(0.09, 0.13),
      })
    }
  }

  function handleResize() {
    if (!poolRef.value) return
    const rect = poolRef.value.getBoundingClientRect()
    W = rect.width; H = rect.height
    // 底边与容器可见边缘对齐；卡片渲染时的抬升/缩放/倾斜余量在 resolveBoundaries 中精确扣除
    boundBottom = H
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    ;[waterCanvas.value, causticsCanvas.value, mistCanvas.value].forEach(c => {
      if (!c) return
      c.width = W * dpr; c.height = H * dpr; c.style.width = W + 'px'; c.style.height = H + 'px'
    })
    if (!water) water = new WaterSurface(W, H, 5); else water.resize(W, H)
    for (const b of bodies) {
      const { hw, hh } = halfExtents(b)
      b.x = clamp(b.x, hw, Math.max(hw, W - hw))
      b.y = clamp(b.y, hh, Math.max(hh, boundBottom - hh))
    }
    if (!waveSources.length) initWaveSources()
    for (const s of waveSources) {
      s.x = clamp(s.x, 60, Math.max(60, W - 60))
      s.y = clamp(s.y, 60, Math.max(60, H - 60))
    }
    mist.resize(W, H)
    // 同步卡片尺寸 CSS 变量；列数/尺寸档位真正变化时防抖重排网格。
    // 仅高度变化（如手机地址栏伸缩）签名不变，卡片保持原位不跳变。
    const layout = applyLayout(poolRef.value)
    const sig = layout.cols + ':' + layout.widths.xl
    if (initialized && !rebuilding && bodies.length && sig !== gridSig) {
      clearTimeout(relayoutTimer)
      relayoutTimer = setTimeout(() => {
        if (!initialized) return
        rebuildBodies()
        if (onLayoutChange) onLayoutChange(curLayout)
      }, 180)
    }
  }

  // 根据当前帖子列表重建物理浮块，并按网格排布初始位置
  function rebuildBodies() {
    if (!initialized || !poolRef.value) return
    const pool = poolRef.value
    const cards = Array.from(pool.querySelectorAll('.post-card'))
    if (resizeObs) for (const b of bodies) resizeObs.unobserve(b.el)
    bodies.length = 0
    bodyMap.clear()

    // 先按浴池宽度下发卡片尺寸 CSS 变量，再读取各卡真实宽高排版
    const layout = applyLayout(pool)
    const cols = layout.cols
    const ROW_GAP = 24 // 纵向保留更大行距，方便手机触摸且不改变桌面纵向节奏
    // 列间距（layout.gap）已在卡片宽度计算时预留，此处只用于行布局
    // 各卡实际尺寸由尺寸类（浏览量）决定，先读取再排版
    const dims = cards.map(el => ({ w: el.offsetWidth || 140, h: el.offsetHeight || 187 }))
    const rows = Math.max(1, Math.ceil(cards.length / cols))
    const rowHeights = []
    for (let r = 0; r < rows; r++) {
      let h = 0
      for (let c = 0; c < cols; c++) {
        const idx = r * cols + c
        if (idx < dims.length) h = Math.max(h, dims[idx].h)
      }
      rowHeights.push(h)
    }
    const contentH = rowHeights.reduce((a, b) => a + b, 0) + ROW_GAP * (rows - 1)
    // 初始网格从标题/工具栏悬浮层下方开始铺（运动不受限，之后可自由漂上去）
    const uiEl = pool.parentElement ? pool.parentElement.querySelector('.float-ui') : null
    const gridTop = uiEl ? uiEl.offsetTop + uiEl.offsetHeight + 12 : 170
    // 容器自身即浴池：高度需容纳顶部工具栏悬浮层 + 卡片内容 + 底部分页预留，
    // 否则移动端 6 卡（3 行）会被整体上推遮挡工具栏；不低于 CSS min-height（由 CSS 响应式控制）
    // 必须先清空 inline minHeight 再读 cssMin：否则 getComputedStyle 返回上次 inline 输出，
    // cols 跨档过渡态（旧 pageSize × 新 cols 行数翻倍）会污染下限，让浴场高度只增不减卡死在过渡态
    pool.style.minHeight = ''
    const cssMin = parseFloat(getComputedStyle(pool).minHeight) || 0
    const targetH = cards.length ? Math.max(gridTop + contentH + 90, cssMin) : 0
    pool.style.minHeight = targetH ? targetH + 'px' : ''
    rebuilding = true
    handleResize()
    rebuilding = false
    gridSig = layout.cols + ':' + layout.widths.xl
    if (!cards.length) return
    const cellW = W / cols
    // 各行中心按行高累计，避免大小卡初始重叠
    const rowCenters = []
    let acc = gridTop
    for (let r = 0; r < rows; r++) {
      rowCenters.push(acc + rowHeights[r] / 2)
      acc += rowHeights[r] + ROW_GAP
    }
    // 卡片组整体上移，避免初始位置遮挡底部分页按钮（分页在 bottom:22px，预留约 90px）
    const safeBottom = H - 90
    const groupBottom = rowCenters[rows - 1] + rowHeights[rows - 1] / 2
    if (groupBottom > safeBottom) {
      const offset = groupBottom - safeBottom
      for (let r = 0; r < rows; r++) rowCenters[r] -= offset
    }
    cards.forEach((el, i) => {
      const col = i % cols
      const row = Math.floor(i / cols)
      const { w, h } = dims[i]
      let x = (col + 0.5) * cellW + rand(-8, 8)
      let y = rowCenters[row] + rand(-8, 8)
      x = clamp(x, w / 2, Math.max(w / 2, W - w / 2))
      y = clamp(y, gridTop + h / 2, Math.max(gridTop + h / 2, boundBottom - h / 2))
      const body = {
        id: el.dataset.postId, el,
        x, y, vx: 0, vy: 0,
        angle: rand(-0.04, 0.04), va: 0,
        // 矩形刚体：绕质心转动惯量 I = m(w²+h²)/12；大卡天然更难转
        w, h, mass: 1, invM: 1,
        invI: 12 / (w * w + h * h),
        dragging: false, moved: false,
        ox: x, oy: y,
        seed: rand(0, 100), lift: 0, tilt: 0,
        startX: 0, startY: 0,
      }
      bodies.push(body)
      bodyMap.set(el.dataset.postId, body)
      if (resizeObs) resizeObs.observe(el)
    })
    renderCards()
  }

  function loop() {
    rafId = requestAnimationFrame(loop)
    if (!water) return
    const dt = 1 / 60; time += dt
    updateWaveSources(dt); updatePhysics(dt); water.update(); mist.update(dt, W, H)
    drawCaustics(); drawWaterSurface(); mist.draw(mCtx, dpr, W, H, time); renderCards()
  }

  function updateWaveSources(dt) {
    const margin = 60
    for (const s of waveSources) {
      s.x += s.vx * dt; s.y += s.vy * dt
      if (s.x < margin) { s.x = margin; s.vx = Math.abs(s.vx) }
      else if (s.x > W - margin) { s.x = W - margin; s.vx = -Math.abs(s.vx) }
      if (s.y < margin) { s.y = margin; s.vy = Math.abs(s.vy) }
      else if (s.y > H - margin) { s.y = H - margin; s.vy = -Math.abs(s.vy) }
      const target = Math.sin(time * s.freq + s.phase) * s.amp
      water.driveTo(s.x, s.y, s.r, target, s.k)
    }
  }

  function updatePhysics(dt) {
    let maxV = 0
    for (const b of bodies) { const sp = Math.hypot(b.vx, b.vy); if (sp > maxV) maxV = sp }
    const steps = clamp(Math.ceil(maxV * dt / SUBSTEP_PX), 1, MAX_SUBSTEPS)
    const sdt = dt / steps
    for (let s = 0; s < steps; s++) {
      integrateForces(sdt); integrateMotion(sdt)
      resolveBoundaries(sdt); resolveCollisions(sdt)
    }
    if (dragBody) {
      const sp = Math.hypot(dragBody.vx, dragBody.vy)
      if (sp > 24) water.disturb(dragBody.x, dragBody.y, 28, clamp(sp * 0.0034, 0, 0.55))
    }
  }

  function integrateForces(dt) {
    for (const b of bodies) {
      if (b.dragging) {
        // 弹簧-阻尼跟随鼠标目标位置
        const k = 260, d = 26
        b.vx += ((b.ox - b.x) * k - b.vx * d) * dt
        b.vy += ((b.oy - b.y) * k - b.vy * d) * dt
      } else {
        // 环境漂浮力 + 水面梯度耦合（流体感）+ 轻微向心约束
        b.vx += Math.sin(time * 0.17 + b.seed) * 0.55 * dt
        b.vy += Math.cos(time * 0.12 + b.seed * 1.3) * 0.55 * dt
        b.va += Math.sin(time * 0.20 + b.seed * 2) * 0.028 * dt
        const eps = 42
        const gx = (water.sampleH(b.x + eps, b.y) - water.sampleH(b.x - eps, b.y)) * 0.5
        const gy = (water.sampleH(b.x, b.y + eps) - water.sampleH(b.x, b.y - eps)) * 0.5
        b.vx -= gx * 40 * dt; b.vy -= gy * 40 * dt
        // 软边界向心：中心自由区（半径=短边28%）内不约束，漂得太远才轻轻拉回，
        // 避免静止时所有卡片挤向浴池正中央
        const cx = W * 0.5, cy = H * 0.5
        const dx = cx - b.x, dy = cy - b.y
        const dist = Math.hypot(dx, dy)
        const freeR = Math.min(W, H) * 0.28
        if (dist > freeR && dist > 0.001) {
          const pull = (dist - freeR) * 0.018
          b.vx += (dx / dist) * pull * dt
          b.vy += (dy / dist) * pull * dt
        }
        b.tilt += (gx * 0.05 - b.tilt) * (1 - Math.exp(-4.5 * dt))
      }
      // 水阻：转动阻尼小于平动阻尼，碰撞产生的自旋可悠悠转一会儿再衰减
      b.vx *= Math.exp(-2.6 * dt); b.vy *= Math.exp(-2.6 * dt); b.va *= Math.exp(-1.8 * dt)
      const sp = Math.hypot(b.vx, b.vy)
      if (sp > MAX_SPEED) { const inv = MAX_SPEED / sp; b.vx *= inv; b.vy *= inv }
    }
  }

  function integrateMotion(dt) {
    for (const b of bodies) {
      b.x += b.vx * dt; b.y += b.vy * dt; b.angle += b.va * dt
      const h = water.sampleH(b.x, b.y)
      b.lift += (h - b.lift) * (1 - Math.exp(-8 * dt))
      if (b.dragging) b.tilt += (0 - b.tilt) * (1 - Math.exp(-7 * dt))
      // 角度限位软截止：到限位时同步消去继续外推的角速度，避免硬掰回弹的僵硬感
      if (b.angle > 0.3) { b.angle = 0.3; if (b.va > 0) b.va = 0 }
      else if (b.angle < -0.3) { b.angle = -0.3; if (b.va < 0) b.va = 0 }
    }
  }

  // 墙/池底冲量：c 为接触角点（相对质心），n 为墙面外法线。
  // 冲量作用在接触点而非质心，偏心碰撞经力臂 r×J 产生自然扭矩；附库仑摩擦。
  function applyWall(b, c, nx, ny, rest, mu) {
    if (b.dragging) return
    let rvx = b.vx - b.va * c.ry
    let rvy = b.vy + b.va * c.rx
    const vn = rvx * nx + rvy * ny
    if (vn >= 0) return
    // 法向冲量（有效质量含转动项）
    const rn = c.rx * ny - c.ry * nx
    const kN = 1 + rn * rn * b.invI
    const jn = -(1 + rest) * vn / kN
    b.vx += jn * nx; b.vy += jn * ny; b.va += rn * jn * b.invI
    if (-vn > 14) water.disturb(b.x, b.y, 42, clamp(-vn * 0.028, 0, 1.0))
    // 切向摩擦冲量
    rvx = b.vx - b.va * c.ry
    rvy = b.vy + b.va * c.rx
    const tx = -ny, ty = nx
    const vt = rvx * tx + rvy * ty
    const rt = c.rx * ty - c.ry * tx
    const kT = 1 + rt * rt * b.invI
    let jt = -vt / kT
    const maxF = mu * jn
    if (jt > maxF) jt = maxF; else if (jt < -maxF) jt = -maxF
    b.vx += jt * tx; b.vy += jt * ty; b.va += rt * jt * b.invI
  }

  function resolveBoundaries() {
    const bounce = 0.38, mu = 0.25
    for (const b of bodies) {
      const { hw, hh } = halfExtents(b)
      // 底部按卡片实际渲染变换计算视觉最低点：旋转角为 angle+tilt、lift 抬升位移、scale 缩放，
      // 否则视觉上还没碰到池底就会被提前弹回
      const rotV = b.angle + b.tilt
      const scaleV = 1 + b.lift * 0.014
      const liftPx = -b.lift * 5.5
      const hhVis = ((b.w / 2) * Math.abs(Math.sin(rotV)) + (b.h / 2) * Math.abs(Math.cos(rotV))) * scaleV
      const bottomLimit = boundBottom - hhVis - liftPx
      // 接触点按物理角 angle 选取（SAT 碰撞体本身不含 tilt）；法线指向自由空间（推回方向）
      if (b.x < hw) { b.x = hw; applyWall(b, wallContact(b, 0), 1, 0, bounce, mu) }
      else if (b.x > W - hw) { b.x = W - hw; applyWall(b, wallContact(b, 1), -1, 0, bounce, mu) }
      if (b.y < hh) { b.y = hh; applyWall(b, wallContact(b, 2), 0, 1, bounce, mu) }
      else if (b.y > bottomLimit) { b.y = bottomLimit; applyWall(b, wallContact(b, 3), 0, -1, bounce, mu) }
    }
  }

  function resolveCollisions() {
    const e = 0.28, mu = 0.3
    for (let iter = 0; iter < SOLVER_ITERS; iter++) {
      let anyHit = false
      for (let i = 0; i < bodies.length; i++) {
        for (let j = i + 1; j < bodies.length; j++) {
          const a = bodies[i], b = bodies[j]
          const hit = satCollide(a, b); if (!hit) continue; anyHit = true
          const { nx, ny, depth } = hit
          const invMA = a.dragging ? 0 : a.invM, invMB = b.dragging ? 0 : b.invM
          const invIA = a.dragging ? 0 : a.invI, invIB = b.dragging ? 0 : b.invI
          const invSum = invMA + invMB
          if (invSum === 0) continue
          // 位置修正（质心级，带 slop 的 Baumgarte）
          const mag = Math.max(depth - SOLVER_SLOP, 0)
          if (mag > 0) {
            const corr = (mag * SOLVER_PERCENT) / invSum
            a.x -= nx * corr * invMA; a.y -= ny * corr * invMA
            b.x += nx * corr * invMB; b.y += ny * corr * invMB
          }
          // 接触点近似取两质心中点；r 为质心→接触点的力臂
          const cx = (a.x + b.x) * 0.5, cy = (a.y + b.y) * 0.5
          const rax = cx - a.x, ray = cy - a.y
          const rbx = cx - b.x, rby = cy - b.y
          // 接触点速度 = 质心速度 + ω×r
          let rvx = (b.vx - b.va * rby) - (a.vx - a.va * ray)
          let rvy = (b.vy + b.va * rbx) - (a.vy + a.va * rax)
          const vn = rvx * nx + rvy * ny
          if (vn >= 0) continue
          // 法向冲量：有效质量含转动项 (r×n)²·invI
          const rnA = rax * ny - ray * nx
          const rnB = rbx * ny - rby * nx
          const kN = invMA + invMB + rnA * rnA * invIA + rnB * rnB * invIB
          const jn = (-(1 + e) * vn) / kN
          a.vx -= jn * nx * invMA; a.vy -= jn * ny * invMA; a.va -= rnA * jn * invIA
          b.vx += jn * nx * invMB; b.vy += jn * ny * invMB; b.va += rnB * jn * invIB
          // 法向冲量施加后重算接触点相对速度，再施加库仑摩擦（切向冲量）
          rvx = (b.vx - b.va * rby) - (a.vx - a.va * ray)
          rvy = (b.vy + b.va * rbx) - (a.vy + a.va * rax)
          const tx = -ny, ty = nx
          const vt = rvx * tx + rvy * ty
          const rtA = rax * ty - ray * tx
          const rtB = rbx * ty - rby * tx
          const kT = invMA + invMB + rtA * rtA * invIA + rtB * rtB * invIB
          let jt = -vt / kT
          // 库仑锥截断：|jt| ≤ μ·jn
          const maxF = mu * jn
          if (jt > maxF) jt = maxF; else if (jt < -maxF) jt = -maxF
          a.vx -= jt * tx * invMA; a.vy -= jt * ty * invMA; a.va -= rtA * jt * invIA
          b.vx += jt * tx * invMB; b.vy += jt * ty * invMB; b.va += rtB * jt * invIB
          if (-vn > 55) water.disturb((a.x + b.x) / 2, (a.y + b.y) / 2, 46, clamp(-vn * 0.018, 0, 2.2))
        }
      }
      if (!anyHit) break
    }
  }

  function drawCaustics() {
    if (!cCtx) return
    cCtx.setTransform(dpr, 0, 0, dpr, 0, 0)
    cCtx.clearRect(0, 0, W, H)
    cCtx.globalCompositeOperation = 'lighter'
    const step = 28, R = step * 1.45, D = R * 2
    for (let y = -step; y < H + step; y += step) {
      // 顶部 25% 区域焦散衰减到最低 30%，避免上部过亮
      const topFade = 0.3 + 0.7 * clamp(y / (H * 0.25), 0, 1)
      for (let x = -step; x < W + step; x += step) {
        const h = water.sampleH(x, y); if (h <= 0.2) continue
        const a = Math.min(0.32, (h - 0.2) * 0.22) * topFade; if (a < 0.015) continue
        const g = water.sampleGrad(x, y)
        const ox = clamp(g.gx * 240, -20, 20)
        const oy = clamp(g.gy * 240, -20, 20)
        cCtx.globalAlpha = a
        cCtx.drawImage(glowSprite, x + ox - R, y + oy - R, D, D)
      }
    }
    cCtx.globalAlpha = 1
  }

  function drawWaterSurface() {
    if (!wCtx) return
    wCtx.setTransform(dpr, 0, 0, dpr, 0, 0)
    wCtx.clearRect(0, 0, W, H)
    wCtx.globalCompositeOperation = 'source-over'
    // 卡片在水中的阴影
    for (const b of bodies) {
      wCtx.save()
      wCtx.translate(b.x + 5, b.y + 16)
      wCtx.rotate(b.angle + b.tilt)
      const r = b.w * 0.9
      const g = wCtx.createRadialGradient(0, 0, 0, 0, 0, r)
      g.addColorStop(0, 'rgba(8,52,88,0.28)')
      g.addColorStop(1, 'rgba(8,52,88,0)')
      wCtx.fillStyle = g
      wCtx.scale(1, 0.62)
      wCtx.beginPath(); wCtx.arc(0, 0, r, 0, TAU); wCtx.fill()
      wCtx.restore()
    }
    // 水面高光/暗纹
    const step = 12
    for (let y = -step; y < H + step; y += step) {
      for (let x = -step; x < W + step; x += step) {
        const h = water.sampleH(x, y)
        if (h > 0.12) {
          wCtx.fillStyle = `rgba(255,255,255,${Math.min(0.15, h * 0.065)})`
          wCtx.fillRect(x, y, step + 1, step + 1)
        } else if (h < -0.12) {
          wCtx.fillStyle = `rgba(0,45,85,${Math.min(0.18, -h * 0.075)})`
          wCtx.fillRect(x, y, step + 1, step + 1)
        }
      }
    }
  }

  function renderCards() {
    for (const b of bodies) {
      const liftPx = -b.lift * 5.5
      const scale = 1 + b.lift * 0.014
      const rot = b.angle + b.tilt
      b.el.style.transform =
        `translate3d(${(b.x - b.w / 2).toFixed(2)}px,${(b.y - b.h / 2 + liftPx).toFixed(2)}px,0) ` +
        `rotate(${rot.toFixed(4)}rad) scale(${scale.toFixed(4)})`
    }
  }

  // 拖动：按下立即起拖，卡片中心弹性跟随指针；移动距离仅用于区分点击/拖拽
  function onDown(e, post) {
    if (dragBody) return
    // dataset.postId 为字符串，bodyMap 以字符串建键，这里必须转换类型
    const b = bodyMap.get(String(post.postId))
    if (!b) return
    const rect = poolRef.value.getBoundingClientRect()
    const px = e.clientX - rect.left
    const py = e.clientY - rect.top
    b.dragging = true
    b.moved = false
    b.startX = px
    b.startY = py
    b.ox = px
    b.oy = py
    b.el.classList.add('dragging')
    b.el.style.zIndex = ++zIdx
    dragBody = b
    dragPointerId = e.pointerId
    try { b.el.setPointerCapture(e.pointerId) } catch (_) {}
    water.disturb(b.x, b.y, 42, 1.8)
  }

  function onMove(e) {
    if (!dragBody) return
    if (e.buttons === 0) { onUp(e); return }
    if (dragPointerId !== null && e.pointerId !== undefined && e.pointerId !== dragPointerId) return
    e.preventDefault()
    const b = dragBody
    const rect = poolRef.value.getBoundingClientRect()
    const px = e.clientX - rect.left
    const py = e.clientY - rect.top
    if (!b.moved && Math.hypot(px - b.startX, py - b.startY) > 5) b.moved = true
    const hw = b.w / 2, hh = b.h / 2
    b.ox = clamp(px, hw, Math.max(hw, W - hw))
    b.oy = clamp(py, hh, Math.max(hh, boundBottom - hh))
  }

  function onUp() {
    if (!dragBody) return
    const b = dragBody
    b.dragging = false
    b.el.classList.remove('dragging')
    if (dragPointerId !== null) { try { b.el.releasePointerCapture(dragPointerId) } catch (_) {} }
    water.disturb(b.x, b.y, 52, 2.2)
    // 只有真正拖动过（移动 > 5px）才抑制点击导航
    if (b.moved) {
      justDragged = true
      setTimeout(() => { justDragged = false }, 300)
    }
    dragPointerId = null
    dragBody = null
  }

  function onVisibilityChange() { if (document.hidden) onUp() }

  async function initPool() {
    await nextTick()
    if (!poolRef.value || !waterCanvas.value || !causticsCanvas.value || !mistCanvas.value) return
    wCtx = waterCanvas.value.getContext('2d', { alpha: true })
    cCtx = causticsCanvas.value.getContext('2d', { alpha: true })
    mCtx = mistCanvas.value.getContext('2d', { alpha: true })
    glowSprite = buildGlowSprite()
    resizeObs = new ResizeObserver(entries => {
      for (const ent of entries) {
        const el = ent.target
        const b = bodyMap.get(el.dataset.postId)
        if (!b) continue
        b.w = el.offsetWidth
        b.h = el.offsetHeight
        b.invI = 12 / (b.w * b.w + b.h * b.h)
        b.x = clamp(b.x, b.w / 2, Math.max(b.w / 2, W - b.w / 2))
        b.y = clamp(b.y, b.h / 2, Math.max(b.h / 2, H - b.h / 2))
      }
    })
    handleResize()
    initialized = true
    mist.init(W, H)
    rebuildBodies()
    window.addEventListener('pointermove', onMove, { passive: false })
    window.addEventListener('pointerup', onUp)
    window.addEventListener('pointercancel', onUp)
    window.addEventListener('blur', onUp)
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener('resize', handleResize)
    loop()
  }

  function disposePool() {
    cancelAnimationFrame(rafId); rafId = 0
    clearTimeout(relayoutTimer)
    if (resizeObs) { resizeObs.disconnect(); resizeObs = null }
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerup', onUp)
    window.removeEventListener('pointercancel', onUp)
    window.removeEventListener('blur', onUp)
    document.removeEventListener('visibilitychange', onVisibilityChange)
    window.removeEventListener('resize', handleResize)
    initialized = false
  }

  return {
    initPool,
    disposePool,
    rebuildBodies,
    onDown,
    isJustDragged: () => justDragged,
    getLayout: () => curLayout,
  }
}
