-- PostgreSQL 14 Migration File
-- Initial database setup for Delayed Messaging System

-- Create custom enum types for status tracking
CREATE TYPE user_status AS ENUM (
    'ONLINE',
    'AWAY',
    'DO_NOT_DISTURB',
    'OFFLINE'
);

CREATE TYPE message_status AS ENUM (
    'DRAFT',
    'QUEUED',
    'SENDING',
    'DELIVERED'
);

-- Create users table with comprehensive user management
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status user_status NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_active TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT username_length CHECK (length(username) >= 3),
    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Create messages table with precise delay tracking
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status message_status NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    scheduled_for TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    delivered_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT prevent_self_message CHECK (sender_id != recipient_id),
    CONSTRAINT content_length CHECK (length(content) > 0 AND length(content) <= 1000),
    CONSTRAINT message_delay_check CHECK (
        CASE 
            WHEN status IN ('QUEUED', 'SENDING', 'DELIVERED') THEN
                scheduled_for >= created_at + INTERVAL '30 seconds' 
                AND scheduled_for <= created_at + INTERVAL '60 seconds'
            ELSE TRUE
        END
    )
);

-- Create user sessions table with security features
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    device_info JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT session_expiry_check CHECK (expires_at > created_at),
    CONSTRAINT token_length CHECK (length(token) >= 32)
);

-- Create function for message delay validation
CREATE OR REPLACE FUNCTION check_message_delay()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'QUEUED' THEN
        IF NEW.scheduled_for IS NULL THEN
            RAISE EXCEPTION 'Scheduled delivery time must be set for queued messages';
        END IF;
        
        IF NEW.scheduled_for < NEW.created_at + INTERVAL '30 seconds' OR 
           NEW.scheduled_for > NEW.created_at + INTERVAL '60 seconds' THEN
            RAISE EXCEPTION 'Message delivery time must be between 30 and 60 seconds from creation';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for message delay validation
CREATE TRIGGER validate_message_delay
    BEFORE INSERT OR UPDATE ON messages
    FOR EACH ROW
    EXECUTE FUNCTION check_message_delay();

-- Create trigger for user last active update
CREATE OR REPLACE FUNCTION update_last_active()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_active = CURRENT_TIMESTAMP(6);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_user_last_active
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_last_active();

-- Create optimized indexes for performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_last_active ON users(last_active);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_recipient ON messages(recipient_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_status ON messages(status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_scheduled ON messages(scheduled_for);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_status_scheduled ON messages(status, scheduled_for) 
    WHERE status = 'QUEUED';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_messages_delivery ON messages(delivered_at) 
    WHERE delivered_at IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_sessions_user ON user_sessions(user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_sessions_token ON user_sessions(token);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_active_sessions ON user_sessions(user_id, expires_at) 
    WHERE expires_at > CURRENT_TIMESTAMP;

-- Add comments for documentation
COMMENT ON TABLE users IS 'Stores user account information and status';
COMMENT ON TABLE messages IS 'Stores messages with precise delivery delay tracking';
COMMENT ON TABLE user_sessions IS 'Manages user authentication sessions with security controls';

COMMENT ON COLUMN messages.scheduled_for IS 'Planned delivery time (30-60 seconds after creation)';
COMMENT ON COLUMN messages.delivered_at IS 'Actual message delivery timestamp';
COMMENT ON COLUMN user_sessions.device_info IS 'JSON containing client device information';