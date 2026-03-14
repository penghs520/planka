import { defineConfig, presetUno, presetAttributify } from 'unocss'
import transformerDirectives from '@unocss/transformer-directives'

export default defineConfig({
  presets: [
    presetUno(),
    presetAttributify(),
  ],
  transformers: [
    transformerDirectives(),
  ],
  theme: {
    colors: {
      // 飞书风格主色
      primary: {
        DEFAULT: '#3370FF',
        hover: '#4E83FD',
        active: '#245BDB',
        light: '#E8F3FF',
        lighter: '#F0F5FF',
      },
      // 成功色
      success: {
        DEFAULT: '#34C759',
        hover: '#4CD964',
        active: '#2DB84D',
        light: '#E8F8ED',
      },
      // 警告色
      warning: {
        DEFAULT: '#FF9500',
        hover: '#FFAA33',
        active: '#E68600',
        light: '#FFF7E6',
      },
      // 危险色
      danger: {
        DEFAULT: '#F54A45',
        hover: '#F76560',
        active: '#D93F3A',
        light: '#FEECE8',
      },
      // 中性色 - 文本
      text: {
        1: '#1F2329',      // 主文本
        2: '#646A73',      // 次要文本
        3: '#8F959E',      // 辅助文本
        4: '#BBBFC4',      // 禁用文本
      },
      // 中性色 - 填充/背景
      fill: {
        1: '#F5F6F7',      // 页面背景
        2: '#F0F1F2',      // 容器背景
        3: '#DEE0E3',      // 悬停背景
        4: '#C9CDD4',      // 禁用背景
      },
      // 中性色 - 边框
      border: {
        1: '#E5E6EB',      // 常规边框
        2: '#C9CDD4',      // 深边框
        3: '#F2F3F5',      // 浅边框
      },
      // 白色
      white: '#FFFFFF',
    },
    // 字体
    fontFamily: {
      sans: [
        '"PingFang SC"',
        '"HarmonyOS Sans SC"',
        '"Microsoft YaHei"',
        '-apple-system',
        'BlinkMacSystemFont',
        'sans-serif',
      ].join(','),
    },
    // 字号
    fontSize: {
      'xs': ['12px', '18px'],
      'sm': ['13px', '20px'],
      'base': ['14px', '22px'],
      'lg': ['16px', '24px'],
      'xl': ['18px', '28px'],
      '2xl': ['20px', '30px'],
      '3xl': ['24px', '36px'],
    },
    // 圆角 - 飞书风格偏大
    borderRadius: {
      'none': '0',
      'sm': '4px',
      'DEFAULT': '6px',
      'md': '6px',
      'lg': '8px',
      'xl': '12px',
      '2xl': '16px',
      'full': '9999px',
    },
    // 阴影 - 飞书风格柔和
    boxShadow: {
      'none': 'none',
      'sm': '0 1px 2px rgba(31, 35, 41, 0.05)',
      'DEFAULT': '0 2px 8px rgba(31, 35, 41, 0.08)',
      'md': '0 4px 14px rgba(31, 35, 41, 0.1)',
      'lg': '0 8px 24px rgba(31, 35, 41, 0.12)',
      'xl': '0 16px 48px rgba(31, 35, 41, 0.16)',
    },
    // 间距
    spacing: {
      '0': '0',
      '0.5': '2px',
      '1': '4px',
      '1.5': '6px',
      '2': '8px',
      '2.5': '10px',
      '3': '12px',
      '3.5': '14px',
      '4': '16px',
      '5': '20px',
      '6': '24px',
      '7': '28px',
      '8': '32px',
      '9': '36px',
      '10': '40px',
      '12': '48px',
      '14': '56px',
      '16': '64px',
    },
  },
  // 快捷方式 - 常用组合
  shortcuts: {
    // 按钮基础
    'btn': 'h-8 px-4 rounded-md text-sm font-medium transition-colors cursor-pointer inline-flex items-center justify-center',
    'btn-sm': 'h-7 px-3 rounded text-xs font-medium transition-colors cursor-pointer inline-flex items-center justify-center',
    'btn-lg': 'h-10 px-6 rounded-lg text-base font-medium transition-colors cursor-pointer inline-flex items-center justify-center',

    // 按钮变体
    'btn-primary': 'btn bg-primary text-white hover:bg-primary-hover active:bg-primary-active',
    'btn-secondary': 'btn bg-fill-2 text-text-1 hover:bg-fill-3 active:bg-fill-4',
    'btn-outline': 'btn bg-white text-text-1 border border-border-1 hover:border-primary hover:text-primary',
    'btn-danger': 'btn bg-danger text-white hover:bg-danger-hover active:bg-danger-active',
    'btn-text': 'btn bg-transparent text-text-2 hover:bg-fill-2 hover:text-text-1',

    // 卡片
    'card': 'bg-white rounded-lg shadow',
    'card-bordered': 'bg-white rounded-lg border border-border-1',
    'card-header': 'flex items-center justify-between px-4 py-3 border-b border-border-3',
    'card-body': 'p-4',
    'card-footer': 'flex items-center justify-end gap-3 px-4 py-3 border-t border-border-3',

    // 表单
    'form-label': 'text-sm text-text-1 font-medium mb-1.5',
    'form-hint': 'text-xs text-text-3 mt-1',
    'form-error': 'text-xs text-danger mt-1',

    // 布局
    'flex-center': 'flex items-center justify-center',
    'flex-between': 'flex items-center justify-between',
    'flex-start': 'flex items-center justify-start',
    'flex-end': 'flex items-center justify-end',
    'flex-col-center': 'flex flex-col items-center justify-center',

    // 文本截断
    'truncate-1': 'truncate',
    'truncate-2': 'line-clamp-2',
    'truncate-3': 'line-clamp-3',

    // 分隔线
    'divider': 'h-px bg-border-3 w-full',
    'divider-v': 'w-px bg-border-3 h-full',
  },
  // 安全列表 - 确保这些类始终生成
  safelist: [
    'text-primary',
    'text-success',
    'text-warning',
    'text-danger',
    'bg-primary',
    'bg-success',
    'bg-warning',
    'bg-danger',
  ],
})
