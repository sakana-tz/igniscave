<template>
  <div class="editor-container">
    <div class="editor-topbar">
      <button class="btn" @click="goBack">← 返回</button>
      <span class="editor-mode">{{ isEdit ? '编辑帖子' : '发表新帖' }}</span>
      <div class="topbar-actions">
        <button class="btn" :disabled="!canSave || busy" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <button class="btn btn-primary" :disabled="!canSave || busy" @click="handleToggleStatus">
          <template v-if="postStatus === 0">{{ publishing ? '发布中...' : '发布' }}</template>
          <template v-else>{{ unpublishing ? '处理中...' : '转为草稿' }}</template>
        </button>
      </div>
    </div>

    <div v-if="loading" class="state-tip">加载中...</div>
    <div v-else-if="loadError" class="state-tip error">{{ loadError }}</div>

    <template v-else>
      <!-- 封面区域 -->
      <div class="cover-section">
        <div v-if="coverUrl" class="cover-preview">
          <img :src="coverUrl" alt="帖子封面" class="cover-img" />
          <div class="cover-overlay">
            <button class="cover-btn" :disabled="uploadingCover" @click="triggerCoverUpload">更换</button>
            <button class="cover-btn danger" :disabled="uploadingCover" @click="handleCoverRemove">移除</button>
          </div>
        </div>
        <button v-else class="cover-placeholder" :disabled="uploadingCover || !postId" @click="triggerCoverUpload">
          <span v-if="uploadingCover">上传中...</span>
          <span v-else>+ 添加封面（PNG）</span>
        </button>
        <input
          ref="coverInputRef"
          type="file"
          accept="image/png"
          hidden
          @change="handleCoverFileChange"
        />
      </div>

      <input
        v-model="title"
        type="text"
        class="title-input"
        placeholder="请输入帖子标题..."
        maxlength="128"
      />

      <div class="format-toolbar">
        <button type="button" title="标题" @click="insertPrefix('## ')">H</button>
        <button type="button" title="加粗" @click="wrapSelection('**', '**')">B</button>
        <button type="button" title="斜体" @click="wrapSelection('*', '*')">I</button>
        <button type="button" title="代码" @click="wrapSelection('`', '`')">&lt;/&gt;</button>
        <button type="button" title="链接" @click="insertLink">链接</button>
        <button type="button" title="引用" @click="insertPrefix('> ')">引用</button>
        <button type="button" title="代码块" @click="insertCodeBlock">代码块</button>
        <button type="button" title="插入图片" :disabled="uploadingImage || !postId" @click="triggerImageUpload">图片</button>
        <input
          ref="imageInputRef"
          type="file"
          accept="image/*"
          multiple
          hidden
          @change="handleImageFileChange"
        />
      </div>

      <div class="editor-pane">
        <textarea
          ref="textareaRef"
          v-model="content"
          class="md-textarea"
          placeholder="在这里输入 Markdown 内容...（可拖拽或粘贴图片自动上传）"
          spellcheck="false"
          @drop="handleDrop"
          @dragover.prevent
          @paste="handlePaste"
        ></textarea>
        <div class="preview-pane">
          <div v-if="renderedContent" class="markdown-body" v-html="renderedContent"></div>
          <div v-else class="preview-placeholder">实时预览</div>
        </div>
      </div>

      <div class="editor-foot">
        <span class="tip" v-if="!title.trim()">标题不能为空</span>
        <span v-else-if="!postId" class="tip">正在初始化草稿...</span>
        <span v-else class="status-tip">
          {{ postStatus === 1 ? '已发布' : '草稿' }} #{{ postId }}
        </span>
        <span class="char-count">正文 {{ content.length }} 字</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import {
  getPostDetail,
  createPost,
  updatePost,
  updatePostStatus,
  getCoverUploadUrl,
  uploadPostImage
} from '../api/forum'
import { getCachedCover, invalidateCover } from '../utils/coverCache'
import { getUserId } from '../utils/auth'

const route = useRoute()
const router = useRouter()

const initialPostId = route.params.postId ? Number(route.params.postId) : null
const isEdit = initialPostId !== null

const postId = ref(initialPostId)
const postStatus = ref(0) // 0=草稿 1=已发布
const title = ref('')
const content = ref('')
const loading = ref(isEdit)
const loadError = ref('')
const saving = ref(false)
const publishing = ref(false)
const unpublishing = ref(false)

// 任一异步操作进行中时，所有操作按钮统一禁用
const busy = computed(() => saving.value || publishing.value || unpublishing.value)

const coverUrl = ref('')
const uploadingCover = ref(false)
const uploadingImage = ref(false)

const textareaRef = ref(null)
const coverInputRef = ref(null)
const imageInputRef = ref(null)

const canSave = computed(() => !!title.value.trim())

const renderedContent = computed(() => {
  if (!content.value.trim()) return ''
  const rawHtml = marked.parse(content.value, { async: false })
  return DOMPurify.sanitize(rawHtml)
})

// ===== 文本操作 =====

// 在光标处包裹选中文本
const wrapSelection = (before, after) => {
  const ta = textareaRef.value
  if (!ta) return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const selected = content.value.slice(start, end) || '文本'
  content.value = content.value.slice(0, start) + before + selected + after + content.value.slice(end)
  requestAnimationFrame(() => {
    ta.focus()
    ta.selectionStart = start + before.length
    ta.selectionEnd = start + before.length + selected.length
  })
}

// 行首插入前缀（标题、引用）
const insertPrefix = (prefix) => {
  const ta = textareaRef.value
  if (!ta) return
  const start = ta.selectionStart
  const lineStart = content.value.lastIndexOf('\n', start - 1) + 1
  content.value = content.value.slice(0, lineStart) + prefix + content.value.slice(lineStart)
  requestAnimationFrame(() => {
    ta.focus()
    ta.selectionStart = ta.selectionEnd = start + prefix.length
  })
}

const insertLink = () => {
  wrapSelection('[', '](https://)')
}

const insertCodeBlock = () => {
  const ta = textareaRef.value
  if (!ta) return
  const start = ta.selectionStart
  const snippet = '\n```\n代码\n```\n'
  content.value = content.value.slice(0, start) + snippet + content.value.slice(start)
  requestAnimationFrame(() => {
    ta.focus()
    ta.selectionStart = start + 5
    ta.selectionEnd = start + 7
  })
}

// 在光标位置插入文本片段
const insertAtCursor = (snippet) => {
  const ta = textareaRef.value
  if (!ta) {
    content.value += snippet
    return
  }
  const start = ta.selectionStart
  content.value = content.value.slice(0, start) + snippet + content.value.slice(start)
  requestAnimationFrame(() => {
    ta.focus()
    ta.selectionStart = ta.selectionEnd = start + snippet.length
  })
}

// ===== 保存 / 切换发布状态 =====

// 保存内容，不改变当前状态
const handleSave = async () => {
  const finalTitle = title.value.trim()
  if (!finalTitle || busy.value) return
  if (!postId.value) {
    alert('草稿尚未初始化完成，请稍候')
    return
  }
  saving.value = true
  try {
    await updatePost(postId.value, finalTitle, content.value)
  } catch (error) {
    alert(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 切换发布状态：草稿→发布 或 已发布→草稿
const handleToggleStatus = async () => {
  const finalTitle = title.value.trim()
  if (!finalTitle || busy.value) return
  if (!postId.value) {
    alert('草稿尚未初始化完成，请稍候')
    return
  }
  const targetStatus = postStatus.value === 0 ? 1 : 0
  // 转为草稿时二次确认
  if (targetStatus === 0 && !window.confirm('转为草稿后，帖子将从浴场列表中隐藏，其他用户无法查看。确定吗？')) return
  // 草稿→发布用 publishing，已发布→草稿用 unpublishing，控制按钮文案
  if (targetStatus === 1) publishing.value = true
  else unpublishing.value = true
  try {
    await updatePost(postId.value, finalTitle, content.value)
    await updatePostStatus(postId.value, targetStatus)
    postStatus.value = targetStatus
    // 发布成功后跳转到详情页
    if (targetStatus === 1) {
      router.replace(`/square/post/${postId.value}`)
    }
  } catch (error) {
    alert(error.message || '操作失败')
  } finally {
    publishing.value = false
    unpublishing.value = false
  }
}

const goBack = () => {
  if (isEdit) {
    router.push(`/square/post/${postId.value}`)
  } else {
    router.push('/square')
  }
}

// ===== 封面管理 =====

const triggerCoverUpload = () => {
  if (!postId.value || uploadingCover.value) return
  coverInputRef.value?.click()
}

const handleCoverFileChange = async (e) => {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  await uploadCoverFile(file)
}

const uploadCoverFile = async (file) => {
  if (!postId.value) {
    alert('草稿尚未初始化完成，请稍候')
    return
  }
  uploadingCover.value = true
  try {
    const uploadUrl = await getCoverUploadUrl(postId.value)
    await fetch(uploadUrl, {
      method: 'PUT',
      body: file,
      headers: { 'Content-Type': 'image/png' }
    })
    // 封面内容已更新，先失效缓存再重新拉取，确保预览显示新封面
    await invalidateCover(postId.value)
    const url = await getCachedCover(postId.value)
    coverUrl.value = url || ''
  } catch (error) {
    alert(error.message || '封面上传失败')
  } finally {
    uploadingCover.value = false
  }
}

const handleCoverRemove = async () => {
  // OSS 中封面会随 deletePost 级联清理；编辑期暂不支持单独删除 OSS 对象，
  // 仅在前端隐藏预览。保存/发布时若需要正式删除，可通过后端扩展接口处理。
  coverUrl.value = ''
}

const loadCover = async () => {
  if (!postId.value) return
  try {
    const url = await getCachedCover(postId.value)
    coverUrl.value = url || ''
  } catch (error) {
    // 草稿态或无封面时静默处理
    coverUrl.value = ''
  }
}

// ===== 帖子图片插入 =====

const triggerImageUpload = () => {
  if (!postId.value || uploadingImage.value) return
  imageInputRef.value?.click()
}

const handleImageFileChange = async (e) => {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (files.length === 0) return
  for (const file of files) {
    await uploadAndInsertImage(file)
  }
}

const uploadAndInsertImage = async (file, nameOverride) => {
  if (!postId.value) {
    alert('草稿尚未初始化完成，请稍候')
    return
  }
  if (!file.type.startsWith('image/')) {
    alert(`仅支持图片文件：${file.name}`)
    return
  }
  uploadingImage.value = true
  const fileName = nameOverride || file.name || `image-${Date.now()}.${file.type.split('/')[1] || 'png'}`
  try {
    const { uploadUrl, proxyUrl } = await uploadPostImage(
      postId.value,
      fileName,
      file.type
    )
    await fetch(uploadUrl, {
      method: 'PUT',
      body: file,
      headers: { 'Content-Type': file.type }
    })
    // 在光标位置插入 Markdown 图片语法
    const snippet = `\n![${fileName}](${proxyUrl})\n`
    insertAtCursor(snippet)
  } catch (error) {
    alert(error.message || `图片 ${fileName} 上传失败`)
  } finally {
    uploadingImage.value = false
  }
}

// 拖拽上传
const handleDrop = async (e) => {
  const items = Array.from(e.dataTransfer?.files || [])
  if (items.length === 0) return
  const images = items.filter(f => f.type.startsWith('image/'))
  if (images.length === 0) return
  e.preventDefault()
  // 让光标定位到释放位置后再插入
  const ta = textareaRef.value
  if (ta) {
    let caret = -1
    if (document.caretPositionFromPoint) {
      const pos = document.caretPositionFromPoint(e.clientX, e.clientY)
      if (pos && pos.offsetNode === ta) caret = pos.offset
    } else if (document.caretRangeFromPoint) {
      const range = document.caretRangeFromPoint(e.clientX, e.clientY)
      if (range && range.startContainer === ta) caret = range.startOffset
    }
    if (caret >= 0) {
      ta.selectionStart = ta.selectionEnd = caret
    } else {
      ta.focus()
    }
  }
  for (const file of images) {
    await uploadAndInsertImage(file)
  }
}

// 粘贴上传（截图等）
const handlePaste = async (e) => {
  const items = Array.from(e.clipboardData?.items || [])
  const imageFiles = items
    .filter(item => item.type.startsWith('image/'))
    .map(item => item.getAsFile())
    .filter(Boolean)
  if (imageFiles.length === 0) return
  e.preventDefault()
  for (const file of imageFiles) {
    // 粘贴的图片通常没有文件名，生成一个
    const ext = file.type.split('/')[1] || 'png'
    const nameOverride = file.name ? null : `paste-${Date.now()}.${ext}`
    await uploadAndInsertImage(file, nameOverride)
  }
}

// ===== 初始化 =====

let initialized = false

const ensureDraft = async () => {
  if (initialized || isEdit) return
  initialized = true
  try {
    const newPostId = await createPost('', '')
    postId.value = newPostId
  } catch (error) {
    loadError.value = error.message || '草稿创建失败，请刷新重试'
  }
}

onMounted(async () => {
  if (!isEdit) {
    // 创建模式：立即创建草稿获取 postId，后续上传封面/图片都基于此 id
    await ensureDraft()
    return
  }
  try {
    const data = await getPostDetail(initialPostId)
    if (data.userId !== getUserId()) {
      loadError.value = '权限不足：只能编辑自己的帖子'
      return
    }
    postId.value = data.postId
    postStatus.value = data.postStatus || 0
    title.value = data.title || ''
    content.value = data.content || ''
    await loadCover()
  } catch (error) {
    loadError.value = error.message || '帖子加载失败'
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  // 离开页面时清理未完成的资源（如有）
})
</script>

<style scoped>
.editor-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 160px);
  background: #f7f8fc;
}

.editor-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.editor-mode {
  font-size: 16px;
  font-weight: 600;
  color: #4a4e5e;
}

.topbar-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid #dcdfea;
  background: #fff;
  color: #555;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #b06ab3 100%);
  border: none;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  color: #fff;
  opacity: 0.92;
  border: none;
}

.state-tip {
  text-align: center;
  padding: 80px 20px;
  color: #8a8fa3;
  font-size: 15px;
}

.state-tip.error {
  color: #d8614b;
}

/* 封面区域 */
.cover-section {
  margin-bottom: 14px;
}

.cover-preview {
  position: relative;
  width: 100%;
  max-height: 280px;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #eceef5;
}

.cover-img {
  display: block;
  width: 100%;
  max-height: 280px;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 6px;
}

.cover-btn {
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  background: rgba(255, 255, 255, 0.9);
  color: #4a4e5e;
  font-size: 13px;
  cursor: pointer;
  backdrop-filter: blur(4px);
  transition: all 0.15s;
}

.cover-btn:hover:not(:disabled) {
  background: #fff;
  border-color: #667eea;
  color: #667eea;
}

.cover-btn.danger:hover:not(:disabled) {
  border-color: #d8614b;
  color: #d8614b;
}

.cover-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cover-placeholder {
  width: 100%;
  height: 120px;
  border: 2px dashed #c5cdf5;
  border-radius: 12px;
  background: #f5f7ff;
  color: #6b7185;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.cover-placeholder:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
  background: #eef0ff;
}

.cover-placeholder:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.title-input {
  width: 100%;
  box-sizing: border-box;
  padding: 13px 16px;
  border: 1px solid #dcdfea;
  border-radius: 10px;
  font-size: 17px;
  outline: none;
  background: #fff;
  color: #2c2f3a;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.title-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.format-toolbar {
  display: flex;
  gap: 6px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.format-toolbar button {
  background: #fff;
  border: 1px solid #dcdfea;
  border-radius: 6px;
  padding: 5px 12px;
  font-size: 13px;
  color: #5b6280;
  cursor: pointer;
  transition: all 0.15s;
  min-width: 38px;
}

.format-toolbar button:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.format-toolbar button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.editor-pane {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  height: calc(100vh - 440px);
  min-height: 380px;
}

.md-textarea {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  padding: 16px;
  border: 1px solid #dcdfea;
  border-radius: 10px;
  font-size: 14.5px;
  font-family: 'Cascadia Code', Consolas, Monaco, monospace;
  line-height: 1.7;
  resize: none;
  outline: none;
  background: #fff;
  color: #333;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.md-textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.preview-pane {
  background: #fff;
  border: 1px solid #eceef5;
  border-radius: 10px;
  padding: 16px 20px;
  overflow-y: auto;
}

.preview-placeholder {
  color: #c0c5d3;
  font-size: 14px;
}

/* Markdown 排版，与详情页保持一致 */
.markdown-body {
  color: #3c3f4d;
  font-size: 15px;
  line-height: 1.8;
  word-break: break-word;
  text-align: left;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 20px 0 10px;
  color: #2c2f3a;
}

.markdown-body :deep(h1) { font-size: 21px; }
.markdown-body :deep(h2) { font-size: 18px; }
.markdown-body :deep(h3) { font-size: 16px; }

.markdown-body :deep(p) { margin: 0 0 12px; }
.markdown-body :deep(a) { color: #667eea; }

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 12px;
  padding-left: 24px;
}

.markdown-body :deep(blockquote) {
  margin: 0 0 12px;
  padding: 6px 14px;
  border-left: 4px solid #c5cdf5;
  background: #f5f7ff;
  color: #6b7185;
}

.markdown-body :deep(code) {
  background: #f1f2f8;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: #b0506e;
}

.markdown-body :deep(pre) {
  background: #282b38;
  border-radius: 10px;
  padding: 14px 16px;
  overflow-x: auto;
  margin: 0 0 14px;
}

.markdown-body :deep(pre code) {
  background: transparent;
  color: #e6e8f2;
  padding: 0;
}

.markdown-body :deep(img) { max-width: 100%; }

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  margin-bottom: 14px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e2e5f0;
  padding: 7px 11px;
  text-align: left;
}

.markdown-body :deep(th) { background: #f5f7ff; }

.editor-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  font-size: 13px;
}

.editor-foot .tip {
  color: #d8614b;
}

.editor-foot .status-tip {
  color: #8a8fa3;
}

.char-count {
  color: #aab0c0;
}

@media (max-width: 768px) {
  .editor-container {
    padding: 14px 12px;
  }

  .editor-pane {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr 1fr;
    height: auto;
    min-height: 0;
  }

  .md-textarea {
    height: 300px;
  }

  .preview-pane {
    min-height: 260px;
    max-height: 400px;
  }
}
</style>
