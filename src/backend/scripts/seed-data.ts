import { PrismaClient } from '@prisma/client'; // v4.x
import argon2 from 'argon2'; // v0.30.0
import { User } from '../src/models/User';
import { Message } from '../src/models/Message';
import { MessageStatus } from '../src/constants/messageStatus';
import { UserStatus } from '../src/constants/userStatus';
import { logger } from '../src/utils/logger';
import { prisma } from '../src/config/database';

// Test user data with secure password requirements
const TEST_USERS = [
    {
        username: 'testuser1',
        email: 'testuser1@example.com',
        password: 'TestPass123!',
        status: UserStatus.ONLINE
    },
    {
        username: 'testuser2',
        email: 'testuser2@example.com',
        password: 'TestPass456@',
        status: UserStatus.OFFLINE
    },
    {
        username: 'testuser3',
        email: 'testuser3@example.com',
        password: 'TestPass789#',
        status: UserStatus.ONLINE
    }
];

// Sample messages with various states for testing
const TEST_MESSAGES = [
    {
        content: 'Test message 1 for delivery delay testing.',
        status: MessageStatus.QUEUED
    },
    {
        content: 'Test message 2 for successful delivery validation.',
        status: MessageStatus.DELIVERED
    },
    {
        content: 'Test message 3 for message processing verification.',
        status: MessageStatus.SENDING
    }
];

// Configuration for seeding process
const SEED_CONFIG = {
    messageDelayMin: 30000, // 30 seconds
    messageDelayMax: 60000, // 60 seconds
    batchSize: 100,
    retryAttempts: 3,
    retryDelay: 1000
};

/**
 * Creates test users with secure password hashing
 */
async function createTestUsers(): Promise<User[]> {
    const users: User[] = [];

    try {
        for (const userData of TEST_USERS) {
            // Hash password using argon2
            const passwordHash = await argon2.hash(userData.password, {
                type: argon2.argon2id,
                memoryCost: 65536,
                timeCost: 3,
                parallelism: 4
            });

            // Create user with transaction
            const user = await prisma.$transaction(async (tx) => {
                const newUser = await tx.user.create({
                    data: {
                        username: userData.username,
                        email: userData.email,
                        passwordHash,
                        status: userData.status,
                        lastActive: new Date(),
                        createdAt: new Date(),
                        updatedAt: new Date()
                    }
                });

                logger.info(`Created test user: ${userData.username}`);
                return newUser;
            });

            users.push(user as unknown as User);
        }

        return users;
    } catch (error) {
        logger.error('Failed to create test users', { error: (error as Error).message });
        throw error;
    }
}

/**
 * Creates test messages with various states and delivery delays
 */
async function createTestMessages(users: User[]): Promise<Message[]> {
    const messages: Message[] = [];

    try {
        for (const messageData of TEST_MESSAGES) {
            // Select random sender and recipient
            const [sender, recipient] = users
                .sort(() => Math.random() - 0.5)
                .slice(0, 2);

            // Calculate delivery time
            const delay = Math.floor(
                Math.random() * 
                (SEED_CONFIG.messageDelayMax - SEED_CONFIG.messageDelayMin) + 
                SEED_CONFIG.messageDelayMin
            );
            const scheduledFor = new Date(Date.now() + delay);

            // Create message with transaction
            const message = await prisma.$transaction(async (tx) => {
                const newMessage = await tx.message.create({
                    data: {
                        content: messageData.content,
                        senderId: sender.id,
                        recipientId: recipient.id,
                        status: messageData.status,
                        createdAt: new Date(),
                        scheduledFor,
                        deliveredAt: messageData.status === MessageStatus.DELIVERED ? 
                            new Date() : null
                    }
                });

                // Create message status history
                await tx.messageStatus.create({
                    data: {
                        messageId: newMessage.id,
                        status: messageData.status,
                        timestamp: new Date()
                    }
                });

                logger.info(`Created test message with status: ${messageData.status}`);
                return newMessage;
            });

            messages.push(message as unknown as Message);
        }

        return messages;
    } catch (error) {
        logger.error('Failed to create test messages', { error: (error as Error).message });
        throw error;
    }
}

/**
 * Cleans existing data from the database
 */
async function cleanDatabase(): Promise<void> {
    try {
        await prisma.$transaction(async (tx) => {
            await tx.messageStatus.deleteMany();
            await tx.message.deleteMany();
            await tx.user.deleteMany();
        });

        logger.info('Database cleaned successfully');
    } catch (error) {
        logger.error('Failed to clean database', { error: (error as Error).message });
        throw error;
    }
}

/**
 * Main seeding function with comprehensive error handling
 */
export async function main(): Promise<void> {
    try {
        logger.info('Starting database seeding process');

        // Clean existing data
        await cleanDatabase();

        // Create test users
        const users = await createTestUsers();
        logger.info(`Created ${users.length} test users`);

        // Create test messages
        const messages = await createTestMessages(users);
        logger.info(`Created ${messages.length} test messages`);

        logger.info('Database seeding completed successfully');
    } catch (error) {
        logger.error('Database seeding failed', { error: (error as Error).message });
        throw error;
    } finally {
        await prisma.$disconnect();
    }
}

// Execute seeding if run directly
if (require.main === module) {
    main()
        .catch((error) => {
            logger.error('Seeding script failed', { error: error.message });
            process.exit(1);
        });
}