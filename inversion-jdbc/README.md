Integration Environment Setup


### MySql Docker Setup

Do this one time
```
docker rm mysql57
docker run --name mysql57 -p 3307:3306 -e MYSQL_ROOT_PASSWORD=password -d mysql/mysql-server:5.7
docker exec -it mysql57 bash
mysql -h localhost -u root -p
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'password' WITH GRANT OPTION;
FLUSH PRIVILEGES;
quite;
exit;
```

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

