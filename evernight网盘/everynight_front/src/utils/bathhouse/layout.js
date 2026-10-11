// 浴场响应式布局：按浴池内容区宽度动态计算卡片尺寸等级、列数与建议每页卡片数
import { clamp } from './physics'

const COL_GAP = 12
const XL_MAX = 245 // 宽屏 xl 卡上限（桌面原始基准）

// 宽屏各等级相对 xl 的比例：s .571 / m .714 / l .857 / xl 1
// 窄屏主动压缩大小卡差距（s 提到 .80），避免两张大卡同排时互相挤压重叠
const FRAC_DESKTOP = { s: 0.571, m: 0.714, l: 0.857 }
const FRAC_MOBILE = { s: 0.8, m: 0.87, l: 0.93 }

// avail = 浴池 clientWidth - 左右内边距
// 返回 { cols, gap, widths: { s, m, l, xl } }
export function computeCardLayout(avail) {
  const cols = avail >= 1000 ? 4 : avail >= 620 ? 3 : 2
  // 先让最大的 xl 卡每行 cols 张也能宽松放下，其余等级按比例收缩
  const xl = Math.round(clamp((avail - COL_GAP * (cols - 1) - 20) / cols, 104, XL_MAX))
  // t：0=手机（压缩差距）→ 1=宽屏（完整四档差距）
  const t = clamp((avail - 300) / 760, 0, 1)
  const lerp = (a, b) => a + (b - a) * t
  const widths = {
    s: Math.round(xl * lerp(FRAC_MOBILE.s, FRAC_DESKTOP.s)),
    m: Math.round(xl * lerp(FRAC_MOBILE.m, FRAC_DESKTOP.m)),
    l: Math.round(xl * lerp(FRAC_MOBILE.l, FRAC_DESKTOP.l)),
    xl,
  }
  return { cols, gap: COL_GAP, widths }
}

// 建议每页卡片数：保持约 3 行的浏览密度（4列12 / 3列9 / 2列6）
export function recommendedPageSize(cols) {
  return cols >= 4 ? 12 : cols === 3 ? 9 : 6
}
