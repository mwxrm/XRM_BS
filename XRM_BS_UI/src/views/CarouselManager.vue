<template>
  <div class="carousel-manager">
    <el-main>
      <el-button type="primary" @click="addDialog = true">新增广告</el-button>
      <el-table :data="ads" style="margin-top: 20px" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="图片">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" style="width: 100px; height: 60px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="linkUrl" label="跳转链接" />
        <el-table-column prop="sortOrder" label="顺序" width="80" />
        <el-table-column prop="enabled" label="启用" width="80">
          <template #default="{ row }">{{ row.enabled ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="editAd(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteAd(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-main>

    <el-dialog v-model="addDialog" title="新增广告" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="图片URL"><el-input v-model="form.imageUrl" /></el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="form.linkUrl" /></el-form-item>
        <el-form-item label="顺序"><el-input-number v-model="form.sortOrder" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAd">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑广告" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="图片URL"><el-input v-model="editForm.imageUrl" /></el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="editForm.linkUrl" /></el-form-item>
        <el-form-item label="顺序"><el-input-number v-model="editForm.sortOrder" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="editForm.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="updateAd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Ad {
  id?: number
  imageUrl: string
  linkUrl: string
  sortOrder: number
  enabled: boolean
}

const ads = ref<Ad[]>([])
const addDialog = ref(false)
const editDialog = ref(false)
const form = reactive<Ad>({ imageUrl: '', linkUrl: '', sortOrder: 0, enabled: true })
const editForm = reactive<Ad>({ id: 0, imageUrl: '', linkUrl: '', sortOrder: 0, enabled: true })

const loadAds = async () => {
  const res = await axios.get('/admin/carousel')
  ads.value = res.data
}

const saveAd = async () => {
  await axios.post('/admin/carousel', form)
  ElMessage.success('新增成功')
  addDialog.value = false
  loadAds()
}

const editAd = (ad: Ad) => {
  editForm.id = ad.id!
  editForm.imageUrl = ad.imageUrl
  editForm.linkUrl = ad.linkUrl
  editForm.sortOrder = ad.sortOrder
  editForm.enabled = ad.enabled
  editDialog.value = true
}

const updateAd = async () => {
  await axios.put(`/admin/carousel/${editForm.id}`, {
    imageUrl: editForm.imageUrl,
    linkUrl: editForm.linkUrl,
    sortOrder: editForm.sortOrder,
    enabled: editForm.enabled,
  })
  ElMessage.success('更新成功')
  editDialog.value = false
  loadAds()
}

const deleteAd = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除？')
    await axios.delete(`/admin/carousel/${id}`)
    ElMessage.success('删除成功')
    loadAds()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(loadAds)
</script>
