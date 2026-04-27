<template>
  <div>
    <div class="filter-bar">
      <div class="page-toolbar__filters">
        <el-input
          v-model.trim="keyword"
          clearable
          placeholder="输入角色编码或名称"
          class="search-input"
          @keyup.enter.native="search"
        />
        <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增角色</el-button>
    </div>

    <div class="page-card">
      <el-table :data="records" v-loading="loading">
      <el-table-column label="角色编码" min-width="180"><template slot-scope="{ row }"><code class="code-tag">{{ row.cdRole || '--' }}</code></template></el-table-column>
      <el-table-column prop="naRole" label="角色名称" min-width="160" />
      <el-table-column label="状态" width="90">
        <template slot-scope="{ row }">
          <span :class="['status-pill', statusPillClass(row.sdStatus)]"><i class="dot"></i>{{ statusMeta(row.sdStatus).label }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色说明" min-width="280">
        <template slot-scope="{ row }">
          {{ row.desRole || '--' }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="170">
        <template slot-scope="{ row }">
          {{ formatDateTime(row.updateTime || row.dtUpdate) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template slot-scope="{ row }">
          <div class="table-actions">
            <a class="table-action" @click="openEdit(row)">编辑</a>
            <a class="table-action table-action--danger" @click="removeRecord(row)">停用</a>
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

    <el-dialog v-if="dialogVisible" :title="dialogTitle" :visible.sync="dialogVisible" width="720px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="角色编码" prop="cdRole">
            <el-input v-model.trim="form.cdRole" maxlength="64" />
          </el-form-item>
          <el-form-item label="角色名称" prop="naRole">
            <el-input v-model.trim="form.naRole" maxlength="64" />
          </el-form-item>
          <el-form-item label="状态" prop="sdStatus">
            <div class="segmented"><button type="button" :class="{ active: form.sdStatus === '1' }" @click="form.sdStatus = '1'">启用</button><button type="button" :class="{ active: form.sdStatus === '0' }" @click="form.sdStatus = '0'">停用</button></div>
          </el-form-item>
          <el-form-item label="角色说明" class="form-span-2">
            <el-input v-model.trim="form.desRole" type="textarea" :rows="4" maxlength="500" />
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
import { configStatusOptions, findStatusMeta, formatDateTime } from '../utils/admin'

function createDefaultForm() {
  return {
    idRole: '',
    cdRole: '',
    naRole: '',
    desRole: '',
    sdStatus: '1'
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
      form: createDefaultForm(),
      rules: {
        cdRole: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
        naRole: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增角色' : '编辑角色'
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    formatDateTime,
    statusMeta(value) {
      return findStatusMeta(configStatusOptions, value)
    },
    statusPillClass(value) {
      const type = this.statusMeta(value).type
      if (type === 'success') return 'status-pill--success'
      if (type === 'warning') return 'status-pill--warning'
      if (type === 'danger') return 'status-pill--danger'
      return 'status-pill--muted'
    },
    async loadData() {
      this.loading = true
      try {
        const data = await http.get('/admin/api/roles', {
          params: {
            current: this.current,
            size: this.size,
            keyword: this.keyword || undefined
          }
        })
        this.records = data.records || []
        this.total = data.total || 0
      } catch (error) {
        this.$message.error((error && error.message) || '加载失败')
      } finally {
        this.loading = false
      }
    },
    search() {
      this.current = 1
      this.loadData()
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
        idRole: row.idRole,
        cdRole: row.cdRole || '',
        naRole: row.naRole || '',
        desRole: row.desRole || '',
        sdStatus: row.sdStatus || '1'
      }
      this.dialogVisible = true
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
            cdRole: this.form.cdRole,
            naRole: this.form.naRole,
            desRole: this.form.desRole || '',
            sdStatus: this.form.sdStatus
          }

          if (this.dialogMode === 'create') {
            await http.post('/admin/api/roles', payload)
          } else {
            await http.put(`/admin/api/roles/${this.form.idRole}`, payload)
          }

          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadData()
        } catch (error) {
          this.$message.error((error && error.message) || '保存失败')
        } finally {
          this.saving = false
        }
      })
    },
    removeRecord(row) {
      this.$confirm(`确认停用角色「${row.naRole || row.cdRole}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.delete(`/admin/api/roles/${row.idRole}`)
          this.$message.success('停用成功')
          this.loadData()
        } catch (error) {
          this.$message.error((error && error.message) || '停用失败')
        }
      }).catch(() => {})
    }
  }
}
</script>
