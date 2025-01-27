import type { Config } from '@jest/types';

// Jest configuration for Delayed Messaging System backend testing
const config: Config.InitialOptions = {
  // Use ts-jest preset for TypeScript support
  preset: 'ts-jest',

  // Set Node.js as the test environment
  testEnvironment: 'node',

  // Define root directories for test discovery
  roots: [
    '<rootDir>/src',
    '<rootDir>/tests'
  ],

  // Test file patterns to match
  testMatch: [
    '**/__tests__/**/*.+(ts|tsx)',
    '**/?(*.)+(spec|test).+(ts|tsx)'
  ],

  // TypeScript file transformation configuration
  transform: {
    '^.+\\.(ts|tsx)$': 'ts-jest'
  },

  // Path alias mapping for imports
  moduleNameMapper: {
    '@/(.*)': '<rootDir>/src/$1'
  },

  // Global test setup file
  setupFilesAfterEnv: [
    '<rootDir>/tests/setup.ts'
  ],

  // Code coverage configuration
  collectCoverage: true,
  coverageDirectory: 'coverage',
  coveragePathIgnorePatterns: [
    '/node_modules/',
    '/dist/',
    '/tests/setup.ts'
  ],
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80
    }
  },

  // Test execution configuration
  testTimeout: 10000,
  verbose: true,
  clearMocks: true,
  restoreMocks: true,

  // Paths to ignore during testing
  testPathIgnorePatterns: [
    '/node_modules/',
    '/dist/'
  ],

  // Supported file extensions
  moduleFileExtensions: [
    'ts',
    'tsx',
    'js',
    'jsx',
    'json',
    'node'
  ],

  // TypeScript configuration
  globals: {
    'ts-jest': {
      tsconfig: 'tsconfig.json'
    }
  }
};

export default config;