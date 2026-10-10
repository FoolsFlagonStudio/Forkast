-- Meal plans: recipe selections that cover one or more days, dated meals, plan costs, and
-- the columns the planner reads (slot scores, pantry staples, leftovers for lunch, timezone).

-- ---------- planned_meals: rebuilt around dates and selections ----------

-- It has never held data, and its day-of-week shape can't hold dates or multi-day recipes.
drop table planned_meals;

-- One recipe choice: cooked once on start_date, eaten for days_covered days
create table plan_selections (
    id uuid not null,
    meal_plan_id uuid not null,
    recipe_id uuid not null,
    meal_slot varchar(10) not null check (meal_slot in ('BREAKFAST', 'LUNCH', 'DINNER')),
    position integer not null check (position >= 1),
    start_date date not null,
    days_covered integer not null check (days_covered between 1 and 7),
    servings integer not null check (servings >= 1),
    portion_multiplier numeric(6,3) not null check (portion_multiplier > 0),
    estimated_cost numeric(10,2) check (estimated_cost >= 0),
    locked boolean not null default false,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_plan_selections unique (meal_plan_id, meal_slot, position),
    constraint fk_plan_selections_meal_plan foreign key (meal_plan_id)
        references meal_plans (id) on delete cascade,
    constraint fk_plan_selections_recipe foreign key (recipe_id) references recipes (id)
);

create index idx_plan_selections_recipe on plan_selections (recipe_id);

-- The calendar: one row per day and slot. A dinner selection can own the next day's lunch
-- (leftovers for lunch), so the slot here can differ from the selection's.
create table planned_meals (
    id uuid not null,
    meal_plan_id uuid not null,
    selection_id uuid not null,
    meal_date date not null,
    meal_slot varchar(10) not null check (meal_slot in ('BREAKFAST', 'LUNCH', 'DINNER')),
    leftover boolean not null default false,
    primary key (id),
    constraint uk_planned_meals unique (meal_plan_id, meal_date, meal_slot),
    constraint fk_planned_meals_meal_plan foreign key (meal_plan_id)
        references meal_plans (id) on delete cascade,
    constraint fk_planned_meals_selection foreign key (selection_id)
        references plan_selections (id) on delete cascade
);

create index idx_planned_meals_selection on planned_meals (selection_id);

-- ---------- meal_plans: how it was made and what it costs ----------

alter table meal_plans
    -- the Random seed, so the same plan can be rebuilt
    add column generation_seed bigint,
    -- what the week's meals use, and what the shopping trip costs (whole packages)
    add column usage_cost numeric(10,2),
    add column grocery_cost numeric(10,2),
    add column over_budget boolean not null default false,
    -- meals in the week no recipe covers (a count of 1, or too few recipes to choose from)
    add column open_meals integer not null default 0,
    add column generated_at timestamp(6) with time zone,
    add column source varchar(10) not null default 'MANUAL',
    add constraint chk_meal_plans_source check (source in ('AUTO', 'MANUAL')),
    add constraint chk_meal_plans_usage_cost check (usage_cost >= 0),
    add constraint chk_meal_plans_grocery_cost check (grocery_cost >= 0),
    add constraint chk_meal_plans_open_meals check (open_meals >= 0);

-- ---------- grocery_list_items: whole packages and pantry staples ----------

alter table grocery_list_items
    add column package_count integer,
    add column package_label varchar(255),
    add column package_price numeric(10,2),
    -- a pantry staple: listed under "check your pantry", counted at usage cost
    add column pantry boolean not null default false,
    -- the user marked a pantry staple as needed, so it counts at its full package price
    add column need_to_buy boolean not null default false,
    add constraint chk_grocery_list_items_package_count check (package_count >= 1),
    add constraint chk_grocery_list_items_package_price check (package_price >= 0);

-- ---------- what the planner reads ----------

-- Slot fit from SlotClassifier; 0 until the next reprocess fills them in
alter table recipes
    add column breakfast_score integer not null default 0,
    add column lunch_score integer not null default 0,
    add column dinner_score integer not null default 0,
    add constraint chk_recipes_breakfast_score check (breakfast_score between 0 and 100),
    add constraint chk_recipes_lunch_score check (lunch_score between 0 and 100),
    add constraint chk_recipes_dinner_score check (dinner_score between 0 and 100);

alter table ingredients
    add column pantry_staple boolean not null default false;

alter table user_preferences
    add column leftovers_for_lunch boolean not null default false;

-- An IANA name such as America/New_York; plans generate at 11pm in this zone
alter table users
    add column timezone varchar(64) not null default 'America/New_York';