<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div class="page-toolbar__filters">
        <el-input
          v-model="keyword"
          clearable
          placeholder="输入配置编码或名称"
          class="search-input"
          @keyup.enter.native="loadData"
        />
        <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增配置</el-button>
    </div>

    <el-table :data="records" border stripe v-loading="loading">
      <el-table-column prop="cdConfig" label="配置编码" min-width="140" />
      <el-table-column prop="naConfig" label="配置名称" min-width="160" />
      <el-table-column prop="provider" label="提供商" width="120" />
      <el-table-column prop="modelName" label="模型" min-width="140" />
      <el-table-column label="作用域" min-width="140">
        <template slot-scope="{ row }">
          {{ resolveScope(row) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template slot-scope="{ row }">
          <el-tag size="mini" :type="statusMeta(row.sdStatus).type">{{ statusMeta(row.sdStatus).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="apiKeyMasked" label="API Key" min-width="140" />
      <el-table-column label="功能开关" min-width="220">
        <template slot-scope="{ row }">
          {{ truncate(row.featuresJson) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template slot-scope="{ row }">
          <div class="table-actions">
            <el-button size="mini" type="text" @click="openEdit(row)">编辑</el-button>
            <el-button size="mini" type="text" @click="removeRecord(row)">停用</el-button>
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="1080px" custom-class="config-dialog" @closed="resetForm">
      <div class="config-dialog__body">
        <el-alert
          title="这里维护的是服务端统一托管配置，保存后会影响当前作用域下的聊天、语音、知识库和审查模型能力。"
          type="info"
          :closable="false"
          show-icon
          class="config-banner"
        />

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="config-form">
          <section class="config-section">
            <div class="config-section__header">
              <h3>基础信息</h3>
              <p>先确定这条配置的编码、名称、提供商和启停状态。</p>
            </div>
            <div class="form-grid">
              <el-form-item label="配置编码">
                <el-input v-model.trim="form.cdConfig" maxlength="64" placeholder="例如 DEFAULT / ORG001_MAIN" />
              </el-form-item>
              <el-form-item label="配置名称" prop="naConfig">
                <el-input v-model.trim="form.naConfig" maxlength="128" placeholder="输入便于识别的配置名称" />
              </el-form-item>
              <el-form-item label="提供商">
                <el-input v-model.trim="form.provider" maxlength="32" placeholder="例如 openai-compatible" />
              </el-form-item>
              <el-form-item label="状态" prop="sdStatus">
                <el-radio-group v-model="form.sdStatus">
                  <el-radio-button label="1">启用</el-radio-button>
                  <el-radio-button label="0">停用</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </div>
          </section>

          <section class="config-section">
            <div class="config-section__header">
              <h3>主模型配置</h3>
              <p>桌面端默认聊天和推荐能力使用这里的 AI 地址、密钥与模型。</p>
            </div>
            <div class="form-grid">
              <el-form-item label="AI Base URL" prop="apiBaseUrl" class="form-span-2">
                <el-input v-model.trim="form.apiBaseUrl" maxlength="500" placeholder="https://api.example.com/v1" />
              </el-form-item>
              <el-form-item label="API Key">
                <el-input v-model.trim="form.apiKey" show-password maxlength="1000" placeholder="编辑时留空表示保留原值" />
                <div class="field-hint">新建时建议立即填写，编辑时留空将继续保留现有密钥。</div>
              </el-form-item>
              <el-form-item label="模型名称" prop="modelName">
                <el-input v-model.trim="form.modelName" maxlength="128" placeholder="例如 gpt-4o-mini" />
              </el-form-item>
            </div>
            <div class="test-connection-row">
              <el-button type="primary" plain :loading="testingConnection" @click="testConnection">
                {{ testingConnection ? '测试中...' : '测试服务端到 LLM' }}
              </el-button>
              <span v-if="testResult" :class="['test-connection-result', testResult.success ? 'success' : 'error']">
                {{ testResult.message }}
              </span>
            </div>
          </section>

          <section class="config-section">
            <div class="config-section__header">
              <h3>语音配置</h3>
              <p>用于语音转写与实时语音识别；留空时默认尽量复用主模型服务。</p>
            </div>
            <div class="form-grid">
              <el-form-item label="语音 Base URL">
                <el-input v-model.trim="form.audioBaseUrl" maxlength="500" placeholder="留空时默认复用 AI Base URL" />
              </el-form-item>
              <el-form-item label="语音模型">
                <el-input v-model.trim="form.audioModel" maxlength="128" placeholder="例如 whisper-1" />
              </el-form-item>
              <el-form-item label="语音服务商">
                <el-input v-model.trim="form.speechProvider" maxlength="64" placeholder="例如 openai / dashscope" />
              </el-form-item>
              <el-form-item label="语音识别模型">
                <el-input v-model.trim="form.speechModel" maxlength="128" placeholder="用于实时识别或供应商特定模型" />
              </el-form-item>
            </div>
          </section>

          <section class="config-section">
            <div class="config-section__header">
              <h3>知识库配置</h3>
              <p>按能力分开维护通用知识库与 PMPHAI，避免把地址和密钥混在同一层。</p>
            </div>
            <div class="config-subsection">
              <div class="config-subsection__title">
                <span>通用知识库</span>
                <el-switch v-model="form.knowledgeBaseEnabled" />
              </div>
              <p class="config-subsection__desc">启用后，桌面端可读取服务端下发的知识库入口地址。</p>
              <div class="form-grid">
                <el-form-item label="知识库地址" class="form-span-2">
                  <el-input v-model.trim="form.knowledgeBaseBaseUrl" maxlength="500" :disabled="!form.knowledgeBaseEnabled" placeholder="https://knowledge.example.com" />
                </el-form-item>
              </div>
            </div>

            <div class="config-subsection">
              <div class="config-subsection__title">
                <span>PMPHAI</span>
                <el-switch v-model="form.pmphaiEnabled" />
              </div>
              <p class="config-subsection__desc">人卫知识库密钥只保留在服务端，桌面端不会拿到这些敏感字段。</p>
              <div class="form-grid">
                <el-form-item label="PMPHAI 地址" class="form-span-2">
                  <el-input v-model.trim="form.pmphaiBaseUrl" maxlength="500" :disabled="!form.pmphaiEnabled" placeholder="PMPHAI 服务地址" />
                </el-form-item>
                <el-form-item label="PMPHAI App Key">
                  <el-input v-model.trim="form.pmphaiAppKey" show-password maxlength="1000" :disabled="!form.pmphaiEnabled" placeholder="编辑时留空表示保留原值" />
                </el-form-item>
                <el-form-item label="PMPHAI App Secret">
                  <el-input v-model.trim="form.pmphaiAppSecret" show-password maxlength="1000" :disabled="!form.pmphaiEnabled" placeholder="编辑时留空表示保留原值" />
                </el-form-item>
              </div>
            </div>
          </section>

          <section class="config-section">
            <div class="config-section__header">
              <h3>独立审查模型</h3>
              <p>用于事实核查或第二意见；未启用时会自动回退主模型配置。</p>
            </div>
            <div class="config-subsection">
              <div class="config-subsection__title">
                <span>审查模型开关</span>
                <el-switch v-model="form.reviewerEnabled" />
              </div>
              <p class="config-subsection__desc">开启后可配置独立地址、密钥与模型；关闭后服务端默认回退主模型。</p>
              <div class="form-grid">
                <el-form-item label="审查 Base URL">
                  <el-input v-model.trim="form.reviewerBaseUrl" maxlength="500" :disabled="!form.reviewerEnabled" placeholder="留空时默认复用主模型地址" />
                </el-form-item>
                <el-form-item label="审查模型名称">
                  <el-input v-model.trim="form.reviewerModel" maxlength="128" :disabled="!form.reviewerEnabled" placeholder="留空时默认复用主模型名称" />
                </el-form-item>
                <el-form-item label="审查 API Key" class="form-span-2">
                  <el-input v-model.trim="form.reviewerApiKey" show-password maxlength="1000" :disabled="!form.reviewerEnabled" placeholder="编辑时留空表示保留原值" />
                </el-form-item>
              </div>
            </div>
          </section>

          <section class="config-section">
            <div class="config-section__header">
              <h3>作用域与功能开关</h3>
              <p>同一条配置可绑定到全局、区域或机构；功能开关 JSON 用于补充细粒度能力。</p>
            </div>
            <div class="form-grid">
              <el-form-item label="所属区域">
                <el-select v-model="form.idRegion" clearable filterable placeholder="全局">
                  <el-option v-for="item in regionOptions" :key="item.idRegion" :label="item.naRegion" :value="item.idRegion" />
                </el-select>
              </el-form-item>
              <el-form-item label="所属机构">
                <el-select v-model="form.idOrg" clearable filterable placeholder="区域/全局" @change="syncRegionByOrg">
                  <el-option v-for="item in orgOptions" :key="item.idOrg" :label="item.naOrg" :value="item.idOrg" />
                </el-select>
              </el-form-item>
              <el-form-item label="功能开关 JSON" prop="featuresJson" class="form-span-2">
                <el-input
                  v-model="form.featuresJson"
                  type="textarea"
                  :rows="6"
                  placeholder='例如：{"regionalMode":true,"aiProxyEnabled":true,"auditEnabled":true}'
                />
                <div class="field-hint">建议只写明确需要覆盖的布尔开关，避免把临时实验配置长期留在默认配置里。</div>
              </el-form-item>
            </div>
          </section>
        </el-form>
      </div>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import http from '../api/http'
import { fetchOrgs, fetchRegions } from '../api/reference'
import {
  buildLabelMap,
  configStatusOptions,
  findStatusMeta,
  flagToBoolean,
  resolveScopeLabel,
  truncate
} from '../utils/admin'

function createDefaultForm() {
  return {
    idConfig: '',
    cdConfig: '',
    naConfig: '',
    provider: '',
    apiBaseUrl: '',
    apiKey: '',
    modelName: '',
    audioBaseUrl: '',
    audioModel: '',
    speechProvider: '',
    speechModel: '',
    knowledgeBaseEnabled: false,
    knowledgeBaseBaseUrl: '',
    pmphaiEnabled: false,
    pmphaiBaseUrl: '',
    pmphaiAppKey: '',
    pmphaiAppSecret: '',
    reviewerEnabled: false,
    reviewerBaseUrl: '',
    reviewerApiKey: '',
    reviewerModel: '',
    featuresJson: '',
    idOrg: '',
    idRegion: '',
    sdStatus: '1'
  }
}

export default {
  data() {
    return {
      loading: false,
      saving: false,
      testingConnection: false,
      dialogVisible: false,
      dialogMode: 'create',
      keyword: '',
      current: 1,
      size: 10,
      total: 0,
      records: [],
      regionOptions: [],
      orgOptions: [],
      regionMap: {},
      orgMap: {},
      testResult: null,
      form: createDefaultForm(),
      rules: {
        naConfig: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        apiBaseUrl: [{ required: true, message: '请输入 AI 服务地址', trigger: 'blur' }],
        modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增配置' : '编辑配置'
    }
  },
  async mounted() {
    await this.loadReferences()
    this.loadData()
  },
  methods: {
    truncate,
    statusMeta(value) {
      return findStatusMeta(configStatusOptions, value)
    },
    resolveScope(row) {
      return resolveScopeLabel(row, this.orgMap, this.regionMap)
    },
    async loadReferences() {
      const [regions, orgs] = await Promise.all([fetchRegions(), fetchOrgs()])
      this.regionOptions = regions
      this.orgOptions = orgs
      this.regionMap = buildLabelMap(regions, 'idRegion', 'naRegion')
      this.orgMap = buildLabelMap(orgs, 'idOrg', 'naOrg')
    },
    async loadData() {
      this.loading = true
      try {
        const data = await http.get('/admin/api/configs', {
          params: {
            current: this.current,
            size: this.size,
            keyword: this.keyword || undefined
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
      this.current = 1
      this.loadData()
    },
    openCreate() {
      this.dialogMode = 'create'
      this.form = createDefaultForm()
      this.testResult = null
      this.dialogVisible = true
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.form = {
        idConfig: row.idConfig,
        cdConfig: row.cdConfig || '',
        naConfig: row.naConfig || '',
        provider: row.provider || '',
        apiBaseUrl: row.apiBaseUrl || '',
        apiKey: '',
        modelName: row.modelName || '',
        audioBaseUrl: row.audioBaseUrl || '',
        audioModel: row.audioModel || '',
        speechProvider: row.speechProvider || '',
        speechModel: row.speechModel || '',
        knowledgeBaseEnabled: flagToBoolean(row.knowledgeBaseEnabled),
        knowledgeBaseBaseUrl: row.knowledgeBaseBaseUrl || '',
        pmphaiEnabled: flagToBoolean(row.pmphaiEnabled),
        pmphaiBaseUrl: row.pmphaiBaseUrl || '',
        pmphaiAppKey: '',
        pmphaiAppSecret: '',
        reviewerEnabled: flagToBoolean(row.reviewerEnabled),
        reviewerBaseUrl: row.reviewerBaseUrl || '',
        reviewerApiKey: '',
        reviewerModel: row.reviewerModel || '',
        featuresJson: row.featuresJson || '',
        idOrg: row.idOrg || '',
        idRegion: row.idRegion || '',
        sdStatus: row.sdStatus || '1'
      }
      this.testResult = null
      this.dialogVisible = true
    },
    syncRegionByOrg(idOrg) {
      const org = this.orgOptions.find(item => item.idOrg === idOrg)
      if (org) {
        this.form.idRegion = org.idRegion || ''
      }
    },
    resetForm() {
      this.form = createDefaultForm()
      this.testResult = null
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
    },
    async testConnection() {
      this.testingConnection = true
      this.testResult = null
      try {
        const payload = {
          idConfig: this.form.idConfig || undefined,
          apiBaseUrl: this.form.apiBaseUrl,
          apiKey: this.form.apiKey,
          modelName: this.form.modelName
        }
        const result = await http.post('/admin/api/configs/test', payload)
        this.testResult = {
          success: true,
          message: result.message || `已连通 ${result.modelName || this.form.modelName}`
        }
      } catch (error) {
        this.testResult = {
          success: false,
          message: error.message || '测试失败'
        }
      } finally {
        this.testingConnection = false
      }
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) {
          return
        }
        this.saving = true
        try {
          const payload = {
            cdConfig: this.form.cdConfig,
            naConfig: this.form.naConfig,
            provider: this.form.provider,
            apiBaseUrl: this.form.apiBaseUrl,
            apiKey: this.form.apiKey,
            modelName: this.form.modelName,
            audioBaseUrl: this.form.audioBaseUrl,
            audioModel: this.form.audioModel,
            speechProvider: this.form.speechProvider,
            speechModel: this.form.speechModel,
            knowledgeBaseEnabled: this.form.knowledgeBaseEnabled,
            knowledgeBaseBaseUrl: this.form.knowledgeBaseBaseUrl,
            pmphaiEnabled: this.form.pmphaiEnabled,
            pmphaiBaseUrl: this.form.pmphaiBaseUrl,
            pmphaiAppKey: this.form.pmphaiAppKey,
            pmphaiAppSecret: this.form.pmphaiAppSecret,
            reviewerEnabled: this.form.reviewerEnabled,
            reviewerBaseUrl: this.form.reviewerBaseUrl,
            reviewerApiKey: this.form.reviewerApiKey,
            reviewerModel: this.form.reviewerModel,
            featuresJson: this.form.featuresJson,
            idOrg: this.form.idOrg || null,
            idRegion: this.form.idRegion || null,
            sdStatus: this.form.sdStatus
          }
          if (this.dialogMode === 'create') {
            await http.post('/admin/api/configs', payload)
          } else {
            await http.put(`/admin/api/configs/${this.form.idConfig}`, payload)
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '保存失败')
        } finally {
          this.saving = false
        }
      })
    },
    removeRecord(row) {
      this.$confirm(`确认停用配置「${row.naConfig}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.delete(`/admin/api/configs/${row.idConfig}`)
          this.$message.success('停用成功')
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '停用失败')
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.config-dialog__body {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 6px;
}

.config-banner {
  margin-bottom: 18px;
}

.config-section {
  padding: 18px 20px 8px;
  border: 1px solid #e7eef6;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
}

.config-section + .config-section {
  margin-top: 16px;
}

.config-section__header {
  margin-bottom: 14px;
}

.config-section__header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #22324a;
}

.config-section__header p {
  margin: 6px 0 0;
  color: #7a8ca5;
  font-size: 13px;
  line-height: 1.6;
}

.config-subsection {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #edf3f9;
  background: #fff;
}

.config-subsection + .config-subsection {
  margin-top: 14px;
}

.config-subsection__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #22324a;
}

.config-subsection__desc {
  margin: 0 0 14px;
  color: #7a8ca5;
  font-size: 12px;
  line-height: 1.6;
}

.field-hint {
  margin-top: 6px;
  color: #8b9bb0;
  font-size: 12px;
  line-height: 1.5;
}

.test-connection-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 2px;
  margin-bottom: 12px;
}

.test-connection-result {
  font-size: 13px;
  line-height: 1.6;
}

.test-connection-result.success {
  color: #2f8f57;
}

.test-connection-result.error {
  color: #d14343;
}

.config-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.config-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  line-height: 1.4;
  font-weight: 600;
  color: #304156;
}

.config-form :deep(.el-textarea__inner),
.config-form :deep(.el-input__inner) {
  border-radius: 10px;
}
</style>
