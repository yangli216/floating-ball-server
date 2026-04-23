<template>
  <div class="symptom-template-page">
    <div class="page-card">
      <div class="page-toolbar symptom-toolbar">
        <div class="page-toolbar__filters symptom-filters">
          <el-select v-model="filters.medicalMode" class="filter-select" @change="handleMedicalModeChange">
            <el-option v-for="item in medicalModeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-input
            v-model.trim="filters.keyword"
            clearable
            placeholder="输入症状名称或 Key"
            class="search-input"
            @keyup.enter.native="loadData"
          />
          <el-select v-model="filters.systemCategory" clearable class="filter-select" placeholder="系统分类">
            <el-option v-for="item in systemCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filters.sdStatus" clearable class="filter-select" placeholder="状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select
            v-model="filters.idRegion"
            clearable
            filterable
            placeholder="所属区域"
            class="filter-select"
            @change="handleFilterRegionChange"
          >
            <el-option v-for="item in regionOptions" :key="item.idRegion" :label="item.naRegion" :value="item.idRegion" />
          </el-select>
          <el-select
            v-model="filters.idOrg"
            clearable
            filterable
            placeholder="所属机构"
            class="filter-select"
            @change="handleFilterOrgChange"
          >
            <el-option v-for="item in filteredOrgOptions" :key="item.idOrg" :label="item.naOrg" :value="item.idOrg" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
        <div class="symptom-toolbar__actions">
          <el-button :loading="importingJson" @click="triggerJsonImport">导入 JSON 文件</el-button>
          <el-button :loading="importing" @click="importBuiltin">导入内置模板</el-button>
          <el-button @click="exportCurrent">导出 JSON</el-button>
          <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增症状</el-button>
        </div>
      </div>
      <input
        ref="importJsonInput"
        type="file"
        accept=".json,application/json"
        class="hidden-file-input"
        @change="handleJsonFileChange"
      >
    </div>

    <div class="symptom-layout">
      <div class="page-card symptom-sidebar" v-loading="loading">
        <div class="symptom-sidebar__header">
          <div>
            <div class="section-title">症状列表</div>
            <div class="muted-text">共 {{ records.length }} 条，当前模式 {{ medicalModeLabel(filters.medicalMode) }}</div>
          </div>
        </div>
        <div class="symptom-list">
          <button
            v-for="item in records"
            :key="item.id"
            type="button"
            class="symptom-item"
            :class="{ 'is-active': selectedId === item.id }"
            @click="selectRecord(item)"
          >
            <div class="symptom-item__title">{{ item.name || '未命名症状' }}</div>
            <div class="symptom-item__meta">{{ item.key || '--' }}</div>
            <div class="symptom-item__tags">
              <span class="inline-tag">{{ statusLabel(item.sdStatus) }}</span>
              <span class="inline-tag">{{ resolveScope(item) }}</span>
            </div>
          </button>
          <div v-if="!records.length" class="empty-state">当前筛选条件下暂无症状模板</div>
        </div>
      </div>

      <div class="page-card symptom-editor">
        <div v-if="editingRecord" class="editor-shell">
          <div class="editor-header">
            <div>
              <div class="section-title">{{ editingRecord.id ? '编辑症状模板' : '新增症状模板' }}</div>
              <div class="muted-text">保存后将直接参与后台模板合并，客户端下次同步时生效。</div>
            </div>
            <div class="editor-actions">
              <el-button @click="resetCurrent">重置</el-button>
              <el-button type="danger" plain @click="removeCurrent">删除</el-button>
              <el-button type="primary" :loading="saving" @click="saveCurrent">保存当前症状</el-button>
            </div>
          </div>

          <div class="editor-scroll">
            <section class="editor-section">
              <div class="section-title">基本信息</div>
              <div class="form-grid">
                <div class="form-item">
                  <label>症状名称</label>
                  <el-input v-model.trim="editingRecord.name" maxlength="200" />
                </div>
                <div class="form-item">
                  <label>Key</label>
                  <el-input v-model.trim="editingRecord.key" maxlength="128" />
                </div>
                <div class="form-item">
                  <label>医学模式</label>
                  <el-select v-model="editingRecord.medicalMode" @change="handleDraftMedicalModeChange">
                    <el-option v-for="item in medicalModeOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </div>
                <div class="form-item">
                  <label>排序号</label>
                  <el-input-number v-model="editingRecord.sortOrder" :min="0" :step="1" />
                </div>
                <div class="form-item">
                  <label>状态</label>
                  <el-radio-group v-model="editingRecord.sdStatus">
                    <el-radio-button label="1">启用</el-radio-button>
                    <el-radio-button label="0">停用</el-radio-button>
                  </el-radio-group>
                </div>
                <div class="form-item">
                  <label class="row-label">
                    <input type="checkbox" v-model="editingRecord.isCommonSymptom" />
                    设为常用症状
                  </label>
                </div>
                <div class="form-item full">
                  <label>症状描述</label>
                  <el-input v-model="editingRecord.description" type="textarea" :rows="3" />
                </div>
                <div class="form-item full">
                  <label>系统分类</label>
                  <div class="check-group">
                    <label v-for="item in systemCategoryOptions" :key="item.value" class="check-label">
                      <input type="checkbox" :value="item.value" :checked="editingRecord.systemCategory.includes(item.value)" @change="toggleListValue(editingRecord.systemCategory, item.value, $event.target.checked)" />
                      {{ item.label }}
                    </label>
                  </div>
                </div>
                <div class="form-item full">
                  <label>部位标签</label>
                  <div class="token-editor">
                    <span v-for="(item, index) in editingRecord.bodyParts" :key="item + index" class="token-chip">
                      {{ item }}
                      <button type="button" class="token-chip__remove" @click="editingRecord.bodyParts.splice(index, 1)">×</button>
                    </span>
                    <input
                      v-model.trim="newBodyPart"
                      type="text"
                      class="token-input"
                      placeholder="输入后回车添加部位"
                      @keyup.enter.prevent="addBodyPart"
                    />
                  </div>
                </div>
                <div class="form-item">
                  <label>所属区域</label>
                  <el-select v-model="editingRecord.idRegion" clearable filterable placeholder="全局" @change="handleDraftRegionChange">
                    <el-option v-for="item in regionOptions" :key="item.idRegion" :label="item.naRegion" :value="item.idRegion" />
                  </el-select>
                </div>
                <div class="form-item">
                  <label>所属机构</label>
                  <el-select v-model="editingRecord.idOrg" clearable filterable placeholder="区域/全局" @change="handleDraftOrgChange">
                    <el-option v-for="item in draftOrgOptions" :key="item.idOrg" :label="item.naOrg" :value="item.idOrg" />
                  </el-select>
                </div>
                <div class="form-item full">
                  <label>适用性别</label>
                  <div class="check-group">
                    <label class="check-label">
                      <input type="checkbox" :checked="getSymptomGenders().includes('1')" @change="toggleListValue(getSymptomGenders(), '1', $event.target.checked)" />
                      男性
                    </label>
                    <label class="check-label">
                      <input type="checkbox" :checked="getSymptomGenders().includes('2')" @change="toggleListValue(getSymptomGenders(), '2', $event.target.checked)" />
                      女性
                    </label>
                  </div>
                </div>
                <div class="form-item full">
                  <label>自定义脚本</label>
                  <el-input v-model="editingRecord.customScript" type="textarea" :rows="3" placeholder="可选，自定义运行脚本" />
                </div>
              </div>
            </section>

            <section class="editor-section">
              <div class="section-header">
                <div class="section-title">问诊结构</div>
                <div class="section-header__actions">
                  <el-button size="mini" @click="addSection">新增分组</el-button>
                </div>
              </div>
              <div class="muted-text section-desc">保留 desktop disease editor 的“分组 + 字段”结构，客户端会直接消费这里生成的 JSON。</div>

              <div v-for="(section, sectionIndex) in editingRecord.config.sections" :key="section.id || sectionIndex" class="section-card">
                <div class="section-card__header">
                  <div class="section-card__title">分组 {{ sectionIndex + 1 }}</div>
                  <div class="section-card__actions">
                    <el-button size="mini" @click="addField(section)">新增字段</el-button>
                    <el-button size="mini" type="danger" plain @click="removeSection(sectionIndex)">删除分组</el-button>
                  </div>
                </div>
                <div class="form-grid">
                  <div class="form-item">
                    <label>分组标题</label>
                    <el-input v-model.trim="section.title" maxlength="128" />
                  </div>
                  <div class="form-item">
                    <label>分组 ID</label>
                    <el-input v-model.trim="section.id" maxlength="128" />
                  </div>
                  <div class="form-item full">
                    <label>分组描述</label>
                    <el-input v-model="section.description" type="textarea" :rows="2" placeholder="可选，中医模板常用" />
                  </div>
                </div>

                <div v-for="(field, fieldIndex) in section.fields" :key="field.id || fieldIndex" class="field-card">
                  <div class="field-card__header">
                    <div class="field-card__title">{{ field.label || `字段 ${fieldIndex + 1}` }}</div>
                    <div class="field-card__actions">
                      <el-button size="mini" type="danger" plain @click="removeField(section, fieldIndex)">删除字段</el-button>
                    </div>
                  </div>
                  <div class="form-grid">
                    <div class="form-item">
                      <label>字段标签</label>
                      <el-input v-model.trim="field.label" maxlength="128" />
                    </div>
                    <div class="form-item">
                      <label>字段 Key</label>
                      <el-input v-model.trim="field.key" maxlength="128" />
                    </div>
                    <div class="form-item">
                      <label>Storage Key</label>
                      <el-input v-model.trim="field.storageKey" maxlength="128" />
                    </div>
                    <div class="form-item">
                      <label>字段类型</label>
                      <el-select v-model="field.type" @change="handleFieldTypeChange(field)">
                        <el-option v-for="item in fieldTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                      </el-select>
                    </div>
                    <div class="form-item">
                      <label class="row-label">
                        <input type="checkbox" v-model="field.required" />
                        必填字段
                      </label>
                    </div>
                    <div class="form-item full" v-if="usesOptionList(field)">
                      <label>{{ field.type === 'input_radio' ? '单位选项' : '候选项' }}</label>
                      <div class="token-editor">
                        <span v-for="(item, index) in getOptionList(field)" :key="item + index" class="token-chip">
                          {{ item }}
                          <button type="button" class="token-chip__remove" @click="removeOption(field, index)">×</button>
                        </span>
                        <input
                          v-model.trim="field.__newOption"
                          type="text"
                          class="token-input"
                          placeholder="输入后回车添加"
                          @keyup.enter.prevent="addOption(field)"
                        />
                      </div>
                    </div>
                    <div class="form-item" v-if="field.type === 'degree_slider'">
                      <label>最小值</label>
                      <el-input-number v-model="field.props.min" :min="0" :step="1" />
                    </div>
                    <div class="form-item" v-if="field.type === 'degree_slider'">
                      <label>最大值</label>
                      <el-input-number v-model="field.props.max" :min="0" :step="1" />
                    </div>
                    <div class="form-item full" v-if="field.type === 'degree_slider'">
                      <label>等级文案</label>
                      <div class="token-editor">
                        <span v-for="(item, index) in ensureArray(field.props, 'labels')" :key="item + index" class="token-chip">
                          {{ item }}
                          <button type="button" class="token-chip__remove" @click="field.props.labels.splice(index, 1)">×</button>
                        </span>
                        <input
                          v-model.trim="field.__newLabel"
                          type="text"
                          class="token-input"
                          placeholder="输入后回车添加"
                          @keyup.enter.prevent="addSliderLabel(field)"
                        />
                      </div>
                    </div>
                    <div class="form-item full" v-if="field.type === 'preference_pair'">
                      <label>偏好配对</label>
                      <div class="pair-list">
                        <div v-for="(pair, pairIndex) in ensurePairs(field)" :key="pairIndex" class="pair-row">
                          <el-input v-model.trim="pair[0]" placeholder="左侧选项" />
                          <span class="pair-sep">vs</span>
                          <el-input v-model.trim="pair[1]" placeholder="右侧选项" />
                          <button type="button" class="icon-button danger" @click="field.props.pairs.splice(pairIndex, 1)">×</button>
                        </div>
                        <el-button size="mini" @click="addPreferencePair(field)">新增配对</el-button>
                      </div>
                    </div>
                    <div class="form-item" v-if="field.type === 'input' || field.type === 'number' || field.type === 'input_radio'">
                      <label>占位提示</label>
                      <el-input v-model.trim="field.props.placeholder" maxlength="128" />
                    </div>
                    <div class="form-item" v-if="field.type === 'number'">
                      <label>单位</label>
                      <el-input v-model.trim="field.props.unit" maxlength="32" />
                    </div>
                    <div class="form-item full" v-if="field.type === 'checkbox'">
                      <label>互斥组</label>
                      <div class="mutual-group-list">
                        <div v-for="(group, groupIndex) in ensureMutualGroups(field)" :key="groupIndex" class="mutual-group">
                          <div class="mutual-group__items">
                            <span v-for="(item, index) in group" :key="item + index" class="token-chip">
                              {{ item }}
                              <button type="button" class="token-chip__remove" @click="group.splice(index, 1)">×</button>
                            </span>
                            <select class="mutual-select" @change="addToMutualGroup(field, groupIndex, $event.target.value); $event.target.value = ''">
                              <option value="">添加到组...</option>
                              <option v-for="item in getMutualAvailableOptions(field, group)" :key="item" :value="item">{{ item }}</option>
                            </select>
                          </div>
                          <button type="button" class="icon-button danger" @click="field.props.mutualExclusions.splice(groupIndex, 1)">删除组</button>
                        </div>
                        <el-button size="mini" @click="addMutualGroup(field)">新增互斥组</el-button>
                      </div>
                    </div>
                    <div class="form-item full">
                      <label>字段适用性别</label>
                      <div class="check-group">
                        <label class="check-label">
                          <input type="checkbox" :checked="fieldGenders(field).includes('1')" @change="toggleListValue(fieldGenders(field), '1', $event.target.checked)" />
                          男性
                        </label>
                        <label class="check-label">
                          <input type="checkbox" :checked="fieldGenders(field).includes('2')" @change="toggleListValue(fieldGenders(field), '2', $event.target.checked)" />
                          女性
                        </label>
                      </div>
                    </div>
                    <div class="form-item">
                      <label>年龄下限</label>
                      <el-input-number v-model="fieldAgeRange(field).min" :min="0" :step="1" />
                    </div>
                    <div class="form-item">
                      <label>年龄上限</label>
                      <el-input-number v-model="fieldAgeRange(field).max" :min="0" :step="1" />
                    </div>
                    <div class="form-item">
                      <label>年龄单位</label>
                      <el-select v-model="fieldAgeRange(field).unit">
                        <el-option label="岁" value="Y" />
                        <el-option label="月" value="M" />
                        <el-option label="天" value="D" />
                      </el-select>
                    </div>
                    <div class="form-item full">
                      <label>文本生成目标</label>
                      <div class="check-group">
                        <label class="check-label">
                          <input type="checkbox" :checked="fieldTargets(field).includes('chiefComplaint')" @change="toggleListValue(fieldTargets(field), 'chiefComplaint', $event.target.checked)" />
                          主诉
                        </label>
                        <label class="check-label">
                          <input type="checkbox" :checked="fieldTargets(field).includes('historyOfPresentIllness')" @change="toggleListValue(fieldTargets(field), 'historyOfPresentIllness', $event.target.checked)" />
                          现病史
                        </label>
                      </div>
                    </div>
                    <div class="form-item full">
                      <label>文本模板</label>
                      <el-input v-model="ensureTextGenConfig(field).template" maxlength="256" placeholder="例如：{label}：{value}" />
                    </div>
                    <div class="form-item">
                      <label>多选分隔符</label>
                      <el-input v-model.trim="ensureTextGenConfig(field).separator" maxlength="8" placeholder="默认 、" />
                    </div>
                    <div class="form-item full">
                      <label>忽略值</label>
                      <div class="token-editor">
                        <span v-for="(item, index) in ignoreValues(field)" :key="item + index" class="token-chip">
                          {{ item }}
                          <button type="button" class="token-chip__remove" @click="ignoreValues(field).splice(index, 1)">×</button>
                        </span>
                        <select class="mutual-select" @change="addIgnoreValue(field, $event.target.value); $event.target.value = ''">
                          <option value="">选择要忽略的值...</option>
                          <option v-for="item in getOptionList(field)" :key="item" :value="item" :disabled="ignoreValues(field).includes(item)">{{ item }}</option>
                        </select>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <section class="editor-section" v-if="editingRecord.medicalMode === 'tcm'">
              <div class="section-title">TCM 扩展元数据</div>
              <div class="muted-text section-desc">保留 `tcmMetadata` 原始结构，便于维护辨证分类、脏腑经络与可能证型。</div>
              <el-input v-model="tcmMetadataText" type="textarea" :rows="12" placeholder="请输入合法 JSON，对应 tcmMetadata" />
            </section>
          </div>
        </div>

        <div v-else class="empty-editor">
          <div class="section-title">请选择或新建一个症状模板</div>
          <div class="muted-text">左侧选择记录后即可在这里编辑，保存后客户端会从后台表读取。</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import http from '../api/http'
import { fetchOrgs, fetchRegions } from '../api/reference'
import { buildLabelMap, resolveScopeLabel } from '../utils/admin'

const medicalModeOptions = [
  { value: 'western', label: '西医' },
  { value: 'tcm', label: '中医' }
]

const statusOptions = [
  { value: '1', label: '启用' },
  { value: '0', label: '停用' }
]

const fieldTypeOptions = [
  { value: 'radio', label: '单选' },
  { value: 'checkbox', label: '多选' },
  { value: 'input', label: '文本输入' },
  { value: 'input_radio', label: '数值+单位' },
  { value: 'number', label: '数字输入' },
  { value: 'degree_slider', label: '程度滑条' },
  { value: 'preference_pair', label: '偏好配对' }
]

const systemCategoryOptions = [
  { value: 'respiratory', label: '呼吸系统' },
  { value: 'circulatory', label: '循环系统' },
  { value: 'endocrine', label: '内分泌系统' },
  { value: 'digestive', label: '消化系统' },
  { value: 'urinary', label: '泌尿系统' },
  { value: 'reproductive', label: '生殖系统' },
  { value: 'nervous', label: '神经系统' },
  { value: 'motor', label: '运动系统' },
  { value: 'other', label: '其他' }
]

function createFilters() {
  return {
    keyword: '',
    medicalMode: 'western',
    systemCategory: '',
    sdStatus: '1',
    idRegion: '',
    idOrg: ''
  }
}

function createDefaultSection(index) {
  const ts = Date.now() + index
  return {
    id: `section_${ts}`,
    title: '症状属性问诊',
    description: '',
    fields: []
  }
}

function createDefaultField() {
  const ts = Date.now()
  return {
    id: `field_${ts}`,
    key: `field_${ts}`,
    label: '新字段',
    type: 'radio',
    props: {
      options: ['选项1', '选项2']
    },
    storageKey: `field_${ts}`,
    required: false,
    applicablePopulation: {
      genders: [],
      ageRange: {
        unit: 'Y'
      }
    },
    textGenConfig: {
      targets: [],
      template: '{value}',
      separator: '、',
      optionConfig: {
        ignoreValues: []
      }
    }
  }
}

function createDefaultRecord(mode, scope) {
  return {
    id: '',
    medicalMode: mode || 'western',
    key: '',
    name: '',
    description: '',
    isCommonSymptom: false,
    systemCategory: [],
    bodyParts: [],
    customScript: '',
    config: {
      title: '智能问诊',
      sections: [createDefaultSection(0)]
    },
    applicablePopulation: {
      genders: [],
      ageGroups: []
    },
    tcmMetadata: null,
    sortOrder: 0,
    sdStatus: '1',
    idRegion: (scope && scope.idRegion) || '',
    idOrg: (scope && scope.idOrg) || '',
    createdAt: Date.now(),
    updatedAt: Date.now()
  }
}

function cloneRecord(value) {
  return JSON.parse(JSON.stringify(value))
}

export default {
  data() {
    return {
      medicalModeOptions,
      statusOptions,
      fieldTypeOptions,
      systemCategoryOptions,
      loading: false,
      saving: false,
      importing: false,
      importingJson: false,
      filters: createFilters(),
      records: [],
      selectedId: '',
      editingRecord: null,
      originalRecord: null,
      regionOptions: [],
      orgOptions: [],
      regionMap: {},
      orgMap: {},
      tcmMetadataText: '',
      newBodyPart: ''
    }
  },
  computed: {
    filteredOrgOptions() {
      if (!this.filters.idRegion) {
        return this.orgOptions
      }
      return this.orgOptions.filter(item => item.idRegion === this.filters.idRegion)
    },
    draftOrgOptions() {
      if (!this.editingRecord || !this.editingRecord.idRegion) {
        return this.orgOptions
      }
      return this.orgOptions.filter(item => item.idRegion === this.editingRecord.idRegion)
    }
  },
  async mounted() {
    await this.loadReferences()
    this.loadData()
  },
  methods: {
    medicalModeLabel(value) {
      const item = medicalModeOptions.find(option => option.value === value)
      return item ? item.label : value || '--'
    },
    statusLabel(value) {
      const item = statusOptions.find(option => option.value === value)
      return item ? item.label : value || '--'
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
        const data = await http.get('/admin/api/symptom-templates', {
          params: {
            current: 1,
            size: 500,
            keyword: this.filters.keyword || undefined,
            medicalMode: this.filters.medicalMode || undefined,
            systemCategory: this.filters.systemCategory || undefined,
            sdStatus: this.filters.sdStatus || undefined,
            idRegion: this.filters.idRegion || undefined,
            idOrg: this.filters.idOrg || undefined
          }
        })
        this.records = data.records || []
        if (this.selectedId) {
          const selected = this.records.find(item => item.id === this.selectedId)
          if (selected) {
            this.selectRecord(selected)
            return
          }
        }
        if (this.records.length) {
          this.selectRecord(this.records[0])
        } else {
          this.selectedId = ''
          this.editingRecord = null
          this.originalRecord = null
          this.tcmMetadataText = ''
        }
      } catch (error) {
        this.$message.error((error && error.message) || '加载症状模板失败')
      } finally {
        this.loading = false
      }
    },
    handleMedicalModeChange() {
      this.selectedId = ''
      this.editingRecord = null
      this.originalRecord = null
      this.loadData()
    },
    handleFilterRegionChange(idRegion) {
      if (!idRegion) {
        return
      }
      const org = this.orgOptions.find(item => item.idOrg === this.filters.idOrg)
      if (org && org.idRegion !== idRegion) {
        this.filters.idOrg = ''
      }
    },
    handleFilterOrgChange(idOrg) {
      const org = this.orgOptions.find(item => item.idOrg === idOrg)
      if (org) {
        this.filters.idRegion = org.idRegion || ''
      }
    },
    handleDraftRegionChange(idRegion) {
      if (!this.editingRecord || !idRegion) {
        return
      }
      const org = this.orgOptions.find(item => item.idOrg === this.editingRecord.idOrg)
      if (org && org.idRegion !== idRegion) {
        this.editingRecord.idOrg = ''
      }
    },
    handleDraftOrgChange(idOrg) {
      if (!this.editingRecord) {
        return
      }
      const org = this.orgOptions.find(item => item.idOrg === idOrg)
      if (org) {
        this.editingRecord.idRegion = org.idRegion || ''
      }
    },
    handleDraftMedicalModeChange() {
      if (this.editingRecord && this.editingRecord.medicalMode !== 'tcm') {
        this.editingRecord.tcmMetadata = null
        this.tcmMetadataText = ''
      }
    },
    resetFilters() {
      this.filters = createFilters()
      this.selectedId = ''
      this.editingRecord = null
      this.originalRecord = null
      this.tcmMetadataText = ''
      this.loadData()
    },
    selectRecord(record) {
      this.selectedId = record.id
      this.originalRecord = cloneRecord(record)
      this.editingRecord = cloneRecord(record)
      this.normalizeRecord(this.editingRecord)
      this.tcmMetadataText = this.editingRecord.tcmMetadata ? JSON.stringify(this.editingRecord.tcmMetadata, null, 2) : ''
      this.newBodyPart = ''
    },
    openCreate() {
      const draft = createDefaultRecord(this.filters.medicalMode, this.filters)
      this.normalizeRecord(draft)
      this.selectedId = ''
      this.originalRecord = null
      this.editingRecord = draft
      this.tcmMetadataText = ''
      this.newBodyPart = ''
    },
    normalizeRecord(record) {
      if (!record.systemCategory) record.systemCategory = []
      if (!record.bodyParts) record.bodyParts = []
      if (!record.applicablePopulation) record.applicablePopulation = { genders: [], ageGroups: [] }
      if (!Array.isArray(record.applicablePopulation.genders)) record.applicablePopulation.genders = []
      if (!record.config) record.config = { title: '智能问诊', sections: [] }
      if (!Array.isArray(record.config.sections)) record.config.sections = []
      record.config.sections.forEach((section, index) => {
        if (!section.id) section.id = `section_${Date.now()}_${index}`
        if (!Array.isArray(section.fields)) section.fields = []
        section.fields.forEach(field => this.normalizeField(field))
      })
      if (!record.config.sections.length) {
        record.config.sections.push(createDefaultSection(0))
      }
    },
    normalizeField(field) {
      if (!field.id) field.id = `field_${Date.now()}`
      if (!field.key) field.key = field.id
      if (!field.storageKey) field.storageKey = field.key || field.id
      if (!field.label) field.label = '新字段'
      if (!field.type) field.type = 'radio'
      if (!field.props || typeof field.props !== 'object') field.props = {}
      if (!field.applicablePopulation || typeof field.applicablePopulation !== 'object') {
        field.applicablePopulation = { genders: [], ageRange: { unit: 'Y' } }
      }
      if (!Array.isArray(field.applicablePopulation.genders)) {
        field.applicablePopulation.genders = []
      }
      if (!field.applicablePopulation.ageRange || typeof field.applicablePopulation.ageRange !== 'object') {
        field.applicablePopulation.ageRange = { unit: 'Y' }
      }
      if (!field.textGenConfig || typeof field.textGenConfig !== 'object') {
        field.textGenConfig = { targets: [], template: '{value}', separator: '、', optionConfig: { ignoreValues: [] } }
      }
      if (!Array.isArray(field.textGenConfig.targets)) {
        field.textGenConfig.targets = []
      }
      if (!field.textGenConfig.optionConfig || typeof field.textGenConfig.optionConfig !== 'object') {
        field.textGenConfig.optionConfig = { ignoreValues: [] }
      }
      if (!Array.isArray(field.textGenConfig.optionConfig.ignoreValues)) {
        field.textGenConfig.optionConfig.ignoreValues = []
      }
      this.handleFieldTypeChange(field)
    },
    addSection() {
      this.editingRecord.config.sections.push(createDefaultSection(this.editingRecord.config.sections.length))
    },
    removeSection(index) {
      if (this.editingRecord.config.sections.length === 1) {
        this.$message.warning('至少保留一个分组')
        return
      }
      this.editingRecord.config.sections.splice(index, 1)
    },
    addField(section) {
      section.fields.push(createDefaultField())
    },
    removeField(section, index) {
      section.fields.splice(index, 1)
    },
    handleFieldTypeChange(field) {
      if (!field.props || typeof field.props !== 'object') {
        field.props = {}
      }
      if (field.type === 'radio' || field.type === 'checkbox') {
        if (!Array.isArray(field.props.options)) {
          this.$set(field.props, 'options', ['选项1', '选项2'])
        }
      } else if (field.type === 'input_radio') {
        if (!Array.isArray(field.props.radioOptions)) {
          this.$set(field.props, 'radioOptions', ['小时', '天'])
        }
      } else if (field.type === 'degree_slider') {
        if (typeof field.props.min !== 'number') {
          this.$set(field.props, 'min', 1)
        }
        if (typeof field.props.max !== 'number') {
          this.$set(field.props, 'max', 10)
        }
        if (!Array.isArray(field.props.labels)) {
          this.$set(field.props, 'labels', ['轻微', '中等', '剧烈'])
        }
      } else if (field.type === 'preference_pair') {
        if (!Array.isArray(field.props.pairs)) {
          this.$set(field.props, 'pairs', [['选项A', '选项B']])
        }
      }
      if (field.type === 'checkbox' && !Array.isArray(field.props.mutualExclusions)) {
        this.$set(field.props, 'mutualExclusions', [])
      }
    },
    usesOptionList(field) {
      return field.type === 'radio' || field.type === 'checkbox' || field.type === 'input_radio'
    },
    getOptionList(field) {
      this.handleFieldTypeChange(field)
      if (field.type === 'input_radio') {
        return field.props.radioOptions
      }
      return field.props.options
    },
    addOption(field) {
      const value = (field.__newOption || '').trim()
      if (!value) {
        return
      }
      const list = this.getOptionList(field)
      if (!list.includes(value)) {
        list.push(value)
      }
      field.__newOption = ''
    },
    removeOption(field, index) {
      this.getOptionList(field).splice(index, 1)
    },
    ensureArray(target, key) {
      if (!Array.isArray(target[key])) {
        this.$set(target, key, [])
      }
      return target[key]
    },
    addSliderLabel(field) {
      const value = (field.__newLabel || '').trim()
      if (!value) {
        return
      }
      this.ensureArray(field.props, 'labels').push(value)
      field.__newLabel = ''
    },
    ensurePairs(field) {
      if (!Array.isArray(field.props.pairs)) {
        this.$set(field.props, 'pairs', [])
      }
      return field.props.pairs
    },
    addPreferencePair(field) {
      this.ensurePairs(field).push(['', ''])
    },
    ensureMutualGroups(field) {
      if (!Array.isArray(field.props.mutualExclusions)) {
        this.$set(field.props, 'mutualExclusions', [])
      }
      return field.props.mutualExclusions
    },
    addMutualGroup(field) {
      this.ensureMutualGroups(field).push([])
    },
    addToMutualGroup(field, groupIndex, value) {
      if (!value) {
        return
      }
      const groups = this.ensureMutualGroups(field)
      const group = groups[groupIndex]
      if (group && !group.includes(value)) {
        group.push(value)
      }
    },
    getMutualAvailableOptions(field, group) {
      return this.getOptionList(field).filter(item => !group.includes(item))
    },
    fieldGenders(field) {
      if (!field.applicablePopulation) {
        this.$set(field, 'applicablePopulation', { genders: [], ageRange: { unit: 'Y' } })
      }
      if (!Array.isArray(field.applicablePopulation.genders)) {
        this.$set(field.applicablePopulation, 'genders', [])
      }
      return field.applicablePopulation.genders
    },
    fieldAgeRange(field) {
      if (!field.applicablePopulation) {
        this.$set(field, 'applicablePopulation', { genders: [], ageRange: { unit: 'Y' } })
      }
      if (!field.applicablePopulation.ageRange || typeof field.applicablePopulation.ageRange !== 'object') {
        this.$set(field.applicablePopulation, 'ageRange', { unit: 'Y' })
      }
      if (!field.applicablePopulation.ageRange.unit) {
        this.$set(field.applicablePopulation.ageRange, 'unit', 'Y')
      }
      return field.applicablePopulation.ageRange
    },
    ensureTextGenConfig(field) {
      if (!field.textGenConfig || typeof field.textGenConfig !== 'object') {
        this.$set(field, 'textGenConfig', { targets: [], template: '{value}', separator: '、', optionConfig: { ignoreValues: [] } })
      }
      if (!field.textGenConfig.optionConfig || typeof field.textGenConfig.optionConfig !== 'object') {
        this.$set(field.textGenConfig, 'optionConfig', { ignoreValues: [] })
      }
      if (!Array.isArray(field.textGenConfig.optionConfig.ignoreValues)) {
        this.$set(field.textGenConfig.optionConfig, 'ignoreValues', [])
      }
      if (!Array.isArray(field.textGenConfig.targets)) {
        this.$set(field.textGenConfig, 'targets', [])
      }
      return field.textGenConfig
    },
    fieldTargets(field) {
      return this.ensureTextGenConfig(field).targets
    },
    ignoreValues(field) {
      return this.ensureTextGenConfig(field).optionConfig.ignoreValues
    },
    addIgnoreValue(field, value) {
      if (!value) {
        return
      }
      const list = this.ignoreValues(field)
      if (!list.includes(value)) {
        list.push(value)
      }
    },
    toggleListValue(list, value, checked) {
      const index = list.indexOf(value)
      if (checked && index === -1) {
        list.push(value)
      }
      if (!checked && index !== -1) {
        list.splice(index, 1)
      }
    },
    addBodyPart() {
      const value = this.newBodyPart.trim()
      if (!value || this.editingRecord.bodyParts.includes(value)) {
        this.newBodyPart = ''
        return
      }
      this.editingRecord.bodyParts.push(value)
      this.newBodyPart = ''
    },
    resetCurrent() {
      if (this.originalRecord) {
        this.selectRecord(this.originalRecord)
        return
      }
      this.openCreate()
    },
    validateCurrent() {
      if (!this.editingRecord) {
        return false
      }
      if (!this.editingRecord.name) {
        this.$message.error('症状名称不能为空')
        return false
      }
      if (!this.editingRecord.key) {
        this.$message.error('症状 Key 不能为空')
        return false
      }
      if (!this.editingRecord.config || !Array.isArray(this.editingRecord.config.sections) || !this.editingRecord.config.sections.length) {
        this.$message.error('至少需要一个问诊分组')
        return false
      }
      for (const section of this.editingRecord.config.sections) {
        if (!section.title) {
          this.$message.error('分组标题不能为空')
          return false
        }
        if (!Array.isArray(section.fields)) {
          this.$message.error('分组字段结构不合法')
          return false
        }
        for (const field of section.fields) {
          if (!field.label || !field.storageKey || !field.type) {
            this.$message.error('字段标签、storageKey、type 不能为空')
            return false
          }
        }
      }
      if (this.editingRecord.medicalMode === 'tcm' && this.tcmMetadataText) {
        try {
          JSON.parse(this.tcmMetadataText)
        } catch (error) {
          this.$message.error('TCM 扩展元数据不是合法 JSON')
          return false
        }
      }
      return true
    },
    buildPayload() {
      const payload = cloneRecord(this.editingRecord)
      if (payload.medicalMode !== 'tcm') {
        payload.tcmMetadata = null
      } else if (this.tcmMetadataText) {
        payload.tcmMetadata = JSON.parse(this.tcmMetadataText)
      } else {
        payload.tcmMetadata = null
      }
      payload.idOrg = payload.idOrg || null
      payload.idRegion = payload.idRegion || null
      return payload
    },
    async saveCurrent() {
      if (!this.validateCurrent()) {
        return
      }
      this.saving = true
      try {
        const payload = this.buildPayload()
        const data = payload.id
          ? await http.put(`/admin/api/symptom-templates/${payload.id}`, payload)
          : await http.post('/admin/api/symptom-templates', payload)
        this.$message.success('保存成功')
        this.selectedId = data.id
        await this.loadData()
      } catch (error) {
        this.$message.error((error && error.message) || '保存失败')
      } finally {
        this.saving = false
      }
    },
    async removeCurrent() {
      if (!this.editingRecord) {
        return
      }
      if (!this.editingRecord.id) {
        this.editingRecord = null
        this.originalRecord = null
        this.selectedId = ''
        return
      }
      this.$confirm(`确认删除症状模板「${this.editingRecord.name || this.editingRecord.key}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.delete(`/admin/api/symptom-templates/${this.editingRecord.id}`)
          this.$message.success('删除成功')
          this.selectedId = ''
          this.editingRecord = null
          this.originalRecord = null
          this.tcmMetadataText = ''
          this.loadData()
        } catch (error) {
          this.$message.error((error && error.message) || '删除失败')
        }
      }).catch(() => {})
    },
    async importBuiltin() {
      this.$confirm(`确认将${this.medicalModeLabel(this.filters.medicalMode)}内置模板导入当前作用域吗？同 Key 的现有记录会被覆盖更新。`, '提示', {
        type: 'warning'
      }).then(async () => {
        this.importing = true
        try {
          const data = await http.post('/admin/api/symptom-templates/import-builtin', {
            medicalMode: this.filters.medicalMode,
            idRegion: this.filters.idRegion || null,
            idOrg: this.filters.idOrg || null,
            overwriteExisting: true
          })
          this.$message.success(`导入完成：新增 ${data.createdCount || 0} 条，更新 ${data.updatedCount || 0} 条`)
          this.loadData()
        } catch (error) {
          this.$message.error((error && error.message) || '导入内置模板失败')
        } finally {
          this.importing = false
        }
      }).catch(() => {})
    },
    triggerJsonImport() {
      if (!this.$refs.importJsonInput) {
        return
      }
      this.$refs.importJsonInput.value = ''
      this.$refs.importJsonInput.click()
    },
    handleJsonFileChange(event) {
      const input = event && event.target
      const file = input && input.files && input.files[0]
      if (!file) {
        return
      }
      const reader = new FileReader()
      reader.onerror = () => {
        this.$message.error('读取模板文件失败')
      }
      reader.onload = () => {
        const contentJson = typeof reader.result === 'string' ? reader.result : ''
        if (!contentJson.trim()) {
          this.$message.error('模板文件内容不能为空')
          return
        }
        try {
          JSON.parse(contentJson)
        } catch (error) {
          this.$message.error('模板文件不是合法 JSON')
          return
        }
        this.$confirm(`确认将文件「${file.name}」按${this.medicalModeLabel(this.filters.medicalMode)}导入当前作用域吗？同 Key 的现有记录会被覆盖更新。`, '提示', {
          type: 'warning'
        }).then(async () => {
          this.importingJson = true
          try {
            const data = await http.post('/admin/api/symptom-templates/import-json', {
              medicalMode: this.filters.medicalMode,
              idRegion: this.filters.idRegion || null,
              idOrg: this.filters.idOrg || null,
              overwriteExisting: true,
              contentJson
            })
            this.$message.success(`导入完成：新增 ${data.createdCount || 0} 条，更新 ${data.updatedCount || 0} 条`)
            this.loadData()
          } catch (error) {
            this.$message.error((error && error.message) || '导入模板文件失败')
          } finally {
            this.importingJson = false
          }
        }).catch(() => {})
      }
      reader.readAsText(file, 'utf-8')
    },
    exportCurrent() {
      const content = JSON.stringify(this.records, null, 2)
      const blob = new Blob([content], { type: 'application/json;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `symptom-templates-${this.filters.medicalMode}.json`
      link.click()
      URL.revokeObjectURL(url)
    },
    getSymptomGenders() {
      if (!this.editingRecord) {
        return []
      }
      if (!this.editingRecord.applicablePopulation) {
        this.$set(this.editingRecord, 'applicablePopulation', { genders: [], ageGroups: [] })
      }
      if (!Array.isArray(this.editingRecord.applicablePopulation.genders)) {
        this.$set(this.editingRecord.applicablePopulation, 'genders', [])
      }
      return this.editingRecord.applicablePopulation.genders
    }
  }
}
</script>

<style scoped>
.symptom-template-page {
  display: grid;
  gap: 16px;
}

.symptom-toolbar {
  align-items: flex-start;
  gap: 16px;
}

.hidden-file-input {
  display: none;
}

.symptom-filters {
  flex-wrap: wrap;
}

.symptom-toolbar__actions {
  display: flex;
  gap: 10px;
}

.symptom-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  min-height: 760px;
}

.symptom-sidebar,
.symptom-editor {
  min-height: 0;
}

.symptom-sidebar {
  display: flex;
  flex-direction: column;
}

.symptom-sidebar__header {
  padding-bottom: 12px;
  border-bottom: 1px solid #eef2f7;
}

.symptom-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
}

.symptom-item {
  width: 100%;
  border: 1px solid #e4ebf3;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.symptom-item:hover,
.symptom-item.is-active {
  border-color: #3770ab;
  box-shadow: 0 10px 22px rgba(55, 112, 171, 0.12);
  transform: translateY(-1px);
}

.symptom-item__title {
  font-size: 14px;
  font-weight: 600;
  color: #16324f;
}

.symptom-item__meta {
  margin-top: 4px;
  font-size: 12px;
  color: #7d8ca0;
}

.symptom-item__tags {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.inline-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef4fb;
  color: #3770ab;
  font-size: 12px;
}

.empty-state,
.empty-editor {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #7d8ca0;
}

.editor-shell {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef2f7;
}

.editor-actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-shrink: 0;
}

.editor-scroll {
  margin-top: 16px;
  display: grid;
  gap: 16px;
}

.editor-section {
  border: 1px solid #e9eef5;
  border-radius: 16px;
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(245, 248, 251, 0.7), rgba(255, 255, 255, 0)),
    #fff;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #16324f;
}

.section-desc,
.muted-text {
  margin-top: 6px;
  color: #7d8ca0;
  line-height: 1.7;
}

.section-header,
.section-card__header,
.field-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.section-header__actions,
.section-card__actions,
.field-card__actions {
  display: flex;
  gap: 8px;
}

.form-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item.full {
  grid-column: 1 / -1;
}

.form-item label {
  font-size: 13px;
  font-weight: 600;
  color: #4f6278;
}

.row-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.check-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f7f9fc;
  border: 1px solid #edf2f8;
}

.check-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #41556c;
  font-size: 13px;
}

.token-editor {
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #dbe5ef;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  background: #fff;
}

.token-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 999px;
  background: #eef4fb;
  color: #2f5e90;
  font-size: 12px;
}

.token-chip__remove,
.icon-button {
  border: none;
  background: transparent;
  cursor: pointer;
}

.icon-button.danger,
.token-chip__remove {
  color: #d14343;
}

.token-input,
.mutual-select {
  border: none;
  outline: none;
  font-size: 13px;
  color: #33475c;
  min-width: 180px;
  background: transparent;
}

.section-card,
.field-card {
  margin-top: 14px;
  border: 1px solid #e8eef5;
  border-radius: 14px;
  padding: 14px;
  background: #fff;
}

.section-card__title,
.field-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #20466d;
}

.pair-list,
.mutual-group-list {
  display: grid;
  gap: 10px;
}

.pair-row,
.mutual-group {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pair-row .el-input {
  flex: 1;
}

.pair-sep {
  color: #7d8ca0;
  font-size: 12px;
}

.mutual-group__items {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #dbe5ef;
}

@media (max-width: 1280px) {
  .symptom-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .editor-header,
  .symptom-toolbar {
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
