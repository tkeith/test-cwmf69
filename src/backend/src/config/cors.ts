import dotenv from 'dotenv'; // ^16.0.3
import cors from 'cors'; // ^2.8.5

// Load environment variables
dotenv.config();

// Interface for CORS configuration
interface CorsConfig {
  origin: string[] | boolean;
  methods: string[];
  allowedHeaders: string[];
  exposedHeaders: string[];
  credentials: boolean;
  maxAge: number;
}

// Default CORS configuration constants
const DEFAULT_ALLOWED_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'];
const DEFAULT_ALLOWED_HEADERS = ['Content-Type', 'Authorization', 'X-Requested-With'];
const DEFAULT_EXPOSED_HEADERS = ['Content-Length', 'X-RateLimit-Limit', 'X-RateLimit-Remaining'];
const CORS_MAX_AGE = 86400; // 24 hours in seconds

/**
 * Validates the format of allowed origins and ensures proper configuration
 * @throws Error if configuration is invalid
 */
const validateCorsConfig = (): void => {
  const isProd = process.env.NODE_ENV === 'production';
  const originsString = process.env.ALLOWED_ORIGINS;

  if (isProd && !originsString) {
    throw new Error('ALLOWED_ORIGINS must be defined in production environment');
  }

  if (originsString) {
    try {
      const origins = parseAllowedOrigins(originsString);
      if (isProd && origins.length === 0) {
        throw new Error('At least one origin must be specified in production environment');
      }
    } catch (error) {
      throw new Error(`Invalid ALLOWED_ORIGINS configuration: ${(error as Error).message}`);
    }
  }
};

/**
 * Parses and validates the comma-separated list of allowed origins
 * @param originsString Comma-separated list of origins
 * @returns Array of validated origin URLs
 */
const parseAllowedOrigins = (originsString: string): string[] => {
  const origins = originsString
    .split(',')
    .map(origin => origin.trim())
    .filter(origin => origin.length > 0);

  // Validate each origin URL format
  origins.forEach(origin => {
    try {
      if (origin !== '*') {
        new URL(origin);
      }
    } catch (error) {
      throw new Error(`Invalid origin URL format: ${origin}`);
    }
  });

  return origins;
};

// Validate CORS configuration on module load
validateCorsConfig();

// Build CORS configuration object
export const corsConfig: CorsConfig = {
  // In development, allow all origins. In production, use configured origins
  origin: process.env.NODE_ENV === 'production'
    ? parseAllowedOrigins(process.env.ALLOWED_ORIGINS || '')
    : true,
  
  // Standard CORS headers and methods
  methods: DEFAULT_ALLOWED_METHODS,
  allowedHeaders: DEFAULT_ALLOWED_HEADERS,
  exposedHeaders: DEFAULT_EXPOSED_HEADERS,
  
  // Enable credentials for authenticated requests
  credentials: true,
  
  // Cache preflight requests for 24 hours
  maxAge: CORS_MAX_AGE
};

// Export the CORS middleware configuration
export default cors(corsConfig);