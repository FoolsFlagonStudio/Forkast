-- When the weekly job wrote recipe_history for a plan whose week has ended. Null until then,
-- so each ended plan is recorded exactly once.
alter table meal_plans
    add column history_recorded_at timestamp(6) with time zone;