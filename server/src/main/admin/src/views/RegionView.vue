<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div class="page-toolbar__filters">
        <el-input
          v-model="keyword"
          clearable
          placeholder="输入区域名称"
          class="search-input"
          @keyup.enter.native="search"
        />
        <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增区域</el-button>
    </div>

    <el-table :data="records" border stripe v-loading="loading">
      <el-table-column prop="cdRegion" label="区域编码" min-width="140" />
      <el-table-column prop="naRegion" label="区域名称" min-width="160" />
      <el-table-column label="上级区域" min-width="160">
        <template slot-scope="{ row }">
          {{ resolveParentName(row.idParent) }}
        </template>
      </el-table-column>
      <el-table-column prop="sdRegionType" label="区域类型" min-width="120" />
      <el-table-column label="状态" width="90">
        <template slot-scope="{ row }">
          <el-tag size="mini" :type="statusMeta(row.sdStatus).type">{{ statusMeta(row.sdStatus).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="更新时间" width="170">
        <template slot-scope="{ row }">
          {{ formatDateTime(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="说明" min-width="220">
        <template slot-scope="{ row }">
          {{ row.desRegion || '--' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="760px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="区域编码">
            <el-input v-model.trim="form.cdRegion" maxlength="64" />
          </el-form-item>
          <el-form-item label="区域名称" prop="naRegion">
            <el-input v-model.trim="form.naRegion" maxlength="128" />
          </el-form-item>
          <el-form-item label="上级区域">
            <el-select v-model="form.idParent" clearable filterable placeholder="可选">
              <el-option
                v-for="item in parentRegionOptions"
                :key="item.idRegion"
                :label="item.naRegion"
                :value="item.idRegion"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="区域类型">
            <el-input v-model.trim="form.sdRegionType" maxlength="32" placeholder="如 district" />
          </el-form-item>
          <el-form-item label="状态" prop="sdStatus">
            <el-radio-group v-model="form.sdStatus">
              <el-radio-button label="1">启用</el-radio-button>
              <el-radio-button label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number
              v-model="form.sortOrder"
              :min="0"
              :max="9999"
              controls-position="right"
              style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="说明" class="form-span-2">
            <el-input v-model.trim="form.desRegion" type="textarea" :rows="4" maxlength="500" show-word-limit />
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
import { fetchRegions } from '../api/reference'
import http from '../api/http'
import { buildLabelMap, configStatusOptions, findStatusMeta, formatDateTime } from '../utils/admin'

function createDefaultForm() {
  return {
    idRegion: '',
    cdRegion: '',
    naRegion: '',
    idParent: '',
    sdRegionType: '',
    sdStatus: '1',
    sortOrder: 0,
    desRegion: ''
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
      regionMap: {},
      form: createDefaultForm(),
      rules: {
        naRegion: [{ required: true, message: '请输入区域名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增区域' : '编辑区域'
    },
    parentRegionOptions() {
      return this.regionOptions.filter(item => item.idRegion !== this.form.idRegion)
    }
  },
  async mounted() {
    await this.loadReferences()
    this.loadData()
  },
  methods: {
    formatDateTime,
    statusMeta(value) {
      return findStatusMeta(configStatusOptions, value)
    },
    resolveParentName(idParent) {
      return idParent ? this.regionMap[idParent] || idParent : '--'
    },
    async loadReferences() {
      try {
        const regions = await fetchRegions()
        this.regionOptions = regions
        this.regionMap = buildLabelMap(regions, 'idRegion', 'naRegion')
      } catch (error) {
        this.$message.error(error.message || '加载区域选项失败')
      }
    },
    async loadData() {
      this.loading = true
      try {
        const data = await http.get('/admin/api/regions', {
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
        idRegion: row.idRegion,
        cdRegion: row.cdRegion || '',
        naRegion: row.naRegion || '',
        idParent: row.idParent || '',
        sdRegionType: row.sdRegionType || '',
        sdStatus: row.sdStatus || '1',
        sortOrder: typeof row.sortOrder === 'number' ? row.sortOrder : Number(row.sortOrder) || 0,
        desRegion: row.desRegion || ''
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
            cdRegion: this.form.cdRegion || '',
            naRegion: this.form.naRegion,
            idParent: this.form.idParent || '',
            sdRegionType: this.form.sdRegionType || '',
            sdStatus: this.form.sdStatus,
            sortOrder: this.form.sortOrder || 0,
            desRegion: this.form.desRegion || ''
          }
          if (this.dialogMode === 'create') {
            await http.post('/admin/api/regions', payload)
          } else {
            await http.put(`/admin/api/regions/${this.form.idRegion}`, payload)
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          await this.loadReferences()
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '保存失败')
        } finally {
          this.saving = false
        }
      })
    },
    removeRecord(row) {
      this.$confirm(`确认停用区域「${row.naRegion || row.cdRegion}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.delete(`/admin/api/regions/${row.idRegion}`)
          this.$message.success('停用成功')
          await this.loadReferences()
          this.loadData()
        } catch (error) {
          this.$message.error(error.message || '停用失败')
        }
      }).catch(() => {})
    }
  }
}
</script>
