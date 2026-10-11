<template>
  <div class="detail-container">
    <button class="btn btn-back" @click="goBack">← 返回浴场</button>

    <div v-if="loading" class="state-tip">加载中...</div>
    <div v-else-if="errorMsg" class="state-tip error">{{ errorMsg }}</div>

    <template v-else-if="post">
      <!-- 帖子正文 -->
      <div class="post-card">
        <div v-if="coverUrl" class="post-cover-large">
          <img :src="coverUrl" alt="帖子封面" />
        </div>

        <div class="post-head">
          <h1 class="post-title">{{ post.title }}</h1>
          <div v-if="isAuthor" class="post-actions">
            <button class="btn btn-small" @click="goEdit">编辑</button>
            <button class="btn btn-small btn-danger" @click="handleDeletePost">删除</button>
          </div>
        </div>

        <div class="post-info">
          <span>{{ post.username }}</span>
          <span>浏览 {{ formatNumber(post.viewCount) }}</span>
          <span>{{ post.updatedTime }}</span>
        </div>

        <div class="divider"></div>

        <!-- v-html 内容经 marked 解析 + DOMPurify 消毒 -->
        <div v-if="renderedContent" class="markdown-body" v-html="renderedContent"></div>
        <div v-else class="empty-content">（作者还没有写内容）</div>
      </div>

      <!-- 评论区 -->
      <div class="comment-card">
        <h2 class="comment-title">评论 {{ commentCount }}</h2>

        <div v-if="commentsLoading" class="state-tip small">评论加载中...</div>
        <div v-else-if="comments.length === 0" class="state-tip small">还没有评论，来抢沙发吧~</div>

        <ul v-else class="comment-list">
          <li v-for="comment in comments" :key="comment.commentId" class="comment-item">
            <div class="comment-meta">
              <span class="comment-user">{{ comment.username }}</span>
              <span class="comment-time">{{ comment.createdTime }}</span>
              <button
                v-if="currentUserId === comment.userId"
                class="comment-delete"
                @click="handleDeleteComment(comment.commentId)"
              >
                删除
              </button>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
          </li>
        </ul>

        <div class="divider"></div>

        <div v-if="isLoggedIn" class="comment-form">
          <textarea
            v-model="commentInput"
            class="comment-textarea"
            placeholder="说点什么吧..."
            rows="3"
            maxlength="500"
          ></textarea>
          <div class="comment-form-foot">
            <span class="char-count" :class="{ warn: commentInput.length >= 500 }">
              {{ commentInput.length }}/500
            </span>
            <button class="btn btn-primary" :disabled="submitting || !commentInput.trim()" @click="handleAddComment">
              {{ submitting ? '发送中...' : '发表评论' }}
            </button>
          </div>
        </div>
        <div v-else class="login-tip">
          <router-link to="/login">登录</router-link> 后即可发表评论
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import {
  getPostDetail,
  incrementViewCount,
  deletePost,
  getCommentList,
  addComment,
  deleteComment
} from '../api/forum'
import { getCachedCover } from '../utils/coverCache'
import { isAuthenticated, getUserId } from '../utils/auth'

// 兼容后端 Jackson default typing：将 ["java.util.ArrayList", [...]] 类型数组解包为普通数组
const unwrapTyped = (value) => {
  if (Array.isArray(value) && value.length === 2 && typeof value[0] === 'string' && Array.isArray(value[1])) {
    return value[1]
  }
  return value
}

const route = useRoute()
const router = useRouter()

const postId = Number(route.params.postId)

const post = ref(null)
const coverUrl = ref('')
const loading = ref(true)
const errorMsg = ref('')

const comments = ref([])
const commentsLoading = ref(false)
const commentCount = ref(0)
const commentInput = ref('')
const submitting = ref(false)

const currentUserId = ref(getUserId())
const isLoggedIn = ref(isAuthenticated())
const isAuthor = computed(() => isLoggedIn.value && currentUserId.value === post.value?.userId)

const renderedContent = computed(() => {
  const md = post.value?.content
  if (!md) return ''
  const rawHtml = marked.parse(md, { async: false })
  return DOMPurify.sanitize(rawHtml)
})

const formatNumber = (num) => {
  const n = Number(num) || 0
  return n >= 10000 ? (n / 10000).toFixed(1) + 'w' : n
}

const loadDetail = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await getPostDetail(postId)
    post.value = data
    commentCount.value = data?.commentCount || 0
    // 异步加载封面，不阻塞详情主体
    loadCover()
  } catch (error) {
    errorMsg.value = error.message || '帖子加载失败'
  } finally {
    loading.value = false
  }
}

const loadCover = async () => {
  try {
    const url = await getCachedCover(postId)
    coverUrl.value = url || ''
  } catch (e) {
    coverUrl.value = ''
  }
}

const loadComments = async () => {
  commentsLoading.value = true
  try {
    const data = await getCommentList(postId)
    comments.value = unwrapTyped(data) || []
  } catch (error) {
    console.error('评论加载失败:', error.message)
  } finally {
    commentsLoading.value = false
  }
}

const handleAddComment = async () => {
  const content = commentInput.value.trim()
  if (!content || submitting.value) return
  submitting.value = true
  try {
    await addComment(postId, content)
    commentInput.value = ''
    await loadComments()
    commentCount.value += 1
  } catch (error) {
    alert(error.message || '评论发表失败')
  } finally {
    submitting.value = false
  }
}

const handleDeleteComment = async (commentId) => {
  if (!window.confirm('确定删除这条评论吗？')) return
  try {
    await deleteComment(commentId)
    comments.value = comments.value.filter(c => c.commentId !== commentId)
    commentCount.value = Math.max(0, commentCount.value - 1)
  } catch (error) {
    alert(error.message || '评论删除失败')
  }
}

const handleDeletePost = async () => {
  if (!window.confirm('确定删除这篇帖子吗？评论也会一并删除，且不可恢复。')) return
  try {
    await deletePost(postId)
    alert('帖子已删除')
    router.push('/square')
  } catch (error) {
    alert(error.message || '帖子删除失败')
  }
}

const goEdit = () => {
  router.push(`/square/edit/${postId}`)
}

const goBack = () => {
  router.push('/square')
}

onMounted(() => {
  currentUserId.value = getUserId()
  isLoggedIn.value = isAuthenticated()
  loadDetail()
  loadComments()
  // 浏览量 +1，失败不阻塞页面
  incrementViewCount(postId).catch(error => console.error('浏览量上报失败:', error.message))
})
</script>

<style scoped>
.detail-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 20px 50px;
  min-height: calc(100vh - 160px);
  background: #f7f8fc;
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

.btn-back {
  margin-bottom: 16px;
}

.btn-small {
  padding: 5px 14px;
  font-size: 13px;
}

.btn-danger {
  color: #d8614b;
}

.btn-danger:hover {
  border-color: #d8614b;
  color: #d8614b;
}

.state-tip {
  text-align: center;
  padding: 60px 20px;
  color: #8a8fa3;
  font-size: 15px;
}

.state-tip.small {
  padding: 30px 10px;
  font-size: 14px;
}

.state-tip.error {
  color: #d8614b;
}

.post-card,
.comment-card {
  background: #fff;
  border: 1px solid #eceef5;
  border-radius: 14px;
  padding: 26px 30px;
  margin-bottom: 18px;
}

.post-cover-large {
  margin: -26px -30px 18px;
  border-radius: 14px 14px 0 0;
  overflow: hidden;
  background: #f5f7ff;
}

.post-cover-large img {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: cover;
}

.post-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.post-title {
  margin: 0;
  font-size: 24px;
  color: #2c2f3a;
  line-height: 1.35;
  word-break: break-word;
}

.post-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.post-info {
  display: flex;
  gap: 18px;
  margin-top: 12px;
  font-size: 13px;
  color: #9aa0b2;
  flex-wrap: wrap;
}

.divider {
  height: 1px;
  background: #eef0f6;
  margin: 18px 0;
}

.empty-content {
  color: #aab0c0;
  font-size: 14px;
  padding: 10px 0;
}

/* Markdown 排版（v-html 注入的子元素需用 :deep；基础样式放容器上级联） */
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
  margin: 24px 0 12px;
  color: #2c2f3a;
  line-height: 1.4;
}

.markdown-body :deep(h1) { font-size: 22px; }
.markdown-body :deep(h2) { font-size: 19px; }
.markdown-body :deep(h3) { font-size: 17px; }
.markdown-body :deep(h4) { font-size: 15px; }

.markdown-body :deep(p) {
  margin: 0 0 14px;
}

.markdown-body :deep(a) {
  color: #667eea;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 14px;
  padding-left: 26px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(blockquote) {
  margin: 0 0 14px;
  padding: 8px 16px;
  border-left: 4px solid #c5cdf5;
  background: #f5f7ff;
  color: #6b7185;
}

.markdown-body :deep(code) {
  background: #f1f2f8;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13.5px;
  font-family: 'Cascadia Code', Consolas, Monaco, monospace;
  color: #b0506e;
}

.markdown-body :deep(pre) {
  background: #282b38;
  border-radius: 10px;
  padding: 16px 18px;
  overflow-x: auto;
  margin: 0 0 16px;
}

.markdown-body :deep(pre code) {
  background: transparent;
  color: #e6e8f2;
  padding: 0;
  font-size: 13.5px;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 16px;
  font-size: 14px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e2e5f0;
  padding: 8px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f5f7ff;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e2e5f0;
  margin: 22px 0;
}

/* 评论区 */
.comment-title {
  margin: 0;
  font-size: 18px;
  color: #2c2f3a;
}

.comment-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.comment-item {
  padding: 14px 0;
  border-bottom: 1px solid #f1f2f8;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  margin-bottom: 6px;
}

.comment-user {
  color: #5b6280;
  font-weight: 600;
}

.comment-time {
  color: #aab0c0;
}

.comment-delete {
  margin-left: auto;
  background: none;
  border: none;
  color: #d8614b;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.comment-delete:hover {
  text-decoration: underline;
}

.comment-content {
  margin: 0;
  font-size: 14.5px;
  color: #4a4e5e;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}

.comment-form {
  margin-top: 4px;
}

.comment-textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #dcdfea;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 14px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  background: #fff;
  color: #333;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.comment-textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.comment-form-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.char-count {
  font-size: 13px;
  color: #aab0c0;
}

.char-count.warn {
  color: #d8614b;
}

.login-tip {
  text-align: center;
  font-size: 14px;
  color: #8a8fa3;
  padding: 8px 0;
}

.login-tip a {
  color: #667eea;
}

@media (max-width: 768px) {
  .detail-container {
    padding: 16px 12px 40px;
  }

  .post-card,
  .comment-card {
    padding: 20px 18px;
  }

  .post-title {
    font-size: 20px;
  }

  .post-head {
    flex-direction: column;
  }
}
</style>
