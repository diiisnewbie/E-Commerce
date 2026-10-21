import { useEffect, useState } from 'react'
import { Alert, Button, Card, Descriptions, Spin, Typography } from 'antd'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { getPublicProduct } from '../api'

const { Title } = Typography

const formatPrice = (price) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)

export default function ProductDetailPage() {
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    getPublicProduct(id)
      .then((response) => {
        if (!cancelled) {
          setProduct(response.data.data)
          setError(null)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('Sản phẩm không tồn tại hoặc đã ngừng bán')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id])

  if (loading) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ padding: 24 }}>
        <Alert type="error" message={error} showIcon />
        <Button style={{ marginTop: 16 }} icon={<ArrowLeftOutlined />}>
          <Link to="/products">Quay lại danh sách</Link>
        </Button>
      </div>
    )
  }

  return (
    <div style={{ padding: 24 }}>
      <Button style={{ marginBottom: 16 }} icon={<ArrowLeftOutlined />}>
        <Link to="/products">Quay lại danh sách</Link>
      </Button>
      <Card>
        <Title level={3}>{product.name}</Title>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="SKU">{product.sku}</Descriptions.Item>
          <Descriptions.Item label="Danh mục">{product.categoryName}</Descriptions.Item>
          <Descriptions.Item label="Giá">{formatPrice(product.price)}</Descriptions.Item>
          {product.description && (
            <Descriptions.Item label="Mô tả">{product.description}</Descriptions.Item>
          )}
          <Descriptions.Item label="Ngày tạo">{product.createdAt}</Descriptions.Item>
        </Descriptions>
        {/* Không có nút giỏ hàng — Phase 3 */}
      </Card>
    </div>
  )
}
