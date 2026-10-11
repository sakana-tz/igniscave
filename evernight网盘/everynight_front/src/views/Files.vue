<template>
  <div class="files">
    <h1>文件管理</h1>
    <div class="files-container">
      <div class="files-card">
        <div class="toolbar">
          <div class="toolbar-left">
            <button v-if="!isRecycleBin" @click="showUploadModal = true" class="btn-upload">
              <span>上传文件</span>
            </button>
            <button v-if="!isRecycleBin" @click="showUploadList = true" class="btn-upload-list">
              <span>上传列表</span>
            </button>
            <button v-if="!isRecycleBin" @click="showDownloadList = true" class="btn-upload-list">
              <span>下载列表</span>
            </button>
            <button @click="isRecycleBin ? navigateToRoot() : showRecycleBin()" class="btn-recycle">
              <span>{{ isRecycleBin ? '🏠 返回根目录' : '🗑️ 回收站' }}</span>
            </button>
          </div>
          <div class="current-path">
            <span v-for="(item, index) in pathBreadcrumbs" :key="index" class="breadcrumb">
              <span @click="navigateToPath(item.path)" class="breadcrumb-link">{{ item.label }}</span>
              <span v-if="index < pathBreadcrumbs.length - 1" class="separator">/</span>
            </span>
          </div>
        </div>

        <div class="file-list">
          <div v-if="currentFiles.length === 0" class="empty-state">
            <p>{{ isRecycleBin ? '回收站为空' : '暂无文件' }}</p>
          </div>
          <div v-else>
            <div 
              v-for="file in currentFiles" 
              :key="file.fileId || file.path"
              class="file-item"
              @click="handleFileClick(file)"
              @contextmenu.prevent="handleShowContextMenu($event, file)"
            >
              <div class="file-icon">
                <span v-if="file.type === 1" class="folder-icon">📁</span>
                <span v-else class="file-icon-text">{{ getFileIcon(file.label) }}</span>
              </div>
              <div class="file-info">
                <div class="file-name">{{ file.label }}</div>
                <div v-if="isRecycleBin" class="file-meta">{{ file.deleteTime }}</div>
                <div v-else-if="file.type === 0" class="file-meta">
                  <span v-if="file.size">{{ formatFileSize(file.size) }}</span>
                  <span v-if="file.size && file.updateTime" class="meta-separator">•</span>
                  <span v-if="file.updateTime">{{ formatUpdateTime(file.updateTime) }}</span>
                </div>
              </div>
              <div class="file-actions" @click.stop>
                <template v-if="isRecycleBin">
                  <button @click="recoverFile(file)" class="action-btn recover" title="恢复">
                    🔄
                  </button>
                  <button @click="deleteFileReal(file)" class="action-btn delete" title="永久删除">
                    🗑️
                  </button>
                </template>
                <template v-else>
                  <button v-if="file.type === 0" @click="downloadFile(file)" class="action-btn" title="下载">
                    ⬇️
                  </button>
                  <button v-if="file.type === 0" @click="shareFile(file)" class="action-btn" title="分享">
                    🔗
                  </button>
                  <button v-if="file.type === 0" @click="showMoveModal(file)" class="action-btn" title="移动">
                    📁
                  </button>
                  <button @click="deleteFile(file)" class="action-btn delete" title="删除">
                    🗑️
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showUploadModal" class="modal-overlay" @click="closeUploadModal">
      <div class="modal" @click.stop>
        <h3>上传文件</h3>
        <div class="upload-form">
          <div class="form-group">
            <label>选择文件</label>
            <input 
              type="file" 
              ref="fileInput" 
              @change="handleFileSelect"
              class="file-input"
            />
          </div>
          <div class="form-group">
            <label>上传路径</label>
            <input 
              v-model="uploadPath" 
              type="text" 
              placeholder="例如: /我的文件"
              class="path-input"
            />
          </div>
          <div v-if="selectedFile" class="form-group">
            <label>文件信息</label>
            <div class="file-info-display">
              <p>文件名: {{ selectedFile.name }}</p>
              <p>文件大小: {{ formatFileSize(selectedFile.size) }}</p>
            </div>
          </div>
          <div v-if="uploading" class="form-group">
            <label>上传进度</label>
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                :style="{ width: uploadProgress + '%' }"
              ></div>
            </div>
            <p class="progress-text">{{ uploadProgress }}%</p>
            <p class="upload-status">{{ uploadStatus }}</p>
          </div>
          <div class="modal-actions">
            <button @click="closeUploadModal" class="btn-cancel" :disabled="uploading">取消</button>
            <button v-if="uploading && !isPaused" @click="pauseUpload" class="btn-pause">暂停</button>
            <button v-else-if="uploading && isPaused" @click="resumeUpload" class="btn-resume">继续</button>
            <button @click="uploadFile" class="btn-confirm" :disabled="!selectedFile || uploading">
              {{ uploading ? '上传中...' : '上传' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showShareModal" class="modal-overlay" @click="closeShareModal">
      <div class="modal" @click.stop>
        <h3>分享文件</h3>
        <div class="share-form">
          <div class="form-group">
            <label>文件名称</label>
            <div class="file-name-display">{{ currentShareFile?.label }}</div>
          </div>
          <div class="form-group">
            <label>有效期（秒）</label>
            <input 
              v-model.number="shareForm.limitSecond" 
              type="number" 
              placeholder="例如: 3600（1小时）"
              class="number-input"
            />
          </div>
          <div class="form-group">
            <label>访问次数限制</label>
            <input 
              v-model.number="shareForm.limitCount" 
              type="number" 
              placeholder="例如: 10"
              class="number-input"
            />
          </div>
          <div class="modal-actions">
            <button @click="closeShareModal" class="btn-cancel">取消</button>
            <button @click="generateShareLink" class="btn-confirm" :disabled="sharing">
              {{ sharing ? '生成中...' : '生成分享链接' }}
            </button>
          </div>
          <div v-if="shareLink" class="share-result">
            <label>分享链接</label>
            <div class="share-link">
              <input v-model="shareLink" readonly class="share-input" />
              <button @click="copyShareLink" class="btn-copy">复制</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showMoveModalVisible" class="modal-overlay" @click="closeMoveModal">
      <div class="modal" @click.stop>
        <h3>移动文件</h3>
        <div class="move-form">
          <div class="form-group">
            <label>文件名称</label>
            <div class="file-name-display">{{ currentMoveFile?.label }}</div>
          </div>
          <div class="form-group">
            <label>目标路径</label>
            <input 
              v-model="movePath" 
              type="text" 
              placeholder="例如: /我的文件"
              class="path-input"
            />
          </div>
          <div class="modal-actions">
            <button @click="closeMoveModal" class="btn-cancel">取消</button>
            <button @click="moveFile" class="btn-confirm" :disabled="moving">
              {{ moving ? '移动中...' : '移动' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showUploadList" class="modal-overlay upload-list-overlay" @click="closeUploadList">
      <div class="modal upload-list-modal" @click.stop>
        <h3>上传列表</h3>
        <div class="upload-list">
          <div v-if="uploadList.length === 0" class="empty-state">
            <p>暂无上传记录</p>
          </div>
          <div v-else>
            <div 
              v-for="item in uploadList" 
              :key="item.id"
              class="upload-item"
            >
              <div class="upload-item-content" @click="resumeUploadTask(item)">
                <div class="upload-item-info">
                  <div class="upload-item-name">{{ item.fileName }}</div>
                  <div class="upload-item-meta">
                    <span>{{ formatFileSize(item.fileSize) }}</span>
                    <span class="meta-separator">•</span>
                    <span>{{ item.status }}</span>
                  </div>
                </div>
                <div class="upload-item-progress">
                  <div class="progress-bar">
                    <div 
                      class="progress-fill" 
                      :style="{ width: item.progress + '%' }"
                    ></div>
                  </div>
                  <div class="progress-text">{{ item.progress }}%</div>
                </div>
                <div class="upload-item-time">{{ item.uploadTime }}</div>
              </div>
              <div class="upload-item-actions">
                <button 
                  v-if="item.status === '上传中...' || item.status === '初始化上传...' || item.status === '获取上传状态...' || item.status === '检查上传状态...' || item.status === '上报分片状态...' || item.status === '合并文件...' || item.status.indexOf('上传分片') === 0 || item.status.indexOf('刷新分片') === 0"
                  @click.stop="cancelUploadTask(item)"
                  class="action-btn delete"
                  title="取消上传"
                >
                  🗑️
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="clearUploadList" class="btn-cancel">清空列表</button>
          <button @click="closeUploadList" class="btn-confirm">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="showDownloadList" class="modal-overlay upload-list-overlay" @click="closeDownloadList">
      <div class="modal upload-list-modal" @click.stop>
        <h3>下载列表</h3>
        <div class="upload-list">
          <div v-if="downloadList.length === 0" class="empty-state">
            <p>暂无下载记录</p>
          </div>
          <div v-else>
            <div 
              v-for="item in downloadList" 
              :key="item.id"
              class="upload-item"
            >
              <div class="upload-item-content">
                <div class="upload-item-info">
                  <div class="upload-item-name">{{ item.fileName }}</div>
                  <div class="upload-item-meta">
                    <span>{{ formatFileSize(item.fileSize) }}</span>
                    <span class="meta-separator">•</span>
                    <span>{{ item.status }}</span>
                  </div>
                </div>
                <div class="upload-item-progress">
                  <div class="progress-bar">
                    <div 
                      class="progress-fill" 
                      :style="{ width: item.progress + '%' }"
                    ></div>
                  </div>
                  <div class="progress-text">{{ item.progress }}%</div>
                </div>
                <div class="upload-item-time">{{ item.downloadTime }}</div>
              </div>
              <div class="upload-item-actions">
                <button 
                  v-if="item.status === '下载中...' || item.status === '断点续传中...' || item.status.indexOf('下载分片') === 0"
                  @click.stop="pauseDownload(item)"
                  class="action-btn"
                  title="暂停"
                >
                  ⏸️
                </button>
                <button 
                  v-else-if="item.status === '下载已暂停'"
                  @click.stop="resumeDownload(item)"
                  class="action-btn"
                  title="继续"
                >
                  ▶️
                </button>
                <button 
                  v-if="item.status === '下载中...' || item.status === '断点续传中...' || item.status === '下载已暂停' || item.status.indexOf('下载分片') === 0"
                  @click.stop="cancelDownload(item)"
                  class="action-btn delete"
                  title="取消"
                >
                  🗑️
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="clearDownloadList" class="btn-cancel">清空已完成</button>
          <button @click="closeDownloadList" class="btn-confirm">关闭</button>
        </div>
      </div>
    </div>

    <div 
      v-if="showContextMenu" 
      class="context-menu"
      :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
      @click="closeContextMenu"
    >
      <template v-if="isRecycleBin">
        <div @click="recoverFile(contextMenuFile)" class="menu-item">
          恢复
        </div>
        <div @click="deleteFileReal(contextMenuFile)" class="menu-item delete">
          永久删除
        </div>
      </template>
      <template v-else>
        <div v-if="contextMenuFile?.type === 0" @click="downloadFile(contextMenuFile)" class="menu-item">
          下载
        </div>
        <div v-if="contextMenuFile?.type === 0" @click="shareFile(contextMenuFile)" class="menu-item">
          分享
        </div>
        <div v-if="contextMenuFile?.type === 0" @click="showMoveModal(contextMenuFile)" class="menu-item">
          移动
        </div>
        <div @click="deleteFile(contextMenuFile)" class="menu-item delete">
          删除
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { getUploadUrl, confirmUpload, preUploadFile, reportUploadedParts, refreshPresignedUrls, mergeFile, checkPendingUpload, getUserFileList, getUserFile, downloadBigFile, deleteUserFile, shareUserFile, recoverUserFile, deleteFileByFileId, updateUserFilePath } from '../api/file'
import { downloadWithRange, clearDownloadChunks, getDownloadedBytes } from '../utils/fileDownload'

const fileTree = ref([])
const recycleFiles = ref([])
const currentPath = ref('')
const isRecycleBin = ref(false)
const showUploadModal = ref(false)
const showShareModal = ref(false)
const showMoveModalVisible = ref(false)
const showContextMenu = ref(false)
const uploading = ref(false)
const sharing = ref(false)
const moving = ref(false)
const selectedFile = ref(null)
const shareLink = ref('')
const fileInput = ref(null)

const uploadPath = ref('')
const currentShareFile = ref(null)
const currentMoveFile = ref(null)
const movePath = ref('')
const uploadProgress = ref(0)
const uploadStatus = ref('')
const abortController = ref(null)
const isPaused = ref(false)
const pauseController = ref(null)
const showUploadList = ref(false)
const uploadList = ref([])
// 点击列表项恢复任务时暂存该任务信息，文件选择回调据此判断是续传流程还是新选文件
const pendingResumeItem = ref(null)
const showDownloadList = ref(false)
const downloadList = ref([])
const uploadTasks = ref(new Map()) // 存储多个上传任务
const currentTaskId = ref(null)

const shareForm = reactive({
  limitSecond: 3600,
  limitCount: 10
})

const contextMenuPosition = reactive({
  x: 0,
  y: 0
})
const contextMenuFile = ref(null)

const pathBreadcrumbs = computed(() => {
  if (isRecycleBin.value) {
    return [{ label: '回收站', path: '' }]
  }
  
  if (!currentPath.value) return [{ label: '根目录', path: '' }]
  
  const parts = currentPath.value.split('/').filter(p => p)
  const breadcrumbs = [{ label: '根目录', path: '' }]
  
  let path = ''
  parts.forEach((part, index) => {
    path = path ? `${path}/${part}` : part
    breadcrumbs.push({ label: part, path })
  })
  
  return breadcrumbs
})

const currentFiles = computed(() => {
  if (isRecycleBin.value) {
    return recycleFiles.value
  }
  
  if (!currentPath.value) {
    return fileTree.value
  }
  
  const findFiles = (files, path) => {
    for (const file of files) {
      if (file.path === path) {
        return file.children || []
      }
      if (file.children) {
        const result = findFiles(file.children, path)
        if (result && result.length > 0) {
          return result
        }
      }
    }
    return []
  }
  
  return findFiles(fileTree.value, currentPath.value)
})

const buildFileTree = (files) => {
  const root = []
  const pathMap = new Map()
  
  files.forEach(file => {
    const node = {
      ...file,
      children: []
    }
    pathMap.set(file.path, node)
  })
  
  files.forEach(file => {
    const node = pathMap.get(file.path)
    const parentPath = file.path.substring(0, file.path.lastIndexOf('/')) || ''
    
    if (parentPath) {
      const parentNode = pathMap.get(parentPath)
      if (parentNode) {
        parentNode.children.push(node)
      } else {
        root.push(node)
      }
    } else {
      root.push(node)
    }
  })
  
  return root
}

// 兼容后端 Jackson default typing：将 ["java.util.ArrayList", [...]] 形式的类型数组解包为普通数组
const unwrapTyped = (value) => {
  if (Array.isArray(value) && value.length === 2 && typeof value[0] === 'string' && Array.isArray(value[1])) {
    return value[1]
  }
  return value
}

// 递归规范化文件树节点：解包 children 类型数组
const normalizeNodes = (nodes) => {
  const list = unwrapTyped(nodes)
  if (!Array.isArray(list)) return []
  return list.map(node => ({
    ...node,
    children: normalizeNodes(node.children)
  }))
}

const parseFileList = (fileList) => {
  // 入口先解包并规范化整棵树
  const normalizedList = normalizeNodes(fileList)
  // 辅助函数：递归构建文件树
  const buildTree = (items, includeDeleted) => {
    const result = []
    const pathMap = new Map()
    
    // 首先创建所有节点并存储到路径映射中
    const createNodes = (nodes) => {
      nodes.forEach(node => {
        const newNode = {
          ...node,
          children: []
        }
        pathMap.set(node.path, newNode)
        
        if (node.children && node.children.length > 0) {
          createNodes(node.children)
        }
      })
    }
    
    createNodes(items)
    
    // 然后构建树结构
    const processNodes = (nodes) => {
      const processed = []
      
      nodes.forEach(node => {
        // 兼容后端 del / isDel 两种删除标记字段
        const delFlag = node.isDel ?? node.del
        if (delFlag === includeDeleted) {
          const newNode = pathMap.get(node.path)
          
          if (node.children && node.children.length > 0) {
            newNode.children = processNodes(node.children)
            // 只有当文件夹有子节点时才添加
            if (newNode.children.length > 0 || node.type === 0) {
              processed.push(newNode)
            }
          } else if (node.type === 0) {
            // 单个文件直接添加
            processed.push(newNode)
          }
          // 空文件夹（type为1且没有子节点）不添加
        } else if (node.children && node.children.length > 0) {
          // 如果当前节点不符合条件，但有子节点，递归处理子节点
          const childNodes = processNodes(node.children)
          if (childNodes.length > 0) {
            processed.push(...childNodes)
          }
        }
      })
      
      return processed
    }
    
    return processNodes(items)
  }
  
  // 解析未删除的文件树（根目录）
  const rootTree = buildTree(normalizedList, false)
  
  // 解析已删除的文件树（回收站）
  const recycleTree = buildTree(normalizedList, true)
  
  return {
    rootTree,
    recycleTree
  }
}

const fetchFileList = async () => {
  try {
    const allFiles = await getUserFileList()
    const { rootTree } = parseFileList(allFiles)
    fileTree.value = rootTree
  } catch (error) {
    console.error('获取文件列表失败:', error.message)
  }
}

const fetchRecycleFileList = async () => {
  try {
    const allFiles = await getUserFileList()
    const { recycleTree } = parseFileList(allFiles)
    recycleFiles.value = recycleTree
  } catch (error) {
    console.error('获取回收站文件列表失败:', error.message)
  }
}

const showRecycleBin = () => {
  isRecycleBin.value = true
  currentPath.value = ''
  fetchRecycleFileList()
}

const recoverFile = async (file) => {
  if (!confirm(`确定要恢复"${file.label}"吗？`)) {
    return
  }

  try {
    await recoverUserFile(file.fileId)
    alert('文件已恢复')
    fetchRecycleFileList()
  } catch (error) {
    alert(error.message || '文件恢复失败')
  }
}

const deleteFileReal = async (file) => {
  if (!confirm(`确定要永久删除"${file.label}"吗？此操作不可恢复！`)) {
    return
  }

  try {
    await deleteFileByFileId(file.fileId)
    alert('文件已永久删除')
    fetchRecycleFileList()
  } catch (error) {
    alert(error.message || '文件删除失败')
  }
}

const navigateToRoot = () => {
  isRecycleBin.value = false
  currentPath.value = ''
  fetchFileList()
}

const navigateToPath = (path) => {
  if (isRecycleBin.value) {
    navigateToRoot()
    return
  }
  currentPath.value = path
}

const handleFileClick = (file) => {
  if (file.type === 1) {
    currentPath.value = file.path
  }
}

const handleFileSelect = (event) => {
  const file = event.target.files[0]
  selectedFile.value = file
  // 清空 input value 允许同一文件可重复选择
  event.target.value = ''

  // 续传场景：用户点击列表项触发文件选择，校验是否为同一文件后直接续传
  const pending = pendingResumeItem.value
  pendingResumeItem.value = null
  if (pending && file) {
    if (file.name !== pending.fileName || file.size !== pending.fileSize) {
      alert(`请选择同一文件续传：\n期望: ${pending.fileName} (${formatFileSize(pending.fileSize)})\n实际: ${file.name} (${formatFileSize(file.size)})`)
      // 重新打开选择器让用户重选
      setTimeout(() => { if (fileInput.value) fileInput.value.click() }, 50)
      return
    }
    // 复用 uploadPath 后启动上传（uploadFile 会创建新任务，后端 checkPendingUpload 自动恢复进度）
    uploadPath.value = pending.path || uploadPath.value || '/'
    showUploadModal.value = true
    showUploadList.value = false
    // 直接开始上传
    uploadFile()
  }
}

const uploadFile = async () => {
  if (!selectedFile.value) {
    alert('请选择文件')
    return
  }

  const file = selectedFile.value
  const path = uploadPath.value || '/'
  const taskId = Date.now().toString()
  
  // 创建任务状态
  const taskState = {
    id: taskId,
    file: file,
    path: path,
    progress: 0,
    status: '上传中',
    isPaused: false,
    uploading: true,
    abortController: new AbortController(),
    pauseController: null
  }
  
  // 保存任务状态
  uploadTasks.value.set(taskId, taskState)
  currentTaskId.value = taskId
  
  // 更新全局状态
  uploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = ''
  isPaused.value = false
  abortController.value = taskState.abortController
  
  const uploadItem = addToUploadList(file, '上传中', 0, path)

  try {
    // 大文件上传阈值：10MB（与文档一致，< 10MB 走普通文件直传）
    const BIG_FILE_THRESHOLD = 10 * 1024 * 1024
    
    if (file.size > BIG_FILE_THRESHOLD) {
      // 大文件上传逻辑
      await uploadBigFile(file, path, uploadItem.id, taskId)
      updateUploadList(uploadItem.id, { status: '上传成功', progress: 100 })
    } else {
      // 小文件：预签名URL直传OSS（getUploadUrl → PUT OSS → confirmUpload）
      const contentType = file.type || 'application/octet-stream'
      uploadStatus.value = '获取上传地址...'
      updateUploadList(uploadItem.id, { status: '获取上传地址...' })
      const info = await getUploadUrl(file.name, file.size, contentType, path)
      if (!info || !info.uploadUrl) {
        throw new Error('获取上传地址失败')
      }
      uploadStatus.value = '上传中...'
      updateUploadList(uploadItem.id, { status: '上传中...' })
      await putFileToOss(info.uploadUrl, file, contentType, uploadItem.id)
      uploadStatus.value = '确认上传...'
      updateUploadList(uploadItem.id, { status: '确认上传...' })
      await confirmUpload(info.ossKey, file.name, file.size, contentType, path)
      uploadStatus.value = '上传完成'
      updateUploadList(uploadItem.id, { status: '上传成功', progress: 100 })
      alert('文件上传成功')
      fetchFileList()
    }
  } catch (error) {
    let errorMessage = '上传失败'
    if (error.name === 'AbortError') {
      errorMessage = '上传已取消'
      uploadStatus.value = '上传已取消'
    } else {
      errorMessage = error.message || '文件上传失败'
      alert(errorMessage)
    }
    updateUploadList(uploadItem.id, { status: errorMessage, progress: uploadProgress.value })
  } finally {
    // 更新任务状态
    const task = uploadTasks.value.get(taskId)
    if (task) {
      task.uploading = false
      task.isPaused = false
    }
    // 清除全局状态
    uploading.value = false
    abortController.value = null
  }
}

// PUT 直传 OSS（XMLHttpRequest 以获取上传进度）
const putFileToOss = (uploadUrl, file, contentType, uploadItemId) => {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', uploadUrl)
    xhr.setRequestHeader('Content-Type', contentType)
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        const progress = Math.round((e.loaded / e.total) * 100)
        uploadProgress.value = progress
        updateUploadList(uploadItemId, { progress })
      }
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`OSS 上传失败 (HTTP ${xhr.status})`))
      }
    }
    xhr.onerror = () => reject(new Error('OSS 上传失败'))
    xhr.send(file)
  })
}

// 使用预签名 URL 将单个分片 PUT 直传 OSS（不经过后端），XHR 支持进度回调与取消。
// 失败时 error.status 携带 HTTP 状态码，调用方可据此识别 403（URL 过期）并刷新重试
const putChunkToOss = (url, chunk, signal, onProgress) => {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', url)
    if (onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress(e.loaded, e.total)
      }
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        const error = new Error(`OSS 分片上传失败 (HTTP ${xhr.status})`)
        error.status = xhr.status
        reject(error)
      }
    }
    xhr.onerror = () => reject(new Error('OSS 分片上传失败（网络错误）'))
    xhr.onabort = () => reject(new DOMException('上传已取消', 'AbortError'))
    if (signal) {
      if (signal.aborted) {
        xhr.abort()
      } else {
        signal.addEventListener('abort', () => xhr.abort(), { once: true })
      }
    }
    xhr.send(chunk)
  })
}

const uploadBigFile = async (file, path, uploadItemId, taskId) => {
  // ctx: { uploadId, stChunkSize, totalChunks, urlsByPart: Map<partNumber, url> }
  let ctx = null
  let uploadedChunks = new Set()

  try {
    // 1. 断点续传：通过后端 Redis 索引（ossKey → uploadId）反查是否有未完成的上传。
    //    后端返回 ChunkStatusDto（含 uploadId + chunkStatusList），前端据此计算剩余分片并刷新预签名 URL。
    //    无需依赖 localStorage 保存 uploadId，页面刷新或换设备都能恢复进度。
    //    注意：URL 过期不能重新 preUploadFile（会生成新 uploadId，已传分片进度作废）
    uploadStatus.value = '检查上传状态...'
    updateUploadList(uploadItemId, { status: '检查上传状态...' })

    const pending = await checkPendingUpload(path, file.name)
    if (pending) {
      const stChunkSize = pending.stChunkSize
      const totalChunks = pending.totalChunks
      uploadedChunks = new Set(unwrapTyped(pending.chunkStatusList) || [])

      // 计算未完成分片，仅为这些分片获取新 URL（返回顺序与编号顺序一一对应）
      const remainingParts = []
      for (let i = 1; i <= totalChunks; i++) {
        if (!uploadedChunks.has(i)) remainingParts.push(i)
      }

      const urlsByPart = new Map()
      if (remainingParts.length > 0) {
        uploadStatus.value = '刷新分片地址...'
        updateUploadList(uploadItemId, { status: '刷新分片地址...' })
        const refreshed = await refreshPresignedUrls(pending.uploadId, remainingParts)
        const urls = unwrapTyped(refreshed?.presignedUrls)
        if (!Array.isArray(urls) || urls.length !== remainingParts.length) {
          throw new Error('刷新分片上传地址失败：返回 URL 数量与分片数不匹配')
        }
        remainingParts.forEach((part, index) => urlsByPart.set(part, urls[index]))
      }

      ctx = { uploadId: pending.uploadId, stChunkSize, totalChunks, urlsByPart }
    }

    // 2. 无待恢复上传时预上传：获取 uploadId、分片大小、分片总数、全量预签名 PUT URL
    if (!ctx) {
      uploadStatus.value = '初始化上传...'
      updateUploadList(uploadItemId, { status: '初始化上传...' })
      const initInfo = await preUploadFile(path, file.name, file.size)
      const initUrls = unwrapTyped(initInfo?.presignedUrls)
      if (!initInfo || !initInfo.uploadId || !Array.isArray(initUrls)) {
        throw new Error('初始化分片上传失败：返回数据不完整')
      }
      const urlsByPart = new Map()
      initUrls.forEach((url, index) => urlsByPart.set(index + 1, url))
      ctx = {
        uploadId: initInfo.uploadId,
        stChunkSize: initInfo.chunkSize,
        totalChunks: initInfo.totalChunks,
        urlsByPart
      }
    }

    // 3. 计算已上传字节，恢复进度
    let uploadedBytes = 0
    for (let i = 1; i <= ctx.totalChunks; i++) {
      if (uploadedChunks.has(i)) {
        const sz = i === ctx.totalChunks
          ? Math.max(0, file.size - (i - 1) * ctx.stChunkSize)
          : ctx.stChunkSize
        uploadedBytes += sz
      }
    }
    const startProgress = file.size > 0 ? Math.round((uploadedBytes / file.size) * 100) : 0
    uploadProgress.value = startProgress
    updateUploadList(uploadItemId, { progress: startProgress })

    // 4. 逐片 PUT 直传 OSS（跳过已上传分片），分批上报分片编号
    const pendingReport = []
    const REPORT_BATCH_SIZE = 5
    for (let i = 1; i <= ctx.totalChunks; i++) {
      if (uploadedChunks.has(i)) continue

      const task = uploadTasks.value.get(taskId)
      if (!task) {
        throw new Error('任务已取消')
      }

      while (task.isPaused || isPaused.value) {
        updateUploadList(uploadItemId, { status: '上传已暂停' })
        await new Promise(resolve => setTimeout(resolve, 100))
        const updatedTask = uploadTasks.value.get(taskId)
        if (!updatedTask) {
          throw new Error('任务已取消')
        }
        if (!updatedTask.isPaused && !isPaused.value) {
          break
        }
      }

      if (abortController.value?.signal.aborted || task.abortController.signal.aborted) {
        throw new DOMException('上传已取消', 'AbortError')
      }

      uploadStatus.value = `上传分片 ${i}/${ctx.totalChunks}...`
      updateUploadList(uploadItemId, { status: `上传分片 ${i}/${ctx.totalChunks}...` })

      const start = (i - 1) * ctx.stChunkSize
      const end = i === ctx.totalChunks ? file.size : start + ctx.stChunkSize
      const chunk = file.slice(start, end)
      const baseBytes = uploadedBytes

      // 前端直接 PUT 到 OSS 预签名 URL，不经过后端
      const onChunkProgress = (loaded) => {
        const progress = Math.round(((baseBytes + loaded) / file.size) * 100)
        uploadProgress.value = progress
        updateUploadList(uploadItemId, { progress })
      }
      try {
        await putChunkToOss(ctx.urlsByPart.get(i), chunk, task.abortController.signal, onChunkProgress)
      } catch (err) {
        // 预签名 URL 过期（OSS 返回 403）：只刷新当前分片的 URL 并重试一次，不影响整体进度
        if (err.status !== 403) throw err
        uploadStatus.value = `分片 ${i} 地址过期，刷新中...`
        updateUploadList(uploadItemId, { status: `刷新分片 ${i} 地址...` })
        const refreshed = await refreshPresignedUrls(ctx.uploadId, [i])
        const freshUrl = refreshed?.presignedUrls?.[0]
        if (!freshUrl) {
          throw new Error(`刷新分片 ${i} 上传地址失败`)
        }
        ctx.urlsByPart.set(i, freshUrl)
        await putChunkToOss(freshUrl, chunk, task.abortController.signal, onChunkProgress)
      }

      uploadedChunks.add(i)
      uploadedBytes += chunk.size
      const progress = Math.round((uploadedBytes / file.size) * 100)
      uploadProgress.value = progress
      updateUploadList(uploadItemId, { progress })

      const updatedTask = uploadTasks.value.get(taskId)
      if (updatedTask) {
        updatedTask.progress = progress
      }

      // 分批上报已传分片（后端写入 BitMap 并续期 TTL），避免最后一次性上报丢失进度
      pendingReport.push(i)
      if (pendingReport.length >= REPORT_BATCH_SIZE) {
        await reportUploadedParts(ctx.uploadId, pendingReport.splice(0))
      }
    }

    // 5. 上报剩余分片，确保合并前 BitMap 完整
    if (pendingReport.length > 0) {
      uploadStatus.value = '上报分片状态...'
      updateUploadList(uploadItemId, { status: '上报分片状态...' })
      await reportUploadedParts(ctx.uploadId, pendingReport.splice(0))
    }

    // 6. 合并文件
    uploadStatus.value = '合并文件...'
    updateUploadList(uploadItemId, { status: '合并文件...' })
    await mergeFile(ctx.uploadId)

    uploadStatus.value = '上传完成'
    updateUploadList(uploadItemId, { status: '上传完成', progress: 100 })
    alert('文件上传成功')
    fetchFileList()
  } catch (error) {
    console.error('大文件上传失败:', error)
    // 失败/取消时已传分片已分批上报到后端 BitMap，下次重试通过 checkPendingUpload 恢复
    throw error
  }
}

// 断点续传阈值，与后端 download-chunk-threshold 一致（10MB）
const DOWNLOAD_CHUNK_THRESHOLD = 10 * 1024 * 1024

const downloadFile = async (file) => {
  try {
    const fileName = file.label
    // 根据文件树中的 size 判定是否走分片下载
    const isBigFile = file.size && file.size >= DOWNLOAD_CHUNK_THRESHOLD

    let url, fileSize, contentType, chunkSize

    if (isBigFile) {
      // 大文件：调用 downloadBigFile 获取下载 URL 与推荐分片大小
      const info = await downloadBigFile(file.fileId)
      if (!info || !info.url) {
        alert('获取文件下载信息失败')
        return
      }
      url = info.url
      fileSize = info.fileSize
      contentType = info.contentType
      chunkSize = info.chunkSize || 5 * 1024 * 1024
    } else {
      // 小文件：调用 getUserFile 获取直链后直接下载
      const info = await getUserFile(file.fileId)
      if (!info || !info.url) {
        alert('获取文件信息失败')
        return
      }
      url = info.url
      fileSize = info.fileSize
      contentType = info.contentType
    }

    // 小文件直接通过浏览器下载
    if (!isBigFile) {
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      link.click()
      return
    }

    const existItem = downloadList.value.find(
      item => item.fileName === fileName && item.fileSize === fileSize && (item.status === '下载中...' || item.status === '断点续传中...' || item.status.indexOf('下载分片') === 0)
    )
    if (existItem) {
      alert('该文件正在下载中')
      return
    }

    const downloadKey = fileName + '_' + fileSize
    let cachedBytes = 0
    try {
      cachedBytes = await getDownloadedBytes(downloadKey)
    } catch (e) {
      cachedBytes = 0
    }
    const startProgress = fileSize > 0 ? Math.round((cachedBytes / fileSize) * 100) : 0

    const downloadId = Date.now().toString()
    const controller = new AbortController()

    const downloadItem = reactive({
      id: downloadId,
      fileId: file.fileId,
      fileName,
      fileSize,
      progress: startProgress,
      status: startProgress > 0 && startProgress < 100 ? '断点续传中...' : '下载中...',
      downloadTime: new Date().toLocaleString('zh-CN'),
      abortController: controller,
      paused: false,
      url,
      contentType,
      chunkSize,
      isBigFile
    })
    downloadList.value.unshift(downloadItem)
    saveDownloadList()

    // 自动打开下载列表，让用户能看到进度/失败信息（大文件为后台分片下载）
    showDownloadList.value = true
    runDownloadTask(downloadItem)
  } catch (error) {
    alert(error.message || '文件下载失败')
  }
}

const runDownloadTask = async (downloadItem) => {
  try {
    await downloadWithRange({
      url: downloadItem.url,
      fileSize: downloadItem.fileSize,
      fileName: downloadItem.fileName,
      contentType: downloadItem.contentType,
      chunkSize: downloadItem.chunkSize || 5 * 1024 * 1024,
      signal: downloadItem.abortController.signal,
      onProgress: (downloaded, total, percent) => {
        downloadItem.progress = percent
      },
      onPauseCheck: () => downloadItem.paused,
      onChunk: (partNumber, totalChunks) => {
        downloadItem.status = '下载分片 ' + partNumber + '/' + totalChunks + '...'
      }
    })
    downloadItem.status = '下载完成'
    downloadItem.progress = 100
    saveDownloadList()
  } catch (err) {
    if (err.name === 'AbortError') {
      downloadItem.status = '下载已取消'
    } else {
      downloadItem.status = '下载失败: ' + err.message
    }
    saveDownloadList()
  }
}

const pauseDownload = (item) => {
  item.paused = true
  if (item.status === '下载中...' || item.status === '断点续传中...' || item.status.indexOf('下载分片') === 0) {
    item.status = '下载已暂停'
  }
  saveDownloadList()
}

const resumeDownload = async (item) => {
  if (item.abortController && !item.abortController.signal.aborted) {
    item.paused = false
    item.status = '断点续传中...'
    return
  }
  if (!item.fileId) {
    alert('无法恢复下载：缺少文件ID')
    return
  }
  try {
    // OSS 预签名 URL 1 小时过期，续传时重新获取有效 URL
    const fileInfo = item.isBigFile
      ? await downloadBigFile(item.fileId)
      : await getUserFile(item.fileId)
    if (!fileInfo || !fileInfo.url) {
      alert('获取下载链接失败')
      return
    }
    const controller = new AbortController()
    item.abortController = controller
    item.url = fileInfo.url
    if (fileInfo.chunkSize) {
      item.chunkSize = fileInfo.chunkSize
    }
    item.paused = false
    item.status = '断点续传中...'
    await runDownloadTask(item)
  } catch (e) {
    item.status = '下载失败: ' + e.message
    saveDownloadList()
  }
}

const cancelDownload = (item) => {
  if (item.abortController) {
    item.abortController.abort()
  }
  item.status = '下载已取消'
  saveDownloadList()
}

const clearDownloadList = () => {
  if (confirm('确定要清空下载列表吗？（已下载的分片缓存会保留以便续传）')) {
    downloadList.value = downloadList.value.filter(item =>
      item.status === '下载中...' || item.status === '断点续传中...' || item.status.indexOf('下载分片') === 0
    )
    saveDownloadList()
  }
}

const closeDownloadList = () => {
  showDownloadList.value = false
}

const saveDownloadList = () => {
  const serializable = downloadList.value.map(({ abortController, paused, ...rest }) => rest)
  localStorage.setItem('downloadList', JSON.stringify(serializable))
}

const deleteFile = async (file) => {
  if (file.type === 1) {
    if (!confirm(`确定要删除文件夹"${file.label}"及其下所有文件吗？`)) {
      return
    }
  } else {
    if (!confirm(`确定要删除"${file.label}"吗？`)) {
      return
    }
  }

  try {
    // 对于文件夹，需要递归删除其下所有文件
    if (file.type === 1) {
      const deleteFolderFiles = async (folder) => {
        // 先删除子文件和子文件夹
        if (folder.children && folder.children.length > 0) {
          for (const child of folder.children) {
            if (child.type === 1) {
              // 递归删除子文件夹
              await deleteFolderFiles(child)
            } else if (child.fileId) {
              // 删除文件
              await deleteUserFile(child.fileId)
            }
          }
        }
      }

      // 执行递归删除
      await deleteFolderFiles(file)
    } else if (file.fileId) {
      // 对于单个文件，直接删除
      await deleteUserFile(file.fileId)
    }

    alert('删除成功')
    fetchFileList()
  } catch (error) {
    alert(error.message || '删除失败')
  }
}

const shareFile = (file) => {
  currentShareFile.value = file
  shareLink.value = ''
  shareForm.limitSecond = 3600
  shareForm.limitCount = 10
  showShareModal.value = true
}

const generateShareLink = async () => {
  if (!shareForm.limitSecond || !shareForm.limitCount) {
    alert('请填写完整分享信息')
    return
  }

  sharing.value = true
  try {
    const result = await shareUserFile(
      currentShareFile.value.fileId,
      shareForm.limitSecond,
      shareForm.limitCount
    )
    // 后端 shareUserFile 直接返回 shareKey 字符串（非 ShareFileDto 对象）
    const shareKey = typeof result === 'string' ? result : result?.shareKey
    if (!shareKey) {
      alert('生成分享链接失败：未返回分享密钥')
      return
    }
    shareLink.value = shareKey
  } catch (error) {
    alert(error.message || '生成分享链接失败')
  } finally {
    sharing.value = false
  }
}

const copyShareLink = async () => {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(shareLink.value)
    } else {
      // 非安全上下文（http 部署）兜底：execCommand 复制
      const ta = document.createElement('textarea')
      ta.value = shareLink.value
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    alert('分享码已复制到剪贴板')
  } catch {
    alert('复制失败，请手动选择复制')
  }
}

const handleShowContextMenu = (event, file) => {
  contextMenuFile.value = file
  contextMenuPosition.x = event.clientX
  contextMenuPosition.y = event.clientY
  showContextMenu.value = true
}

const closeContextMenu = () => {
  showContextMenu.value = false
  contextMenuFile.value = null
}

const closeUploadModal = () => {
  // 只关闭模态框，不取消上传
  showUploadModal.value = false
  // 清空续传暂存：用户可能取消了文件选择器
  pendingResumeItem.value = null
  // 任务在后台继续执行
  if (uploading.value || isPaused.value) {
    // 任务在后台继续执行
  }
}

const pauseUpload = () => {
  isPaused.value = true
  uploadStatus.value = '上传已暂停'
  pauseController.value = { paused: true }
  
  // 更新当前任务状态
  if (currentTaskId.value) {
    const task = uploadTasks.value.get(currentTaskId.value)
    if (task) {
      task.isPaused = true
    }
  }
}

const resumeUpload = () => {
  isPaused.value = false
  uploadStatus.value = '继续上传...'
  if (pauseController.value) {
    pauseController.value.paused = false
    pauseController.value = null
  }
  
  // 更新当前任务状态
  if (currentTaskId.value) {
    const task = uploadTasks.value.get(currentTaskId.value)
    if (task) {
      task.isPaused = false
    }
  }
}

const closeUploadList = () => {
  showUploadList.value = false
}

const clearUploadList = () => {
  if (confirm('确定要清空上传列表吗？')) {
    uploadList.value = []
    localStorage.removeItem('uploadList')
  }
}

const resumeUploadTask = (item) => {
  // 查找对应的任务
  let task = null
  for (const [taskId, taskState] of uploadTasks.value.entries()) {
    if (taskState.file && taskState.file.name === item.fileName) {
      task = taskState
      currentTaskId.value = taskId
      break
    }
  }
  
  if (task) {
    // 打开上传模态框
    showUploadModal.value = true
    
    // 恢复任务状态到全局状态
    selectedFile.value = task.file
    uploadPath.value = task.path
    uploadProgress.value = task.progress || item.progress
    uploadStatus.value = item.status
    isPaused.value = task.isPaused || item.status === '上传已暂停'
    uploading.value = task.uploading
    abortController.value = task.abortController
    
    // 如果任务已完成，不需要恢复上传状态
    if (item.status === '上传成功' || item.status === '上传失败' || item.status === '上传已取消') {
      uploading.value = false
    }
  } else {
    // 任务不在内存中（页面刷新或已完成）：
    // - 已完成/失败/取消：不处理
    // - 已中断：让用户重选同一文件续传（File 对象无法持久化，必须重选，
    //   后端 checkPendingUpload 会自动恢复已上传分片进度）
    if (item.status === '上传成功' || item.status === '上传失败' || item.status === '上传已取消') {
      return
    }
    pendingResumeItem.value = item
    // 关闭列表模态框，打开上传模态框（fileInput 在该模态框内，需先渲染）
    showUploadList.value = false
    showUploadModal.value = true
    // 异步触发文件选择器，让模态框先渲染
    setTimeout(() => {
      if (fileInput.value) fileInput.value.click()
    }, 50)
  }
}

const cancelUploadTask = (item) => {
  if (confirm('确定要取消这个上传任务吗？')) {
    // 查找对应的任务
    let taskIdToRemove = null
    for (const [taskId, taskState] of uploadTasks.value.entries()) {
      if (taskState.file && taskState.file.name === item.fileName) {
        // 取消任务
        if (taskState.abortController) {
          taskState.abortController.abort()
        }
        taskIdToRemove = taskId
        break
      }
    }
    
    // 从任务列表中移除
    if (taskIdToRemove) {
      uploadTasks.value.delete(taskIdToRemove)
    }
    
    // 更新上传列表状态
    const index = uploadList.value.findIndex(uploadItem => uploadItem.fileName === item.fileName && uploadItem.status !== '上传成功' && uploadItem.status !== '上传失败' && uploadItem.status !== '上传已取消')
    if (index !== -1) {
      uploadList.value[index].status = '上传已取消'
      localStorage.setItem('uploadList', JSON.stringify(uploadList.value))
    }
  }
}

const addToUploadList = (file, status, progress = 0, path = '') => {
  const uploadItem = {
    id: Date.now().toString(),
    fileName: file.name,
    fileSize: file.size,
    path,
    status,
    progress,
    uploadTime: new Date().toLocaleString('zh-CN')
  }
  uploadList.value.unshift(uploadItem)
  // 保存到本地存储
  localStorage.setItem('uploadList', JSON.stringify(uploadList.value))
  return uploadItem
}

const updateUploadList = (id, updates) => {
  const index = uploadList.value.findIndex(item => item.id === id)
  if (index !== -1) {
    uploadList.value[index] = { ...uploadList.value[index], ...updates }
    // 保存到本地存储
    localStorage.setItem('uploadList', JSON.stringify(uploadList.value))
  }
}

const closeShareModal = () => {
  showShareModal.value = false
  shareLink.value = ''
  currentShareFile.value = null
}

const showMoveModal = (file) => {
  currentMoveFile.value = file
  movePath.value = currentPath.value || '/'
  showMoveModalVisible.value = true
}

const closeMoveModal = () => {
  showMoveModalVisible.value = false
  currentMoveFile.value = null
  movePath.value = ''
}

const moveFile = async () => {
  if (!movePath.value) {
    alert('请输入目标路径')
    return
  }

  moving.value = true
  try {
    await updateUserFilePath(currentMoveFile.value.fileId, movePath.value)
    alert('文件移动成功')
    closeMoveModal()
    fetchFileList()
  } catch (error) {
    alert(error.message || '文件移动失败')
  } finally {
    moving.value = false
  }
}

const getFileIcon = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const iconMap = {
    'jpg': '🖼️', 'jpeg': '🖼️', 'png': '🖼️', 'gif': '🖼️', 'svg': '🖼️',
    'pdf': '📄', 'doc': '📝', 'docx': '📝', 'txt': '📝',
    'mp4': '🎬', 'avi': '🎬', 'mov': '🎬',
    'mp3': '🎵', 'wav': '🎵',
    'zip': '📦', 'rar': '📦', '7z': '📦',
    'json': '📋', 'xml': '📋', 'html': '🌐', 'css': '🎨', 'js': '⚡'
  }
  return iconMap[ext] || '📄'
}

const formatFileSize = (size) => {
  if (!size) return ''
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

const formatUpdateTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleClickOutside = (event) => {
  if (showContextMenu.value) {
    closeContextMenu()
  }
}

onMounted(() => {
  fetchFileList()
  const savedUploadList = localStorage.getItem('uploadList')
  if (savedUploadList) {
    try {
      const arr = JSON.parse(savedUploadList)
      uploadList.value = arr.map(item => {
        // 页面刷新/关闭后，原本运行中的任务实际已中断（uploadTasks Map 已重置），
        // 但 localStorage 中的状态仍为运行中，需标记为已中断，否则点击列表项会误报"任务已取消"
        // 终态状态保留原样：上传成功 / 上传失败 / 上传已取消
        const RUNNING_PREFIXES = ['上传中', '上传分片', '初始化上传', '检查上传状态', '刷新分片地址', '获取上传地址', '合并文件', '上报分片状态', '上传已暂停']
        const wasRunning = typeof item.status === 'string' &&
          RUNNING_PREFIXES.some(p => item.status === p || item.status.startsWith(p))
        return {
          ...item,
          status: wasRunning ? '已中断（点击恢复，重选同一文件续传）' : item.status
        }
      })
      localStorage.setItem('uploadList', JSON.stringify(uploadList.value))
    } catch (error) {
      console.error('加载上传列表失败:', error)
      uploadList.value = []
    }
  }
  const savedDownloadList = localStorage.getItem('downloadList')
  if (savedDownloadList) {
    try {
      const arr = JSON.parse(savedDownloadList)
      downloadList.value = arr.map(item => {
        // 页面刷新/关闭后，原本进行中的任务实际已中断（fetch 随页面断开），
        // 但状态仍为“下载中”，需标记为已中断，否则会误判“正在下载中”而阻止重新下载。
        const wasRunning =
          item.status === '下载中...' ||
          item.status === '断点续传中...' ||
          (typeof item.status === 'string' && item.status.indexOf('下载分片') === 0)
        return {
          ...item,
          abortController: null,
          paused: false,
          status: wasRunning ? '已中断（点击文件可续传）' : item.status
        }
      })
    } catch (error) {
      console.error('加载下载列表失败:', error)
      downloadList.value = []
    }
  }
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.files {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.files h1 {
  text-align: center;
  margin-bottom: 40px;
  color: #333;
}

.files-container {
  display: flex;
  justify-content: center;
}

.files-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.8), rgba(255, 182, 193, 0.8));
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  padding: 40px;
  color: #fff;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.2);
  max-width: 900px;
  width: 100%;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  gap: 20px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.btn-upload,
.btn-recycle {
  padding: 10px 24px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  white-space: nowrap;
}

.btn-upload:hover,
.btn-recycle:hover,
.btn-upload-list:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.btn-upload-list {
  padding: 10px 24px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  white-space: nowrap;
}

.current-path {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  overflow-x: auto;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-link {
  cursor: pointer;
  color: #fff;
  text-decoration: none;
  transition: opacity 0.3s;
}

.breadcrumb-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.separator {
  color: rgba(255, 255, 255, 0.5);
}

.file-list {
  min-height: 400px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: rgba(255, 255, 255, 0.7);
}

.file-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.file-item:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateX(4px);
}

.file-icon {
  font-size: 32px;
  margin-right: 16px;
  min-width: 40px;
  text-align: center;
}

.folder-icon {
  font-size: 36px;
}

.file-icon-text {
  font-size: 28px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.file-meta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.meta-separator {
  margin: 0 8px;
  opacity: 0.6;
}

.file-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.file-item:hover .file-actions {
  opacity: 1;
}

.action-btn {
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.action-btn.delete:hover {
  background: rgba(255, 0, 0, 0.3);
  border-color: rgba(255, 0, 0, 0.4);
}

.action-btn.recover:hover {
  background: rgba(0, 255, 0, 0.3);
  border-color: rgba(0, 255, 0, 0.4);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 上传列表模态框的overlay层级低于上传模态框 */
.upload-list-overlay {
  z-index: 900;
}

.modal {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.95), rgba(255, 182, 193, 0.95));
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  padding: 30px;
  color: #fff;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
}

.modal h3 {
  font-size: 20px;
  margin-bottom: 24px;
  text-align: center;
}

.upload-form,
.share-form,
.move-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
}

.file-input,
.path-input,
.number-input {
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.file-input::placeholder,
.path-input::placeholder,
.number-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.file-input:focus,
.path-input:focus,
.number-input:focus {
  border-color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.15);
}

.file-name-display {
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 14px;
}

.file-info-display {
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 14px;
}

.file-info-display p {
  margin: 4px 0;
}

.progress-bar {
  width: 100%;
  height: 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.8), rgba(255, 182, 193, 0.8));
  border-radius: 6px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  font-weight: 500;
  margin: 4px 0;
  text-align: center;
}

.upload-status {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin: 4px 0;
  text-align: center;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 10px;
}

.btn-cancel,
.btn-confirm,
.btn-copy,
.btn-pause,
.btn-resume {
  flex: 1;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
  min-width: 80px;
}

.btn-cancel {
  background: rgba(128, 128, 128, 0.4); /* 灰色 */
}

.btn-cancel:hover:not(:disabled) {
  background: rgba(128, 128, 128, 0.5); /* 灰色 */
}

.btn-confirm,
.btn-copy {
  background: rgba(102, 126, 234, 0.4); /* 蓝色 */
}

.btn-confirm:hover:not(:disabled),
.btn-copy:hover {
  background: rgba(102, 126, 234, 0.5); /* 蓝色 */
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-pause,
.btn-resume {
  background: rgba(255, 182, 193, 0.4); /* 粉色 */
}

.btn-pause:hover:not(:disabled),
.btn-resume:hover:not(:disabled) {
  background: rgba(255, 182, 193, 0.5); /* 粉色 */
}

.upload-list-modal {
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.upload-list {
  margin-bottom: 20px;
}

.upload-item {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-item:hover {
  background: rgba(255, 255, 255, 0.15);
}

.upload-item-content {
  flex: 1;
  cursor: pointer;
}

.upload-item-info {
  margin-bottom: 12px;
}

.upload-item-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.upload-item-meta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.upload-item-progress {
  margin-bottom: 8px;
}

.upload-item-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  text-align: right;
}

.upload-item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.upload-item-actions .action-btn {
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.upload-item-actions .action-btn:hover {
  background: rgba(255, 0, 0, 0.3);
  border-color: rgba(255, 0, 0, 0.4);
}

.share-result {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.share-link {
  display: flex;
  gap: 8px;
}

.share-input {
  flex: 1;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 12px;
  outline: none;
}

.context-menu {
  position: fixed;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 8px 0;
  min-width: 150px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}

.menu-item {
  padding: 10px 16px;
  color: #333;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 14px;
}

.menu-item:hover {
  background: rgba(102, 126, 234, 0.1);
}

.menu-item.delete {
  color: #ff4757;
}

.menu-item.delete:hover {
  background: rgba(255, 71, 87, 0.1);
}

@media (max-width: 768px) {
  .files {
    padding: 20px 15px;
  }
  
  .files h1 {
    font-size: 24px;
    margin-bottom: 30px;
  }
  
  .files-card {
    padding: 20px;
  }
  
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .file-item {
    padding: 12px;
  }
  
  .file-actions {
    opacity: 1;
  }
  
  .modal {
    padding: 20px;
  }
}
</style>