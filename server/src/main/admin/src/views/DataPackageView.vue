<template>
  <div>
    <div class="filter-bar data-package-toolbar">
      <div class="page-toolbar__filters data-package-filters">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="输入数据包编码或名称"
          class="search-input"
          @keyup.enter.native="search"
        />
        <el-select v-model="filters.sdPackageType" clearable placeholder="类型" class="filter-select">
          <el-option v-for="item in packageTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.sdStatus" clearable placeholder="状态" class="filter-select">
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
        <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增数据包</el-button>
    </div>

    <div class="page-card">
      <el-table :data="records" v-loading="loading">
      <el-table-column label="数据包编码" min-width="160"><template slot-scope="{ row }"><code class="code-tag">{{ row.cdPackage || '--' }}</code></template></el-table-column>
      <el-table-column prop="naPackage" label="数据包名称" min-width="160" />
      <el-table-column label="类型" width="110">
        <template slot-scope="{ row }">
          {{ typeLabel(row.sdPackageType) }}
        </template>
      </el-table-column>
      <el-table-column label="版本号" width="120"><template slot-scope="{ row }"><code class="code-tag">{{ row.versionNum || '--' }}</code></template></el-table-column>
      <el-table-column label="作用域" min-width="140">
        <template slot-scope="{ row }">
          {{ resolveScope(row) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template slot-scope="{ row }">
          <span :class="['status-pill', statusPillClass(row.sdStatus)]"><i class="dot"></i>{{ statusMeta(row.sdStatus).label }}</span>
        </template>
      </el-table-column>
      <el-table-column label="内容预览" min-width="260">
        <template slot-scope="{ row }">
          <span class="content-preview">{{ truncate(row.contentJson, 96) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template slot-scope="{ row }">
          {{ formatDateTime(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template slot-scope="{ row }">
          <div class="table-actions">
            <a class="table-action" @click="openEdit(row)">编辑</a>
            <a class="table-action" :class="{ 'is-disabled': row.sdStatus === '1' }" @click="row.sdStatus !== '1' && publishRecord(row)">发布</a>
            <a class="table-action" :class="{ 'is-disabled': row.sdStatus === '2' }" @click="row.sdStatus !== '2' && archiveRecord(row)">归档</a>
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

    <el-dialog v-if="dialogVisible" :title="dialogTitle" :visible.sync="dialogVisible" width="920px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="数据包编码">
            <el-input v-model.trim="form.cdPackage" maxlength="128" />
          </el-form-item>
          <el-form-item label="数据包名称" prop="naPackage">
            <el-input v-model.trim="form.naPackage" maxlength="128" />
          </el-form-item>
          <el-form-item label="数据包类型" prop="sdPackageType">
            <el-select v-model="form.sdPackageType" placeholder="请选择类型" @change="handlePackageTypeChange">
              <el-option v-for="item in packageTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="版本号" prop="versionNum">
            <el-input v-model.trim="form.versionNum" maxlength="64" />
          </el-form-item>
          <el-form-item label="状态" prop="sdStatus">
            <div class="segmented"><button type="button" :class="{ active: form.sdStatus === '0' }" @click="form.sdStatus = '0'">草稿</button><button type="button" :class="{ active: form.sdStatus === '1' }" @click="form.sdStatus = '1'">已发布</button><button type="button" :class="{ active: form.sdStatus === '2' }" @click="form.sdStatus = '2'">已归档</button></div>
          </el-form-item>
          <el-form-item label="所属区域">
            <el-select
              v-model="form.idRegion"
              clearable
              filterable
              placeholder="全局"
              @change="handleFormRegionChange"
            >
              <el-option v-for="item in regionOptions" :key="item.idRegion" :label="item.naRegion" :value="item.idRegion" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属机构">
            <el-select
              v-model="form.idOrg"
              clearable
              filterable
              placeholder="区域/全局"
              @change="handleFormOrgChange"
            >
              <el-option v-for="item in formOrgOptions" :key="item.idOrg" :label="item.naOrg" :value="item.idOrg" />
            </el-select>
          </el-form-item>
          <el-form-item label="内容配置" prop="contentJson" class="form-span-2">
            <div v-if="form.sdPackageType === 'template'" class="content-actions">
              <el-button
                size="mini"
                :loading="loadingBuiltinTemplate"
                @click="loadBuiltinTemplate"
              >
                载入内置模板
              </el-button>
                          </div>
            <el-input
              v-model="form.contentJson"
              type="textarea"
              :rows="12"
              :placeholder="contentJsonPlaceholder"
            />
                      </el-form-item>
        </div>
      </el-form>
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
  findStatusMeta,
  formatDateTime,
  resolveScopeLabel,
  truncate
} from '../utils/admin'

const packageTypeOptions = [
  { value: 'template', label: '模板包' },
  { value: 'mapping', label: '映射包' }
]

const statusOptions = [
  { value: '0', label: '草稿', type: 'info' },
  { value: '1', label: '已发布', type: 'success' },
  { value: '2', label: '已归档', type: 'warning' }
]

const mappingContentKeys = ['diagnoses', 'medicines', 'items', 'tcmDiagnoses', 'tcmSyndromes', 'tcmTreatments']

function createDefaultFilters() {
  return {
    keyword: '',
    sdPackageType: '',
    sdStatus: '',
    idRegion: '',
    idOrg: ''
  }
}

function createDefaultForm() {
  return {
    idPackage: '',
    cdPackage: '',
    naPackage: '',
    sdPackageType: 'template',
    versionNum: '',
    contentJson: defaultContentJson('template'),
    sdStatus: '0',
    idOrg: '',
    idRegion: ''
  }
}

function defaultContentJson(packageType) {
  if (packageType === 'mapping') {
    return '{\n  "diagnoses": "id,code,name\\n1,A00,霍乱"\n}'
  }
  return '{\n  "western": [],\n  "tcm": []\n}'
}

export default {
  data() {
    return {
      packageTypeOptions,
      statusOptions,
      loading: false,
      saving: false,
      loadingBuiltinTemplate: false,
      dialogVisible: false,
      dialogMode: 'create',
      current: 1,
      size: 10,
      total: 0,
      records: [],
      filters: createDefaultFilters(),
      regionOptions: [],
      orgOptions: [],
      regionMap: {},
      orgMap: {},
      form: createDefaultForm(),
      rules: {
        naPackage: [{ required: true, message: '请输入数据包名称', trigger: 'blur' }],
        sdPackageType: [{ required: true, message: '请选择数据包类型', trigger: 'change' }],
        versionNum: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
        contentJson: [{ required: true, message: '请输入内容配置', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增数据包' : '编辑数据包'
    },
    filteredOrgOptions() {
      if (!this.filters.idRegion) {
        return this.orgOptions
      }
      return this.orgOptions.filter(item => item.idRegion === this.filters.idRegion)
    },
    formOrgOptions() {
      if (!this.form.idRegion) {
        return this.orgOptions
      }
      return this.orgOptions.filter(item => item.idRegion === this.form.idRegion)
    },
    contentJsonPlaceholder() {
      return defaultContentJson(this.form.sdPackageType)
    },
    contentHint() {
      return ''
    }
  },
  async mounted() {
    await this.loadReferences()
    this.loadData()
  },
  methods: {
    truncate,
    formatDateTime,
    statusMeta(value) {
      return findStatusMeta(statusOptions, value)
    },
    typeLabel(value) {
      const item = packageTypeOptions.find(option => option.value === value)
      return item ? item.label : value || '--'
    },
    resolveScope(row) {
      return resolveScopeLabel(row, this.orgMap, this.regionMap)
    },
    async loadReferences() {
      try {
        const [regions, orgs] = await Promise.all([fetchRegions(), fetchOrgs()])
        this.regionOptions = regions
        this.orgOptions = orgs
        this.regionMap = buildLabelMap(regions, 'idRegion', 'naRegion')
        this.orgMap = buildLabelMap(orgs, 'idOrg', 'naOrg')
      } catch (error) {
        this.$message.error(error.message || '加载基础数据失败')
      }
    },
    async loadData() {
      this.loading = true
      try {
        const data = await http.get('/admin/api/data-packages', {
          params: {
            current: this.current,
            size: this.size,
            keyword: this.filters.keyword || undefined,
            sdPackageType: this.filters.sdPackageType || undefined,
            sdStatus: this.filters.sdStatus || undefined,
            idRegion: this.filters.idRegion || undefined,
            idOrg: this.filters.idOrg || undefined
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
    search() {
      this.current = 1
      this.loadData()
    },
    reset() {
      this.filters = createDefaultFilters()
      this.current = 1
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
    handleFormRegionChange(idRegion) {
      if (!idRegion) {
        return
      }
      const org = this.orgOptions.find(item => item.idOrg === this.form.idOrg)
      if (org && org.idRegion !== idRegion) {
        this.form.idOrg = ''
      }
    },
    handleFormOrgChange(idOrg) {
      const org = this.orgOptions.find(item => item.idOrg === idOrg)
      if (org) {
        this.form.idRegion = org.idRegion || ''
      }
    },
    handlePackageTypeChange(packageType) {
      if (!this.form.contentJson || this.form.contentJson === defaultContentJson('template') || this.form.contentJson === defaultContentJson('mapping')) {
        this.form.contentJson = defaultContentJson(packageType)
      }
    },
    openCreate() {
      this.dialogMode = 'create'
      this.form = createDefaultForm()
      this.dialogVisible = true
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.form = {
        idPackage: row.idPackage,
        cdPackage: row.cdPackage || '',
        naPackage: row.naPackage || '',
        sdPackageType: row.sdPackageType || 'template',
        versionNum: row.versionNum || '',
        contentJson: row.contentJson || defaultContentJson(row.sdPackageType || 'template'),
        sdStatus: row.sdStatus || '0',
        idOrg: row.idOrg || '',
        idRegion: row.idRegion || ''
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.form = createDefaultForm()
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
    },
    validateContentJson() {
      try {
        const parsed = JSON.parse(this.form.contentJson)
        if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
          throw new Error('内容配置必须是对象结构')
        }
        if (this.form.sdPackageType === 'template') {
          if (parsed.western === undefined && parsed.tcm === undefined) {
            throw new Error('内容至少需要包含西医或中医模板')
          }
          if (parsed.western !== undefined && parsed.western !== null && !Array.isArray(parsed.western)) {
            throw new Error('西医模板必须是数组')
          }
          if (parsed.tcm !== undefined && parsed.tcm !== null && !Array.isArray(parsed.tcm)) {
            throw new Error('中医模板必须是数组')
          }
          return true
        }
        const hasMappingField = mappingContentKeys.some(key => parsed[key] !== undefined && parsed[key] !== null && parsed[key] !== '')
        if (!hasMappingField) {
          throw new Error('映射数据至少需要填写一个字段')
        }
        const invalidKey = mappingContentKeys.find(key => {
          return parsed[key] !== undefined && parsed[key] !== null && typeof parsed[key] !== 'string'
        })
        if (invalidKey) {
          throw new Error(`${invalidKey} 字段必须是字符串`)
        }
        return true
      } catch (error) {
        this.$message.error(error.message || '内容配置校验失败')
        return false
      }
    },
    async loadBuiltinTemplate() {
      this.loadingBuiltinTemplate = true
      try {
        const data = await http.get('/admin/api/data-packages/template-default')
        this.form.contentJson = JSON.stringify({
          western: data.western || [],
          tcm: data.tcm || []
        }, null, 2)
        if (!this.form.versionNum) {
          this.form.versionNum = data.version || ''
        }
        if (!this.form.cdPackage) {
          this.form.cdPackage = 'template-builtin'
        }
        if (!this.form.naPackage) {
          this.form.naPackage = '内置症状模板包'
        }
        this.$message.success('已载入内置模板')
      } catch (error) {
        this.$message.error(error.message || '载入内置模板失败')
      } finally {
        this.loadingBuiltinTemplate = false
      }
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid || !this.validateContentJson()) {
          return
        }
        const submit = async () => {
          this.saving = true
          try {
            const payload = {
              cdPackage: this.form.cdPackage,
              naPackage: this.form.naPackage,
              sdPackageType: this.form.sdPackageType,
              versionNum: this.form.versionNum,
              contentJson: this.form.contentJson,
              sdStatus: this.form.sdStatus,
              idOrg: this.form.idOrg || null,
              idRegion: this.form.idRegion || null
            }
            if (this.dialogMode === 'create') {
              await http.post('/admin/api/data-packages', payload)
            } else {
              await http.put(`/admin/api/data-packages/${this.form.idPackage}`, payload)
            }
            this.$message.success('保存成功')
            this.dialogVisible = false
            this.loadData()
          } catch (error) {
            this.$message.error(error.message || '保存失败')
          } finally {
            this.saving = false
          }
        }

        if (this.form.sdStatus === '1') {
          this.$confirm('保存并发布后，同类型同作用域下已发布版本会自动归档，是否继续？', '提示', {
            type: 'warning'
          }).then(() => {
            submit()
          }).catch(() => {})
          return
        }

        submit()
      })
    },
    publishRecord(row) {
      this.$confirm(`确认发布数据包「${row.naPackage}」吗？同类型同作用域已发布版本会自动归档。`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.post(`/admin/api/data-packages/${row.idPackage}/publish`)
          this.$message.success('发布成功')
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '发布失败')
        }
      }).catch(() => {})
    },
    archiveRecord(row) {
      this.$confirm(`确认归档数据包「${row.naPackage}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.post(`/admin/api/data-packages/${row.idPackage}/archive`)
          this.$message.success('归档成功')
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '归档失败')
        }
      }).catch(() => {})
    },
    removeRecord(row) {
      this.$confirm(`确认删除数据包「${row.naPackage}」吗？删除后将执行逻辑删除。`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.delete(`/admin/api/data-packages/${row.idPackage}`)
          this.$message.success('删除成功')
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '删除失败')
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.data-package-toolbar {
  align-items: flex-start;
  flex-wrap: wrap;
}

.data-package-filters {
  flex-wrap: wrap;
}

.content-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-select {
  width: 140px;
}

.content-preview {
  white-space: pre-wrap;
  line-height: 1.5;
}

.content-hint {
  margin-top: 6px;
}
</style>
