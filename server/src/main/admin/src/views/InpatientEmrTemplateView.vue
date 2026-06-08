<template>
  <div>
    <div class="filter-bar">
      <div class="page-toolbar__filters">
        <el-input
          v-model.trim="keyword"
          clearable
          placeholder="输入模板名称或 hash"
          class="search-input"
          @keyup.enter.native="loadData"
        />
        <el-select v-model="sdStatus" clearable placeholder="状态" class="filter-select">
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="page-card">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="templateName" label="模板名称" min-width="180">
          <template slot-scope="{ row }">{{ row.templateName || '未命名模板' }}</template>
        </el-table-column>
        <el-table-column prop="templateHash" label="模板 hash" min-width="220">
          <template slot-scope="{ row }"><code>{{ row.templateHash }}</code></template>
        </el-table-column>
        <el-table-column prop="fieldCount" label="字段数" width="90" />
        <el-table-column label="状态" width="90">
          <template slot-scope="{ row }">
            <span :class="['status-pill', row.sdStatus === '1' ? 'status-pill--success' : 'status-pill--muted']">
              <i class="dot"></i>{{ row.sdStatus === '1' ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template slot-scope="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template slot-scope="{ row }">
            <div class="table-actions">
              <a class="table-action" @click="openTemplateViewer(row)">查看模板</a>
              <a class="table-action" @click="openDetail(row)">字段维护</a>
              <a class="table-action" v-if="row.sdStatus !== '1'" @click="enableRecord(row)">启用</a>
              <a class="table-action" v-else @click="disableRecord(row)">停用</a>
              <a class="table-action table-action--danger" @click="removeRecord(row)">删除</a>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="page-footer">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page.sync="current"
          :page-size="size"
          :total="total"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog
      v-if="detailVisible"
      :title="detailTitle"
      :visible.sync="detailVisible"
      width="980px"
      @closed="resetDetail"
    >
      <el-table :data="detailFields" max-height="520">
        <el-table-column prop="id" label="data-id" min-width="180" />
        <el-table-column prop="meaning" label="字段含义" min-width="220" />
        <el-table-column label="AI生成" width="90">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="row.aiSuitable ? 'success' : 'info'">{{ row.aiSuitable ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提示词" min-width="260">
          <template slot-scope="{ row }">{{ truncate(resolvePrompt(row), 80) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template slot-scope="{ row }">
            <a
              class="table-action"
              :class="{ 'is-disabled': !row.aiSuitable }"
              @click="row.aiSuitable && openPrompt(row)"
            >维护</a>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      v-if="promptDialogVisible"
      title="维护字段提示词"
      :visible.sync="promptDialogVisible"
      width="680px"
    >
      <el-form label-position="top">
        <el-form-item label="字段">
          <el-input :value="activeField && activeField.id" disabled />
        </el-form-item>
        <el-form-item label="AI辅助生成提示词">
          <el-input v-model="promptText" type="textarea" :rows="10" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="promptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPrompt" @click="savePrompt">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog
      v-if="templateViewerVisible"
      :title="templateViewerTitle"
      :visible.sync="templateViewerVisible"
      width="1080px"
      top="5vh"
      @closed="resetTemplateViewer"
    >
      <el-tabs v-model="templateViewMode">
        <el-tab-pane label="HTML预览" name="preview">
          <iframe
            class="template-preview-frame"
            sandbox=""
            :srcdoc="templateViewerHtml"
            title="病历模板HTML预览"
          ></iframe>
        </el-tab-pane>
        <el-tab-pane label="源码" name="source">
          <el-input
            :value="templateViewerHtml"
            type="textarea"
            :rows="22"
            readonly
            class="template-source-input"
          />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script>
import http from '../api/http'
import { truncate } from '../utils/admin'

export default {
  data() {
    return {
      loading: false,
      keyword: '',
      sdStatus: '',
      current: 1,
      size: 10,
      total: 0,
      records: [],
      detailVisible: false,
      selectedRecord: null,
      templateViewerVisible: false,
      templateViewerRecord: null,
      templateViewMode: 'preview',
      promptDialogVisible: false,
      activeField: null,
      promptText: '',
      savingPrompt: false
    }
  },
  computed: {
    detailTitle() {
      return this.selectedRecord ? `${this.selectedRecord.templateName || '未命名模板'} - 字段维护` : '字段维护'
    },
    detailFields() {
      return this.selectedRecord && Array.isArray(this.selectedRecord.fields) ? this.selectedRecord.fields : []
    },
    templateViewerTitle() {
      return this.templateViewerRecord ? `${this.templateViewerRecord.templateName || '未命名模板'} - 模板查看` : '模板查看'
    },
    templateViewerHtml() {
      return this.templateViewerRecord && this.templateViewerRecord.htmlContent ? this.templateViewerRecord.htmlContent : ''
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    truncate,
    async loadData() {
      this.loading = true
      try {
        const data = await http.get('/admin/api/inpatient-emr/templates', {
          params: {
            current: this.current,
            size: this.size,
            keyword: this.keyword || undefined,
            sdStatus: this.sdStatus || undefined
          }
        })
        this.records = data.records || []
        this.total = data.total || 0
      } catch (error) {
        this.$message.error(error.message || '加载失败')
      } finally {
        this.loading = false
      }
    },
    reset() {
      this.keyword = ''
      this.sdStatus = ''
      this.current = 1
      this.loadData()
    },
    async openDetail(row) {
      try {
        this.selectedRecord = await http.get(`/admin/api/inpatient-emr/templates/${row.id}`)
        this.detailVisible = true
      } catch (error) {
        this.$message.error(error.message || '读取模板缓存失败')
      }
    },
    async openTemplateViewer(row) {
      try {
        this.templateViewerRecord = await http.get(`/admin/api/inpatient-emr/templates/${row.id}`)
        this.templateViewMode = 'preview'
        this.templateViewerVisible = true
      } catch (error) {
        this.$message.error(error.message || '读取模板源码失败')
      }
    },
    resetDetail() {
      this.selectedRecord = null
      this.activeField = null
      this.promptText = ''
    },
    resetTemplateViewer() {
      this.templateViewerRecord = null
      this.templateViewMode = 'preview'
    },
    openPrompt(field) {
      this.activeField = field
      this.promptText = this.resolvePrompt(field)
      this.promptDialogVisible = true
    },
    resolvePrompt(field) {
      return field && field.rule && field.rule.prompt ? field.rule.prompt : ''
    },
    async savePrompt() {
      if (!this.selectedRecord || !this.activeField) return
      this.savingPrompt = true
      try {
        this.selectedRecord = await http.put(
          `/admin/api/inpatient-emr/templates/${this.selectedRecord.id}/fields/${encodeURIComponent(this.activeField.id)}/prompt`,
          { prompt: this.promptText }
        )
        this.promptDialogVisible = false
        this.$message.success('提示词已保存')
        await this.loadData()
      } catch (error) {
        this.$message.error(error.message || '保存失败')
      } finally {
        this.savingPrompt = false
      }
    },
    async enableRecord(row) {
      await this.changeStatus(row, 'enable')
    },
    async disableRecord(row) {
      await this.changeStatus(row, 'disable')
    },
    async changeStatus(row, action) {
      try {
        await http.post(`/admin/api/inpatient-emr/templates/${row.id}/${action}`)
        this.$message.success(action === 'enable' ? '已启用' : '已停用')
        this.loadData()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    removeRecord(row) {
      this.$confirm('删除后客户端会重新上传同模板解析结果，确认删除？', '删除模板缓存', { type: 'warning' })
        .then(async () => {
          await http.delete(`/admin/api/inpatient-emr/templates/${row.id}`)
          this.$message.success('已删除')
          this.loadData()
        })
        .catch(() => {})
    },
    formatTime(value) {
      if (!value) return '-'
      return new Date(value).toLocaleString()
    }
  }
}
</script>

<style scoped>
.template-preview-frame {
  width: 100%;
  height: 560px;
  border: 1px solid #d8e0e3;
  border-radius: 6px;
  background: #ffffff;
}

.template-source-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.55;
}
</style>
