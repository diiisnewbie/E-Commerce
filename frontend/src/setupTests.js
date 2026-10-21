import '@testing-library/jest-dom/vitest'

// jsdom không có matchMedia — antd (Layout/responsive observer) cần nó
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

// jsdom không có ResizeObserver — antd Table/Select dùng nó
globalThis.ResizeObserver = class {
  observe() {}
  unobserve() {}
  disconnect() {}
}
