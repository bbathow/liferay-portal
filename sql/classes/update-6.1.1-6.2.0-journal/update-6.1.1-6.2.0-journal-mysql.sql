alter table JournalArticle add folderId bigint;
alter table JournalArticle add treePath longtext null;

commit;

update JournalArticle set folderId = 0;
update JournalArticle set treePath = '/';

create table JournalFolder (
	uuid_ varchar(75) null,
	folderId bigint not null primary key,
	groupId bigint,
	companyId bigint,
	userId bigint,
	userName varchar(75) null,
	createDate datetime(6) null,
	modifiedDate datetime(6) null,
	parentFolderId bigint,
	treePath longtext null,
	name varchar(100) null,
	description longtext null,
	status integer,
	statusByUserId bigint,
	statusByUserName varchar(75) null,
	statusDate datetime(6) null
) engine InnoDB;
