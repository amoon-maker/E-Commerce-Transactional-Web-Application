/*
  Um Hamza E-Commerce Transactional Web Application
  DATABASE COMPLETION
  Physical Implementation - Microsoft SQL Server

  Creates the database if it does not already exist.
  Does not drop an existing database.
*/
IF DB_ID(N'UmHamzaECommerce') IS NULL
BEGIN
    CREATE DATABASE UmHamzaECommerce;
END
GO

USE UmHamzaECommerce;
GO
