alter table user_preferences
    add column plan_start_day varchar(10) not null default 'SUNDAY'
        check (plan_start_day in ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'));

alter table user_preferences
    add column prefers_meal_prep boolean not null default false;

create table user_meal_slots (
    user_preference_id uuid not null,
    meal_slot varchar(10) not null
        check (meal_slot in ('BREAKFAST','LUNCH','DINNER')),
    recipes_per_week integer not null
        check (recipes_per_week between 1 and 7),
    primary key (user_preference_id, meal_slot),
    constraint fk_user_meal_slots_preferences foreign key (user_preference_id)
        references user_preferences (id) on delete cascade
);