-- Admin outbox list orders by created_at DESC without status filter.
CREATE INDEX idx_outbox_events_created_at ON outbox_events (created_at DESC);
