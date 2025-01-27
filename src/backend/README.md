# Delayed Messaging System - Backend Service

Enterprise-grade backend service implementation for the Delayed Messaging System, providing message queuing, real-time updates, and secure communication channels.

## System Requirements

- Node.js >= 18.0.0 LTS
- npm >= 8.0.0
- Docker >= 20.10.0
- Docker Compose >= 2.0.0
- PostgreSQL 14
- Redis 7.x

## Technology Stack

- **Runtime Environment**: Node.js 18.x LTS
- **Language**: TypeScript 4.9.x
- **API Framework**: Express 4.x
- **WebSocket Server**: Socket.io 4.x
- **ORM**: Prisma 4.x
- **Database**: PostgreSQL 14
- **Message Queue**: Redis 7.x
- **Testing Framework**: Jest 29.x
- **Code Quality**: ESLint 8.x
- **Containerization**: Docker & Docker Compose

## Getting Started

### Environment Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd src/backend
```

2. Configure environment:
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. Install dependencies:
```bash
npm install
```

4. Generate Prisma client:
```bash
npm run prisma:generate
```

5. Start development services:
```bash
docker-compose up -d
npm run dev
```

### Verify Installation

```bash
# Check API health
curl http://localhost:3000/health

# Run test suite
npm run test
```

## Development Guidelines

### Code Structure

```
src/
├── api/           # REST API endpoints
├── config/        # Configuration files
├── models/        # Data models
├── services/      # Business logic
├── queue/         # Message queue handlers
├── websocket/     # WebSocket handlers
├── utils/         # Utility functions
└── tests/         # Test files
```

### Development Workflow

1. Create feature branch from `develop`
2. Implement changes with tests
3. Run linting and tests
4. Submit pull request
5. Code review and merge

### Code Quality

```bash
# Run linting
npm run lint

# Run type checking
npm run type-check

# Run tests with coverage
npm run test:coverage
```

## API Documentation

### Authentication

```typescript
// JWT Authentication
POST /auth/login
{
  "username": string,
  "password": string
}

// Response
{
  "token": string,
  "user": UserObject
}
```

### Message Operations

```typescript
// Send Message
POST /messages
{
  "recipient_id": string,
  "content": string
}

// Get Messages
GET /messages?limit=20&offset=0

// Get Message Status
GET /messages/:id/status
```

## Database Management

### Migrations

```bash
# Create migration
npm run prisma:migrate:dev

# Apply migrations
npm run prisma:migrate:deploy

# Reset database
npm run prisma:reset
```

### Backup and Restore

```bash
# Backup database
npm run db:backup

# Restore database
npm run db:restore
```

## Message Queue Implementation

### Queue Architecture

- Primary Queue: Message delay management
- Dead Letter Queue: Failed message handling
- Retry Queue: Message retry mechanism

### Queue Operations

```typescript
// Message Processing Flow
Message Received
  → Validation
  → Delay Calculation
  → Queue Placement
  → Scheduled Processing
  → Delivery Attempt
  → Status Update
```

## Testing Strategy

### Test Types

1. Unit Tests
   - Service logic
   - Utility functions
   - Model validations

2. Integration Tests
   - API endpoints
   - Database operations
   - Queue processing

3. End-to-End Tests
   - Complete message flow
   - WebSocket connections
   - Authentication flow

## Deployment Procedures

### Production Deployment

```bash
# Build production image
docker build -t dms-backend:latest .

# Deploy to ECS
npm run deploy:production
```

### Environment Configuration

```bash
# Required Environment Variables
NODE_ENV=production
PORT=3000
DATABASE_URL=postgresql://...
REDIS_URL=redis://...
JWT_SECRET=<secret>
```

## Monitoring and Logging

### Health Checks

- `/health`: API health status
- `/health/db`: Database connectivity
- `/health/redis`: Redis connectivity

### Logging

```typescript
// Log Levels
ERROR: Critical system errors
WARN:  Important warnings
INFO:  General information
DEBUG: Detailed debugging
```

## Security Considerations

### Security Measures

1. Authentication
   - JWT with refresh tokens
   - Rate limiting
   - Session management

2. Data Protection
   - TLS 1.3
   - AES-256 encryption
   - SQL injection prevention

3. Infrastructure
   - WAF configuration
   - Network isolation
   - Regular security scans

## Troubleshooting

### Common Issues

1. Database Connection
```bash
# Check database connectivity
npm run db:check
```

2. Redis Connection
```bash
# Verify Redis status
npm run redis:check
```

3. Message Processing
```bash
# View queue status
npm run queue:status
```

### Debug Mode

```bash
# Enable debug logging
DEBUG=dms:* npm run dev
```

## License

Copyright © 2024 Delayed Messaging System. All rights reserved.