-- Create urls table for storing shortened URL mappings
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    alias VARCHAR(255) NOT NULL UNIQUE,
    destination_url TEXT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    redirect_count BIGINT NOT NULL DEFAULT 0,
    last_redirect_at TIMESTAMP
);

-- Create indexes for common query patterns
CREATE INDEX idx_urls_user_id ON urls(user_id);
CREATE INDEX idx_urls_created_at ON urls(created_at DESC);
CREATE INDEX idx_urls_is_active ON urls(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_urls_expires_at ON urls(expires_at) WHERE expires_at IS NOT NULL;

-- Create url_stats table for daily redirect analytics
CREATE TABLE url_stats (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL REFERENCES urls(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,
    redirect_count BIGINT NOT NULL DEFAULT 0,
    unique_visitors BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_url_stats_url_date UNIQUE (url_id, stat_date)
);

-- Create index for stats queries
CREATE INDEX idx_url_stats_url_id_date ON url_stats(url_id, stat_date DESC);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to auto-update updated_at
CREATE TRIGGER update_urls_updated_at BEFORE UPDATE ON urls
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_url_stats_updated_at BEFORE UPDATE ON url_stats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
