
    create table dietary_labels (
        id uuid not null,
        name varchar(255) not null unique,
        primary key (id)
    );

    create table favorites (
        created_at timestamp(6) with time zone not null,
        id uuid not null,
        recipe_id uuid not null,
        user_id uuid not null,
        primary key (id),
        unique (user_id, recipe_id)
    );

    create table grocery_list_items (
        estimated_cost numeric(10,2),
        purchased boolean not null,
        total_amount numeric(10,3) not null,
        id uuid not null,
        ingredient_id uuid not null,
        meal_plan_id uuid not null,
        unit varchar(50) not null,
        primary key (id),
        unique (meal_plan_id, ingredient_id, unit)
    );

    create table ingredient_portions (
        gram_weight numeric(8,2) not null,
        id uuid not null,
        ingredient_id uuid not null,
        description varchar(255) not null,
        primary key (id),
        unique (ingredient_id, description)
    );

    create table ingredient_prices (
        currency varchar(3) not null,
        price numeric(10,2) not null,
        quantity numeric(10,3) not null,
        recorded_at timestamp(6) with time zone not null,
        id uuid not null,
        ingredient_id uuid not null,
        source varchar(20) not null check ((source in ('OPEN_PRICES','MANUAL'))),
        unit varchar(50) not null,
        store_name varchar(255),
        primary key (id)
    );

    create table ingredients (
        calories_per_100g numeric(7,2),
        carbs_g_per_100g numeric(7,2),
        fat_g_per_100g numeric(7,2),
        protein_g_per_100g numeric(7,2),
        created_at timestamp(6) with time zone not null,
        fdc_id bigint unique,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        name varchar(255) not null,
        primary key (id)
    );

    create table meal_plans (
        budget_target numeric(10,2),
        is_meal_prep_mode boolean not null,
        week_start_date date not null,
        created_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        user_id uuid not null,
        primary key (id),
        unique (user_id, week_start_date)
    );

    create table planned_meals (
        portion_multiplier numeric(5,2) not null,
        day_of_week varchar(10) not null check ((day_of_week in ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))),
        meal_slot varchar(10) not null check ((meal_slot in ('BREAKFAST','LUNCH','DINNER'))),
        id uuid not null,
        meal_plan_id uuid not null,
        recipe_id uuid not null,
        primary key (id),
        unique (meal_plan_id, day_of_week, meal_slot)
    );

    create table recipe_dietary_labels (
        dietary_label_id uuid not null,
        recipe_id uuid not null,
        primary key (dietary_label_id, recipe_id)
    );

    create table recipe_history (
        served_at timestamp(6) with time zone not null,
        id uuid not null,
        recipe_id uuid not null,
        user_id uuid not null,
        primary key (id)
    );

    create table recipe_ingredients (
        amount numeric(10,3),
        is_optional boolean not null,
        needs_review boolean not null,
        created_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        ingredient_id uuid,
        recipe_id uuid not null,
        unit varchar(50) not null,
        prep_note varchar(255),
        raw_text text not null,
        primary key (id)
    );

    create table recipe_steps (
        step_number integer not null,
        id uuid not null,
        recipe_id uuid not null,
        instruction_text text not null,
        primary key (id),
        unique (recipe_id, step_number)
    );

    create table recipes (
        base_servings integer not null,
        cook_time_minutes integer,
        prep_time_minutes integer,
        created_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        description text,
        name varchar(255) not null,
        notes text,
        primary key (id)
    );

    create table user_dietary_restrictions (
        dietary_label_id uuid not null,
        user_preference_id uuid not null,
        primary key (dietary_label_id, user_preference_id)
    );

    create table user_preferences (
        carbs_target_grams integer,
        fat_target_grams integer,
        household_serving_size integer not null,
        max_calories_per_day integer,
        max_calories_per_meal integer,
        protein_target_grams integer,
        repeat_avoidance_days integer not null,
        weekly_budget numeric(10,2) not null,
        created_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        user_id uuid not null unique,
        primary key (id)
    );

    create table users (
        is_premium boolean not null,
        created_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        email varchar(255) not null unique,
        first_name varchar(255) not null,
        last_name varchar(255) not null,
        password_hash varchar(255) not null,
        primary key (id)
    );

    create index idx_price_ingredient_recorded 
       on ingredient_prices (ingredient_id, recorded_at);

    create index idx_history_user_served 
       on recipe_history (user_id, served_at);

    alter table if exists favorites 
       add constraint FKf7myexo8ccfw0faigicluhrrh 
       foreign key (recipe_id) 
       references recipes;

    alter table if exists favorites 
       add constraint FKk7du8b8ewipawnnpg76d55fus 
       foreign key (user_id) 
       references users;

    alter table if exists grocery_list_items 
       add constraint FKmpcl90g9l4k3po1safola3qxw 
       foreign key (ingredient_id) 
       references ingredients;

    alter table if exists grocery_list_items 
       add constraint FKpi0bv1qjrecxxr2lau7dktfq3 
       foreign key (meal_plan_id) 
       references meal_plans;

    alter table if exists ingredient_portions 
       add constraint FKnyb8exvlpd8e2gu1ijbbk29lb 
       foreign key (ingredient_id) 
       references ingredients;

    alter table if exists ingredient_prices 
       add constraint FK1b1ufi8mv6x20sm845g5nl4a0 
       foreign key (ingredient_id) 
       references ingredients;

    alter table if exists meal_plans 
       add constraint FK7friea1stnx97lswpyxlb9ln1 
       foreign key (user_id) 
       references users;

    alter table if exists planned_meals 
       add constraint FKdlp9ht9aynbka3c5jk477gbnx 
       foreign key (meal_plan_id) 
       references meal_plans;

    alter table if exists planned_meals 
       add constraint FKl1lyl8ppjpolade7u2gld8fv9 
       foreign key (recipe_id) 
       references recipes;

    alter table if exists recipe_dietary_labels 
       add constraint FKmfmoupu4rnmq5q52pc3icagr7 
       foreign key (dietary_label_id) 
       references dietary_labels;

    alter table if exists recipe_dietary_labels 
       add constraint FKfx6j2yfvjavn3v6omrm98fq3d 
       foreign key (recipe_id) 
       references recipes;

    alter table if exists recipe_history 
       add constraint FKrk6qbahxlaq6ddrt6uxumu4rd 
       foreign key (recipe_id) 
       references recipes;

    alter table if exists recipe_history 
       add constraint FK1qqu34sdspbwuylje97gt2uea 
       foreign key (user_id) 
       references users;

    alter table if exists recipe_ingredients 
       add constraint FKgukrw6na9f61kb8djkkuvyxy8 
       foreign key (ingredient_id) 
       references ingredients;

    alter table if exists recipe_ingredients 
       add constraint FKcqlw8sor5ut10xsuj3jnttkc 
       foreign key (recipe_id) 
       references recipes;

    alter table if exists recipe_steps 
       add constraint FKof4i3g3aiwgro5ykaf1j28iw1 
       foreign key (recipe_id) 
       references recipes;

    alter table if exists user_dietary_restrictions 
       add constraint FKx2vc55jcv36jrom411t14pc8 
       foreign key (dietary_label_id) 
       references dietary_labels;

    alter table if exists user_dietary_restrictions 
       add constraint FKcsblgpc9n6i4yr4a21w8vhm61 
       foreign key (user_preference_id) 
       references user_preferences;

    alter table if exists user_preferences 
       add constraint FKepakpib0qnm82vmaiismkqf88 
       foreign key (user_id) 
       references users;
