import js from '@eslint/js';
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import { FlatCompat } from '@eslint/eslintrc';
import parser from '@typescript-eslint/parser';
import tseslint from '@typescript-eslint/eslint-plugin';

const compat = new FlatCompat();

export default [
  // Migrate patterns previously stored in .eslintignore into the
  // flat config `ignores` property. The old .eslintignore file triggers
  // a runtime warning and is not supported by the new flat config loader.
  // See: https://eslint.org/docs/latest/use/configure/migration-guide#ignoring-files
  {
    ignores: [
      'node_modules/',
      'dist/',
      '.vite/',
      'coverage/',
      '*.min.js',
      '*.log',
      '*.tmp',
      'playwright-report/'
    ]
  },
  js.configs.recommended,
  ...compat.extends('plugin:react/recommended'),
  ...compat.extends('plugin:@typescript-eslint/recommended'),
  ...compat.extends('plugin:react-hooks/recommended'),
  {
    files: ['**/*.ts', '**/*.tsx'],
    languageOptions: {
      parser,
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: { jsx: true },
      },
    },
    plugins: {
      '@typescript-eslint': tseslint,
      react,
      'react-hooks': reactHooks,
    },
    rules: {
      'react/react-in-jsx-scope': 'off',
      'react/prop-types': 'off',
      '@typescript-eslint/no-unused-vars': 'warn',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
    },
    settings: {
      react: {
        version: 'detect',
      },
    },
  },
];
