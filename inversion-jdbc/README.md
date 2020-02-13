Integration Environment Setup

docker rm mysql57
docker run --name mysql57 -p 3307:3306 -e MYSQL_ROOT_PASSWORD=password -d mysql/mysql-server:5.7
docker exec -it mysql57 bash
mysql -h localhost -u root -p
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'password' WITH GRANT OPTION;
FLUSH PRIVILEGES;
CREATE DATABASE northwind;