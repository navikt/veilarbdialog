CREATE INDEX idx_outbox_topic
    ON outbox (topic);

CREATE INDEX idx_outbox_key
    ON outbox (key);
