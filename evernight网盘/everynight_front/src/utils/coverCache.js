// 论坛封面缓存：IndexedDB 持久化 Blob + 会话内 objectURL 复用
// 目的：减少服务器流量 —— 命中缓存时不再请求 /forum/getCoverUrl 接口，
// 也不再从 OSS 重复下载图片（预签名 URL 参数每次都变，浏览器 HTTP 缓存无法命中）
import { getCoverUrl } from '../api/forum'

const DB_NAME = 'forum_cover_db'
const DB_VERSION = 1
const STORE = 'covers'

const COVER_TTL = 24 * 60 * 60 * 1000 // 封面图片缓存有效期：24 小时（兜底其他端更新封面的场景）
const NO_COVER_TTL = 10 * 60 * 1000 // “无封面”标记有效期：10 分钟（帖子补传封面后能较快看到）

let dbPromise = null
const urlMem = new Map() // postId -> objectURL，会话内复用避免重复创建
const inflight = new Map() // postId -> Promise，去重并发请求

const openDB = () => {
  if (dbPromise) return dbPromise
  dbPromise = new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = (e) => {
      const db = e.target.result
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE)
      }
    }
    req.onsuccess = (e) => resolve(e.target.result)
    req.onerror = (e) => reject(e.target.error)
  })
  return dbPromise
}

const idbGet = async (key) => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly')
    const req = tx.objectStore(STORE).get(key)
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

const idbPut = async (key, value) => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    const req = tx.objectStore(STORE).put(value, key)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

const idbDelete = async (key) => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    const req = tx.objectStore(STORE).delete(key)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

// 使某帖子封面缓存失效（如编辑页重新上传封面后调用）
export const invalidateCover = async (postId) => {
  const old = urlMem.get(postId)
  urlMem.delete(postId)
  if (old) URL.revokeObjectURL(old)
  try {
    await idbDelete(postId)
  } catch (e) {
    // IndexedDB 异常不阻塞主流程
  }
}

// 从预签名 URL 下载图片并持久化；失败时降级返回原始 URL（本次直连显示，不写缓存）
const fetchAndCache = async (postId) => {
  const url = await getCoverUrl(postId)
  if (!url) {
    await idbPut(postId, { blob: null, cachedAt: Date.now() })
    return ''
  }
  try {
    const resp = await fetch(url)
    if (!resp.ok) throw new Error(`封面下载失败: ${resp.status}`)
    const blob = await resp.blob()
    if (!blob.size) throw new Error('封面内容为空')
    await idbPut(postId, { blob, cachedAt: Date.now() })
    const objUrl = URL.createObjectURL(blob)
    urlMem.set(postId, objUrl)
    return objUrl
  } catch (e) {
    return url
  }
}

// 获取封面展示地址：内存 objectURL → IndexedDB Blob → 网络（getCoverUrl + OSS 下载）
// 返回 '' 表示无封面
export const getCachedCover = async (postId) => {
  // 1) 会话内存
  if (urlMem.has(postId)) return urlMem.get(postId)

  // 2) IndexedDB 持久缓存
  try {
    const rec = await idbGet(postId)
    if (rec) {
      const ttl = rec.blob ? COVER_TTL : NO_COVER_TTL
      if (Date.now() - rec.cachedAt < ttl) {
        if (!rec.blob) return '' // “无封面”标记仍在有效期内
        const objUrl = URL.createObjectURL(rec.blob)
        urlMem.set(postId, objUrl)
        return objUrl
      }
    }
  } catch (e) {
    // IndexedDB 异常时走网络兜底
  }

  // 3) 网络拉取（并发去重）
  if (inflight.has(postId)) return inflight.get(postId)
  const p = fetchAndCache(postId).finally(() => inflight.delete(postId))
  inflight.set(postId, p)
  return p
}
