CREATE TABLE IF NOT EXISTS referral_clicks (
    click_id BIGSERIAL PRIMARY KEY,
    affiliate_id BIGINT NOT NULL,
    product_card_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_referral_clicks_affiliate FOREIGN KEY (affiliate_id) REFERENCES users(user_id),
    CONSTRAINT fk_referral_clicks_product_card FOREIGN KEY (product_card_id) REFERENCES product_cards(product_card_id)
);

CREATE INDEX IF NOT EXISTS idx_referral_clicks_affiliate ON referral_clicks(affiliate_id);
CREATE INDEX IF NOT EXISTS idx_referral_clicks_product_card ON referral_clicks(product_card_id);
