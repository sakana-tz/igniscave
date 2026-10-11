// 雾气粒子系统：团状软雾精灵 + 随机方向飘荡（wander 转向游走）
// 雾团数量/尺寸/浓度按浴场实际面积自适应，避免小浴场上雾团堆叠糊成白屏
import { TAU, rand, clamp } from './physics'

const MIST_COUNT_DESKTOP = 34
// 桌面参考面积：1280 宽浴场约 900 高时恰好 34 个雾团
const REF_AREA = 1280 * 900
const MIN_COUNT = 10, MAX_COUNT = 34

// 离屏雾团精灵：一个蓬松主团 + 3~5 个围绕小瓣，整体呈“一团雾”而非散点
function buildMistSprites() {
  const sprites = []
  for (let s = 0; s < 7; s++) {
    const size = 256
    const cv = document.createElement('canvas')
    cv.width = cv.height = size
    const g = cv.getContext('2d')

    // 主团：大而软的中心
    const lobes = 3 + ((Math.random() * 3) | 0)
    const allLobes = [{ cx: size / 2, cy: size / 2, r: size * rand(0.4, 0.48), main: true }]
    for (let i = 0; i < lobes; i++) {
      const ang = rand(0, TAU)
      const dist = size * rand(0.12, 0.26)
      allLobes.push({
        cx: size / 2 + Math.cos(ang) * dist,
        cy: size / 2 + Math.sin(ang) * dist,
        r: size * rand(0.22, 0.34),
      })
    }
    for (const L of allLobes) {
      const grad = g.createRadialGradient(L.cx, L.cy, 0, L.cx, L.cy, L.r)
      if (L.main) {
        grad.addColorStop(0, 'rgba(255,255,255,0.85)')
        grad.addColorStop(0.4, 'rgba(242,250,255,0.55)')
        grad.addColorStop(0.75, 'rgba(220,240,255,0.2)')
        grad.addColorStop(1, 'rgba(208,234,255,0)')
      } else {
        grad.addColorStop(0, 'rgba(255,255,255,0.66)')
        grad.addColorStop(0.55, 'rgba(232,246,255,0.3)')
        grad.addColorStop(1, 'rgba(210,236,255,0)')
      }
      g.fillStyle = grad
      g.beginPath(); g.arc(L.cx, L.cy, L.r, 0, TAU); g.fill()
    }
    sprites.push(cv)
  }
  return sprites
}

// 按浴场尺寸计算雾气参数：
// - sizeFactor：雾团整体缩放（375px 手机约 0.5，最大不超过 1）
// - count：数量按面积等比（手机约 10 个，桌面 34 个）
// - alphaFactor：小浴场同步降低单团浓度，防止多层叠加盖住帖子
function computeParams(W, H) {
  const sizeFactor = clamp(W / 1024, 0.5, 1)
  const count = clamp(Math.round(MIST_COUNT_DESKTOP * (W * H) / REF_AREA), MIN_COUNT, MAX_COUNT)
  const alphaFactor = clamp(0.72 + 0.28 * ((W - 340) / (1024 - 340)), 0.72, 1)
  return { sizeFactor, count, alphaFactor }
}

export function createMistLayer() {
  let sprites = []
  let blobs = []
  let sizeFactor = 1
  let alphaFactor = 1

  function makeBlob(initial, W, H) {
    const sprite = sprites[(Math.random() * sprites.length) | 0]
    const maxLife = rand(16, 30)
    const heading = rand(0, TAU)
    return {
      // 雾主要在浴池中下部生成，避免顶部白雾堆积
      x: rand(-W * 0.1, W * 1.1), y: rand(H * 0.25, H * 1.02),
      heading,
      wander: rand(-0.5, 0.5),         // 当前转向速率
      wanderSpd: rand(0.5, 1.4),       // 转向变化快慢
      speed: rand(6, 16),              // 飘荡速度
      lift: rand(-3, 1),               // 额外的缓慢浮沉
      sprite,
      scale: rand(1.1, 2.3),
      breathe: rand(0.02, 0.06), breathePhase: rand(0, TAU),
      rot: rand(0, TAU), rotSpd: rand(-0.05, 0.05),
      baseAlpha: rand(0.36, 0.56),
      life: initial ? Math.random() * maxLife : 0, maxLife,
    }
  }

  return {
    init(W, H) {
      sprites = buildMistSprites()
      const p = computeParams(W, H)
      sizeFactor = p.sizeFactor
      alphaFactor = p.alphaFactor
      blobs = []
      for (let i = 0; i < p.count; i++) blobs.push(makeBlob(true, W, H))
    },
    // 浴场尺寸变化：只增删雾团数量并更新缩放/浓度参数，不打乱存活雾团的运动
    resize(W, H) {
      if (!sprites.length) return
      const p = computeParams(W, H)
      sizeFactor = p.sizeFactor
      alphaFactor = p.alphaFactor
      while (blobs.length < p.count) blobs.push(makeBlob(true, W, H))
      if (blobs.length > p.count) blobs.length = p.count
      for (const b of blobs) {
        b.x = clamp(b.x, -W * 0.3, W * 1.3)
        b.y = clamp(b.y, -H * 0.3, H * 1.15)
      }
    },
    update(dt, W, H) {
      for (let i = 0; i < blobs.length; i++) {
        let b = blobs[i]
        b.life += dt
        if (b.life >= b.maxLife) { blobs[i] = makeBlob(false, W, H); continue }

        // 随机飘荡：转向速率本身被随机扰动（wander），航向缓慢变化 -> 自然曲线漂移
        b.wander += rand(-1, 1) * b.wanderSpd * dt
        b.wander = clamp(b.wander, -0.9, 0.9)
        b.heading += b.wander * dt
        b.x += Math.cos(b.heading) * b.speed * dt
        b.y += (Math.sin(b.heading) * b.speed + b.lift) * dt
        b.rot += b.rotSpd * dt

        // 漂出浴池边缘：从对侧回到场内，保持雾量稳定；边距随雾团尺寸收缩
        const m = 120 * sizeFactor
        if (b.x < -m) b.x = W + m * 0.5
        else if (b.x > W + m) b.x = -m * 0.5
        if (b.y < -m) b.y = H + m * 0.5
        else if (b.y > H + m) b.y = -m * 0.5
      }
    },
    draw(mCtx, dpr, W, H, time) {
      if (!mCtx) return
      mCtx.setTransform(dpr, 0, 0, dpr, 0, 0)
      mCtx.clearRect(0, 0, W, H)
      mCtx.globalCompositeOperation = 'source-over'
      for (const b of blobs) {
        const t = b.life / b.maxLife
        // 雾漂进浴池顶部 16% 区域时逐渐隐去，保证上部通透
        const topFade = clamp(b.y / (H * 0.16), 0, 1)
        // 呼吸式轻微缩放
        const breatheScale = 1 + Math.sin(time * 0.6 + b.breathePhase) * b.breathe
        const alpha = b.baseAlpha * alphaFactor * Math.sin(Math.PI * t) * topFade
        if (alpha < 0.008) continue
        const size = 256 * b.scale * breatheScale * sizeFactor
        mCtx.save()
        mCtx.globalAlpha = alpha
        mCtx.translate(b.x, b.y)
        mCtx.rotate(b.rot)
        mCtx.drawImage(b.sprite, -size / 2, -size / 2, size, size)
        mCtx.restore()
      }
      mCtx.globalAlpha = 1
    },
  }
}
