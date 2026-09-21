<template>
  <div>
    <div class="toolbar">
      <div class="filters">
        <el-input v-model="city" placeholder="城市" size="small" style="width:120px" clearable />
        <el-input v-model="keyword" placeholder="名称关键词" size="small" style="width:160px" clearable />
        <el-select v-model="status" placeholder="状态" size="small" style="width:110px" clearable>
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button size="small" type="primary" @click="load">查询</el-button>
      </div>
      <div class="actions">
        <el-button size="small" @click="openImport">从高德收录</el-button>
        <el-button size="small" type="success" @click="openAdd">+ 新增</el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="width:100%">
      <el-table-column label="图片" width="80">
        <template #default="{ row }">
          <img v-if="row.image" class="thumb" :src="row.image" alt="" />
          <span v-else class="no-img">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="city" label="城市" width="90" />
      <el-table-column prop="address" label="地址" min-width="180" />
      <el-table-column label="参考价" width="100">
        <template #default="{ row }">
          <span v-if="row.price || row.price === 0">¥{{ row.price }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="80" />
      <el-table-column prop="tags" label="标签" width="150" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" @click="toggle(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !list.length" description="暂无数据" />

    <el-dialog v-model="dialog" :title="(form.id ? '编辑' : '新增') + title" width="520px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="参考价"><el-input v-model="form.price" type="number" /></el-form-item>
        <el-form-item label="评分"><el-input v-model="form.rating" placeholder="如 4.8" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" placeholder="近地铁,亲子" /></el-form-item>
        <el-form-item label="图片链接"><el-input v-model="form.image" placeholder="可选" /></el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序"><el-input v-model="form.sort" type="number" placeholder="越大越靠前" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 从高德收录 -->
    <el-dialog v-model="impDialog" :title="'从高德收录' + title" width="820px">
      <div class="imp-search">
        <el-input v-model="impCity" placeholder="城市，如 上海" size="small" style="width:150px" clearable />
        <el-input
          v-model="impKeyword"
          :placeholder="type === 'HOTEL' ? '关键词，如 酒店 / 外滩' : '关键词，如 景点 / 西湖'"
          size="small"
          style="width:230px"
          clearable
        />
        <el-button size="small" type="primary" :loading="impLoading" @click="doImportSearch">搜索高德</el-button>
        <span class="imp-tip">需已配置高德 Key（AMAP_KEY）</span>
      </div>

      <el-table :data="impList" v-loading="impLoading" height="340" @selection-change="onSel">
        <el-table-column type="selection" width="45" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <img v-if="row.image" class="thumb" :src="row.image" alt="" />
            <span v-else class="no-img">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column prop="city" label="城市" width="90" />
        <el-table-column prop="address" label="地址" min-width="200" />
        <el-table-column prop="rating" label="评分" width="80" />
        <el-table-column prop="type" label="分类" width="160" />
      </el-table>
      <el-empty v-if="!impLoading && !impList.length" description="没有结果，换个关键词试试" />

      <template #footer>
        <el-button @click="impDialog = false">取消</el-button>
        <el-button type="primary" :loading="impSaving" :disabled="!selected.length" @click="doImport">
          导入选中（{{ selected.length }}）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  type: { type: String, required: true },
  title: { type: String, default: '管理' },
})

const token = () => localStorage.getItem('adminToken') || ''
const city = ref('')
const keyword = ref('')
const status = ref(null)
const list = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const impDialog = ref(false)
const impCity = ref('')
const impKeyword = ref('')
const impList = ref([])
const impLoading = ref(false)
const impSaving = ref(false)
const selected = ref([])

const form = reactive({
  id: null, type: '', name: '', city: '', address: '', price: null,
  rating: '', tags: '', image: '', description: '', sort: 0, status: 1,
})

const load = async () => {
  loading.value = true
  try {
    const p = new URLSearchParams()
    p.append('type', props.type)
    if (city.value) p.append('city', city.value)
    if (keyword.value) p.append('keyword', keyword.value)
    if (status.value !== null && status.value !== '') p.append('status', status.value)
    const resp = await fetch('/api/admin/places?' + p.toString(), {
      headers: { Authorization: `Bearer ${token()}` },
    })
    if (resp.status === 401 || resp.status === 403) {
      ElMessage.error('登录过期或无权限')
      return
    }
    const res = await resp.json()
    if (res.code === 200) list.value = res.data || []
    else ElMessage.error(res.msg || '获取失败')
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  Object.assign(form, {
    id: null, type: props.type, name: '', city: '', address: '', price: null,
    rating: '', tags: '', image: '', description: '', sort: 0, status: 1,
  })
}

const openAdd = () => {
  reset()
  dialog.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  dialog.value = true
}

const save = async () => {
  if (!form.name || !form.city) {
    ElMessage.warning('请填写名称和城市')
    return
  }
  saving.value = true
  try {
    const isEdit = !!form.id
    const resp = await fetch(isEdit ? '/api/admin/places/' + form.id : '/api/admin/places', {
      method: isEdit ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token()}` },
      body: JSON.stringify(form),
    })
    const res = await resp.json()
    if (res.code === 200) {
      ElMessage.success(isEdit ? '修改成功' : '新增成功')
      dialog.value = false
      load()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const toggle = async (row) => {
  const resp = await fetch('/api/admin/places/' + row.id + '/toggle', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token()}` },
  })
  const res = await resp.json()
  if (res.code === 200) {
    ElMessage.success('已' + (row.status === 1 ? '下架' : '上架'))
    load()
  } else {
    ElMessage.error(res.msg || '操作失败')
  }
}

// ===== 从高德收录 =====
const openImport = () => {
  impCity.value = ''
  impKeyword.value = ''
  impList.value = []
  selected.value = []
  impDialog.value = true
}

const onSel = (rows) => {
  selected.value = rows
}

const doImportSearch = async () => {
  impLoading.value = true
  try {
    const base = props.type === 'HOTEL' ? '/api/travel/hotels?' : '/api/travel/spots?'
    const p = new URLSearchParams()
    if (impCity.value) p.append('city', impCity.value)
    if (impKeyword.value) p.append('keyword', impKeyword.value)
    if (props.type === 'HOTEL') p.append('machineId', 'admin')
    const resp = await fetch(base + p.toString(), {
      headers: { Authorization: `Bearer ${token()}` },
    })
    const res = await resp.json()
    impList.value = res.code === 200 ? res.data || [] : []
  } catch (e) {
    ElMessage.error('搜索失败')
  } finally {
    impLoading.value = false
  }
}

const doImport = async () => {
  impSaving.value = true
  let ok = 0
  try {
    for (const it of selected.value) {
      const body = {
        type: props.type,
        name: it.name,
        city: it.city || impCity.value,
        address: it.address,
        rating: it.rating || '',
        tags: it.type || '',
        description: '',
        image: it.image || '',
        price: null,
        status: 1,
        sort: 0,
      }
      const resp = await fetch('/api/admin/places', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token()}` },
        body: JSON.stringify(body),
      })
      const res = await resp.json()
      if (res.code === 200) ok++
    }
    ElMessage.success('已导入 ' + ok + ' 条')
    impDialog.value = false
    load()
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    impSaving.value = false
  }
}

const remove = (row) => {
  ElMessageBox.confirm(`确认删除「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      const resp = await fetch('/api/admin/places/' + row.id, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token()}` },
      })
      const res = await resp.json()
      if (res.code === 200) {
        ElMessage.success('已删除')
        load()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
    .catch(() => {})
}

watch(() => props.type, () => {
  city.value = ''
  keyword.value = ''
  status.value = null
  load()
})
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; flex-wrap: wrap; gap: 10px; }
.filters { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.actions { display: flex; align-items: center; gap: 8px; }
.imp-search { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.imp-tip { font-size: 12px; color: #999; }
.thumb { width: 56px; height: 40px; object-fit: cover; border-radius: 4px; display: block; }
.no-img { color: #bbb; }
</style>
