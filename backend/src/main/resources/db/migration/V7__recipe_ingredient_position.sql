-- Keep each recipe's ingredient lines in the order the recipe lists them.

alter table recipe_ingredients add column position integer;

-- Existing rows: number them in the order they were inserted (the importer adds lines in order).
update recipe_ingredients ri
set position = numbered.rn
from (
    select id, row_number() over (partition by recipe_id order by created_at, id) as rn
    from recipe_ingredients
) numbered
where ri.id = numbered.id;

alter table recipe_ingredients alter column position set not null;
alter table recipe_ingredients add constraint chk_recipe_ingredients_position check (position >= 1);