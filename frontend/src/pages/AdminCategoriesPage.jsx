import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  Typography,
  message,
} from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { createAdminCategory, deleteAdminCategory, getCategories, updateAdminCategory } from '../api'

const { Title } = Typography

function errorMessageOf(err, fallback) {
  const data = err?.response?.data
  if (data?.message) return data.message
  return fallback
}

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form] = Form.useForm()
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)

  const fetchCategories = () => {
    setLoading(true)
    getCategories()
      .then((response) => {
        setCategories(response.data.data)
        setError(null)
      })
      .catch(() => setError('Không tải được danh mục'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchCategories()
  }, [])

  const openCreate = () => {
    setEditing(null)
    setSaveError(null)
    form.resetFields()
    setModalOpen(true)
  }

  const openEdit = (record) => {
    setEditing(record)
    setSaveError(null)
    form.setFieldsValue({ name: record.name, description: record.description })
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    form.resetFields()
    setEditing(null)
    setSaveError(null)
  }

  const onFinish = async (values) => {
    setSaving(true)
    setSaveError(null)
    try {
      if (editing) {
        await updateAdminCategory(editing.id, values)
        message.success('Đã cập nhật danh mục')
      } else {
        await createAdminCategory(values)
        message.success('Đã tạo danh mục')
      }
      closeModal()
      fetchCategories()
    } catch (err) {
      setSaveError(errorMessageOf(err, 'Không lưu được danh mục'))
    } finally {
      setSaving(false)
    }
  }

  const onDelete = async (record) => {
    try {
      await deleteAdminCategory(record.id)
      message.success('Đã xóa danh mục')
      fetchCategories()
    } catch (err) {
      // Hiển thị đúng CATEGORY_HAS_PRODUCTS (FE-PRD-06)
      message.error(errorMessageOf(err, 'Không xóa được danh mục'))
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: 'Tên', dataIndex: 'name', key: 'name' },
    { title: 'Mô tả', dataIndex: 'description', key: 'description' },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
            Sửa
          </Button>
          <Popconfirm title="Xóa danh mục này?" onConfirm={() => onDelete(record)}>
            <Button size="small" danger icon={<DeleteOutlined />}>
              Xóa
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ justifyContent: 'space-between', width: '100%', marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>
          Quản lý danh mục
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          Thêm danh mục
        </Button>
      </Space>
      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}
      <Table rowKey="id" columns={columns} dataSource={categories} loading={loading} pagination={false} />

      <Modal
        title={editing ? 'Sửa danh mục' : 'Thêm danh mục'}
        open={modalOpen}
        onCancel={closeModal}
        onOk={() => form.submit()}
        confirmLoading={saving}
        destroyOnClose
      >
        {saveError && (
          <Alert type="error" message={saveError} showIcon style={{ marginBottom: 16 }} />
        )}
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="name"
            label="Tên danh mục"
            rules={[
              { required: true, message: 'Nhập tên danh mục' },
              { min: 2, max: 100, message: 'Tên 2-100 ký tự' },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="description"
            label="Mô tả"
            rules={[{ required: true, message: 'Nhập mô tả' }]}
          >
            <Input.TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
