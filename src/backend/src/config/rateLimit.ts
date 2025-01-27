/**
 * Rate Limiting Configuration
 * Version: 1.0.0
 * 
 * Defines rate limiting rules and thresholds for different API endpoints
 * in the Delayed Messaging System. Implements a tiered strategy with
 * distinct configurations for general API access, authentication, and
 * message operations.
 */

import { MessageErrorCodes } from '../constants/errorCodes';

// Default rate limit configuration (100 requests per minute)
const DEFAULT_POINTS = 100;
const DEFAULT_DURATION = 60; // seconds
const DEFAULT_BLOCK_DURATION = 300; // 5 minutes

// Authentication endpoint rate limit (5 requests per minute)
const AUTH_POINTS = 5;
const AUTH_DURATION = 60; // seconds
const AUTH_BLOCK_DURATION = 900; // 15 minutes

// Message sending rate limit (30 requests per minute)
const MESSAGE_POINTS = 30;
const MESSAGE_DURATION = 60; // seconds
const MESSAGE_BLOCK_DURATION = 600; // 10 minutes

/**
 * Interface defining the structure of rate limit configurations
 */
interface RateLimitConfig {
    points: number;        // Number of requests allowed
    duration: number;      // Time window in seconds
    blockDuration: number; // Block duration in seconds
    errorCode?: string;    // Custom error code for rate limit exceeded
}

/**
 * Default rate limit configuration for general API endpoints
 */
export const defaultLimit: RateLimitConfig = {
    points: DEFAULT_POINTS,
    duration: DEFAULT_DURATION,
    blockDuration: DEFAULT_BLOCK_DURATION,
    errorCode: MessageErrorCodes.RATE_LIMIT_EXCEEDED
};

/**
 * Stricter rate limit configuration for authentication endpoints
 */
export const authLimit: RateLimitConfig = {
    points: AUTH_POINTS,
    duration: AUTH_DURATION,
    blockDuration: AUTH_BLOCK_DURATION,
    errorCode: MessageErrorCodes.RATE_LIMIT_EXCEEDED
};

/**
 * Rate limit configuration for message sending endpoints
 */
export const messageLimit: RateLimitConfig = {
    points: MESSAGE_POINTS,
    duration: MESSAGE_DURATION,
    blockDuration: MESSAGE_BLOCK_DURATION,
    errorCode: MessageErrorCodes.RATE_LIMIT_EXCEEDED
};

/**
 * Combined rate limit configurations for all endpoint types
 */
export const rateLimitConfig = {
    defaultLimit,
    authLimit,
    messageLimit
} as const;

/**
 * Type definition for endpoint-specific rate limit configurations
 */
export type RateLimitType = keyof typeof rateLimitConfig;

/**
 * Helper function to get rate limit configuration by type
 */
export function getRateLimitConfig(type: RateLimitType): RateLimitConfig {
    return rateLimitConfig[type];
}

/**
 * Redis key prefix for rate limiting
 */
export const RATE_LIMIT_PREFIX = 'ratelimit';

/**
 * Redis key generator for rate limiting
 */
export function getRateLimitKey(type: RateLimitType, identifier: string): string {
    return `${RATE_LIMIT_PREFIX}:${type}:${identifier}`;
}