-- Candela Tiles & Marbles - Initial Schema Migration (Baseline V1)
-- Targets PostgreSQL 15+ / 17 (and H2 in PostgreSQL mode for unit testing)

CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    image_url VARCHAR(1024),
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    product_count INT DEFAULT 0,
    version INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS collections (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    cover_image_url VARCHAR(1024),
    display_order INT DEFAULT 0,
    is_published BOOLEAN DEFAULT TRUE,
    product_count INT DEFAULT 0,
    version INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(64) PRIMARY KEY,
    sku VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(128),
    master_category VARCHAR(128),
    subcategory VARCHAR(128),
    collection_name VARCHAR(255),
    collection_slug VARCHAR(255),
    brand VARCHAR(128),
    material VARCHAR(128),
    finish VARCHAR(64),
    colour VARCHAR(64),
    shape VARCHAR(64),
    origin VARCHAR(128),
    price_per_sqft NUMERIC(12, 2) DEFAULT 0.00,
    sale_price NUMERIC(12, 2),
    stock_sqft_available NUMERIC(12, 2) DEFAULT 0.00,
    in_stock BOOLEAN DEFAULT TRUE,
    is_featured BOOLEAN DEFAULT FALSE,
    main_image_url VARCHAR(1024),
    status VARCHAR(32) DEFAULT 'published',
    short_description TEXT,
    description TEXT,
    version INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_slug ON products (slug);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);
CREATE INDEX IF NOT EXISTS idx_products_collection ON products (collection_slug);
CREATE INDEX IF NOT EXISTS idx_products_status ON products (status);

CREATE TABLE IF NOT EXISTS inventory (
    product_id VARCHAR(64) PRIMARY KEY,
    product_name VARCHAR(255),
    current_stock INT DEFAULT 0,
    low_stock_threshold INT DEFAULT 10,
    reorder_quantity INT DEFAULT 50,
    status VARCHAR(32) DEFAULT 'IN_STOCK',
    warehouse_location VARCHAR(128),
    version INT DEFAULT 1,
    last_restocked_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS enquiries (
    id VARCHAR(64) PRIMARY KEY,
    reference_code VARCHAR(64) NOT NULL UNIQUE,
    client_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(32),
    customer_email VARCHAR(255),
    product_name VARCHAR(255),
    project_type VARCHAR(128),
    message TEXT,
    status VARCHAR(64) DEFAULT 'RECEIVED',
    tracking_token_hash VARCHAR(128),
    notes TEXT,
    version INT DEFAULT 1,
    status_updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_enquiries_ref ON enquiries (reference_code);
CREATE INDEX IF NOT EXISTS idx_enquiries_status ON enquiries (status);

CREATE TABLE IF NOT EXISTS media (
    id VARCHAR(64) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    public_url VARCHAR(1024) NOT NULL,
    content_type VARCHAR(128),
    size_bytes BIGINT,
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    uploaded_by VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS homepage_content (
    section_key VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255),
    subtitle VARCHAR(255),
    description TEXT,
    image_url VARCHAR(1024),
    cta_text VARCHAR(128),
    cta_url VARCHAR(512),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS banners (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    image_url VARCHAR(1024),
    link_url VARCHAR(512),
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
