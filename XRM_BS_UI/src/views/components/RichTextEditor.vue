<template>
  <div style="border: 1px solid #ccc">
    <Toolbar
      style="border-bottom: 1px solid #ccc"
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      mode="default"
    />
    <Editor
      style="height: 500px; overflow-y: hidden"
      v-model="valueHtml"
      :defaultConfig="editorConfig"
      mode="default"
      @onCreated="handleCreated"
    />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IToolbarConfig } from '@wangeditor/editor'

// 定义上传响应类型
interface UploadResponse {
  errno: number
  data?: {
    url?: string
  }
  url?: string
}

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const editorRef = shallowRef<IDomEditor | null>(null)
const valueHtml = ref(props.modelValue)

watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal !== valueHtml.value) {
      valueHtml.value = newVal
    }
  },
)

watch(valueHtml, (newVal) => {
  emit('update:modelValue', newVal)
})

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['todo', 'uploadVideo'],
}

const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: 'http://localhost:8080/api/upload/image',
      fieldName: 'file',
      maxFileSize: 5 * 1024 * 1024,
      maxNumberOfFiles: 10,
      customInsert(
        res: UploadResponse,
        insertFn: (url: string, alt: string, href: string) => void,
      ) {
        const url = res.data?.url || res.url
        if (url) {
          insertFn('http://localhost:8080' + url, '', '')
        }
      },
      // 4. 上传文件前可做校验
      onBeforeUpload(file: File) {
        console.log('准备上传', file.name)
        return file
      },
      onError(file: File, err: Error, res: UploadResponse) {
        console.error('图片上传失败', err, res)
        alert('图片上传失败')
      },
    },
  },
}

const handleCreated = (editor: IDomEditor) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})
</script>

<style scoped></style>
