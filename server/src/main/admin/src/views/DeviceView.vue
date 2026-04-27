<template>
  <div>
    <div class="filter-bar">
      <div class="page-toolbar__filters">
        <el-input
          v-model="keyword"
          clearable
          placeholder="输入设备编码或名称"
          class="search-input"
          @keyup.enter.native="loadData"
        />
        <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增设备</el-button>
    </div>

    <div class="page-card">
      <el-table :data="records" v-loading="loading">
      <el-table-column label="设备编码" min-width="140"><template slot-scope="{ row }"><code class="code-tag">{{ row.cdDevice || '--' }}</code></template></el-table-column>
      <el-table-column prop="naDevice" label="设备名称" min-width="160" />
      <el-table-column prop="naOrg" label="所属机构" min-width="160" />
      <el-table-column prop="naRegion" label="所属区域" min-width="140" />
      <el-table-column label="状态" width="100">
        <template slot-scope="{ row }">
          <span :class="['status-pill', statusPillClass(row.sdStatus)]"><i class="dot"></i>{{ statusMeta(row.sdStatus).label }}</span>
        </template>
      </el-table-column>
      <el-table-column label="设备令牌" min-width="160"><template slot-scope="{ row }"><code class="code-tag">{{ row.deviceTokenMasked || '--' }}</code></template></el-table-column>
      <el-table-column label="客户端版本" width="120"><template slot-scope="{ row }"><code class="code-tag">{{ row.clientVersion || '--' }}</code></template></el-table-column>
      <el-table-column label="最后心跳" min-width="160">
        <template slot-scope="{ row }">
          {{ formatDateTime(row.dtLastHeartbeat) }}
        </template>
      </el-table-column>
      <el-table-column label="注册时间" min-width="160">
        <template slot-scope="{ row }">
          {{ formatDateTime(row.dtRegistered) }}
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
          <el-form-item label="设备编码" prop="cdDevice">
            <el-input v-model.trim="form.cdDevice" maxlength="128" />
          </el-form-item>
          <el-form-item label="设备名称" prop="naDevice">
            <el-input v-model.trim="form.naDevice" maxlength="128" />
          </el-form-item>
          <el-form-item label="所属机构" prop="idOrg">
            <el-select v-model="form.idOrg" filterable placeholder="请选择机构" @change="syncRegionByOrg">
              <el-option v-for="item in orgOptions" :key="item.idOrg" :label="item.naOrg" :value="item.idOrg" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属区域">
            <el-input :value="currentRegionName" disabled />
          </el-form-item>
          <el-form-item label="状态" prop="sdStatus">
            <div class="segmented"><button type="button" :class="{ active: form.sdStatus === '0' }" @click="form.sdStatus = '0'">未激活</button><button type="button" :class="{ active: form.sdStatus === '1' }" @click="form.sdStatus = '1'">活跃</button></div>
          </el-form-item>
          <el-form-item label="客户端版本">
            <el-input v-model.trim="form.clientVersion" maxlength="64" />
          </el-form-item>
          <el-form-item label="操作系统" class="form-span-2">
            <el-input v-model.trim="form.osInfo" maxlength="500" />
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
import { buildLabelMap, deviceStatusOptions, findStatusMeta, formatDateTime } from '../utils/admin'

function createDefaultForm() {
  return {
    idDevice: '',
    cdDevice: '',
    naDevice: '',
    idOrg: '',
    idRegion: '',
    clientVersion: '',
    osInfo: '',
    sdStatus: '0'
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
      regionMap: {},
      orgOptions: [],
      form: createDefaultForm(),
      rules: {
        cdDevice: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
        idOrg: [{ required: true, message: '请选择所属机构', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增设备' : '编辑设备'
    },
    currentRegionName() {
      const org = this.orgOptions.find(item => item.idOrg === this.form.idOrg)
      return org ? this.regionMap[org.idRegion] || org.idRegion || '--' : '--'
    }
  },
  async mounted() {
    await this.loadReferences()
    this.loadData()
  },
  methods: {
    formatDateTime,
    statusMeta(value) {
      return findStatusMeta(deviceStatusOptions, value)
    },
    statusPillClass(value) {
      const type = this.statusMeta(value).type
      if (type === 'success') return 'status-pill--success'
      if (type === 'warning') return 'status-pill--warning'
      if (type === 'danger') return 'status-pill--danger'
      return 'status-pill--muted'
    },
    async loadReferences() {
      const [regions, orgs] = await Promise.all([fetchRegions(), fetchOrgs()])
      this.regionMap = buildLabelMap(regions, 'idRegion', 'naRegion')
      this.orgOptions = orgs
    },
    async loadData() {
      this.loading = true
      try {
        const data = await http.get('/admin/api/devices', {
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
        idDevice: row.idDevice,
        cdDevice: row.cdDevice,
        naDevice: row.naDevice,
        idOrg: row.idOrg,
        idRegion: row.idRegion,
        clientVersion: row.clientVersion || '',
        osInfo: row.osInfo || '',
        sdStatus: row.sdStatus || '0'
      }
      this.dialogVisible = true
    },
    syncRegionByOrg(idOrg) {
      const org = this.orgOptions.find(item => item.idOrg === idOrg)
      this.form.idRegion = org ? org.idRegion : ''
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
            cdDevice: this.form.cdDevice,
            naDevice: this.form.naDevice,
            idOrg: this.form.idOrg,
            clientVersion: this.form.clientVersion,
            osInfo: this.form.osInfo,
            sdStatus: this.form.sdStatus
          }
          if (this.dialogMode === 'create') {
            await http.post('/admin/api/devices', payload)
          } else {
            await http.put(`/admin/api/devices/${this.form.idDevice}`, payload)
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
      this.$confirm(`确认停用设备「${row.naDevice || row.cdDevice}」吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await http.delete(`/admin/api/devices/${row.idDevice}`)
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
