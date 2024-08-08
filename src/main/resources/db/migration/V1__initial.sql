create table veilarbdialog.shedlock
(
    name       varchar(64) not null primary key,
    lock_until timestamp(3),
    locked_at  timestamp(3),
    locked_by  varchar(255)
);
