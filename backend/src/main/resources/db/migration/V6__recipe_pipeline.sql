-- Trigram matching (Supabase installs extensions in the "extensions" schema)
create extension if not exists pg_trgm with schema extensions;

-- Where each recipe came from, and what the classifiers computed
alter table recipes
    add column source_url varchar(2048),
    add column source_host varchar(255),
    add column image_url varchar(2048),
    add column category varchar(100),
    add column cuisine varchar(100),
    add column keywords text,
    add column meal_prep_score integer not null default 0
        check (meal_prep_score between 0 and 100),
    add column calories_per_serving numeric(7,2),
    add column protein_g_per_serving numeric(7,2),
    add column carbs_g_per_serving numeric(7,2),
    add column fat_g_per_serving numeric(7,2),
    add column nutrition_complete boolean not null default false;

create unique index uk_recipes_source_url on recipes (source_url);

-- Other names an ingredient goes by ("scallion" -> green onion)
create table ingredient_aliases (
    id uuid not null,
    ingredient_id uuid not null,
    alias varchar(255) not null,
    primary key (id),
    constraint uk_ingredient_aliases_alias unique (alias),
    constraint fk_ingredient_aliases_ingredient foreign key (ingredient_id)
        references ingredients (id) on delete cascade
);

-- What an ingredient contains, for dietary labels
create table ingredient_tags (
    ingredient_id uuid not null,
    tag varchar(20) not null
        check (tag in ('MEAT','POULTRY','FISH','SHELLFISH','DAIRY','EGG','GLUTEN',
                       'TREE_NUT','PEANUT','SOY','HONEY','GELATIN')),
    primary key (ingredient_id, tag),
    constraint fk_ingredient_tags_ingredient foreign key (ingredient_id)
        references ingredients (id) on delete cascade
);

create index idx_ingredients_name_trgm
    on ingredients using gin (name extensions.gin_trgm_ops);
create index idx_ingredient_aliases_alias_trgm
    on ingredient_aliases using gin (alias extensions.gin_trgm_ops);

-- What the parser read and how confident the match was
alter table recipe_ingredients
    add column parsed_name varchar(255),
    add column match_score numeric(4,3);