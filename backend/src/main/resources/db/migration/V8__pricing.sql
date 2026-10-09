-- Pricing: which store products stand in for each ingredient, unit prices on every price
-- observation, and a stored cost on each recipe.

-- ---------- price sources ----------

-- V1 created the source check without a name, so find it by what it checks and drop it.
do $$
declare
    con record;
begin
    for con in
        select conname from pg_constraint
        where conrelid = 'ingredient_prices'::regclass
          and contype = 'c'
          and pg_get_constraintdef(oid) like '%source%'
    loop
        execute format('alter table ingredient_prices drop constraint %I', con.conname);
    end loop;
end $$;

alter table ingredient_prices
    add constraint chk_ingredient_prices_source
        check (source in ('OPEN_PRICES', 'MANUAL', 'KROGER', 'BLS', 'SEED'));

-- ---------- ingredient_prices: product details and unit prices ----------

alter table ingredient_prices
    -- the sale price when there is one; regular price stays in "price"
    add column promo_price numeric(10,2),
    -- the source's id for what was priced (Kroger product id, BLS series id)
    add column external_id varchar(100),
    -- the package size as the source wrote it ("3 lb", "16 oz"), kept for debugging
    add column size_text varchar(100),
    -- Kroger: WEIGHT means price is per pound, UNIT means per package
    add column sold_by varchar(10),
    -- computed once when the price is stored; null when the size couldn't be read
    add column price_per_100g numeric(10,4),
    add column price_per_item numeric(10,4),
    add constraint chk_ingredient_prices_sold_by check (sold_by in ('UNIT', 'WEIGHT')),
    add constraint chk_ingredient_prices_promo check (promo_price > 0),
    add constraint chk_ingredient_prices_per_100g check (price_per_100g >= 0),
    add constraint chk_ingredient_prices_per_item check (price_per_item >= 0);

-- ---------- ingredient_products: chosen once, re-priced by the refresh job ----------

create table ingredient_products (
    id uuid not null,
    ingredient_id uuid not null,
    source varchar(20) not null
        check (source in ('OPEN_PRICES', 'MANUAL', 'KROGER', 'BLS', 'SEED')),
    external_id varchar(100) not null,
    -- what the product is called at the source, so a person can check the mapping
    label varchar(255),
    active boolean not null default true,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    primary key (id),
    -- several products per ingredient are allowed (a backup when one is out of stock),
    -- but the same product only once
    constraint uk_ingredient_products unique (ingredient_id, source, external_id),
    constraint fk_ingredient_products_ingredient foreign key (ingredient_id)
        references ingredients (id) on delete cascade
);

create index idx_ingredient_products_source_active on ingredient_products (source, active);

-- ---------- recipes: cost, like nutrition ----------

alter table recipes
    add column cost_per_serving numeric(8,2),
    add column cost_complete boolean not null default false,
    add constraint chk_recipes_cost check (cost_per_serving >= 0);