## Database Specific Requirements



## Integration Environment Setup

### MySql Docker Setup

Do this one time
```
docker rm mysql57
docker run --name mysql57 -p 3307:3306 -e MYSQL_ROOT_PASSWORD=password -d mysql/mysql-server:5.7
docker exec -it mysql57 bash
mysql -h localhost -u root -p
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'password' WITH GRANT OPTION;
FLUSH PRIVILEGES;
exit;
exit;
```

TODO: update his!!!! for mysql 8
https://stackoverflow.com/questions/50177216/how-to-grant-all-privileges-to-root-user-in-mysql-8-0

Then run:
```
docker start mysql57
```

### SqlServer Docker Setup

```
docker run --name sqlserver2017 -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Jmk38zZVn' -p 1434:1433 -d mcr.microsoft.com/mssql/server:2017-latest
```


### Postgres Setup
```
docker run --name postgres95 -p 5433:5432 -e POSTGRES_PASSWORD=password -d postgres:9.5
```

### H2 Setup
No H2 Docker containers are required for integ testing.  All tests use the JDBC in-memory server. 


### Restarting Docker Integ Environment

docker start mysql57
docker start sqlserver2017
docker start postgres95

To run integration tests run: "./gradlew test -Dtest.profile=integration"




### MS SQL Server / Azure Sql Database
In order to 'upsert' records without knowing if they exist before running a single
insert/update upsert statement, you must enable identity insertion on your tables.

You can easily enable this feature by executing the following SQL statement:
```
EXEC sp_MSforeachtable @command1="PRINT '?'; SET IDENTITY_INSERT ? ON", @whereand = ' AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id = o.id  AND is_identity = 1) and o.type = ''U'''
```
Azure SQL Database does not include the 'sp_MSforeachtable' procedures.  You can add it by running the following statements:

```
CREATE proc [dbo].[sp_MSforeach_worker]
    @command1 nvarchar(2000), @replacechar nchar(1) = N'?', @command2 nvarchar(2000) = null, @command3 nvarchar(2000) = null, @worker_type int =1
as

    create table #qtemp (	/* Temp command storage */
                            qnum				int				NOT NULL,
                            qchar				nvarchar(2000)	COLLATE database_default NULL
    )

    set nocount on
declare @name nvarchar(517), @namelen int, @q1 nvarchar(2000), @q2 nvarchar(2000)
declare @q3 nvarchar(2000), @q4 nvarchar(2000), @q5 nvarchar(2000)
declare @q6 nvarchar(2000), @q7 nvarchar(2000), @q8 nvarchar(2000), @q9 nvarchar(2000), @q10 nvarchar(2000)
declare @cmd nvarchar(2000), @replacecharindex int, @useq tinyint, @usecmd tinyint, @nextcmd nvarchar(2000)
declare @namesave nvarchar(517), @nametmp nvarchar(517), @nametmp2 nvarchar(258)

declare @local_cursor cursor
    if @worker_type=1
        set @local_cursor = hCForEachDatabase
    else
        set @local_cursor = hCForEachTable

    open @local_cursor
    fetch @local_cursor into @name

    while (@@fetch_status >= 0) begin

        select @namesave = @name
        select @useq = 1, @usecmd = 1, @cmd = @command1, @namelen = datalength(@name)
        while (@cmd is not null) begin		/* Generate @q* for exec() */
        select @replacecharindex = charindex(@replacechar, @cmd)
        while (@replacecharindex <> 0) begin

            /* 7.0, if name contains ' character, and the name has been single quoted in command, double all of them in dbname */
            /* if the name has not been single quoted in command, do not doulbe them */
            /* if name contains ] character, and the name has been [] quoted in command, double all of ] in dbname */
            select @name = @namesave
            select @namelen = datalength(@name)
            declare @tempindex int
            if (substring(@cmd, @replacecharindex - 1, 1) = N'''') begin
                /* if ? is inside of '', we need to double all the ' in name */
                select @name = REPLACE(@name, N'''', N'''''')
            end else if (substring(@cmd, @replacecharindex - 1, 1) = N'[') begin
                /* if ? is inside of [], we need to double all the ] in name */
                select @name = REPLACE(@name, N']', N']]')
            end else if ((@name LIKE N'%].%]') and (substring(@name, 1, 1) = N'[')) begin
                /* ? is NOT inside of [] nor '', and the name is in [owner].[name] format, handle it */
                /* !!! work around, when using LIKE to find string pattern, can't use '[', since LIKE operator is treating '[' as a wide char */
                select @tempindex = charindex(N'].[', @name)
                select @nametmp  = substring(@name, 2, @tempindex-2 )
                select @nametmp2 = substring(@name, @tempindex+3, len(@name)-@tempindex-3 )
                select @nametmp  = REPLACE(@nametmp, N']', N']]')
                select @nametmp2 = REPLACE(@nametmp2, N']', N']]')
                select @name = N'[' + @nametmp + N'].[' + @nametmp2 + ']'
            end else if ((@name LIKE N'%]') and (substring(@name, 1, 1) = N'[')) begin
                /* ? is NOT inside of [] nor '', and the name is in [name] format, handle it */
                /* j.i.c., since we should not fall into this case */
                /* !!! work around, when using LIKE to find string pattern, can't use '[', since LIKE operator is treating '[' as a wide char */
                select @nametmp = substring(@name, 2, len(@name)-2 )
                select @nametmp = REPLACE(@nametmp, N']', N']]')
                select @name = N'[' + @nametmp + N']'
            end
            /* Get the new length */
            select @namelen = datalength(@name)

            /* start normal process */
            if (datalength(@cmd) + @namelen - 1 > 2000) begin
                /* Overflow; put preceding stuff into the temp table */
                if (@useq > 9) begin
                    close @local_cursor
                    if @worker_type=1
                        deallocate hCForEachDatabase
                    else
                        deallocate hCForEachTable
                    return 1
                end
                if (@replacecharindex < @namelen) begin
                    /* If this happened close to beginning, make sure expansion has enough room. */
                    /* In this case no trailing space can occur as the row ends with @name. */
                    select @nextcmd = substring(@cmd, 1, @replacecharindex)
                    select @cmd = substring(@cmd, @replacecharindex + 1, 2000)
                    select @nextcmd = stuff(@nextcmd, @replacecharindex, 1, @name)
                    select @replacecharindex = charindex(@replacechar, @cmd)
                    insert #qtemp values (@useq, @nextcmd)
                    select @useq = @useq + 1
                    continue
                end
                /* Move the string down and stuff() in-place. */
                /* Because varchar columns trim trailing spaces, we may need to prepend one to the following string. */
                /* In this case, the char to be replaced is moved over by one. */
                insert #qtemp values (@useq, substring(@cmd, 1, @replacecharindex - 1))
                if (substring(@cmd, @replacecharindex - 1, 1) = N' ') begin
                    select @cmd = N' ' + substring(@cmd, @replacecharindex, 2000)
                    select @replacecharindex = 2
                end else begin
                    select @cmd = substring(@cmd, @replacecharindex, 2000)
                    select @replacecharindex = 1
                end
                select @useq = @useq + 1
            end
            select @cmd = stuff(@cmd, @replacecharindex, 1, @name)
            select @replacecharindex = charindex(@replacechar, @cmd)
        end

        /* Done replacing for current @cmd.  Get the next one and see if it's to be appended. */
        select @usecmd = @usecmd + 1
        select @nextcmd = case (@usecmd) when 2 then @command2 when 3 then @command3 else null end
        if (@nextcmd is not null and substring(@nextcmd, 1, 2) = N'++') begin
            insert #qtemp values (@useq, @cmd)
            select @cmd = substring(@nextcmd, 3, 2000), @useq = @useq + 1
            continue
        end

        /* Now exec() the generated @q*, and see if we had more commands to exec().  Continue even if errors. */
        /* Null them first as the no-result-set case won't. */
        select @q1 = null, @q2 = null, @q3 = null, @q4 = null, @q5 = null, @q6 = null, @q7 = null, @q8 = null, @q9 = null, @q10 = null
        select @q1 = qchar from #qtemp where qnum = 1
        select @q2 = qchar from #qtemp where qnum = 2
        select @q3 = qchar from #qtemp where qnum = 3
        select @q4 = qchar from #qtemp where qnum = 4
        select @q5 = qchar from #qtemp where qnum = 5
        select @q6 = qchar from #qtemp where qnum = 6
        select @q7 = qchar from #qtemp where qnum = 7
        select @q8 = qchar from #qtemp where qnum = 8
        select @q9 = qchar from #qtemp where qnum = 9
        select @q10 = qchar from #qtemp where qnum = 10
        truncate table #qtemp
        exec (@q1 + @q2 + @q3 + @q4 + @q5 + @q6 + @q7 + @q8 + @q9 + @q10 + @cmd)
        select @cmd = @nextcmd, @useq = 1
        end
        fetch @local_cursor into @name
    end /* while FETCH_SUCCESS */
    close @local_cursor
    if @worker_type=1
        deallocate hCForEachDatabase
    else
        deallocate hCForEachTable

    return 0
go

```

```
CREATE proc [dbo].[sp_MSforeachtable]
    @command1 nvarchar(2000), @replacechar nchar(1) = N'?', @command2 nvarchar(2000) = null,
    @command3 nvarchar(2000) = null, @whereand nvarchar(2000) = null,
    @precommand nvarchar(2000) = null, @postcommand nvarchar(2000) = null
AS
declare @mscat nvarchar(12)
select @mscat = ltrim(str(convert(int, 0x0002)))
    if (@precommand is not null)
        exec(@precommand)
    exec(N'declare hCForEachTable cursor global for select ''['' + REPLACE(schema_name(syso.schema_id), N'']'', N'']]'') + '']'' + ''.'' + ''['' + REPLACE(object_name(o.id), N'']'', N'']]'') + '']'' from dbo.sysobjects o join sys.all_objects syso on o.id = syso.object_id '
        + N' where OBJECTPROPERTY(o.id, N''IsUserTable'') = 1 ' + N' and o.category & ' + @mscat + N' = 0 '
        + @whereand)
declare @retval int
select @retval = @@error
    if (@retval = 0)
        exec @retval = dbo.sp_MSforeach_worker @command1, @replacechar, @command2, @command3, 0
    if (@retval = 0 and @postcommand is not null)
        exec(@postcommand)
    return @retval
go
``` 

After running the 'EXEC sp_MSforeachtable', on Azure SQL Database, you may drop the sp_MSforeach_worker and sp_MSforeachtable if you would like.


