// 断点续传下载工具：基于 IndexedDB 持久化分片缓存，支持 HTTP Range 续传、暂停/恢复、取消
const DB_NAME = 'file_download_db'
const DB_VERSION = 1
const CHUNKS_STORE = 'chunks'
const META_STORE = 'meta'

let dbPromise = null

const openDB = () => {
  if (dbPromise) return dbPromise
  dbPromise = new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = (e) => {
      const db = e.target.result
      if (!db.objectStoreNames.contains(CHUNKS_STORE)) {
        db.createObjectStore(CHUNKS_STORE)
      }
      if (!db.objectStoreNames.contains(META_STORE)) {
        db.createObjectStore(META_STORE)
      }
    }
    req.onsuccess = (e) => resolve(e.target.result)
    req.onerror = (e) => reject(e.target.error)
  })
  return dbPromise
}

const idbGet = async (store, key) => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readonly')
    const req = tx.objectStore(store).get(key)
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

const idbPut = async (store, key, value) => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readwrite')
    const req = tx.objectStore(store).put(value, key)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

const idbDelete = async (store, key) => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readwrite')
    const req = tx.objectStore(store).delete(key)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

const idbKeys = async (store) => {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, 'readonly')
    const req = tx.objectStore(store).getAllKeys()
    req.onsuccess = () => resolve(req.result || [])
    req.onerror = () => reject(req.error)
  })
}

const sleep = (ms) => new Promise(r => setTimeout(r, ms))

export const getDownloadKey = (fileName, fileSize) => {
  return `${fileName}_${fileSize}`
}

const getDownloadedChunkSet = async (downloadKey) => {
  const allKeys = await idbKeys(CHUNKS_STORE)
  const prefix = `${downloadKey}#part_`
  const set = new Set()
  for (const k of allKeys) {
    if (typeof k === 'string' && k.startsWith(prefix)) {
      const num = parseInt(k.slice(prefix.length), 10)
      if (!isNaN(num)) set.add(num)
    }
  }
  return set
}

export const getDownloadedChunks = async (downloadKey) => {
  return await getDownloadedChunkSet(downloadKey)
}

export const getDownloadedBytes = async (downloadKey) => {
  const meta = await idbGet(META_STORE, downloadKey)
  if (!meta) return 0
  const set = await getDownloadedChunkSet(downloadKey)
  let bytes = 0
  for (const part of set) {
    if (part === meta.totalChunks) {
      bytes += Math.max(0, meta.fileSize - (part - 1) * meta.chunkSize)
    } else {
      bytes += meta.chunkSize
    }
  }
  return bytes
}

export const clearDownloadChunks = async (downloadKey) => {
  const allKeys = await idbKeys(CHUNKS_STORE)
  const prefix = `${downloadKey}#part_`
  for (const k of allKeys) {
    if (typeof k === 'string' && k.startsWith(prefix)) {
      await idbDelete(CHUNKS_STORE, k)
    }
  }
  await idbDelete(META_STORE, downloadKey)
}

const fetchRange = async (url, start, end, signal) => {
  const resp = await fetch(url, {
    method: 'GET',
    headers: { Range: `bytes=${start}-${end}` },
    signal
  })
  if (resp.status !== 206 && resp.status !== 200) {
    throw new Error(`分片下载失败 (HTTP ${resp.status})`)
  }
  return await resp.blob()
}

export const downloadWithRange = async ({
  url,
  fileSize,
  fileName,
  contentType = 'application/octet-stream',
  chunkSize = 5 * 1024 * 1024,
  signal,
  onProgress,
  onPauseCheck,
  onChunk
}) => {
  if (!fileSize || fileSize <= 0) {
    throw new Error('文件大小无效，无法分片下载')
  }
  const downloadKey = getDownloadKey(fileName, fileSize)
  const totalChunks = Math.max(1, Math.ceil(fileSize / chunkSize))

  await idbPut(META_STORE, downloadKey, {
    totalChunks,
    chunkSize,
    fileSize,
    fileName,
    createTime: Date.now()
  })

  const downloaded = await getDownloadedChunkSet(downloadKey)
  let downloadedBytes = 0
  for (const part of downloaded) {
    if (part === totalChunks) {
      downloadedBytes += Math.max(0, fileSize - (part - 1) * chunkSize)
    } else {
      downloadedBytes += chunkSize
    }
  }

  if (onProgress) {
    const startPercent = Math.min(100, Math.round((downloadedBytes / fileSize) * 100))
    onProgress(downloadedBytes, fileSize, startPercent)
  }

  for (let i = 1; i <= totalChunks; i++) {
    if (signal?.aborted) {
      throw new DOMException('下载已取消', 'AbortError')
    }

    while (onPauseCheck && onPauseCheck()) {
      if (signal?.aborted) {
        throw new DOMException('下载已取消', 'AbortError')
      }
      await sleep(100)
    }

    if (downloaded.has(i)) continue

    const start = (i - 1) * chunkSize
    const end = i === totalChunks ? fileSize - 1 : start + chunkSize - 1

    if (onChunk) onChunk(i, totalChunks)

    const blob = await fetchRange(url, start, end, signal)
    await idbPut(CHUNKS_STORE, `${downloadKey}#part_${i}`, blob)
    downloaded.add(i)
    downloadedBytes += blob.size

    if (onProgress) {
      const percent = Math.min(100, Math.round((downloadedBytes / fileSize) * 100))
      onProgress(downloadedBytes, fileSize, percent)
    }
  }

  // 合并所有分片并触发浏览器下载
  const allKeys = await idbKeys(CHUNKS_STORE)
  const prefix = `${downloadKey}#part_`
  const partKeys = allKeys
    .filter(k => typeof k === 'string' && k.startsWith(prefix))
    .map(k => ({ k, num: parseInt(k.slice(prefix.length), 10) }))
    .filter(item => !isNaN(item.num))
    .sort((a, b) => a.num - b.num)
    .map(item => item.k)

  const parts = []
  for (const k of partKeys) {
    const blob = await idbGet(CHUNKS_STORE, k)
    if (blob) parts.push(blob)
  }

  if (parts.length === 0) {
    throw new Error('合并下载分片失败：未找到任何分片数据')
  }

  const finalBlob = new Blob(parts, { type: contentType || 'application/octet-stream' })
  const objectUrl = URL.createObjectURL(finalBlob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  setTimeout(() => URL.revokeObjectURL(objectUrl), 2000)

  // 合并成功后清理分片缓存（避免占用空间且下次下载视为新任务）
  await clearDownloadChunks(downloadKey)
}
