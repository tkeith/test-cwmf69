import { host, port } from './redis';
import crypto from 'crypto';

/**
 * Interface defining queue configuration parameters with validation constraints
 */
interface IQueueConfig {
  prefix: string;
  minDelay: number;
  maxDelay: number;
  checkInterval: number;
  retryAttempts: number;
  retryDelay: number;
  validateDelayBounds: boolean;
  maxQueueSize: number;
  processingTimeout: number;
}

// Queue configuration constants
export const QUEUE_PREFIX = 'delayed_msg';
export const MIN_DELAY_MS = 30000; // 30 seconds minimum delay
export const MAX_DELAY_MS = 60000; // 60 seconds maximum delay
export const CHECK_INTERVAL_MS = 1000; // 1 second check interval for <100ms processing latency
export const RETRY_ATTEMPTS = 3;
export const RETRY_DELAY_MS = 5000;
export const MAX_QUEUE_SIZE = 10000;
export const PROCESSING_TIMEOUT_MS = 5000;

/**
 * Generates a secure and unique Redis key for a message in the queue
 * @param messageId - Unique identifier for the message
 * @returns Formatted and validated Redis key
 * @throws Error if messageId is invalid or key generation fails
 */
export const getQueueKey = (messageId: string): string => {
  // Validate messageId format
  if (!messageId || typeof messageId !== 'string') {
    throw new Error('Invalid messageId provided');
  }

  // Sanitize input to prevent injection
  const sanitizedId = messageId.replace(/[^a-zA-Z0-9-_]/g, '');
  
  // Generate hash for collision prevention
  const hash = crypto
    .createHash('sha256')
    .update(`${QUEUE_PREFIX}:${sanitizedId}:${Date.now()}`)
    .digest('hex')
    .substring(0, 8);

  return `${QUEUE_PREFIX}:${sanitizedId}:${hash}`;
};

/**
 * Generates a cryptographically secure random delay between configured bounds
 * @returns Random delay in milliseconds between MIN_DELAY_MS and MAX_DELAY_MS
 */
export const generateRandomDelay = (): number => {
  // Generate cryptographically secure random number between 0 and 1
  const randomBuffer = crypto.randomBytes(4);
  const randomNumber = randomBuffer.readUInt32LE(0) / 0xFFFFFFFF;

  // Scale to range between MIN_DELAY_MS and MAX_DELAY_MS
  const delay = Math.floor(
    MIN_DELAY_MS + (randomNumber * (MAX_DELAY_MS - MIN_DELAY_MS))
  );

  return delay;
};

/**
 * Validates queue configuration parameters against system requirements
 * @param config - Queue configuration object to validate
 * @returns Boolean indicating if configuration is valid
 */
export const validateQueueConfig = (config: IQueueConfig): boolean => {
  // Validate delay bounds
  if (config.minDelay < MIN_DELAY_MS || config.maxDelay > MAX_DELAY_MS) {
    throw new Error('Delay bounds outside allowed range');
  }

  // Validate check interval for latency requirements
  if (config.checkInterval > CHECK_INTERVAL_MS) {
    throw new Error('Check interval too high for latency requirements');
  }

  // Validate retry configuration
  if (config.retryAttempts <= 0 || config.retryDelay <= 0) {
    throw new Error('Invalid retry configuration');
  }

  // Validate queue size limits
  if (config.maxQueueSize <= 0 || config.maxQueueSize > MAX_QUEUE_SIZE) {
    throw new Error('Invalid queue size configuration');
  }

  // Validate processing timeout
  if (config.processingTimeout <= 0 || config.processingTimeout > PROCESSING_TIMEOUT_MS) {
    throw new Error('Invalid processing timeout configuration');
  }

  return true;
};

/**
 * Production queue configuration with validated parameters
 * implementing mandatory message delays and monitoring
 */
export const queueConfig: IQueueConfig = {
  prefix: QUEUE_PREFIX,
  minDelay: MIN_DELAY_MS,
  maxDelay: MAX_DELAY_MS,
  checkInterval: CHECK_INTERVAL_MS,
  retryAttempts: RETRY_ATTEMPTS,
  retryDelay: RETRY_DELAY_MS,
  validateDelayBounds: true,
  maxQueueSize: MAX_QUEUE_SIZE,
  processingTimeout: PROCESSING_TIMEOUT_MS
};

// Validate configuration on module load
validateQueueConfig(queueConfig);