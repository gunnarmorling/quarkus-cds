create schema todo;

create sequence todo.hibernate_sequence start 10 increment 1;

create table todo.Todo (
    id int8 not null,
    title varchar(255),
    primary key (id)
);

insert into todo.Todo values (1, 'Be Awesome');
insert into todo.Todo values (2, 'Learn Quarkus');
