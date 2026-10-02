alter table users add column is_verified boolean not null default false;
alter table users add column password_changed_at timestamp(6) with time zone;
alter table users add column last_login_at timestamp(6) with time zone;

create table refresh_tokens (
    id uuid not null,
    user_id uuid not null,
    token_hash varchar(64) not null,
    expires_at timestamp(6) with time zone not null,
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_refresh_tokens_token_hash unique (token_hash),
    constraint fk_refresh_tokens_user foreign key (user_id)
        references users (id) on delete cascade
);

create index idx_refresh_tokens_user on refresh_tokens (user_id);

create table auth_codes (
    id uuid not null,
    user_id uuid not null,
    type varchar(20) not null
        check (type in ('EMAIL_VERIFY', 'PASSWORD_RESET', 'EMAIL_CHANGE')),
    code_hash varchar(64) not null,
    new_email varchar(255),
    attempts integer not null default 0,
    expires_at timestamp(6) with time zone not null,
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_auth_codes_user_type unique (user_id, type),
    constraint fk_auth_codes_user foreign key (user_id)
        references users (id) on delete cascade
);