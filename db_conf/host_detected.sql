-- phpMyAdmin SQL Dump
-- version 5.1.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Creato il: Feb 14, 2022 alle 03:47
-- Versione del server: 10.4.22-MariaDB
-- Versione PHP: 7.4.27

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `host_detected`
--
CREATE DATABASE IF NOT EXISTS `host_detected` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `host_detected`;

-- --------------------------------------------------------

--
-- Struttura della tabella `host`
--

CREATE TABLE `host` (
  `id` int(11) NOT NULL,
  `os` varchar(200) DEFAULT NULL,
  `ipv4` char(15) NOT NULL,
  `mac` char(17) NOT NULL,
  `status` enum('UNKNOWN','NOT_CHANGED','NEW','UPDATED','DELETED') NOT NULL,
  `network_ip` char(15) NOT NULL,
  `network_subnet_mask` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Struttura della tabella `host_port`
--

CREATE TABLE `host_port` (
  `host_id` int(11) NOT NULL,
  `port_id` int(11) NOT NULL,
  `port_protocol` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Struttura della tabella `network`
--

CREATE TABLE `network` (
  `ip` char(15) NOT NULL,
  `subnet_mask` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Struttura della tabella `port`
--

CREATE TABLE `port` (
  `id` int(11) NOT NULL,
  `protocol` varchar(10) NOT NULL,
  `service_name` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Indici per le tabelle scaricate
--

--
-- Indici per le tabelle `host`
--
ALTER TABLE `host`
  ADD PRIMARY KEY (`id`),
  ADD KEY `network_ip` (`network_ip`,`network_subnet_mask`);

--
-- Indici per le tabelle `host_port`
--
ALTER TABLE `host_port`
  ADD PRIMARY KEY (`host_id`,`port_id`,`port_protocol`),
  ADD KEY `port_id` (`port_id`,`port_protocol`);

--
-- Indici per le tabelle `network`
--
ALTER TABLE `network`
  ADD PRIMARY KEY (`ip`,`subnet_mask`);

--
-- Indici per le tabelle `port`
--
ALTER TABLE `port`
  ADD PRIMARY KEY (`id`,`protocol`);

--
-- AUTO_INCREMENT per le tabelle scaricate
--

--
-- AUTO_INCREMENT per la tabella `host`
--
ALTER TABLE `host`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=66;

--
-- Limiti per le tabelle scaricate
--

--
-- Limiti per la tabella `host`
--
ALTER TABLE `host`
  ADD CONSTRAINT `host_ibfk_1` FOREIGN KEY (`network_ip`,`network_subnet_mask`) REFERENCES `network` (`ip`, `subnet_mask`);

--
-- Limiti per la tabella `host_port`
--
ALTER TABLE `host_port`
  ADD CONSTRAINT `host_port_ibfk_1` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`),
  ADD CONSTRAINT `host_port_ibfk_2` FOREIGN KEY (`port_id`,`port_protocol`) REFERENCES `port` (`id`, `protocol`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
