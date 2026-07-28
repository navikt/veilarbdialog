create table outbox (
    id        bigserial  primary key,
    topic     text       not null,
    key       text       not null,
    payload   text       not null,
    opprettet timestamp  not null default current_timestamp
);
