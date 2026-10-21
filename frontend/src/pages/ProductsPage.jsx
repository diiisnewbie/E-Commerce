import { useCallback, useEffect, useRef, useState } from 'react'
import { Alert, Card, Input, Pagination, Select, Space, Table, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { getCategories, getPublicProducts } from '../api'

const { Title } = Typography

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Mới nhất' },
  { value: 'price,asc', label: 'Giá thấp → cao' },
  { value: 'price,desc', label: 'Giá cao → thấp' },
  { value: 'name,asc', label: 'Tên A → Z' },
  { value: 'name,desc', label: 'Tên Z → A' },
]

const formatPrice = (price) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)

export default function ProductsPage() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [sort, setSort] = useState('createdAt,desc')
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState(null)

  // AbortController hủy request cũ khi param đổi — phản hồi cũ về muộn không ghi đè (FE-PRD-05)
  const abortRef = useRef(null)
  // Debounce timer
  const debounceRef = useRef(null)

  const fetchProducts = useCallback(
    (signal) => {
      setLoading(true)
      setError(null)
      const params = { page, size, sort }
      if (search.trim()) params.search = search.trim()
      if (categoryId != null) params.categoryId = categoryId

      getPublicProducts(params, { signal })
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
    [page, size, sort, search, categoryId],
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
    // Debounce 300ms — gõ nhanh chỉ gọi API một lần (FE-PRD-02)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setPage(0) // Đổi search → về trang 0 (FE-PRD-04)
      setSearch(value)
    }, 300)
  }

  const onCategoryChange = (value) => {
    setPage(0) // Đổi filter → về trang 0 (FE-PRD-04)
    setCategoryId(value ?? null)
  }

  const onSortChange = (value) => {
    setPage(0)
    setSort(value)
  }

  const columns = [
    {
      title: 'SKU',
      dataIndex: 'sku',
      key: 'sku',
      width: 140,
    },
    {
      title: 'Tên sản phẩm',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => <Link to={`/products/${record.id}`}>{name}</Link>,
    },
    {
      title: 'Danh mục',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 140,
    },
    {
      title: 'Giá',
      dataIndex: 'price',
      key: 'price',
      width: 160,
      align: 'right',
      render: (price) => formatPrice(price),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>Sản phẩm</Title>
      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            placeholder="Tìm kiếm sản phẩm..."
            allowClear
            onChange={(e) => onSearchChange(e.target.value)}
            style={{ width: 260 }}
            aria-label="Tìm kiếm sản phẩm"
          />
          <Select
            allowClear
            placeholder="Danh mục"
            style={{ width: 180 }}
            options={categories.map((c) => ({ value: c.id, label: c.name }))}
            onChange={onCategoryChange}
            aria-label="Lọc danh mục"
          />
          <Select
            value={sort}
            style={{ width: 180 }}
            options={SORT_OPTIONS}
            onChange={onSortChange}
            aria-label="Sắp xếp"
          />
        </Space>
      </Card>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={products}
        loading={loading}
        pagination={false}
      />
      <Pagination
        current={page + 1}
        pageSize={size}
        total={total}
        showTotal={(t) => `${t} sản phẩm`}
        onChange={(p) => setPage(p - 1)}
        style={{ marginTop: 16, textAlign: 'right' }}
      />
    </div>
  )
}
