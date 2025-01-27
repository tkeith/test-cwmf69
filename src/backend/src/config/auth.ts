/**
 * Authentication Configuration Module
 * Version: 1.0.0
 * 
 * This module provides centralized authentication configuration settings for the
 * Delayed Messaging System, including JWT settings, password policies, and session management.
 * 
 * @module config/auth
 */

import dotenv from 'dotenv'; // v16.0.3 - Environment variable management

// Initialize environment variables
dotenv.config();

/**
 * Default values and constants for authentication configuration
 */
const DEFAULT_JWT_EXPIRY = '24h';
const DEFAULT_REFRESH_TOKEN_EXPIRY = '7d';
const MIN_PASSWORD_LENGTH = 8;
const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[a-z]).{8,}$/;

/**
 * Interface defining authentication configuration parameters
 */
export interface AuthConfig {
  /** Secret key used for JWT signing */
  jwtSecret: string;
  /** JWT token expiry duration */
  jwtExpiry: string;
  /** Refresh token expiry duration */
  refreshTokenExpiry: string;
  /** Regular expression for password validation */
  passwordPattern: RegExp;
  /** Minimum required password length */
  minPasswordLength: number;
  /** Flag to enable/disable token rotation */
  tokenRotationEnabled: boolean;
  /** Maximum allowed login attempts before lockout */
  maxLoginAttempts: number;
  /** Account lockout duration in minutes */
  lockoutDuration: number;
}

/**
 * Validates token expiry format (e.g., "24h", "7d", "30m")
 * @param expiry - Token expiry duration string
 * @returns boolean indicating if format is valid
 */
const validateTokenExpiry = (expiry: string): boolean => {
  const pattern = /^\d+[hdm]$/;
  if (!pattern.test(expiry)) {
    return false;
  }

  const value = parseInt(expiry.slice(0, -1));
  const unit = expiry.slice(-1);

  // Validate reasonable ranges for different time units
  switch (unit) {
    case 'h':
      return value > 0 && value <= 72; // Max 72 hours
    case 'd':
      return value > 0 && value <= 30; // Max 30 days
    case 'm':
      return value > 0 && value <= 1440; // Max 1440 minutes (24 hours)
    default:
      return false;
  }
};

/**
 * Validates required authentication environment variables and configuration settings
 * @throws Error if required variables are missing or invalid
 */
const validateAuthConfig = (): void => {
  // Validate JWT secret
  if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 32) {
    throw new Error('JWT_SECRET must be defined and at least 32 characters long');
  }

  // Validate token expiry formats
  const jwtExpiry = process.env.JWT_EXPIRY || DEFAULT_JWT_EXPIRY;
  const refreshTokenExpiry = process.env.REFRESH_TOKEN_EXPIRY || DEFAULT_REFRESH_TOKEN_EXPIRY;

  if (!validateTokenExpiry(jwtExpiry)) {
    throw new Error('Invalid JWT_EXPIRY format. Expected format: {number}[h|d|m]');
  }

  if (!validateTokenExpiry(refreshTokenExpiry)) {
    throw new Error('Invalid REFRESH_TOKEN_EXPIRY format. Expected format: {number}[h|d|m]');
  }

  // Validate password pattern compilation
  try {
    new RegExp(PASSWORD_PATTERN);
  } catch (error) {
    throw new Error('Invalid PASSWORD_PATTERN regular expression');
  }

  // Validate numeric configurations
  if (MIN_PASSWORD_LENGTH < 8) {
    throw new Error('MIN_PASSWORD_LENGTH must be at least 8 characters');
  }
};

// Validate configuration on module load
validateAuthConfig();

/**
 * Exported authentication configuration object
 */
export const authConfig: AuthConfig = {
  jwtSecret: process.env.JWT_SECRET!,
  jwtExpiry: process.env.JWT_EXPIRY || DEFAULT_JWT_EXPIRY,
  refreshTokenExpiry: process.env.REFRESH_TOKEN_EXPIRY || DEFAULT_REFRESH_TOKEN_EXPIRY,
  passwordPattern: PASSWORD_PATTERN,
  minPasswordLength: MIN_PASSWORD_LENGTH,
  tokenRotationEnabled: true,
  maxLoginAttempts: 5,
  lockoutDuration: 15 // minutes
};

/**
 * Re-export AuthConfig interface for type safety in other modules
 */
export type { AuthConfig };