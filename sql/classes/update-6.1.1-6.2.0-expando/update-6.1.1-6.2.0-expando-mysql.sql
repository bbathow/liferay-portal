alter table ExpandoRow add modifiedDate datetime(6) null;

commit;

update ExpandoRow set modifiedDate = now();
