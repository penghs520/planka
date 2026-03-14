import eslint from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import prettierConfig from 'eslint-config-prettier'

export default [
  // 忽略文件
  {
    ignores: ['dist/**', 'node_modules/**', '*.d.ts'],
  },

  // JavaScript/TypeScript 基础配置
  eslint.configs.recommended,
  ...tseslint.configs.recommended,

  // Vue 配置
  ...pluginVue.configs['flat/recommended'],

  // 通用规则配置
  {
    files: ['**/*.{js,mjs,cjs,ts,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        // Browser globals
        window: 'readonly',
        document: 'readonly',
        console: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        Event: 'readonly',
        DragEvent: 'readonly',
        MouseEvent: 'readonly',
        KeyboardEvent: 'readonly',
        FocusEvent: 'readonly',
        Node: 'readonly',
        HTMLElement: 'readonly',
        HTMLInputElement: 'readonly',
        HTMLTextAreaElement: 'readonly',
        NodeJS: 'readonly',
        fetch: 'readonly',
        Headers: 'readonly',
        Request: 'readonly',
        Response: 'readonly',
        navigator: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        location: 'readonly',
        history: 'readonly',
        ResizeObserver: 'readonly',
        Blob: 'readonly',
        URL: 'readonly',
        HTMLDivElement: 'readonly',
        HTMLImageElement: 'readonly',
        File: 'readonly',
        FileList: 'readonly',
        DataTransfer: 'readonly',
        AbortController: 'readonly',
        AbortSignal: 'readonly',
        DOMException: 'readonly',
      },
      parserOptions: {
        parser: tseslint.parser,
      },
    },
    rules: {
      // TypeScript 规则
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
        },
      ],
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/prefer-nullish-coalescing': 'off',
      '@typescript-eslint/strict-boolean-expressions': 'off',

      // Vue 规则
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'warn',
      'vue/require-default-prop': 'off',
      'vue/require-prop-types': 'off',
      // 直接修改 props 设为警告（需要业务理解才能修复）
      'vue/no-mutating-props': 'warn',

      // 通用规则
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'error',
      // 重复导入设为警告（TypeScript 中类型导入和普通导入分开写很常见）
      'no-duplicate-imports': 'off',
      '@typescript-eslint/no-duplicate-imports': 'off',
      'no-case-declarations': 'off',
    },
  },

  // Vue 文件特殊配置
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: pluginVue.parser,
      parserOptions: {
        parser: tseslint.parser,
        extraFileExtensions: ['.vue'],
      },
    },
  },

  // Prettier 配置（必须放最后，覆盖其他配置的格式相关规则）
  prettierConfig,
]
