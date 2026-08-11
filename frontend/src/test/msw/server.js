import { setupServer } from 'msw/node';
import { handlers } from './handlers';

/**
 * MSW Node server used in Vitest tests.
 * Started in setup.js with onUnhandledRequest: 'error' so every un-mocked
 * request fails the suite loudly rather than silently resolving.
 */
export const server = setupServer(...handlers);
