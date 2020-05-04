

CREATE TABLE `Object` (
    `partition` VARCHAR(255) NOT NULL,
    `id`        VARCHAR(255) NOT NULL,
    `tenant`    VARCHAR(255) NOT NULL,
    `type`      VARCHAR(255) NOT NULL,
    `value1`    VARCHAR(255),
    `value2`    VARCHAR(255),
    `value3`    VARCHAR(255),
    `value4`    VARCHAR(255),
    `value5`    VARCHAR(255),
    `value6`    VARCHAR(255),
    `value7`    VARCHAR(255),
    `value8`    VARCHAR(255),
    `value9`    VARCHAR(255),
    `value10`   VARCHAR(255),
     CONSTRAINT `PK_Object` PRIMARY KEY (`partition`, `id`)
);