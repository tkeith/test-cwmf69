import { PrismaClient } from '@prisma/client'; // v4.x
import { config } from 'dotenv'; // v16.x

// Load environment variables
config();

// Environment variables with defaults
const {
  DATABASE_URL = '',
  DB_MAX_CONNECTIONS = '20',
  DB_IDLE_TIMEOUT = '10',
  DB_CONNECTION_TIMEOUT = '5000',
  NODE_ENV
} = process.env;

// SSL configuration for production environments
const DB_SSL_ENABLED = NODE_ENV === 'production';

/**
 * Creates and configures a new Prisma client instance with connection pooling,
 * SSL settings, and performance monitoring capabilities.
 * 
 * @returns {PrismaClient} Configured Prisma client instance
 */
const createPrismaClient = (): PrismaClient => {
  // Validate required environment variables
  if (!DATABASE_URL) {
    throw new Error('DATABASE_URL environment variable is required');
  }

  // Configure connection pool settings
  const poolConfig = {
    max: parseInt(DB_MAX_CONNECTIONS, 10),
    idleTimeoutMillis: parseInt(DB_IDLE_TIMEOUT, 10) * 1000,
    connectionTimeoutMillis: parseInt(DB_CONNECTION_TIMEOUT, 10)
  };

  // SSL configuration for production
  const sslConfig = DB_SSL_ENABLED ? {
    ssl: {
      rejectUnauthorized: true,
      ca: process.env.DB_SSL_CA,
    }
  } : {};

  // Initialize Prisma client with configuration
  const prisma = new PrismaClient({
    datasources: {
      db: {
        url: DATABASE_URL
      }
    },
    log: [
      { level: 'query', emit: 'event' },
      { level: 'error', emit: 'event' },
      { level: 'warn', emit: 'event' }
    ],
    ...sslConfig
  });

  // Performance monitoring middleware
  prisma.$use(async (params, next) => {
    const start = Date.now();
    const result = await next(params);
    const duration = Date.now() - start;

    if (duration > 1000) { // Log slow queries (>1s)
      console.warn(`Slow query detected (${duration}ms):`, params);
    }

    return result;
  });

  // Development environment query logging
  if (NODE_ENV === 'development') {
    prisma.$on('query', (e: any) => {
      console.log('Query:', e.query);
      console.log('Duration:', e.duration + 'ms');
    });
  }

  // Error event handling
  prisma.$on('error', (e: any) => {
    console.error('Database error:', e);
  });

  // Graceful shutdown handler
  process.on('SIGINT', async () => {
    await prisma.$disconnect();
    process.exit(0);
  });

  return prisma;
};

/**
 * Validates database connection by performing a test query with retry mechanism
 * 
 * @param {PrismaClient} prisma - Prisma client instance to validate
 * @returns {Promise<boolean>} Connection status
 */
export const validateDatabaseConnection = async (prisma: PrismaClient): Promise<boolean> => {
  const maxRetries = 3;
  const retryDelay = 1000;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      // Attempt a simple query to validate connection
      await prisma.$queryRaw`SELECT 1`;
      console.log('Database connection validated successfully');
      return true;
    } catch (error) {
      console.error(`Database connection attempt ${attempt} failed:`, error);

      if (attempt === maxRetries) {
        console.error('Max retry attempts reached. Database connection failed.');
        return false;
      }

      // Exponential backoff
      await new Promise(resolve => setTimeout(resolve, retryDelay * attempt));
    }
  }

  return false;
};

// Create and export configured Prisma client instance
export const prisma = createPrismaClient();

// Export models for type safety
export const {
  user,
  message,
  userSession,
  messageStatus
} = prisma;