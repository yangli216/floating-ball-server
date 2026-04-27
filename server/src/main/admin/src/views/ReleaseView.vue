<template>
  <div>
    <div class="filter-bar release-toolbar">
      <div class="page-toolbar__filters">
        <el-select v-model="filters.channel" placeholder="发布通道" class="filter-select" @change="loadData">
          <el-option v-for="item in channelOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button @click="loadData">刷新</el-button>
      </div>
      <el-button type="primary" @click="openUpload">上传新版本</el-button>
    </div>

    <el-table v-loading="loading" :data="records" class="page-card admin-table" row-key="channel">
      <el-table-column label="通道" width="130">
        <template slot-scope="{ row }">
          <span class="status-pill status-pill--success">{{ channelLabel(row.channel) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="120">
        <template slot-scope="{ row }">{{ row.version || '--' }}</template>
      </el-table-column>
      <el-table-column prop="target" label="平台" width="160">
        <template slot-scope="{ row }"><code>{{ row.target || '--' }}</code></template>
      </el-table-column>
      <el-table-column prop="fileName" label="安装包" min-width="220">
        <template slot-scope="{ row }">
          <span>{{ row.fileName || '暂无上传文件' }}</span>
          <span v-if="row.fileSize" class="muted">（{{ formatFileSize(row.fileSize) }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="更新源" min-width="300">
        <template slot-scope="{ row }">
          <code>{{ row.latestJsonUrl }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="pubDate" label="发布时间" width="190">
        <template slot-scope="{ row }">{{ row.pubDate || '--' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template slot-scope="{ row }">
          <a class="table-action" :class="{ 'is-disabled': !row.latestJsonUrl }" @click="row.latestJsonUrl && copy(row.latestJsonUrl)">复制源</a>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-if="dialogVisible" title="上传客户端版本" :visible.sync="dialogVisible" width="680px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="发布通道" prop="channel">
            <el-select v-model="form.channel" placeholder="请选择通道">
              <el-option v-for="item in channelOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="Tauri latest.json" prop="metadataFile" class="form-span-2">
            <input ref="metadataInput" type="file" accept=".json,application/json" @change="handleMetadataChange" />
            <div class="muted upload-hint">选择 Tauri 发布产物中的 latest.json，系统会自动解析版本号、target 和签名。</div>
          </el-form-item>
          <el-form-item label="版本号">
            <el-input v-model.trim="form.version" placeholder="默认从 latest.json 读取" />
          </el-form-item>
          <el-form-item label="平台 target">
            <el-select v-model="form.target" filterable allow-create default-first-option placeholder="请选择或输入 target">
              <el-option v-for="item in targetOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布时间">
            <el-input v-model.trim="form.pubDate" placeholder="留空由服务端生成" />
          </el-form-item>
          <el-form-item label="更新说明" class="form-span-2">
            <el-input v-model="form.notes" type="textarea" :rows="4" placeholder="可选，展示在桌面端更新内容中" />
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
    metadataFile: null,
    file: null
  }
}

export default {
  data() {
    return {
      channelOptions,
      targetOptions,
      loading: false,
      saving: false,
      dialogVisible: false,
      records: [],
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
      try {
        const data = await http.get('/admin/api/releases', {
          params: {
            channel: this.filters.channel || undefined
          }
        })
        this.records = Array.isArray(data) ? data : []
      } catch (error) {
        this.$message.error(error.message || '加载版本发布状态失败')
      } finally {
        this.loading = false
      }
    },
    channelLabel(value) {
      const item = channelOptions.find(option => option.value === value)
      return item ? item.label : value || '--'
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
    }
  }
}
</script>

<style scoped>
.release-toolbar {
  justify-content: space-between;
}

.muted {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.upload-hint {
  margin-top: 8px;
}

code {
  font-family: var(--font-mono);
  color: var(--color-text-regular);
  word-break: break-all;
}

.is-disabled {
  color: var(--color-text-placeholder);
  cursor: not-allowed;
}
</style>
