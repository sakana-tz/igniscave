// 水面：网格化的波动方程模拟（高度场 + 速度场 + 边界阻尼）
export class WaterSurface {
  constructor(w, h, resolution = 5) {
    this.res = resolution
    this.baseDamping = 0.996
    this.spread = 0.16
    this.allocate(w, h)
  }
  allocate(w, h) {
    this.cols = Math.max(8, Math.ceil(w / this.res) + 1)
    this.rows = Math.max(8, Math.ceil(h / this.res) + 1)
    this.size = this.cols * this.rows
    this.curr = new Float32Array(this.size)
    this.vel = new Float32Array(this.size)
    this.damp = new Float32Array(this.size)
    const edge = 14
    for (let y = 0; y < this.rows; y++) {
      for (let x = 0; x < this.cols; x++) {
        const d = Math.min(x, y, this.cols - 1 - x, this.rows - 1 - y)
        const k = d < edge ? 0.955 + 0.045 * (d / edge) : 1
        this.damp[y * this.cols + x] = this.baseDamping * k
      }
    }
  }
  resize(w, h) { this.allocate(w, h) }
  update() {
    const { cols, rows, curr, vel, damp, spread } = this
    for (let y = 1; y < rows - 1; y++) {
      const r = y * cols
      for (let x = 1; x < cols - 1; x++) {
        const i = r + x
        const lap = curr[i - 1] + curr[i + 1] + curr[i - cols] + curr[i + cols] - 4 * curr[i]
        vel[i] = (vel[i] + lap * spread) * damp[i]
      }
    }
    for (let i = 0; i < this.size; i++) curr[i] += vel[i]
  }
  disturb(x, y, radius, strength) {
    const cols = this.cols, rows = this.rows, res = this.res
    const cx = Math.floor(x / res), cy = Math.floor(y / res)
    const R = Math.max(1, Math.ceil(radius / res))
    const r1 = R * 0.55, R2 = R * R
    for (let dy = -R; dy <= R; dy++) {
      const py = cy + dy
      if (py < 1 || py >= rows - 1) continue
      for (let dx = -R; dx <= R; dx++) {
        const px = cx + dx
        if (px < 1 || px >= cols - 1) continue
        const d2 = dx * dx + dy * dy
        if (d2 > R2) continue
        const dist = Math.sqrt(d2)
        const i = py * cols + px
        if (dist <= r1) {
          const t = dist / r1
          const f = Math.cos(t * Math.PI * 0.5)
          this.curr[i] += strength * f * f
        } else {
          const t = (dist - r1) / (R - r1)
          this.curr[i] -= strength * 0.40 * Math.sin(t * Math.PI)
        }
      }
    }
  }
  driveTo(x, y, radius, target, k = 0.16) {
    const cols = this.cols, rows = this.rows, res = this.res
    const cx = Math.floor(x / res), cy = Math.floor(y / res)
    const R = Math.max(1, Math.ceil(radius / res)), R2 = R * R
    for (let dy = -R; dy <= R; dy++) {
      const py = cy + dy
      if (py < 1 || py >= rows - 1) continue
      for (let dx = -R; dx <= R; dx++) {
        const px = cx + dx
        if (px < 1 || px >= cols - 1) continue
        const d2 = dx * dx + dy * dy
        if (d2 > R2) continue
        const f = Math.cos(Math.sqrt(d2) / R * Math.PI * 0.5)
        const i = py * cols + px
        this.vel[i] += (target - this.curr[i]) * k * f
      }
    }
  }
  sampleH(x, y) {
    const c = this.curr, cols = this.cols, res = this.res
    let fx = x / res, fy = y / res
    if (fx < 0) fx = 0; else if (fx > cols - 1.001) fx = cols - 1.001
    if (fy < 0) fy = 0; else if (fy > this.rows - 1.001) fy = this.rows - 1.001
    const x0 = fx | 0, y0 = fy | 0
    const tx = fx - x0, ty = fy - y0
    const i = y0 * cols + x0
    const a = c[i] + (c[i + 1] - c[i]) * tx
    const b = c[i + cols] + (c[i + cols + 1] - c[i + cols]) * tx
    return a + (b - a) * ty
  }
  sampleGrad(x, y) {
    const e = this.res * 2
    return {
      gx: (this.sampleH(x + e, y) - this.sampleH(x - e, y)) / (2 * e),
      gy: (this.sampleH(x, y + e) - this.sampleH(x, y - e)) / (2 * e),
    }
  }
}
