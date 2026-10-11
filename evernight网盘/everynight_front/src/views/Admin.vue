<template>
  <div class="admin-container">
    <h1>管理员中心</h1>

    <div class="admin-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        :class="['tab-button', { active: activeTab === tab.id }]"
        @click="activeTab = tab.id"
      >
        {{ tab.name }}
      </button>
    </div>

    <div class="tab-content">
      <!-- 系统仪表盘 -->
      <div v-if="activeTab === 'dashboard'" class="dashboard">
        <div class="section-header">
          <h2>系统仪表盘</h2>
          <button class="refresh-button" @click="fetchOverview">刷新</button>
        </div>
        <div class="stat-cards" v-if="overview">
          <div class="stat-card">
            <h3>用户总数</h3>
            <p class="stat-value">{{ overview.userCount }}</p>
          </div>
          <div class="stat-card">
            <h3>文件总数</h3>
            <p class="stat-value">{{ overview.fileCount }}</p>
          </div>
          <div class="stat-card">
            <h3>存储总量</h3>
            <p class="stat-value">{{ formatFileSize(overview.totalStorageSize) }}</p>
          </div>
          <div class="stat-card">
            <h3>帖子总数</h3>
            <p class="stat-value">{{ overview.postCount }}</p>
          </div>
          <div class="stat-card">
            <h3>评论总数</h3>
            <p class="stat-value">{{ overview.commentCount }}</p>
          </div>
        </div>
        <p v-else class="loading-text">加载中...</p>
      </div>

      <!-- 用户管理 -->
      <div v-if="activeTab === 'users'" class="user-management">
        <h2>用户管理</h2>
        <div class="user-actions">
          <button class="add-button" @click="openAddUserForm">添加用户</button>
        </div>
        <div class="search-bar">
          <input type="text" v-model="userQuery.usernameFuzzy" placeholder="用户名（模糊）">
          <input type="text" v-model="userQuery.emailFuzzy" placeholder="邮箱（模糊）">
          <select v-model="userQuery.userStatus" class="status-select">
            <option :value="undefined">全部状态</option>
            <option value="0">管理员</option>
            <option value="1">禁用</option>
            <option value="2">普通用户</option>
          </select>
          <button class="search-button" @click="searchUsers">查询</button>
          <button class="reset-button" @click="resetUserSearch">重置</button>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>邮箱</th>
              <th>注册时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.userId">
              <td>{{ user.userId }}</td>
              <td>{{ user.username }}</td>
              <td>{{ user.email }}</td>
              <td>{{ formatDateTime(user.createdTime) }}</td>
              <td>{{ getUserStatusText(user.userStatus) }}</td>
              <td>
                <button class="action-button edit" @click="openEditUserForm(user)">编辑</button>
                <button class="action-button delete" @click="deleteUser(user.userId)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <pagination
          :page-no="userQuery.pageNo"
          :page-size="userQuery.pageSize"
          :total="userTotal"
          @change="onUserPageChange"
        />
      </div>

      <!-- 论坛管理 -->
      <div v-if="activeTab === 'forum'" class="forum-management">
        <h2>论坛管理</h2>
        <div class="sub-tabs">
          <button
            :class="['sub-tab-button', { active: forumSubTab === 'posts' }]"
            @click="forumSubTab = 'posts'"
          >帖子</button>
          <button
            :class="['sub-tab-button', { active: forumSubTab === 'comments' }]"
            @click="forumSubTab = 'comments'"
          >评论</button>
        </div>

        <!-- 帖子列表 -->
        <div v-if="forumSubTab === 'posts'">
          <div class="search-bar">
            <input type="text" v-model="postQuery.titleFuzzy" placeholder="标题（模糊）">
            <input type="number" v-model="postQuery.userId" placeholder="用户 ID">
            <select v-model="postQuery.postStatus" class="status-select">
              <option :value="undefined">全部状态</option>
              <option value="0">草稿</option>
              <option value="1">发布</option>
            </select>
            <button class="search-button" @click="searchPosts">查询</button>
            <button class="reset-button" @click="resetPostSearch">重置</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>帖子ID</th>
                <th>用户</th>
                <th>标题</th>
                <th>浏览量</th>
                <th>评论数</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="post in posts" :key="post.postId">
                <td>{{ post.postId }}</td>
                <td>{{ post.username }}({{ post.userId }})</td>
                <td>{{ post.title }}</td>
                <td>{{ post.viewCount }}</td>
                <td>{{ post.commentCount }}</td>
                <td>{{ post.postStatus === 1 ? '发布' : '草稿' }}</td>
                <td>{{ formatDateTime(post.updatedTime) }}</td>
                <td>
                  <button class="action-button edit" @click="viewPost(post.postId)">查看</button>
                  <button class="action-button delete" @click="deletePost(post.postId)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <pagination
            :page-no="postQuery.pageNo"
            :page-size="postQuery.pageSize"
            :total="postTotal"
            @change="onPostPageChange"
          />
        </div>

        <!-- 评论列表 -->
        <div v-if="forumSubTab === 'comments'">
          <div class="search-bar">
            <input type="number" v-model="commentQuery.postId" placeholder="帖子 ID">
            <input type="number" v-model="commentQuery.userId" placeholder="用户 ID">
            <input type="text" v-model="commentQuery.contentFuzzy" placeholder="内容（模糊）">
            <button class="search-button" @click="searchComments">查询</button>
            <button class="reset-button" @click="resetCommentSearch">重置</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>评论ID</th>
                <th>帖子ID</th>
                <th>用户</th>
                <th>内容</th>
                <th>时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="comment in comments" :key="comment.commentId">
                <td>{{ comment.commentId }}</td>
                <td>{{ comment.postId }}</td>
                <td>{{ comment.username }}({{ comment.userId }})</td>
                <td class="comment-content">{{ comment.content }}</td>
                <td>{{ formatDateTime(comment.createdTime) }}</td>
                <td>
                  <button class="action-button delete" @click="deleteComment(comment.commentId)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <pagination
            :page-no="commentQuery.pageNo"
            :page-size="commentQuery.pageSize"
            :total="commentTotal"
            @change="onCommentPageChange"
          />
        </div>
      </div>
    </div>

    <!-- 添加/编辑用户表单 -->
    <div v-if="showUserForm" class="modal-mask" @click.self="cancelUserForm">
      <div class="form-container">
        <h3>{{ editingUser.userId ? '编辑用户' : '添加用户' }}</h3>
        <form @submit.prevent="submitUserForm">
          <div class="form-item">
            <label>用户名:</label>
            <input type="text" v-model="editingUser.username" required>
          </div>
          <div class="form-item">
            <label>邮箱:</label>
            <input type="email" v-model="editingUser.email" required>
          </div>
          <div class="form-item">
            <label>密码{{ editingUser.userId ? '（留空则不修改）' : '' }}:</label>
            <input type="password" v-model="editingUser.password" :required="!editingUser.userId">
          </div>
          <div class="form-item">
            <label>状态:</label>
            <select v-model="editingUser.userStatus" required>
              <option value="0">管理员</option>
              <option value="1">禁用</option>
              <option value="2">普通用户</option>
            </select>
          </div>
          <div class="form-actions form-actions--between">
            <button type="submit" class="save-button">{{ editingUser.userId ? '更新' : '添加' }}</button>
            <button type="button" class="cancel-button" @click="cancelUserForm">取消</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 帖子详情弹窗 -->
    <div v-if="showPostDetail" class="modal-mask" @click.self="showPostDetail = false">
      <div class="form-container post-detail-container">
        <h3>{{ postDetail?.title }}</h3>
        <div class="post-meta">
          <span>作者：{{ postDetail?.username }}({{ postDetail?.userId }})</span>
          <span>浏览：{{ postDetail?.viewCount }}</span>
          <span>评论：{{ postDetail?.commentCount }}</span>
          <span>状态：{{ postDetail?.postStatus === 1 ? '发布' : '草稿' }}</span>
        </div>
        <div class="post-content" v-html="renderedPostContent"></div>
        <div class="form-actions">
          <button type="button" class="cancel-button" @click="showPostDetail = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import {
  getOverview, getFileTotalCount,
  getUserList, addUser, updateUserByUserId, deleteUserByUserId,
  getPostList, getPostByPostId, deletePostByPostId,
  getCommentList, deleteCommentByCommentId
} from '../api/admin'
import { isAdmin } from '../utils/auth'

// 分页组件：内联实现，避免新增文件
const Pagination = {
  name: 'Pagination',
  props: {
    pageNo: { type: Number, default: 1 },
    pageSize: { type: Number, default: 10 },
    total: { type: Number, default: 0 }
  },
  emits: ['change'],
  computed: {
    pageTotal() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  methods: {
    go(n) {
      const target = Math.min(Math.max(1, n), this.pageTotal)
      if (target !== this.pageNo) this.$emit('change', target)
    }
  },
  template: `
    <div class="pagination">
      <span class="pagination-info">共 {{ total }} 条，{{ pageNo }}/{{ pageTotal }} 页</span>
      <button :disabled="pageNo <= 1" @click="go(pageNo - 1)">上一页</button>
      <button :disabled="pageNo >= pageTotal" @click="go(pageNo + 1)">下一页</button>
    </div>
  `
}

export default {
  name: 'Admin',
  components: { Pagination },
  data() {
    return {
      activeTab: 'dashboard',
      tabs: [
        { id: 'dashboard', name: '系统仪表盘' },
        { id: 'users', name: '用户管理' },
        { id: 'forum', name: '论坛管理' }
      ],
      // 仪表盘
      overview: null,
      // 用户管理
      users: [],
      userTotal: 0,
      userQuery: {
        pageNo: 1,
        pageSize: 10,
        usernameFuzzy: '',
        emailFuzzy: '',
        userStatus: undefined
      },
      showUserForm: false,
      editingUser: {
        userId: null,
        username: '',
        email: '',
        password: '',
        userStatus: 2
      },
      // 论坛管理
      forumSubTab: 'posts',
      posts: [],
      postTotal: 0,
      postQuery: {
        pageNo: 1,
        pageSize: 10,
        titleFuzzy: '',
        userId: undefined,
        postStatus: undefined
      },
      showPostDetail: false,
      postDetail: null,
      comments: [],
      commentTotal: 0,
      commentQuery: {
        pageNo: 1,
        pageSize: 10,
        postId: undefined,
        userId: undefined,
        contentFuzzy: ''
      }
    }
  },
  computed: {
    // 帖子 MD 正文经 marked 解析 + DOMPurify 消毒，防存储型 XSS
    renderedPostContent() {
      const md = this.postDetail?.content
      if (!md) return ''
      const rawHtml = marked.parse(md, { async: false })
      return DOMPurify.sanitize(rawHtml)
    }
  },
  methods: {
    // ===== 通用 =====
    formatDateTime(dateTime) {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      if (isNaN(date.getTime())) return dateTime
      return date.toLocaleString('zh-CN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
      })
    },
    formatFileSize(bytes) {
      const n = Number(bytes) || 0
      if (n === 0) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB', 'TB']
      const i = Math.floor(Math.log(n) / Math.log(1024))
      return (n / Math.pow(1024, i)).toFixed(2) + ' ' + units[i]
    },
    // 过滤空字符串/undefined 参数，避免后端把空串当精确匹配
    cleanParams(query) {
      const cleaned = { pageNo: query.pageNo, pageSize: query.pageSize }
      Object.keys(query).forEach(key => {
        if (key === 'pageNo' || key === 'pageSize') return
        const v = query[key]
        if (v !== undefined && v !== '' && v !== null) {
          cleaned[key] = typeof v === 'string' ? v.trim() || undefined : v
        }
      })
      return cleaned
    },

    // ===== 仪表盘 =====
    fetchOverview() {
      // overview 不含文件总数，需额外请求 fileTotalCount 合并
      Promise.all([
        getOverview(),
        getFileTotalCount().catch(err => {
          console.error('获取文件总数失败:', err)
          return 0
        })
      ]).then(([data, fileCount]) => {
        this.overview = { ...data, fileCount }
      }).catch(err => console.error('获取系统概览失败:', err))
    },

    // ===== 用户管理 =====
    getUserStatusText(status) {
      const map = { 0: '管理员', 1: '禁用', 2: '普通用户' }
      return map[status] ?? '未知'
    },
    fetchUsers() {
      getUserList(this.cleanParams(this.userQuery)).then(data => {
        this.users = data.list || []
        this.userTotal = data.totalCount || 0
      }).catch(err => console.error('获取用户列表失败:', err))
    },
    searchUsers() {
      this.userQuery.pageNo = 1
      this.fetchUsers()
    },
    resetUserSearch() {
      this.userQuery.usernameFuzzy = ''
      this.userQuery.emailFuzzy = ''
      this.userQuery.userStatus = undefined
      this.userQuery.pageNo = 1
      this.fetchUsers()
    },
    onUserPageChange(pageNo) {
      this.userQuery.pageNo = pageNo
      this.fetchUsers()
    },
    openAddUserForm() {
      this.editingUser = { userId: null, username: '', email: '', password: '', userStatus: 2 }
      this.showUserForm = true
    },
    openEditUserForm(user) {
      this.editingUser = { ...user, password: '' }
      this.showUserForm = true
    },
    cancelUserForm() {
      this.showUserForm = false
      this.editingUser = { userId: null, username: '', email: '', password: '', userStatus: 2 }
    },
    submitUserForm() {
      if (this.editingUser.userId) {
        // 更新：userId 走 query，body 仅传需修改字段
        const updateData = {
          username: this.editingUser.username,
          email: this.editingUser.email,
          userStatus: Number(this.editingUser.userStatus)
        }
        if (this.editingUser.password) updateData.password = this.editingUser.password
        updateUserByUserId(this.editingUser.userId, updateData).then(() => {
          this.fetchUsers()
          this.cancelUserForm()
          alert('更新成功')
        }).catch(err => alert('更新失败：' + err.message))
      } else {
        const newUser = {
          username: this.editingUser.username,
          email: this.editingUser.email,
          password: this.editingUser.password,
          userStatus: Number(this.editingUser.userStatus)
        }
        addUser(newUser).then(() => {
          this.fetchUsers()
          this.cancelUserForm()
          alert('添加成功')
        }).catch(err => alert('添加失败：' + err.message))
      }
    },
    deleteUser(userId) {
      if (!confirm('确定要删除该用户吗？')) return
      deleteUserByUserId(userId).then(() => {
        this.fetchUsers()
        alert('删除成功')
      }).catch(err => alert('删除失败：' + err.message))
    },

    // ===== 论坛管理 - 帖子 =====
    fetchPosts() {
      getPostList(this.cleanParams(this.postQuery)).then(data => {
        this.posts = data.list || []
        // 帖子列表返回字段为 total（非 totalCount）
        this.postTotal = data.total ?? data.totalCount ?? 0
      }).catch(err => console.error('获取帖子列表失败:', err))
    },
    searchPosts() {
      this.postQuery.pageNo = 1
      this.fetchPosts()
    },
    resetPostSearch() {
      this.postQuery.titleFuzzy = ''
      this.postQuery.userId = undefined
      this.postQuery.postStatus = undefined
      this.postQuery.pageNo = 1
      this.fetchPosts()
    },
    onPostPageChange(pageNo) {
      this.postQuery.pageNo = pageNo
      this.fetchPosts()
    },
    viewPost(postId) {
      getPostByPostId(postId).then(data => {
        this.postDetail = data
        this.showPostDetail = true
      }).catch(err => alert('获取帖子详情失败：' + err.message))
    },
    deletePost(postId) {
      if (!confirm('确定要删除该帖子吗？将同时删除 OSS 内容与所有评论。')) return
      deletePostByPostId(postId).then(() => {
        this.fetchPosts()
        alert('删除成功')
      }).catch(err => alert('删除失败：' + err.message))
    },

    // ===== 论坛管理 - 评论 =====
    fetchComments() {
      getCommentList(this.cleanParams(this.commentQuery)).then(data => {
        this.comments = data.list || []
        this.commentTotal = data.total ?? data.totalCount ?? 0
      }).catch(err => console.error('获取评论列表失败:', err))
    },
    searchComments() {
      this.commentQuery.pageNo = 1
      this.fetchComments()
    },
    resetCommentSearch() {
      this.commentQuery.postId = undefined
      this.commentQuery.userId = undefined
      this.commentQuery.contentFuzzy = ''
      this.commentQuery.pageNo = 1
      this.fetchComments()
    },
    onCommentPageChange(pageNo) {
      this.commentQuery.pageNo = pageNo
      this.fetchComments()
    },
    deleteComment(commentId) {
      if (!confirm('确定要删除该评论吗？')) return
      deleteCommentByCommentId(commentId).then(() => {
        this.fetchComments()
        alert('删除成功')
      }).catch(err => alert('删除失败：' + err.message))
    },

    // 按当前激活 tab 加载对应数据
    loadActiveTab() {
      if (this.activeTab === 'dashboard') this.fetchOverview()
      else if (this.activeTab === 'users') this.fetchUsers()
      else if (this.activeTab === 'forum') {
        if (this.forumSubTab === 'posts') this.fetchPosts()
        else this.fetchComments()
      }
    }
  },
  mounted() {
    // 非管理员不应进入此页（后端也会拦截 603），这里兜底跳走
    if (!isAdmin()) {
      alert('仅管理员可访问')
      this.$router.push('/')
      return
    }
    this.loadActiveTab()
  },
  watch: {
    activeTab() { this.loadActiveTab() },
    forumSubTab() {
      if (this.forumSubTab === 'posts' && this.posts.length === 0) this.fetchPosts()
      if (this.forumSubTab === 'comments' && this.comments.length === 0) this.fetchComments()
    }
  }
}
</script>

<style scoped>
.admin-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.admin-tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #ddd;
}

.tab-button {
  background: none;
  border: none;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
}

.tab-button:hover { color: #333; }

.tab-button.active {
  color: #4CAF50;
  border-bottom-color: #4CAF50;
}

.tab-content {
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h2 {
  margin-top: 0;
  color: #333;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.stat-card {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stat-card h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
  font-size: 16px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #4CAF50;
  margin-bottom: 20px;
}

.loading-text {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background-color: white;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.data-table th {
  background-color: #f2f2f2;
  font-weight: bold;
}

.data-table tr:hover { background-color: #f5f5f5; }

.comment-content {
  max-width: 300px;
  white-space: pre-wrap;
  word-break: break-all;
}

.action-button {
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 5px;
  font-size: 14px;
  color: white;
}

.action-button.edit { background-color: #2196F3; }
.action-button.delete { background-color: #f44336; }

.sub-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.sub-tab-button {
  background: none;
  border: 1px solid #ddd;
  padding: 6px 16px;
  cursor: pointer;
  border-radius: 4px;
  color: #666;
  transition: all 0.3s;
}

.sub-tab-button.active {
  background-color: #4CAF50;
  color: white;
  border-color: #4CAF50;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.search-bar input,
.search-bar select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 160px;
}

.status-select { min-width: 120px; }

.search-button {
  background-color: #2196F3;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.search-button:hover { background-color: #0b7dda; }

.reset-button {
  background-color: #f44336;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.reset-button:hover { background-color: #da190b; }

.refresh-button {
  background-color: #2196F3;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.refresh-button:hover { background-color: #0b7dda; }

.user-actions { margin-bottom: 20px; }

.add-button {
  background-color: #4CAF50;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

.add-button:hover { background-color: #45a049; }

.form-item { margin-bottom: 15px; }

.form-item label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

.form-item input,
.form-item select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-actions--between { justify-content: space-between; }

.save-button {
  background-color: #4CAF50;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

.save-button:hover { background-color: #45a049; }

.pagination {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  justify-content: flex-end;
}

.pagination button {
  padding: 6px 14px;
  border: 1px solid #ddd;
  background-color: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.pagination button:not(:disabled):hover {
  background-color: #4CAF50;
  color: white;
  border-color: #4CAF50;
}

.pagination-info {
  color: #666;
  font-size: 14px;
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.form-container {
  background-color: white;
  padding: 30px;
  border-radius: 8px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  max-height: 90vh;
  overflow-y: auto;
}

.post-detail-container { max-width: 800px; }

.form-container h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}

.post-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  color: #666;
  font-size: 13px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.post-content {
  line-height: 1.7;
  color: #333;
  margin-bottom: 20px;
  word-break: break-word;
}

.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3) {
  margin: 16px 0 8px;
}

.post-content :deep(pre) {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
}

.post-content :deep(code) {
  background: #f0f0f0;
  padding: 2px 4px;
  border-radius: 2px;
}

.post-content :deep(pre code) {
  background: none;
  padding: 0;
}

.post-content :deep(img) {
  max-width: 100%;
}

.post-content :deep(a) {
  color: #2196F3;
}

.post-content :deep(blockquote) {
  border-left: 4px solid #4CAF50;
  padding-left: 12px;
  color: #666;
  margin: 8px 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 25px;
}

.cancel-button {
  background-color: #f44336;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

.cancel-button:hover { background-color: #da190b; }
</style>
