-- Direct children of users
alter table user_preferences drop constraint FKepakpib0qnm82vmaiismkqf88;
alter table user_preferences add constraint fk_user_preferences_user
    foreign key (user_id) references users (id) on delete cascade;

alter table favorites drop constraint FKk7du8b8ewipawnnpg76d55fus;
alter table favorites add constraint fk_favorites_user
    foreign key (user_id) references users (id) on delete cascade;

alter table recipe_history drop constraint FK1qqu34sdspbwuylje97gt2uea;
alter table recipe_history add constraint fk_recipe_history_user
    foreign key (user_id) references users (id) on delete cascade;

alter table meal_plans drop constraint FK7friea1stnx97lswpyxlb9ln1;
alter table meal_plans add constraint fk_meal_plans_user
    foreign key (user_id) references users (id) on delete cascade;

-- Grandchildren: rows owned by preferences and meal plans
alter table user_dietary_restrictions drop constraint FKcsblgpc9n6i4yr4a21w8vhm61;
alter table user_dietary_restrictions add constraint fk_user_dietary_restrictions_preferences
    foreign key (user_preference_id) references user_preferences (id) on delete cascade;

alter table planned_meals drop constraint FKdlp9ht9aynbka3c5jk477gbnx;
alter table planned_meals add constraint fk_planned_meals_meal_plan
    foreign key (meal_plan_id) references meal_plans (id) on delete cascade;

alter table grocery_list_items drop constraint FKpi0bv1qjrecxxr2lau7dktfq3;
alter table grocery_list_items add constraint fk_grocery_list_items_meal_plan
    foreign key (meal_plan_id) references meal_plans (id) on delete cascade;