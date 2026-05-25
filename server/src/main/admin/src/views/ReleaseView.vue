<template>
  <div class="page-surface">
    <admin-filter-bar class="release-toolbar">
      <div class="release-toolbar__content">
        <div class="page-toolbar__filters">
          <el-select v-model="filters.channel" placeholder="发布通道" class="filter-select" @change="loadData">
            <el-option v-for="item in channelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button @click="loadData">刷新</el-button>
        </div>
        <el-button type="primary" icon="el-icon-upload" @click="openUpload">上传新版本</el-button>
      </div>
    </admin-filter-bar>

    <section class="page-section page-section--table release-table-card">
      <el-table v-loading="loading" :data="records" class="admin-table" row-key="channel">
        <el-table-column label="通道" width="130">
          <template slot-scope="{ row }">
            <status-pill tone="success" :label="channelLabel(row.channel)" />
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="120">
          <template slot-scope="{ row }">{{ row.version || '--' }}</template>
        </el-table-column>
        <el-table-column label="强制更新" width="130">
          <template slot-scope="{ row }">
            <el-switch
              v-model="row.forceUpdate"
              :disabled="!row.version || policySavingKey === row.channel"
              active-color="#1D9E75"
              inactive-color="#dcdfe6"
              @change="toggleForceUpdate(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="minSupportedVersion" label="最低可用版本" width="140">
          <template slot-scope="{ row }">{{ row.minSupportedVersion || '--' }}</template>
        </el-table-column>
        <el-table-column prop="target" label="平台" width="160">
          <template slot-scope="{ row }"><code-tag :value="row.target" /></template>
        </el-table-column>
        <el-table-column prop="fileName" label="安装包" min-width="220">
          <template slot-scope="{ row }">
            <span>{{ row.fileName || '暂无上传文件' }}</span>
            <span v-if="row.fileSize" class="muted">（{{ formatFileSize(row.fileSize) }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="更新源" min-width="300">
          <template slot-scope="{ row }">
            <code-tag :value="row.latestJsonUrl" />
          </template>
        </el-table-column>
        <el-table-column prop="pubDate" label="发布时间" width="190">
          <template slot-scope="{ row }">{{ row.pubDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="{ row }">
            <table-action :disabled="!row.latestJsonUrl" @click="copy(row.latestJsonUrl)">复制源</table-action>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <div class="history-header">
      <span>历史版本</span>
    </div>
    <section class="page-section page-section--table release-table-card">
      <el-table
        v-loading="historyLoading"
        :data="historyRecords"
        class="admin-table history-table"
        :row-key="historyKey"
      >
        <el-table-column label="通道" width="130">
          <template slot-scope="{ row }">
            <status-pill tone="success" :label="channelLabel(row.channel)" />
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="120">
          <template slot-scope="{ row }">
            <span>{{ row.version || '--' }}</span>
            <span v-if="row.active" class="current-marker">当前</span>
          </template>
        </el-table-column>
        <el-table-column label="强制更新" width="130">
          <template slot-scope="{ row }">
            <status-pill :tone="row.forceUpdate ? 'warning' : 'muted'" :label="row.forceUpdate ? '已开启' : '未开启'" />
          </template>
        </el-table-column>
        <el-table-column prop="minSupportedVersion" label="最低可用版本" width="140">
          <template slot-scope="{ row }">{{ row.minSupportedVersion || '--' }}</template>
        </el-table-column>
        <el-table-column label="平台" width="180">
          <template slot-scope="{ row }"><code-tag :value="formatList(row.targets)" /></template>
        </el-table-column>
        <el-table-column label="安装包" min-width="260">
          <template slot-scope="{ row }">{{ formatList(row.fileNames) || '暂无上传文件' }}</template>
        </el-table-column>
        <el-table-column prop="pubDate" label="发布时间" width="190">
          <template slot-scope="{ row }">{{ row.pubDate || '--' }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后激活" width="170">
          <template slot-scope="{ row }">{{ formatTimestamp(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="{ row }">
            <table-action :disabled="row.active" danger @click="rollback(row)">回滚</table-action>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-if="dialogVisible" title="上传客户端版本" :visible.sync="dialogVisible" width="680px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="发布通道" prop="channel">
            <el-select v-model="form.channel" placeholder="请选择通道…">
              <el-option v-for="item in channelOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="Tauri latest.json" prop="metadataFile" class="form-span-2">
            <input ref="metadataInput" type="file" accept=".json,application/json" @change="handleMetadataChange" />
            <div class="muted upload-hint">选择 Tauri 发布产物中的 latest.json，系统会自动解析版本号、target 和签名。</div>
          </el-form-item>
          <el-form-item label="版本号">
            <el-input v-model.trim="form.version" placeholder="默认从 latest.json 读取…" />
          </el-form-item>
          <el-form-item label="平台 target">
            <el-select v-model="form.target" filterable allow-create default-first-option placeholder="请选择或输入 target…">
              <el-option v-for="item in targetOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布时间">
            <el-input v-model.trim="form.pubDate" placeholder="留空由服务端生成…" />
          </el-form-item>
          <el-form-item label="强制更新">
            <div class="switch-row">
              <el-switch
                v-model="form.forceUpdate"
                active-color="#1D9E75"
                inactive-color="#dcdfe6"
              />
              <span>低于本版本的客户端不可使用业务功能</span>
            </div>
          </el-form-item>
          <el-form-item label="更新说明" class="form-span-2">
            <el-input v-model="form.notes" type="textarea" :rows="4" placeholder="可选，展示在桌面端更新内容中…" />
          </el-form-item>
          <el-form-item label="安装包文件" prop="file" class="form-span-2">
            <input ref="fileInput" type="file" @change="handleFileChange" />
            <div class="muted upload-hint">必须选择 latest.json 中对应 target.url 指向的同名文件；例如签名指向 app.tar.gz 时不能上传 dmg。</div>
          </el-form-item>
        </div>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">上传发布</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import http from '../api/http'
import { AdminFilterBar, CodeTag, StatusPill, TableAction } from '../components/ui'

const channelOptions = [
  { value: 'production', label: '正式内网' },
  { value: 'testing', label: '测试内网' }
]

const targetOptions = ['darwin-aarch64', 'darwin-x86_64', 'windows-x86_64', 'linux-x86_64']

function createDefaultForm() {
  return {
    channel: 'production',
    version: '',
    target: '',
    notes: '',
    pubDate: '',
    forceUpdate: false,
    metadataFile: null,
    file: null
  }
}

export default {
  components: {
    AdminFilterBar,
    CodeTag,
    StatusPill,
    TableAction
  },
  data() {
    return {
      channelOptions,
      targetOptions,
      loading: false,
      historyLoading: false,
      policySavingKey: '',
      saving: false,
      dialogVisible: false,
      records: [],
      historyRecords: [],
      filters: {
        channel: ''
      },
      form: createDefaultForm(),
      rules: {
        channel: [{ required: true, message: '请选择发布通道', trigger: 'change' }],
        metadataFile: [{ required: true, message: '请选择 latest.json', trigger: 'change' }],
        file: [{ required: true, message: '请选择安装包文件', trigger: 'change' }]
      }
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      this.historyLoading = true
      try {
        const params = {
          channel: this.filters.channel || undefined
        }
        const [data, history] = await Promise.all([
          http.get('/admin/api/releases', { params }),
          http.get('/admin/api/releases/history', { params })
        ])
        this.records = Array.isArray(data) ? data : []
        this.historyRecords = Array.isArray(history) ? history : []
      } catch (error) {
        this.$message.error(error.message || '加载版本发布状态失败')
      } finally {
        this.loading = false
        this.historyLoading = false
      }
    },
    historyKey(row) {
      return `${row.channel || 'unknown'}:${row.version || 'unknown'}`
    },
    channelLabel(value) {
      const item = channelOptions.find(option => option.value === value)
      return item ? item.label : value || '--'
    },
    formatList(value) {
      return Array.isArray(value) ? value.filter(Boolean).join('、') : ''
    },
    formatTimestamp(value) {
      const timestamp = Number(value || 0)
      if (!timestamp) {
        return '--'
      }
      return new Date(timestamp).toLocaleString()
    },
    formatFileSize(value) {
      const size = Number(value || 0)
      if (size >= 1024 * 1024) {
        return `${(size / 1024 / 1024).toFixed(1)} MB`
      }
      if (size >= 1024) {
        return `${(size / 1024).toFixed(1)} KB`
      }
      return `${size} B`
    },
    openUpload() {
      this.form = createDefaultForm()
      if (this.filters.channel) {
        this.form.channel = this.filters.channel
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.form = createDefaultForm()
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = ''
      }
      if (this.$refs.metadataInput) {
        this.$refs.metadataInput.value = ''
      }
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
    },
    handleMetadataChange(event) {
      const files = event.target.files
      this.form.metadataFile = files && files.length > 0 ? files[0] : null
      if (this.$refs.formRef) {
        this.$refs.formRef.validateField('metadataFile')
      }
      if (this.form.metadataFile) {
        this.previewMetadata(this.form.metadataFile)
      }
    },
    previewMetadata(file) {
      const reader = new FileReader()
      reader.onload = () => {
        try {
          const metadata = JSON.parse(String(reader.result || '{}'))
          if (!this.form.version && metadata.version) {
            this.form.version = metadata.version
          }
          if (!this.form.pubDate && metadata.pub_date) {
            this.form.pubDate = metadata.pub_date
          }
          if (!this.form.notes && metadata.notes) {
            this.form.notes = metadata.notes
          }
          const targets = metadata.platforms ? Object.keys(metadata.platforms) : []
          if (!this.form.target && targets.length === 1) {
            this.form.target = targets[0]
          }
        } catch (error) {
          this.$message.warning('latest.json 预览失败，上传时将由服务端再次校验')
        }
      }
      reader.readAsText(file)
    },
    handleFileChange(event) {
      const files = event.target.files
      this.form.file = files && files.length > 0 ? files[0] : null
      if (this.$refs.formRef) {
        this.$refs.formRef.validateField('file')
      }
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) {
          return
        }
        this.saving = true
        try {
          const formData = new FormData()
          formData.append('channel', this.form.channel)
          formData.append('version', this.form.version)
          formData.append('target', this.form.target)
          formData.append('notes', this.form.notes || '')
          formData.append('pubDate', this.form.pubDate || '')
          formData.append('forceUpdate', this.form.forceUpdate ? 'true' : 'false')
          formData.append('metadataFile', this.form.metadataFile)
          formData.append('file', this.form.file)
          await http.post('/admin/api/releases/upload', formData, {
            timeout: 120000,
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          this.$message.success('版本发布成功')
          this.dialogVisible = false
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '版本发布失败')
        } finally {
          this.saving = false
        }
      })
    },
    async copy(value) {
      try {
        await navigator.clipboard.writeText(value)
        this.$message.success('已复制更新源')
      } catch (error) {
        this.$message.error('复制失败，请手动选择文本复制')
      }
    },
    async toggleForceUpdate(row, value) {
      if (value) {
        try {
          await this.$confirm(
            `确认开启${this.channelLabel(row.channel)}强制更新？低于 ${row.version} 的客户端将无法使用业务功能。`,
            '开启强制更新',
            { type: 'warning', confirmButtonText: '确认开启', cancelButtonText: '取消' }
          )
        } catch {
          row.forceUpdate = false
          return
        }
      }

      this.policySavingKey = row.channel
      try {
        await http.post('/admin/api/releases/policy', {
          channel: row.channel,
          forceUpdate: value
        })
        this.$message.success(value ? '已开启强制更新' : '已关闭强制更新')
        await this.loadData()
      } catch (error) {
        row.forceUpdate = !value
        this.$message.error(error.message || '切换强制更新失败')
      } finally {
        this.policySavingKey = ''
      }
    },
    async rollback(row) {
      try {
        await this.$confirm(
          `确认将${this.channelLabel(row.channel)}回滚到 ${row.version}？回滚会恢复该版本的更新包和强制更新策略。`,
          '回滚版本',
          { type: 'warning', confirmButtonText: '确认回滚', cancelButtonText: '取消' }
        )
      } catch {
        return
      }

      this.historyLoading = true
      try {
        await http.post('/admin/api/releases/rollback', {
          channel: row.channel,
          version: row.version
        })
        this.$message.success('版本回滚成功')
        await this.loadData()
      } catch (error) {
        this.$message.error(error.message || '版本回滚失败')
      } finally {
        this.historyLoading = false
      }
    }
  }
}
</script>

<style scoped>
.release-toolbar__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.release-table-card {
  padding: 0;
}

.history-header {
  margin: 18px 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.history-table {
  margin-top: 0;
}

.current-marker {
  display: inline-flex;
  margin-left: 8px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(29, 158, 117, 0.12);
  color: #13795b;
  font-size: 12px;
  font-weight: 600;
}

.muted {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.upload-hint {
  margin-top: 8px;
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  color: var(--color-text-regular);
}

.is-disabled {
  color: var(--color-text-placeholder);
  cursor: not-allowed;
}
</style>
