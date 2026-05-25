<template>
  <div class="page-surface">
    <section class="page-section page-section--padded page-section--toolbar">
      <div class="page-toolbar__filters">
        <el-input
          v-model="keyword"
          clearable
          placeholder="输入配置编码或名称…"
          class="search-input"
          @keyup.enter.native="loadData"
        />
        <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增配置</el-button>
    </section>

    <section class="page-section page-section--table">
      <el-table :data="records" v-loading="loading">
      <el-table-column label="配置编码" min-width="140"><template slot-scope="{ row }"><code-tag :value="row.cdConfig" /></template></el-table-column>
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
          <status-pill :tone="statusTone(statusMeta(row.sdStatus).type)" :label="statusMeta(row.sdStatus).label" />
        </template>
      </el-table-column>
      <el-table-column label="接口密钥" min-width="140"><template slot-scope="{ row }"><code-tag :value="row.apiKeyMasked" /></template></el-table-column>
      <el-table-column label="语音密钥" min-width="140"><template slot-scope="{ row }"><code-tag :value="row.audioApiKeyMasked" placeholder="复用主密钥" /></template></el-table-column>
      <el-table-column label="功能开关" min-width="220">
        <template slot-scope="{ row }">
          {{ truncate(row.featuresJson) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template slot-scope="{ row }">
          <div class="table-actions">
            <table-action @click="openEdit(row)">编辑</table-action>
            <table-action danger @click="removeRecord(row)">停用</table-action>
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
    </section>

    <el-dialog v-if="dialogVisible" :title="dialogTitle" :visible.sync="dialogVisible" width="1080px" custom-class="config-dialog" @closed="resetForm">
      <div class="config-dialog__body">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="config-form">
          <section class="config-section">
            <h3>基础信息</h3>
            <div class="form-grid">
              <el-form-item label="配置编码">
                <el-input v-model.trim="form.cdConfig" maxlength="64" placeholder="例如 default / org001-main" />
              </el-form-item>
              <el-form-item label="配置名称" prop="naConfig">
                <el-input v-model.trim="form.naConfig" maxlength="128" placeholder="输入便于识别的配置名称…" />
              </el-form-item>
              <el-form-item label="提供商">
                <el-input v-model.trim="form.provider" maxlength="32" placeholder="例如 openai-compatible" />
              </el-form-item>
              <el-form-item label="状态" prop="sdStatus">
                <segmented-switch v-model="form.sdStatus" :options="statusOptions" />
              </el-form-item>
            </div>
          </section>

          <section class="config-section">
            <h3>主模型配置</h3>
            <div class="form-grid">
              <el-form-item label="服务地址" prop="apiBaseUrl" class="form-span-2">
                <el-input v-model.trim="form.apiBaseUrl" maxlength="500" placeholder="https://api.example.com/v1" />
              </el-form-item>
              <el-form-item label="接口密钥">
                <el-input v-model.trim="form.apiKey" show-password maxlength="1000" />
              </el-form-item>
              <el-form-item label="模型名称" prop="modelName">
                <el-input v-model.trim="form.modelName" maxlength="128" placeholder="例如 gpt-4o-mini" />
              </el-form-item>
              <el-form-item label="chatFast 模型名称">
                <el-input v-model.trim="form.fastModelName" maxlength="128" placeholder="留空则回退主模型…" />
                <p class="form-hint">仅区域化 `chatFast()` 使用；留空时服务端自动回退 `modelName`。</p>
              </el-form-item>
              <el-form-item label="思考模式">
                <el-switch v-model="form.enableThinking" />
                <p class="form-hint">控制服务端代理主模型 / `chatFast` / 审查模型时是否向上游传 `enable_thinking`。</p>
              </el-form-item>
            </div>
            <div class="test-connection-row">
              <el-button type="primary" plain :loading="testingConnection" @click="testConnection">
                {{ testingConnection ? '测试中…' : '测试连接' }}
              </el-button>
              <span v-if="testResult" :class="['test-connection-result', testResult.success ? 'success' : 'error']">
                {{ testResult.message }}
              </span>
            </div>
          </section>

          <section class="config-section">
            <h3>语音配置</h3>
            <div class="form-grid">
              <el-form-item label="转写服务地址">
                <el-input v-model.trim="form.audioBaseUrl" maxlength="500" placeholder="留空则复用主模型服务地址…" />
                <p class="form-hint">服务端实际语音上游 Base URL；OpenAI 兼容走 /audio/transcriptions，DashScope 走 /chat/completions。</p>
              </el-form-item>
              <el-form-item label="语音接口密钥">
                <el-input v-model.trim="form.audioApiKey" show-password maxlength="1000" placeholder="留空则复用主模型接口密钥…" />
                <p class="form-hint">语音供应商或账号与主模型不一致时必须填写。</p>
              </el-form-item>
              <el-form-item label="转写模型">
                <el-input v-model.trim="form.audioModel" maxlength="128" placeholder="例如 whisper-1" />
                <p class="form-hint">服务端实际提交给上游的语音转写模型；留空默认 whisper-1。</p>
              </el-form-item>
              <el-form-item label="桌面端提供方">
                <el-select v-model="form.speechProvider" placeholder="选择桌面端语音提供方…" @change="handleSpeechProviderChange">
                  <el-option
                    v-for="item in speechProviderOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
                <p class="form-hint">{{ speechProviderHint }}</p>
              </el-form-item>
              <el-form-item label="桌面端显示模型">
                <el-input v-model.trim="form.speechModel" maxlength="128" :placeholder="speechModelPlaceholder" />
                <p class="form-hint">DashScope 时为 /api-ws/v1/inference 实时识别模型，默认 paraformer-realtime-v2，也可填 Fun-ASR/Gummy/Paraformer realtime 模型；OpenAI 兼容时仅用于桌面端展示。</p>
              </el-form-item>
            </div>
          </section>

          <section class="config-section">
            <h3>知识库配置</h3>
            <div class="config-subsection">
              <div class="config-subsection__title">
                <span>通用知识库</span>
                <el-switch v-model="form.knowledgeBaseEnabled" />
              </div>
              <div class="form-grid">
                <el-form-item label="知识库地址" class="form-span-2">
                  <el-input v-model.trim="form.knowledgeBaseBaseUrl" maxlength="500" :disabled="!form.knowledgeBaseEnabled" placeholder="https://knowledge.example.com" />
                </el-form-item>
              </div>
            </div>

            <div class="config-subsection">
              <div class="config-subsection__title">
                <span>人卫知识库</span>
                <el-switch v-model="form.pmphaiEnabled" />
              </div>
              <div class="form-grid">
                <el-form-item label="人卫知识库地址" class="form-span-2">
                  <el-input v-model.trim="form.pmphaiBaseUrl" maxlength="500" :disabled="!form.pmphaiEnabled" placeholder="人卫知识库服务地址" />
                </el-form-item>
                <el-form-item label="人卫应用标识">
                  <el-input v-model.trim="form.pmphaiAppKey" show-password maxlength="1000" :disabled="!form.pmphaiEnabled" />
                </el-form-item>
                <el-form-item label="人卫应用密钥">
                  <el-input v-model.trim="form.pmphaiAppSecret" show-password maxlength="1000" :disabled="!form.pmphaiEnabled" />
                </el-form-item>
              </div>
            </div>
          </section>

          <section class="config-section">
            <h3>独立审查模型</h3>
            <div class="config-subsection">
              <div class="config-subsection__title">
                <span>审查模型开关</span>
                <el-switch v-model="form.reviewerEnabled" />
              </div>
              <div class="form-grid">
                <el-form-item label="审查服务地址">
                  <el-input v-model.trim="form.reviewerBaseUrl" maxlength="500" :disabled="!form.reviewerEnabled" />
                </el-form-item>
                <el-form-item label="审查模型名称">
                  <el-input v-model.trim="form.reviewerModel" maxlength="128" :disabled="!form.reviewerEnabled" />
                </el-form-item>
                <el-form-item label="检查项目审查开关">
                  <el-switch v-model="form.reviewerCheckExaminationEnabled" :disabled="!form.reviewerEnabled" />
                  <p class="form-hint">关闭后桌面端不再触发 check_examination 独立审查，其他审查类型不受影响。</p>
                </el-form-item>
                <el-form-item label="审查接口密钥" class="form-span-2">
                  <el-input v-model.trim="form.reviewerApiKey" show-password maxlength="1000" :disabled="!form.reviewerEnabled" />
                </el-form-item>
              </div>
            </div>
          </section>

          <section class="config-section">
            <h3>作用域与功能开关</h3>
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
              <el-form-item label="功能开关配置" prop="featuresJson" class="form-span-2">
                <el-input
                  v-model="form.featuresJson"
                  type="textarea"
                  :rows="6"
                  placeholder='例如：{"regionalMode":true,"aiProxyEnabled":true,"auditEnabled":true}'
                />
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
  statusTone,
  truncate
} from '../utils/admin'
import { CodeTag, SegmentedSwitch, StatusPill, TableAction } from '../components/ui'

const DEFAULT_AUDIO_MODEL = 'whisper-1'
const DEFAULT_DASHSCOPE_AUDIO_MODEL = 'qwen3-asr-flash'
const DEFAULT_DASHSCOPE_REALTIME_MODEL = 'paraformer-realtime-v2'
const DEFAULT_DASHSCOPE_BASE_URL = 'https://dashscope.aliyuncs.com/compatible-mode/v1'
const DEFAULT_SPEECH_PROVIDER = 'openai-compatible'
const ALIYUN_SPEECH_PROVIDER = 'aliyun-dashscope'
const SPEECH_PROVIDER_OPTIONS = [
  {
    value: DEFAULT_SPEECH_PROVIDER,
    label: 'OpenAI 兼容接口',
    description: '区域后台统一批量转写，默认使用 /audio/transcriptions。'
  },
  {
    value: ALIYUN_SPEECH_PROVIDER,
    label: '阿里云 DashScope',
    description: '下发给桌面端作为 DashScope 语音提供方标识。'
  }
]

function normalizeSpeechProvider(value) {
  const normalized = String(value || '').trim().toLowerCase()
  if (normalized === 'aliyun' || normalized === 'dashscope' || normalized === ALIYUN_SPEECH_PROVIDER) {
    return ALIYUN_SPEECH_PROVIDER
  }
  return DEFAULT_SPEECH_PROVIDER
}

function resolveSpeechModel(provider, speechModel, audioModel) {
  const normalizedProvider = normalizeSpeechProvider(provider)
  if (speechModel) {
    if (normalizedProvider === ALIYUN_SPEECH_PROVIDER && !isDashScopeInferenceRealtimeModel(speechModel)) {
      return DEFAULT_DASHSCOPE_REALTIME_MODEL
    }
    return speechModel
  }
  if (normalizedProvider === ALIYUN_SPEECH_PROVIDER) {
    return DEFAULT_DASHSCOPE_REALTIME_MODEL
  }
  return audioModel || DEFAULT_AUDIO_MODEL
}

function isDashScopeInferenceRealtimeModel(model) {
  const normalized = String(model || '').trim().toLowerCase()
  return (normalized.indexOf('fun-asr') === 0 && normalized.indexOf('realtime') > -1)
    || normalized.indexOf('paraformer-realtime') === 0
    || normalized === 'gummy-realtime-v1'
    || normalized === 'gummy-chat-v1'
}

function createDefaultForm() {
  return {
    idConfig: '',
    cdConfig: '',
    naConfig: '',
    provider: '',
    apiBaseUrl: '',
    apiKey: '',
    modelName: '',
    fastModelName: '',
    enableThinking: false,
    audioBaseUrl: '',
    audioApiKey: '',
    audioModel: DEFAULT_AUDIO_MODEL,
    speechProvider: DEFAULT_SPEECH_PROVIDER,
    speechModel: DEFAULT_AUDIO_MODEL,
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
    reviewerCheckExaminationEnabled: true,
    featuresJson: '',
    idOrg: '',
    idRegion: '',
    sdStatus: '1'
  }
}

export default {
  components: {
    CodeTag,
    SegmentedSwitch,
    StatusPill,
    TableAction
  },
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
      speechProviderOptions: SPEECH_PROVIDER_OPTIONS,
      statusOptions: configStatusOptions,
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
    },
    speechProviderHint() {
      const provider = normalizeSpeechProvider(this.form.speechProvider)
      const option = SPEECH_PROVIDER_OPTIONS.find(item => item.value === provider)
      return option ? option.description : ''
    },
    speechModelPlaceholder() {
      return normalizeSpeechProvider(this.form.speechProvider) === ALIYUN_SPEECH_PROVIDER
        ? DEFAULT_DASHSCOPE_REALTIME_MODEL
        : (this.form.audioModel || DEFAULT_AUDIO_MODEL)
    }
  },
  async mounted() {
    await this.loadReferences()
    this.loadData()
  },
  methods: {
    truncate,
    statusTone,
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
      const audioModel = row.audioModel || DEFAULT_AUDIO_MODEL
      const speechProvider = normalizeSpeechProvider(row.speechProvider)
      this.form = {
        idConfig: row.idConfig,
        cdConfig: row.cdConfig || '',
        naConfig: row.naConfig || '',
        provider: row.provider || '',
        apiBaseUrl: row.apiBaseUrl || '',
        apiKey: '',
        modelName: row.modelName || '',
        fastModelName: row.fastModelName || '',
        enableThinking: Boolean(row.enableThinking),
        audioBaseUrl: row.audioBaseUrl || '',
        audioApiKey: '',
        audioModel,
        speechProvider,
        speechModel: resolveSpeechModel(speechProvider, row.speechModel || '', audioModel),
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
        reviewerCheckExaminationEnabled: row.reviewerCheckExaminationEnabled !== false,
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
    handleSpeechProviderChange(provider) {
      const normalized = normalizeSpeechProvider(provider)
      this.form.speechProvider = normalized
      const defaultModels = [DEFAULT_AUDIO_MODEL, DEFAULT_DASHSCOPE_AUDIO_MODEL, DEFAULT_DASHSCOPE_REALTIME_MODEL]
      if (normalized === ALIYUN_SPEECH_PROVIDER) {
        if (!this.form.audioBaseUrl) {
          this.form.audioBaseUrl = DEFAULT_DASHSCOPE_BASE_URL
        }
        if (!this.form.audioModel || defaultModels.indexOf(this.form.audioModel) > -1) {
          this.form.audioModel = DEFAULT_DASHSCOPE_AUDIO_MODEL
        }
      } else if (!this.form.audioModel || defaultModels.indexOf(this.form.audioModel) > -1) {
        this.form.audioModel = DEFAULT_AUDIO_MODEL
      }
      if (!this.form.speechModel || defaultModels.indexOf(this.form.speechModel) > -1) {
        this.form.speechModel = resolveSpeechModel(normalized, '', this.form.audioModel)
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
          const audioModel = this.form.audioModel || DEFAULT_AUDIO_MODEL
          const speechProvider = normalizeSpeechProvider(this.form.speechProvider)
          const payload = {
            cdConfig: this.form.cdConfig,
            naConfig: this.form.naConfig,
            provider: this.form.provider,
            apiBaseUrl: this.form.apiBaseUrl,
            apiKey: this.form.apiKey,
            modelName: this.form.modelName,
            fastModelName: this.form.fastModelName,
            enableThinking: this.form.enableThinking,
            audioBaseUrl: this.form.audioBaseUrl,
            audioApiKey: this.form.audioApiKey,
            audioModel,
            speechProvider,
            speechModel: resolveSpeechModel(speechProvider, this.form.speechModel, audioModel),
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
            reviewerCheckExaminationEnabled: this.form.reviewerCheckExaminationEnabled,
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

.config-section {
  margin-bottom: 22px;
}

.config-section h3 {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 500;
  color: #2C2C2A;
}

.config-subsection {
  margin-bottom: 18px;
}

.config-subsection__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 500;
  color: #2C2C2A;
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
  color: #0F6E56;
}

.test-connection-result.error {
  color: #A32D2D;
}

.form-hint {
  margin: 6px 0 0;
  color: #7A7D85;
  font-size: 12px;
  line-height: 1.5;
}

.config-form :deep(.el-select) {
  width: 100%;
}

.config-form :deep(.el-form-item) {
  margin-bottom: 18px;
}
</style>
