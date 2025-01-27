# Contributing to Delayed Messaging System

## Table of Contents
- [Introduction](#introduction)
- [Development Setup](#development-setup)
- [Code Style Guidelines](#code-style-guidelines)
- [Git Workflow](#git-workflow)
- [Testing Requirements](#testing-requirements)
- [Security Guidelines](#security-guidelines)
- [CI/CD Pipeline](#cicd-pipeline)
- [Issue Guidelines](#issue-guidelines)
- [Code Review Process](#code-review-process)
- [Documentation](#documentation)

## Introduction

Welcome to the Delayed Messaging System project! This guide outlines our development workflow, coding standards, and contribution process. Our goal is to maintain high-quality, secure, and maintainable code through consistent practices.

### Architecture Overview
The system uses a microservices architecture with:
- Node.js 18.x backend services
- React/TypeScript web frontend
- Native iOS (Swift) and Android (Kotlin) mobile apps
- PostgreSQL 14 database
- Redis 7.x for queuing and caching

## Development Setup

### Prerequisites
- Node.js 18.x LTS
- PostgreSQL 14
- Redis 7.x
- Docker & Docker Compose
- Platform-specific requirements:
  - iOS: Xcode 14+, CocoaPods
  - Android: Android Studio, JDK 11
  - Web: npm 8+

### Local Environment Setup
1. Clone the repository
```bash
git clone https://github.com/organization/delayed-messaging-system.git
cd delayed-messaging-system
```

2. Install dependencies
```bash
npm install
```

3. Start development environment
```bash
docker-compose up -d
```

## Code Style Guidelines

### TypeScript/JavaScript (Backend & Web)
- Follow ESLint configuration in `.eslintrc.json`
- Use Prettier for formatting (configuration in `.prettierrc`)
- Maximum line length: 100 characters
- Use TypeScript strict mode
- Document all public APIs using JSDoc

### Swift (iOS)
- Follow SwiftLint rules
- Use Swift style guide conventions
- Implement protocol-oriented design patterns
- Document public interfaces

### Kotlin (Android)
- Follow Kotlin coding conventions
- Use ktlint for formatting
- Implement MVVM architecture pattern
- Document public methods and classes

## Git Workflow

### Branch Naming
- Feature branches: `feature/DMS-<ticket>-description`
- Bug fixes: `bugfix/DMS-<ticket>-description`
- Hotfixes: `hotfix/DMS-<ticket>-description`

### Commit Messages
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```
Types: feat, fix, docs, style, refactor, test, chore

### Pull Requests
- Use the provided PR template
- Link related issues
- Include test coverage report
- Update documentation
- Require 2 approvals from senior developers

## Testing Requirements

### Backend (Node.js)
- Minimum 80% code coverage
- Unit tests with Jest
- Integration tests for API endpoints
- Load tests for critical paths

### iOS
- XCTest for unit testing
- UI tests for critical flows
- Minimum 75% code coverage

### Android
- JUnit for unit testing
- Espresso for UI testing
- Minimum 75% code coverage

### Web
- Jest for unit testing
- React Testing Library for component tests
- E2E tests with Cypress
- Minimum 80% code coverage

## Security Guidelines

### Code Security
- Follow OWASP secure coding guidelines
- Implement input validation
- Use parameterized queries
- Apply rate limiting
- Regular dependency scanning

### Review Requirements
- Security review for authentication changes
- Vulnerability scanning with SonarQube
- Dependencies audit with npm audit
- Container scanning

## CI/CD Pipeline

### GitHub Actions Workflows
- Build stage: compile and lint
- Test stage: unit and integration tests
- Security scan: SonarQube analysis
- Package: Docker image building
- Deploy: ECS deployment

### Quality Gates
- All tests passing
- Code coverage thresholds met
- No critical security issues
- Successful build deployment
- Performance benchmarks met

## Issue Guidelines

### Bug Reports
Use the bug report template including:
- Environment details
- Reproduction steps
- Expected vs actual behavior
- Relevant logs/screenshots

### Feature Requests
Use the feature request template including:
- Problem description
- Proposed solution
- Alternative approaches
- Success criteria

## Code Review Process

### Review Checklist
- [ ] Code follows style guidelines
- [ ] Tests are comprehensive and passing
- [ ] Documentation is updated
- [ ] Security best practices followed
- [ ] Performance impact considered
- [ ] No breaking changes
- [ ] Approved by 2 senior developers

### Review SLAs
- Initial review: 24 hours
- Follow-up reviews: 12 hours
- Critical fixes: 4 hours

## Documentation

### Code Documentation
- Use JSDoc for TypeScript/JavaScript
- Use SwiftDoc for Swift
- Use KDoc for Kotlin
- Document complex algorithms
- Include usage examples

### Technical Documentation
- Update README.md for new features
- Maintain API documentation
- Update architecture diagrams
- Document configuration changes
- Keep deployment guides current

## Questions or Need Help?

Feel free to reach out to the core team through:
- GitHub Issues
- Development Slack channel
- Technical documentation wiki

Thank you for contributing to the Delayed Messaging System!