/**
 * @fileoverview Main Express application entry point implementing enhanced security,
 * monitoring, and real-time communication for the Delayed Messaging System.
 * 
 * @version 1.0.0
 */

import express, { Express } from 'express'; // ^4.18.2
import cors from 'cors'; // ^2.8.5
import helmet from 'helmet'; // ^7.0.0
import compression from 'compression'; // ^1.7.4
import morgan from 'morgan'; // ^1.10.0
import rateLimit from 'express-rate-limit'; // ^6.7.0
import promBundle from 'express-prom-bundle'; // ^6.6.0
import { createServer } from 'http';
import { v4 as uuidv4 } from 'uuid';

import { corsConfig } from './config/cors';
import createAuthRouter from './routes/authRoutes';
import messageRouter from './routes/messageRoutes';
import { WebSocketServer } from './socket/socketServer';
import errorHandler from './middleware/errorHandler';
import { logger } from './utils/logger';

// Environment variables with defaults
const PORT = process.env.PORT || 3000;
const MAX_REQUEST_SIZE = process.env.MAX_REQUEST_SIZE || '1mb';
const NODE_ENV = process.env.NODE_ENV || 'development';

/**
 * Initializes and configures the Express application with comprehensive
 * security, monitoring, and error handling capabilities.
 */
export function initializeApp(): Express {
    const app = express();

    // Security middleware
    app.use(helmet({
        contentSecurityPolicy: {
            directives: {
                defaultSrc: ["'self'"],
                scriptSrc: ["'self'"],
                styleSrc: ["'self'", "'unsafe-inline'"],
                imgSrc: ["'self'", 'data:', 'https:'],
                connectSrc: ["'self'", 'wss:', 'https:'],
                fontSrc: ["'self'"],
                objectSrc: ["'none'"],
                mediaSrc: ["'none'"],
                frameSrc: ["'none'"]
            }
        },
        crossOriginEmbedderPolicy: true,
        crossOriginOpenerPolicy: true,
        crossOriginResourcePolicy: { policy: "same-site" },
        dnsPrefetchControl: true,
        frameguard: { action: 'deny' },
        hidePoweredBy: true,
        hsts: true,
        ieNoOpen: true,
        noSniff: true,
        referrerPolicy: { policy: 'strict-origin-when-cross-origin' },
        xssFilter: true
    }));

    // CORS configuration
    app.use(cors(corsConfig));

    // Response compression
    app.use(compression({
        filter: (req, res) => {
            if (req.headers['x-no-compression']) {
                return false;
            }
            return compression.filter(req, res);
        },
        threshold: 0,
        level: 6
    }));

    // Request logging with correlation IDs
    app.use(morgan((tokens, req, res) => {
        const correlationId = req.headers['x-correlation-id'] || uuidv4();
        return JSON.stringify({
            method: tokens.method(req, res),
            url: tokens.url(req, res),
            status: tokens.status(req, res),
            responseTime: tokens['response-time'](req, res),
            correlationId,
            timestamp: new Date().toISOString()
        });
    }));

    // Prometheus metrics middleware
    app.use(promBundle({
        includeMethod: true,
        includePath: true,
        includeStatusCode: true,
        includeUp: true,
        customLabels: { app: 'delayed-messaging' },
        promClient: {
            collectDefaultMetrics: {
                timeout: 5000
            }
        }
    }));

    // Body parsing middleware with size limits
    app.use(express.json({ limit: MAX_REQUEST_SIZE }));
    app.use(express.urlencoded({ extended: true, limit: MAX_REQUEST_SIZE }));

    // Rate limiting middleware
    const limiter = rateLimit({
        windowMs: 15 * 60 * 1000, // 15 minutes
        max: 100, // 100 requests per windowMs
        standardHeaders: true,
        legacyHeaders: false,
        message: 'Too many requests from this IP, please try again later'
    });
    app.use(limiter);

    // Request correlation middleware
    app.use((req, res, next) => {
        req.correlationId = req.headers['x-correlation-id'] || uuidv4();
        res.setHeader('x-correlation-id', req.correlationId);
        next();
    });

    // Mount routers
    app.use('/auth', createAuthRouter(global.authController));
    app.use('/messages', messageRouter);

    // Health check endpoint
    app.get('/health', (req, res) => {
        res.status(200).json({
            status: 'healthy',
            timestamp: new Date().toISOString(),
            environment: NODE_ENV
        });
    });

    // Error handling middleware
    app.use(errorHandler);

    return app;
}

/**
 * Starts the HTTP server and initializes WebSocket server with proper
 * error handling and graceful shutdown.
 */
export function startServer(app: Express): void {
    const server = createServer(app);
    const wsServer = new WebSocketServer(server);

    // Initialize WebSocket server
    wsServer.initialize();

    // Start HTTP server
    server.listen(PORT, () => {
        logger.info(`Server started on port ${PORT}`, {
            port: PORT,
            environment: NODE_ENV,
            timestamp: new Date().toISOString()
        });
    });

    // Error handling for server
    server.on('error', (error: Error) => {
        logger.error('Server error occurred', {
            error: error.message,
            stack: error.stack,
            timestamp: new Date().toISOString()
        });
        process.exit(1);
    });

    // Graceful shutdown handler
    const shutdown = async () => {
        logger.info('Shutting down server...');
        
        server.close(() => {
            logger.info('HTTP server closed');
            process.exit(0);
        });

        // Set timeout for forceful shutdown
        setTimeout(() => {
            logger.error('Could not close connections in time, forcefully shutting down');
            process.exit(1);
        }, 10000);
    };

    process.on('SIGTERM', shutdown);
    process.on('SIGINT', shutdown);
}

// Export configured app instance
const app = initializeApp();
export default app;