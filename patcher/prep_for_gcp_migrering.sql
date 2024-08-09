create schema if not exists veilarbdialog;

set search_path to veilarbdialog;

create table veilarbdialog.shedlock
(
    name       varchar(64) not null primary key,
    lock_until timestamp(3),
    locked_at  timestamp(3),
    locked_by  varchar(255)
);

create table veilarbdialog.schema_version
(
    installed_rank integer                 not null
        constraint schema_version_pk
            primary key,
    version        varchar(50),
    description    varchar(200)            not null,
    type           varchar(20)             not null,
    script         varchar(1000)           not null,
    checksum       integer,
    installed_by   varchar(100)            not null,
    installed_on   timestamp default now() not null,
    execution_time integer                 not null,
    success        boolean                 not null
);

create index schema_version_s_idx
    on veilarbdialog.schema_version (success);

grant usage on schema veilarbdialog to "veilarbdialog_midlertidig";
grant select, insert, delete, update on table shedlock to "veilarbdialog_midlertidig";

grant select on table schema_version to "veilarbdialog_midlertidig";
