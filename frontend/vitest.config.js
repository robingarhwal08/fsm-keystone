import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.js'],
    // Ratchet policy: raise these floors as coverage grows — never lower them.
    // Initial values set equal to the measured baseline on first run.
    // Run `npm run test:coverage` to see the current report.
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      include: ['src/**/*.{js,jsx}'],
      exclude: [
        'src/main.jsx',
        'src/styles.css',
        'src/test/**'
      ],
      thresholds: {
        lines: 10,
        functions: 10,
        branches: 5,
        statements: 10
      }
    }
  }
});
