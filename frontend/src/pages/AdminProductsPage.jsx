import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  createAdminProduct,
  getAdminProducts,
  getAdminProduct,
  getCategories,
  patchAdminProductStatus,
  updateAdminProduct,
} from '../api'

const { Title } = Typography

const formatPrice = (price) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)

/** Đọc message từ response lỗi backend — hiển thị theo errorCode đã biết. */
function errorMessageOf(err, fallback) {
  const data = err?.response?.data
  if (data?.message) return data.message
  return fallback
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [statusFilter, setStatusFilter] = useState(null)
  const [search, setSearch] = useState('')

  // Modal state
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null) // null = tạo mới
  const [form] = Form.useForm()
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)

  const abortRef = useRef(null)
  const debounceRef = useRef(null)

  const fetchProducts = useCallback(
    (signal) => {
      setLoading(true)
      setError(null)
      const params = { page, size }
      if (search.trim()) params.search = search.trim()
      if (statusFilter) params.status = statusFilter

      getAdminProducts(params, { signal })
        .then((response) => {
          if (signal.aborted) return
          const data = response.data.data
          setProducts(data.content)
          setTotal(data.totalElements)
        })
        .catch((err) => {
          if (signal.aborted || err.name === 'CanceledError') return
          setError('Không tải được danh sách sản phẩm')
        })
        .finally(() => {
          if (!signal.aborted) setLoading(false)
        })
    },
    [page, size, statusFilter, search],
  )

  useEffect(() => {
    const controller = new AbortController()
    abortRef.current?.abort()
    abortRef.current = controller
    fetchProducts(controller.signal)
    return () => controller.abort()
  }, [fetchProducts])

  useEffect(() => {
    getCategories()
      .then((response) => setCategories(response.data.data))
      .catch(() => {})
  }, [])

  const onSearchChange = (value) => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setPage(0)
      setSearch(value)
    }, 300)
  }

  const onStatusFilterChange = (value) => {
    setPage(0)
    setStatusFilter(value ?? null)
  }

  const openCreate = () => {
    setEditing(null)
    setSaveError(null)
    form.resetFields()
    setModalOpen(true)
  }

  const openEdit = (record) => {
    setSaveError(null)
    // Load từ GET-by-id — reload form không mất dữ liệu (FE-PRD-06)
    getAdminProduct(record.id)
      .then((response) => {
        const p = response.data.data
        setEditing(p)
        form.setFieldsValue({
          sku: p.sku,
          name: p.name,
          description: p.description,
          price: p.price,
          categoryId: p.categoryId,
          status: p.status,
          version: p.version,
        })
        setModalOpen(true)
      })
      .catch(() => message.error('Không tải được chi tiết sản phẩm'))
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
        // PUT kèm version — SKU disabled không gửi (UpdateProductRequest không có sku)
        await updateAdminProduct(editing.id, {
          name: values.name,
          description: values.description ?? null,
          price: values.price,
          categoryId: values.categoryId,
          status: values.status,
          version: values.version,
        })
        message.success('Đã cập nhật sản phẩm')
      } else {
        await createAdminProduct({
          sku: values.sku,
          name: values.name,
          description: values.description ?? null,
          price: values.price,
          categoryId: values.categoryId,
          initialStock: values.initialStock ?? 0,
        })
        message.success('Đã tạo sản phẩm')
      }
      closeModal()
      fetchProducts(abortRef.current?.signal ?? new AbortController().signal)
    } catch (err) {
      const code = err?.response?.data?.errorCode
      if (code === 'CONCURRENT_MODIFICATION') {
        setSaveError('Dữ liệu đã thay đổi, vui lòng tải lại')
      } else {
        setSaveError(errorMessageOf(err, 'Không lưu được sản phẩm'))
      }
    } finally {
      setSaving(false)
    }
  }

  const toggleStatus = async (record) => {
    const next = record.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      const response = await patchAdminProductStatus(record.id, next)
      const updated = response.data.data
      // Cập nhật thẳng dòng bảng từ response — không PUT cả form
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
      message.success(next === 'INACTIVE' ? 'Đã ngừng bán' : 'Đã kích hoạt')
    } catch (err) {
      message.error(errorMessageOf(err, 'Không đổi được trạng thái'))
    }
  }

  const columns = [
    { title: 'SKU', dataIndex: 'sku', key: 'sku', width: 140 },
    { title: 'Tên', dataIndex: 'name', key: 'name' },
    {
      title: 'Giá',
      dataIndex: 'price',
      key: 'price',
      width: 150,
      align: 'right',
      render: (price) => formatPrice(price),
    },
    {
      title: 'Danh mục',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 120,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (status) =>
        status === 'ACTIVE' ? (
          <Tag color="green">Đang bán</Tag>
        ) : (
          <Tag color="red">Ngừng bán</Tag>
        ),
    },
    {
      title: 'Tồn kho',
      dataIndex: 'stock',
      key: 'stock',
      width: 90,
      align: 'right',
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 220,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => openEdit(record)}>
            Sửa
          </Button>
          <Popconfirm
            title={record.status === 'ACTIVE' ? 'Ngừng bán sản phẩm này?' : 'Kích hoạt sản phẩm này?'}
            onConfirm={() => toggleStatus(record)}
          >
            <Button size="small" danger={record.status === 'ACTIVE'}>
              {record.status === 'ACTIVE' ? 'Ngừng bán' : 'Kích hoạt'}
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
          Quản lý sản phẩm
        </Title>
        <Button type="primary" onClick={openCreate}>
          Thêm sản phẩm
        </Button>
      </Space>
      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="Tìm theo tên hoặc SKU..."
          allowClear
          onChange={(e) => onSearchChange(e.target.value)}
          style={{ width: 260 }}
          aria-label="Tìm kiếm sản phẩm admin"
        />
        <Select
          allowClear
          placeholder="Trạng thái"
          style={{ width: 150 }}
          options={[
            { value: 'ACTIVE', label: 'Đang bán' },
            { value: 'INACTIVE', label: 'Ngừng bán' },
          ]}
          onChange={onStatusFilterChange}
          aria-label="Lọc trạng thái"
        />
      </Space>
      <Table rowKey="id" columns={columns} dataSource={products} loading={loading} pagination={false} />
      <Pagination
        current={page + 1}
        pageSize={size}
        total={total}
        onChange={(p) => setPage(p - 1)}
        style={{ marginTop: 16, textAlign: 'right' }}
      />

      <Modal
        title={editing ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}
        open={modalOpen}
        onCancel={closeModal}
        onOk={() => form.submit()}
        confirmLoading={saving}
        destroyOnClose
        width={640}
      >
        {saveError && (
          <Alert type="error" message={saveError} showIcon style={{ marginBottom: 16 }} />
        )}
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="sku"
            label="SKU"
            rules={[
              { required: !editing, message: 'Nhập SKU' },
              { pattern: /^[A-Za-z0-9_-]{3,50}$/, message: 'SKU 3-50 ký tự, chỉ chữ/số/_/-' },
            ]}
          >
            <Input disabled={!!editing} placeholder="PHONE-001" />
          </Form.Item>
          <Form.Item
            name="name"
            label="Tên sản phẩm"
            rules={[
              { required: true, message: 'Nhập tên sản phẩm' },
              { min: 2, max: 200, message: 'Tên 2-200 ký tự' },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item
            name="price"
            label="Giá"
            rules={[
              { required: true, message: 'Nhập giá' },
              { type: 'number', min: 0.01, message: 'Giá phải lớn hơn 0' },
            ]}
          >
            <InputNumber style={{ width: '100%' }} min={0.01} step={1000} />
          </Form.Item>
          <Form.Item
            name="categoryId"
            label="Danh mục"
            rules={[{ required: true, message: 'Chọn danh mục' }]}
          >
            <Select
              placeholder="Chọn danh mục"
              options={categories.map((c) => ({ value: c.id, label: c.name }))}
            />
          </Form.Item>
          {!editing && (
            <Form.Item
              name="initialStock"
              label="Tồn kho ban đầu"
              rules={[{ required: true, message: 'Nhập tồn kho' }]}
              initialValue={0}
            >
              <InputNumber style={{ width: '100%' }} min={0} />
            </Form.Item>
          )}
          {editing && (
            <>
              <Form.Item
                name="status"
                label="Trạng thái"
                rules={[{ required: true, message: 'Chọn trạng thái' }]}
              >
                <Select
                  options={[
                    { value: 'ACTIVE', label: 'Đang bán' },
                    { value: 'INACTIVE', label: 'Ngừng bán' },
                  ]}
                />
              </Form.Item>
              {/* version round-trip — ẩn, giữ trong form (D-2-09) */}
              <Form.Item name="version" hidden>
                <InputNumber />
              </Form.Item>
            </>
          )}
        </Form>
      </Modal>
    </div>
  )
}
