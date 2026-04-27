<template>
  <div>
    <div class="filter-bar">
      <div class="page-toolbar__filters">
        <el-input
          v-model="keyword"
          clearable
          placeholder="输入场景编码或 提示词名称"
          class="search-input"
          @keyup.enter.native="loadData"
        />
        <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增提示词</el-button>
    </div>

    <div class="page-card">
      <el-table :data="records" v-loading="loading">
      <el-table-column prop="cdPrompt" label="场景编码" min-width="160" />
      <el-table-column prop="naPrompt" label="提示词名称" min-width="160" />
      <el-table-column prop="versionNum" label="版本号" width="130" />
      <el-table-column prop="sdPromptType" label="类型" width="130" />
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
      <el-table-column label="系统提示词" min-width="220">
        <template slot-scope="{ row }">
          {{ truncate(row.sysPrompt) }}
        </template>
      </el-table-column>
      <el-table-column label="用户模板" min-width="220">
        <template slot-scope="{ row }">
          {{ truncate(row.userTemplate) }}
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
          <el-form-item label="场景编码" prop="cdPrompt">
            <el-input v-model.trim="form.cdPrompt" maxlength="128" />
          </el-form-item>
          <el-form-item label="提示词名称" prop="naPrompt">
            <el-input v-model.trim="form.naPrompt" maxlength="128" />
          </el-form-item>
          <el-form-item label="版本号" prop="versionNum">
            <el-input v-model.trim="form.versionNum" maxlength="64" />
          </el-form-item>
          <el-form-item label="提示词类型">
            <el-input v-model.trim="form.sdPromptType" maxlength="64" />
          </el-form-item>
          <el-form-item label="状态" prop="sdStatus">
            <div class="segmented"><button type="button" :class="{ active: form.sdStatus === '0' }" @click="form.sdStatus = '0'">草稿</button><button type="button" :class="{ active: form.sdStatus === '1' }" @click="form.sdStatus = '1'">已发布</button><button type="button" :class="{ active: form.sdStatus === '2' }" @click="form.sdStatus = '2'">已归档</button></div>
          </el-form-item>
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
          <el-form-item label="系统提示词" prop="sysPrompt" class="form-span-2">
            <el-input v-model="form.sysPrompt" type="textarea" :rows="6" />
          </el-form-item>
          <el-form-item label="用户模板" class="form-span-2">
            <el-input v-model="form.userTemplate" type="textarea" :rows="6" />
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
  promptStatusOptions,
  resolveScopeLabel,
  truncate
} from '../utils/admin'

function createDefaultForm() {
  return {
    idPrompt: '',
    cdPrompt: '',
    naPrompt: '',
    sysPrompt: '',
    userTemplate: '',
    versionNum: '',
    sdPromptType: '',
    sdStatus: '0',
    idOrg: '',
    idRegion: ''
  }
}

export default {
  data() {
    return {
      loading: false,
      saving: false,
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
      form: createDefaultForm(),
      rules: {
        cdPrompt: [{ required: true, message: '请输入场景编码', trigger: 'blur' }],
        naPrompt: [{ required: true, message: '请输入 提示词名称', trigger: 'blur' }],
        versionNum: [{ required: true, message: '请输入版本号', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增提示词' : '编辑 Prompt'
    }
  },
  async mounted() {
    await this.loadReferences()
    this.loadData()
  },
  methods: {
    truncate,
    statusMeta(value) {
      return findStatusMeta(promptStatusOptions, value)
    },
    statusPillClass(value) {
      const type = this.statusMeta(value).type
      if (type === 'success') return 'status-pill--success'
      if (type === 'warning') return 'status-pill--warning'
      if (type === 'danger') return 'status-pill--danger'
      return 'status-pill--muted'
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
        const data = await http.get('/admin/api/prompts', {
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
      this.dialogVisible = true
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.form = {
        idPrompt: row.idPrompt,
        cdPrompt: row.cdPrompt || '',
        naPrompt: row.naPrompt || '',
        sysPrompt: row.sysPrompt || '',
        userTemplate: row.userTemplate || '',
        versionNum: row.versionNum || '',
        sdPromptType: row.sdPromptType || '',
        sdStatus: row.sdStatus || '0',
        idOrg: row.idOrg || '',
        idRegion: row.idRegion || ''
      }
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
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
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
            cdPrompt: this.form.cdPrompt,
            naPrompt: this.form.naPrompt,
            sysPrompt: this.form.sysPrompt,
            userTemplate: this.form.userTemplate,
            versionNum: this.form.versionNum,
            sdPromptType: this.form.sdPromptType,
            sdStatus: this.form.sdStatus,
            idOrg: this.form.idOrg || null,
            idRegion: this.form.idRegion || null
          }
          if (this.dialogMode === 'create') {
            await http.post('/admin/api/prompts', payload)
          } else {
            await http.put(`/admin/api/prompts/${this.form.idPrompt}`, payload)
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
    publishRecord(row) {
      this.$confirm(`确认发布 提示词「${row.naPrompt}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.post(`/admin/api/prompts/${row.idPrompt}/publish`)
          this.$message.success('发布成功')
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '发布失败')
        }
      }).catch(() => {})
    },
    archiveRecord(row) {
      this.$confirm(`确认归档 提示词「${row.naPrompt}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.post(`/admin/api/prompts/${row.idPrompt}/archive`)
          this.$message.success('归档成功')
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '归档失败')
        }
      }).catch(() => {})
    },
    removeRecord(row) {
      this.$confirm(`确认删除 提示词「${row.naPrompt}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.delete(`/admin/api/prompts/${row.idPrompt}`)
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
