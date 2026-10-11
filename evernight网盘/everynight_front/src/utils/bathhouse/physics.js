// 浴场物理：基础数学工具 + 旋转矩形 SAT 碰撞
export const TAU = Math.PI * 2
export const clamp = (v, a, b) => Math.max(a, Math.min(b, v))
export const rand = (a, b) => a + Math.random() * (b - a)

function getCorners(b) {
  const cos = Math.cos(b.angle), sin = Math.sin(b.angle)
  const hw = b.w / 2, hh = b.h / 2, cx = b.x, cy = b.y
  return [
    { x: cx + cos * -hw - sin * -hh, y: cy + sin * -hw + cos * -hh },
    { x: cx + cos *  hw - sin * -hh, y: cy + sin *  hw + cos * -hh },
    { x: cx + cos *  hw - sin *  hh, y: cy + sin *  hw + cos *  hh },
    { x: cx + cos * -hw - sin *  hh, y: cy + sin * -hw + cos *  hh },
  ]
}

function projectOnto(corners, ax, ay) {
  let min = Infinity, max = -Infinity
  for (const c of corners) { const p = c.x * ax + c.y * ay; if (p < min) min = p; if (p > max) max = p }
  return { min, max }
}

// 返回两旋转矩形的最小穿透轴（nx/ny 指向 b 相对 a 的分离方向），未碰撞返回 null
export function satCollide(a, b) {
  const ca = getCorners(a), cb = getCorners(b)
  const cosA = Math.cos(a.angle), sinA = Math.sin(a.angle)
  const cosB = Math.cos(b.angle), sinB = Math.sin(b.angle)
  const axes = [[cosA, sinA], [-sinA, cosA], [cosB, sinB], [-sinB, cosB]]
  let minDepth = Infinity, nx = 0, ny = 0
  for (let i = 0; i < axes.length; i++) {
    const ax = axes[i][0], ay = axes[i][1]
    const pa = projectOnto(ca, ax, ay), pb = projectOnto(cb, ax, ay)
    const overlap = Math.min(pa.max, pb.max) - Math.max(pa.min, pb.min)
    if (overlap <= 0) return null
    if (overlap < minDepth) { minDepth = overlap; nx = ax; ny = ay }
  }
  if ((b.x - a.x) * nx + (b.y - a.y) * ny < 0) { nx = -nx; ny = -ny }
  return { nx, ny, depth: minDepth }
}

// 旋转矩形的世界轴包围半宽/半高（用于边界约束）
export function halfExtents(b) {
  const c = Math.abs(Math.cos(b.angle)), s = Math.abs(Math.sin(b.angle))
  return { hw: (b.w / 2) * c + (b.h / 2) * s, hh: (b.w / 2) * s + (b.h / 2) * c }
}

// 卡片与轴对齐墙面的接触点（相对质心向量）。dir：0=左墙 1=右墙 2=顶 3=底。
// 角碰取最深入的角点；边与墙平贴时两近角并列，取其平均（=边中点），
// 避免平贴碰撞在边缘点凭空产生扭矩。
export function wallContact(b, dir) {
  const cos = Math.cos(b.angle), sin = Math.sin(b.angle)
  const hw = b.w / 2, hh = b.h / 2
  const cs = [[-hw, -hh], [hw, -hh], [hw, hh], [-hw, hh]]
    .map(([lx, ly]) => [cos * lx - sin * ly, sin * lx + cos * ly])
  const axis = dir <= 1 ? 0 : 1
  let ext
  if (dir === 0) ext = Math.min(...cs.map(c => c[axis]))
  else if (dir === 1) ext = Math.max(...cs.map(c => c[axis]))
  else if (dir === 2) ext = Math.min(...cs.map(c => c[axis]))
  else ext = Math.max(...cs.map(c => c[axis]))
  const hits = cs.filter(c => Math.abs(c[axis] - ext) < 0.75)
  return {
    rx: hits.reduce((a, c) => a + c[0], 0) / hits.length,
    ry: hits.reduce((a, c) => a + c[1], 0) / hits.length,
  }
}
