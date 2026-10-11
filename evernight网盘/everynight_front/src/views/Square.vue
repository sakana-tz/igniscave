<template>
  <div class="square-wrap">
    <!-- 标题在浴池外面：正常文档流，位于浴池上方，不遮挡卡片 -->
    <div class="page-head">
      <h1>奥赫玛浴场</h1>
      <p class="subtitle">{{ viewMode === 'mine' ? '我的帖子 · 草稿与已发布' : '这里其实没有搓澡师傅喵' }}</p>
    </div>

    <div ref="poolRef" class="square-container">
      <canvas ref="causticsCanvas" class="cv caustics"></canvas>
      <canvas ref="waterCanvas" class="cv water"></canvas>

    <!-- 整个容器即浴池：水面/焦散/雾气/浓金光 + 漂浮卡片 -->
      <div v-if="loading" class="state-tip">加载中...</div>
      <div v-else-if="errorMsg" class="state-tip error">{{ errorMsg }}</div>
      <div v-else-if="list.length === 0" class="state-tip">
        <template v-if="viewMode === 'mine'">{{ keyword ? '没有找到相关帖子' : '你还没有发过帖子，点击右上角“发帖”试试吧~' }}</template>
        <template v-else>{{ keyword ? '没有找到相关帖子' : '我家浴场还蛮大的~' }}</template>
      </div>

      <template v-else>
        <div
          v-for="post in list"
          :key="post.postId"
          :data-post-id="post.postId"
          class="post-card"
          :class="['size-' + cardSize(post), { 'is-draft': post.postStatus === 0 }]"
          @pointerdown="onDown($event, post)"
          @click="onCardClick(post, $event)"
        >
          <div v-if="coverMap[post.postId]" class="post-cover">
            <img :src="coverMap[post.postId]" alt="" loading="lazy" draggable="false" />
            <span v-if="post.postStatus === 0" class="draft-badge">草稿</span>
          </div>
          <h3 class="post-title" :title="post.title">
            <span v-if="post.postStatus === 0" class="draft-tag">草稿</span>
            <span v-else-if="isHot(post.postId)" class="hot-flame" title="热门帖子">🔥</span>
            {{ post.title }}
          </h3>
          <div class="post-meta">
            <span class="meta-item">浏览 {{ formatNumber(post.viewCount) }}</span>
            <span class="meta-item">评论 {{ post.commentCount }}</span>
            <span class="meta-item time">{{ post.updatedTime }}</span>
          </div>
        </div>
      </template>

    <div class="pagination">
      <button class="btn" :disabled="pageNo <= 1 || loading" @click="changePage(pageNo - 1)">
        上一页
      </button>
      <span class="page-num">
        <input
          class="page-input"
          type="number"
          min="1"
          :max="totalPages"
          v-model.number="pageInput"
          @keyup.enter="jumpToPage"
          @blur="jumpToPage"
        />
        <span class="page-info">/ {{ totalPages }}</span>
      </span>
      <button
        class="btn"
        :disabled="pageNo >= totalPages || loading"
        @click="changePage(pageNo + 1)"
      >
        下一页
      </button>
      <input
        class="page-size-input"
        type="number"
        min="1"
        max="50"
        v-model.number="pageSizeInput"
        title="每页卡片数"
        @keyup.enter="onPageSizeBlur"
        @blur="onPageSizeBlur"
      />
      <span class="page-size-label">条/页</span>
    </div>

      <canvas ref="mistCanvas" class="cv mist"></canvas>
      <div class="golden-light" aria-hidden="true">
        <div class="glow glow-main"></div>
        <div class="glow glow-ul"></div>
        <div class="glow glow-ur"></div>
        <div class="glow glow-ml"></div>
        <div class="glow glow-mr"></div>
        <div class="glow glow-bl"></div>
        <div class="glow glow-br"></div>
        <div class="glow glow-spark"></div>
        <div class="glow glow-spark2"></div>
      </div>

      <!-- 水波扰动滤镜：让金色条纹随噪声扭曲，产生随水流动的灯光感 -->
      <svg aria-hidden="true" style="position:absolute;width:0;height:0;overflow:hidden">
        <filter id="water-ripple" x="-10%" y="-10%" width="120%" height="120%">
          <feTurbulence type="fractalNoise" baseFrequency="0.013 0.02" numOctaves="2" seed="5" result="noise">
            <animate attributeName="baseFrequency" dur="10s" values="0.013 0.02;0.015 0.023;0.012 0.019;0.013 0.02" keyTimes="0;0.33;0.66;1" calcMode="spline" keySplines="0.4 0 0.6 1;0.4 0 0.6 1;0.4 0 0.6 1" repeatCount="indefinite"/>
            <animate attributeName="seed" dur="14s" values="5;7;5" repeatCount="indefinite"/>
          </feTurbulence>
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="16" xChannelSelector="R" yChannelSelector="G"/>
        </filter>
      </svg>
    </div>

    <!-- 搜索栏悬浮于水面上，z 低于卡片，不参与卡片物理 -->
    <div class="float-ui">
      <div class="toolbar">
        <div class="search-box">
          <input
            v-model="keywordInput"
            type="text"
            class="search-input"
            :placeholder="viewMode === 'mine' ? '在我的帖子中搜索...' : '按标题搜索帖子...'"
            maxlength="128"
            @keyup.enter="handleSearch"
          />
          <button class="btn btn-primary" @click="handleSearch">搜索</button>
          <button v-if="keyword" class="btn btn-ghost" @click="handleClearSearch">清除</button>
        </div>
        <div class="toolbar-actions">
          <button v-if="isLoggedIn" class="btn" @click="toggleViewMode">
            {{ viewMode === 'mine' ? '返回广场' : '我的帖子' }}
          </button>
          <button v-if="isLoggedIn" class="btn btn-primary" @click="goCreate">+ 发帖</button>
          <router-link v-if="!isLoggedIn" to="/login" class="btn btn-primary">登录后发帖</router-link>
        </div>
      </div>

      <p v-if="keyword" class="search-tip">
        搜索 “<strong>{{ keyword }}</strong>” 的结果（共 {{ total }} 条）
      </p>
    </div>
  </div>
</template>

<script setup>
// 浴场引擎（水面波动/焦散/雾气/卡片物理）已拆分到 src/utils/bathhouse/
// 本文件只保留页面模板 + 论坛业务逻辑
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getPostList, getMyPostList, getHotPostList } from '../api/forum'
import { isAuthenticated } from '../utils/auth'
import { getCachedCover } from '../utils/coverCache'
import { useBathhouse } from '../utils/bathhouse/useBathhouse'
import { recommendedPageSize } from '../utils/bathhouse/layout'

const router = useRouter()

const pageSize = ref(12) // 实际发给后端的每页卡片数
const pageSizeInput = ref(12) // 输入框绑定值，用户可自由输入
const userPickedPageSize = ref(false) // 用户手动输入过分页大小后不再自动调整

const list = ref([])
const total = ref(0)
const hotPostIds = ref(new Set())
const coverMap = ref({}) // postId -> coverUrl
const pageNo = ref(1)
const pageInput = ref(1)
const keyword = ref('')
const keywordInput = ref('')
const viewMode = ref('all') // 'all'=广场全部已发布帖子，'mine'=当前用户的帖子（含草稿）
const loading = ref(false)
const errorMsg = ref('')

const isLoggedIn = ref(isAuthenticated())
const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

// ============================ 浴场引擎接入 ============================
const poolRef = ref(null)
const waterCanvas = ref(null)
const causticsCanvas = ref(null)
const mistCanvas = ref(null)

// 浴场宽度跨档（旋转屏幕/缩放窗口）后同步推荐每页卡片数；用户手动输入过则尊重用户选择
const handleLayoutChange = (layout) => {
  if (userPickedPageSize.value || !layout) return
  const rec = recommendedPageSize(layout.cols)
  if (rec === pageSize.value) return
  pageSize.value = rec
  pageSizeInput.value = rec
  pageNo.value = 1
  pageInput.value = 1
  loadData()
}

const { initPool, disposePool, rebuildBodies, onDown, isJustDragged, getLayout } = useBathhouse({
  poolRef,
  waterCanvas,
  causticsCanvas,
  mistCanvas,
  onLayoutChange: handleLayoutChange,
})

// 列表变化时重建浮块（翻页/搜索/切视图/封面加载后高度变化由 ResizeObserver 处理）
watch(list, () => nextTick(rebuildBodies))

const onCardClick = (post, e) => {
  if (isJustDragged()) {
    e.preventDefault()
    e.stopPropagation()
    return
  }
  // 草稿在广场不可见，只能由作者从“我的帖子”进入编辑页继续编辑/发布
  if (post.postStatus === 0) {
    router.push(`/square/edit/${post.postId}`)
    return
  }
  goDetail(post.postId)
}

onUnmounted(() => { disposePool() })

// ============================ 论坛业务 ============================
// 兼容后端 Jackson default typing：将 ["java.util.ArrayList", [...]] 类型数组解包为普通数组
const unwrapTyped = (value) => {
  if (Array.isArray(value) && value.length === 2 && typeof value[0] === 'string' && Array.isArray(value[1])) {
    return value[1]
  }
  return value
}

// 卡片尺寸按浏览量排名分配：第1名=大大(xl)，第2~3名=大(l)，第4~6名=中(m)，其余=小(s)
const cardSizeMap = computed(() => {
  const ranked = [...list.value]
    .sort((a, b) => {
      const diff = (Number(b.viewCount) || 0) - (Number(a.viewCount) || 0)
      return diff !== 0 ? diff : a.postId - b.postId // 浏览量相同按 postId 稳定排序
    })
  const map = new Map()
  ranked.forEach((post, idx) => {
    const rank = idx + 1
    let size = 's'
    if (rank === 1) size = 'xl'
    else if (rank <= 3) size = 'l'
    else if (rank <= 6) size = 'm'
    map.set(post.postId, size)
  })
  return map
})

const cardSize = (post) => cardSizeMap.value.get(post.postId) || 's'

const formatNumber = (num) => {
  const n = Number(num) || 0
  return n >= 10000 ? (n / 10000).toFixed(1) + 'w' : n
}

const loadData = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const fetcher = viewMode.value === 'mine' ? getMyPostList : getPostList
    const data = await fetcher(keyword.value, pageNo.value, pageSize.value)
    list.value = unwrapTyped(data?.list) || []
    total.value = data?.total || 0
    // 异步批量加载封面缩略图，不阻塞列表渲染
    loadCoversForList(list.value)
  } catch (error) {
    errorMsg.value = error.message || '加载帖子失败'
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 切换“广场全部 / 我的帖子”视图
const toggleViewMode = () => {
  viewMode.value = viewMode.value === 'mine' ? 'all' : 'mine'
  keyword.value = ''
  keywordInput.value = ''
  pageNo.value = 1
  loadData()
  // 热门标记只与广场视图相关
  if (viewMode.value === 'all' && hotPostIds.value.size === 0) {
    loadHotPosts()
  }
}

const loadCoversForList = async (posts) => {
  const next = { ...coverMap.value }
  await Promise.all(
    posts.map(async (post) => {
      try {
        // 走 IndexedDB 封面缓存，命中时不再请求后端与 OSS
        next[post.postId] = (await getCachedCover(post.postId)) || ''
      } catch (e) {
        next[post.postId] = next[post.postId] || ''
      }
    })
  )
  coverMap.value = next
}

const loadHotPosts = async () => {
  try {
    const data = await getHotPostList()
    const hotList = unwrapTyped(data) || []
    hotPostIds.value = new Set(hotList.map(p => p.postId))
  } catch (error) {
    console.error('热门帖子加载失败:', error.message)
  }
}

// 判断某帖子是否在热门 Top10 中
const isHot = (postId) => hotPostIds.value.has(postId)

const handleSearch = () => {
  const kw = keywordInput.value.trim()
  if (kw === keyword.value) {
    pageNo.value = 1
    pageInput.value = 1
    loadData()
    return
  }
  keyword.value = kw
  pageNo.value = 1
  pageInput.value = 1
  loadData()
}

const handleClearSearch = () => {
  keywordInput.value = ''
  keyword.value = ''
  pageNo.value = 1
  pageInput.value = 1
  loadData()
}

const changePage = (target) => {
  if (target < 1 || target > totalPages.value || target === pageNo.value) return
  pageNo.value = target
  pageInput.value = target
  loadData()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 用户输入每页条数：失焦/回车时生效，clamp 到 1~50，重置到第 1 页重新加载
const onPageSizeBlur = () => {
  let v = parseInt(pageSizeInput.value, 10)
  if (isNaN(v)) v = pageSize.value
  v = Math.max(1, Math.min(50, v))
  pageSizeInput.value = v
  if (v === pageSize.value) return
  userPickedPageSize.value = true
  pageSize.value = v
  pageNo.value = 1
  pageInput.value = 1
  loadData()
}

const jumpToPage = () => {
  let target = parseInt(pageInput.value, 10)
  if (isNaN(target)) target = pageNo.value
  target = Math.max(1, Math.min(totalPages.value, target))
  pageInput.value = target
  if (target === pageNo.value) return
  pageNo.value = target
  loadData()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goDetail = (postId) => {
  router.push(`/square/post/${postId}`)
}

const goCreate = () => {
  router.push('/square/create')
}

onMounted(async () => {
  isLoggedIn.value = isAuthenticated()
  // 先初始化浴场测得实际宽度，按列数推荐每页卡片数，再发首次列表请求，避免多余请求
  await initPool()
  const layout = getLayout()
  if (layout) {
    const rec = recommendedPageSize(layout.cols)
    pageSize.value = rec
    pageSizeInput.value = rec
  }
  loadData()
  loadHotPosts()
})
</script>

<style scoped src="./square.css"></style>
