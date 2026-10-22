-- MySQL dump 10.13  Distrib 5.7.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: up_self_dev
-- ------------------------------------------------------
-- Server version	5.7.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `assignment`
--

DROP TABLE IF EXISTS `assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `assignment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(20) NOT NULL,
  `title` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `instructions` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `due_at` datetime DEFAULT NULL,
  `max_score` int(11) NOT NULL DEFAULT '100',
  `version` int(11) NOT NULL DEFAULT '0',
  `language_code` varchar(35) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en',
  `created_by` bigint(20) NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_assignment_course` (`course_id`,`status`),
  KEY `created_by` (`created_by`),
  CONSTRAINT `assignment_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `learning_course` (`id`),
  CONSTRAINT `assignment_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assignment`
--

LOCK TABLES `assignment` WRITE;
/*!40000 ALTER TABLE `assignment` DISABLE KEYS */;
INSERT INTO `assignment` VALUES (35,42,'write numbers from 1 -10','write numbers from 1- 10','PUBLISHED','2026-10-23 16:05:00',100,0,'en',3,'2026-10-22 08:06:08','2026-10-22 08:06:08');
/*!40000 ALTER TABLE `assignment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `assignment_attempt`
--

DROP TABLE IF EXISTS `assignment_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `assignment_attempt` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `submission_id` bigint(20) NOT NULL,
  `attempt_no` int(11) NOT NULL,
  `request_key` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `answer_text` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUBMITTED',
  `late` tinyint(4) NOT NULL,
  `submitted_at` datetime NOT NULL,
  `max_score` int(11) NOT NULL,
  `score` int(11) DEFAULT NULL,
  `feedback` text COLLATE utf8mb4_unicode_ci,
  `reviewed_by` bigint(20) DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attempt_number` (`submission_id`,`attempt_no`),
  UNIQUE KEY `uk_attempt_request` (`submission_id`,`request_key`),
  KEY `reviewed_by` (`reviewed_by`),
  CONSTRAINT `assignment_attempt_ibfk_1` FOREIGN KEY (`submission_id`) REFERENCES `assignment_submission` (`id`),
  CONSTRAINT `assignment_attempt_ibfk_2` FOREIGN KEY (`reviewed_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assignment_attempt`
--

LOCK TABLES `assignment_attempt` WRITE;
/*!40000 ALTER TABLE `assignment_attempt` DISABLE KEYS */;
INSERT INTO `assignment_attempt` VALUES (20,20,1,'6ebec8e8-ab3e-42d3-821d-7ae8f690b79f','11111111','GRADED',0,'2026-10-22 16:07:12',100,66,'great',3,'2026-10-22 16:08:07');
/*!40000 ALTER TABLE `assignment_attempt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `assignment_file`
--

DROP TABLE IF EXISTS `assignment_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `assignment_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `submission_id` bigint(20) NOT NULL,
  `attempt_no` int(11) DEFAULT NULL,
  `storage_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint(20) NOT NULL,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `storage_key` (`storage_key`),
  KEY `idx_assignment_file` (`submission_id`,`attempt_no`,`deleted`),
  CONSTRAINT `assignment_file_ibfk_1` FOREIGN KEY (`submission_id`) REFERENCES `assignment_submission` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assignment_file`
--

LOCK TABLES `assignment_file` WRITE;
/*!40000 ALTER TABLE `assignment_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `assignment_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `assignment_submission`
--

DROP TABLE IF EXISTS `assignment_submission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `assignment_submission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `answer_text` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(12) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `version` int(11) NOT NULL DEFAULT '0',
  `attempt_no` int(11) NOT NULL DEFAULT '0',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_student` (`assignment_id`,`student_id`),
  KEY `student_id` (`student_id`),
  CONSTRAINT `assignment_submission_ibfk_1` FOREIGN KEY (`assignment_id`) REFERENCES `assignment` (`id`),
  CONSTRAINT `assignment_submission_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assignment_submission`
--

LOCK TABLES `assignment_submission` WRITE;
/*!40000 ALTER TABLE `assignment_submission` DISABLE KEYS */;
INSERT INTO `assignment_submission` VALUES (20,35,2,'11111111','GRADED',3,1,'2026-10-22 08:08:06');
/*!40000 ALTER TABLE `assignment_submission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','auth schema','SQL','V1__auth_schema.sql',1936483908,'upself_migrate','2026-10-21 07:54:32',88,1),(2,'2','auth roles menus','SQL','V2__auth_roles_menus.sql',-338372908,'upself_migrate','2026-10-21 07:54:32',14,1),(3,'3','learning and english permissions','SQL','V3__learning_and_english_permissions.sql',-93853078,'upself_migrate','2026-10-21 09:50:21',204,1),(4,'4','assignments','SQL','V4__assignments.sql',-1868119746,'upself_migrate','2026-10-22 07:27:00',156,1),(5,'5','weekly mood','SQL','V5__weekly_mood.sql',361193277,'upself_migrate','2026-10-22 08:36:12',50,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `learning_chapter`
--

DROP TABLE IF EXISTS `learning_chapter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `learning_chapter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(20) NOT NULL,
  `title` varchar(150) NOT NULL,
  `body` text NOT NULL,
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `status` varchar(10) NOT NULL DEFAULT 'DRAFT',
  `language_code` varchar(35) NOT NULL DEFAULT 'en',
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chapter_course` (`course_id`,`deleted`,`status`,`sort_order`),
  CONSTRAINT `learning_chapter_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `learning_course` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `learning_chapter`
--

LOCK TABLES `learning_chapter` WRITE;
/*!40000 ALTER TABLE `learning_chapter` DISABLE KEYS */;
INSERT INTO `learning_chapter` VALUES (8,42,'A calm start','Take a breath. Read the short introduction, then watch the video. You can pause whenever you need.',1,'PUBLISHED','en',0,'2026-10-22 04:15:53'),(19,42,'egg','',1,'DRAFT','en',0,'2026-10-22 04:50:35'),(20,95,'egg','haha',2,'PUBLISHED','en',0,'2026-10-22 04:56:44');
/*!40000 ALTER TABLE `learning_chapter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `learning_course`
--

DROP TABLE IF EXISTS `learning_course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `learning_course` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(150) NOT NULL,
  `description` text NOT NULL,
  `language_code` varchar(35) NOT NULL DEFAULT 'en',
  `mentor_id` bigint(20) DEFAULT NULL,
  `status` varchar(10) NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint(20) NOT NULL,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_mentor` (`mentor_id`,`status`),
  KEY `created_by` (`created_by`),
  CONSTRAINT `learning_course_ibfk_1` FOREIGN KEY (`mentor_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `learning_course_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=166 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `learning_course`
--

LOCK TABLES `learning_course` WRITE;
/*!40000 ALTER TABLE `learning_course` DISABLE KEYS */;
INSERT INTO `learning_course` VALUES (42,'Getting started with UP-SELF','Welcome! Watch, pause and explore at your own pace.','en',3,'PUBLISHED',4,0,'2026-10-22 04:15:45','2026-10-22 04:53:17'),(95,'1','','en',3,'PUBLISHED',1,0,'2026-10-22 04:56:21','2026-10-22 04:57:23');
/*!40000 ALTER TABLE `learning_course` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `learning_enrollment`
--

DROP TABLE IF EXISTS `learning_enrollment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `learning_enrollment` (
  `course_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `active` tinyint(4) NOT NULL DEFAULT '1',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`course_id`,`student_id`),
  KEY `idx_enrollment_student` (`student_id`,`active`),
  CONSTRAINT `learning_enrollment_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `learning_course` (`id`),
  CONSTRAINT `learning_enrollment_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `learning_enrollment`
--

LOCK TABLES `learning_enrollment` WRITE;
/*!40000 ALTER TABLE `learning_enrollment` DISABLE KEYS */;
INSERT INTO `learning_enrollment` VALUES (42,2,1,'2026-10-22 04:15:48'),(95,92,1,'2026-10-22 04:57:07');
/*!40000 ALTER TABLE `learning_enrollment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `learning_post`
--

DROP TABLE IF EXISTS `learning_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `learning_post` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(20) NOT NULL,
  `title` varchar(150) NOT NULL,
  `body` text NOT NULL,
  `type` varchar(15) NOT NULL DEFAULT 'ANNOUNCEMENT',
  `meeting_url` varchar(2000) DEFAULT NULL,
  `meeting_starts_at` varchar(40) DEFAULT NULL,
  `language_code` varchar(35) NOT NULL DEFAULT 'en',
  `status` varchar(10) NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint(20) NOT NULL,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_course` (`course_id`,`deleted`,`status`),
  KEY `created_by` (`created_by`),
  CONSTRAINT `learning_post_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `learning_course` (`id`),
  CONSTRAINT `learning_post_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `learning_post`
--

LOCK TABLES `learning_post` WRITE;
/*!40000 ALTER TABLE `learning_post` DISABLE KEYS */;
INSERT INTO `learning_post` VALUES (9,42,'Welcome to our learning space','Take your time. You can ask your mentor for help.','ANNOUNCEMENT',NULL,NULL,'en','PUBLISHED',3,0,'2026-10-22 04:15:57','2026-10-22 04:15:57'),(10,42,'Practice meeting link (example only)','This is a sample link for trying the interface. Your mentor will share the real meeting link here.','MEETING','https://example.org/up-self-meeting','2026-11-01T10:00:00+08:00','en','PUBLISHED',3,0,'2026-10-22 04:15:58','2026-10-22 04:28:43');
/*!40000 ALTER TABLE `learning_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `learning_resource`
--

DROP TABLE IF EXISTS `learning_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `learning_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `chapter_id` bigint(20) NOT NULL,
  `kind` varchar(10) NOT NULL,
  `title` varchar(150) NOT NULL,
  `storage_key` varchar(64) NOT NULL,
  `original_name` varchar(150) NOT NULL,
  `mime_type` varchar(80) NOT NULL,
  `file_size` bigint(20) NOT NULL,
  `alt_text` varchar(500) DEFAULT NULL,
  `caption_resource_id` bigint(20) DEFAULT NULL,
  `transcript` text,
  `language_code` varchar(35) NOT NULL DEFAULT 'en',
  `status` varchar(10) NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint(20) NOT NULL,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `storage_key` (`storage_key`),
  KEY `idx_resource_chapter` (`chapter_id`,`deleted`,`status`),
  KEY `idx_caption_reference` (`caption_resource_id`),
  KEY `created_by` (`created_by`),
  CONSTRAINT `learning_resource_ibfk_1` FOREIGN KEY (`chapter_id`) REFERENCES `learning_chapter` (`id`),
  CONSTRAINT `learning_resource_ibfk_2` FOREIGN KEY (`caption_resource_id`) REFERENCES `learning_resource` (`id`),
  CONSTRAINT `learning_resource_ibfk_3` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `learning_resource`
--

LOCK TABLES `learning_resource` WRITE;
/*!40000 ALTER TABLE `learning_resource` DISABLE KEYS */;
INSERT INTO `learning_resource` VALUES (15,8,'IMAGE','A gentle reminder','4430cf5e5ec546df89a2ff5fcebe4bc4.png','lesson.png','image/png',13364,'Learning reminder: pause, breathe, and take one small step.',NULL,'','en','PUBLISHED',3,0,'2026-10-22 04:15:55'),(16,8,'CAPTION','English captions','ff227032ce984a12a04720aac829152c.vtt','lesson.vtt','text/vtt;charset=UTF-8',109,'',NULL,'','en','PUBLISHED',3,0,'2026-10-22 04:15:53'),(17,8,'VIDEO','Your first small step','92844e01adff4bbc93e73fef036e1077.mp4','lesson.mp4','video/mp4',90242,'',16,'Take one small step. Learn at your own pace.','en','PUBLISHED',3,0,'2026-10-22 04:15:53');
/*!40000 ALTER TABLE `learning_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mood_entry`
--

DROP TABLE IF EXISTS `mood_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mood_entry` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL,
  `week_start` date NOT NULL,
  `mood_level` tinyint(4) NOT NULL,
  `private_note` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mood_student_week` (`student_id`,`week_start`),
  CONSTRAINT `mood_entry_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `mood_notice_ack` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mood_entry`
--

LOCK TABLES `mood_entry` WRITE;
/*!40000 ALTER TABLE `mood_entry` DISABLE KEYS */;
INSERT INTO `mood_entry` VALUES (53,2,'2026-10-19',2,'','2026-10-22 09:09:19');
/*!40000 ALTER TABLE `mood_entry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mood_notice_ack`
--

DROP TABLE IF EXISTS `mood_notice_ack`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mood_notice_ack` (
  `student_id` bigint(20) NOT NULL,
  `notice_version` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `accepted_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`student_id`),
  CONSTRAINT `mood_notice_ack_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mood_notice_ack`
--

LOCK TABLES `mood_notice_ack` WRITE;
/*!40000 ALTER TABLE `mood_notice_ack` DISABLE KEYS */;
INSERT INTO `mood_notice_ack` VALUES (2,'2026-10-22-v1','2026-10-22 09:09:06');
/*!40000 ALTER TABLE `mood_notice_ack` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_audit_log`
--

DROP TABLE IF EXISTS `sys_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `actor_id` bigint(20) DEFAULT NULL,
  `action` varchar(60) NOT NULL,
  `target` varchar(100) DEFAULT NULL,
  `result` varchar(20) NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1334 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_audit_log`
--

LOCK TABLES `sys_audit_log` WRITE;
/*!40000 ALTER TABLE `sys_audit_log` DISABLE KEYS */;
INSERT INTO `sys_audit_log` VALUES (1,8,'LOGIN','verify_57de8f06s','SUCCESS','2026-10-21 08:03:36'),(2,11,'LOGIN','verify_b8def756s','SUCCESS','2026-10-21 08:05:28'),(3,12,'LOGIN','verify_b8def756m','SUCCESS','2026-10-21 08:05:28'),(4,13,'LOGIN','verify_b8def756a','SUCCESS','2026-10-21 08:05:28'),(5,13,'USER_CREATE','14','SUCCESS','2026-10-21 08:05:28'),(6,12,'LOGIN_TOTP','verify_b8def756m','FAIL','2026-10-21 08:05:29'),(7,13,'USER_UPDATE','11','SUCCESS','2026-10-21 08:05:29'),(8,NULL,'LOGIN','verify_b8def756s','FAIL','2026-10-21 08:05:29'),(9,13,'USER_UPDATE','11','SUCCESS','2026-10-21 08:05:29'),(10,11,'LOGIN','verify_b8def756s','SUCCESS','2026-10-21 08:05:29'),(11,13,'PASSWORD_RESET','11','SUCCESS','2026-10-21 08:05:29'),(12,NULL,'LOGIN','verify_b8def756s','FAIL','2026-10-21 08:05:29'),(13,11,'LOGIN','verify_b8def756s','SUCCESS','2026-10-21 08:05:29'),(14,11,'LOGIN','verify_b8def756s','SUCCESS','2026-10-21 08:05:29'),(15,11,'PASSWORD_CHANGE','11','SUCCESS','2026-10-21 08:05:30'),(16,NULL,'LOGIN','verify_b8def756locked','FAIL','2026-10-21 08:05:30'),(17,NULL,'LOGIN','verify_b8def756locked','FAIL','2026-10-21 08:05:30'),(18,NULL,'LOGIN','verify_b8def756locked','FAIL','2026-10-21 08:05:30'),(19,NULL,'LOGIN','verify_b8def756locked','FAIL','2026-10-21 08:05:30'),(20,NULL,'LOGIN','verify_b8def756locked','FAIL','2026-10-21 08:05:30'),(21,2,'LOGIN','student01','SUCCESS','2026-10-21 08:17:23'),(22,16,'LOGIN','verify_ad0b86f7s','SUCCESS','2026-10-21 08:25:26'),(23,17,'LOGIN','verify_ad0b86f7m','SUCCESS','2026-10-21 08:25:27'),(24,18,'LOGIN','verify_ad0b86f7a','SUCCESS','2026-10-21 08:25:27'),(25,18,'USER_CREATE','19','SUCCESS','2026-10-21 08:25:28'),(26,17,'LOGIN_TOTP','verify_ad0b86f7m','FAIL','2026-10-21 08:25:29'),(27,18,'USER_UPDATE','16','SUCCESS','2026-10-21 08:25:29'),(28,NULL,'LOGIN','verify_ad0b86f7s','FAIL','2026-10-21 08:25:29'),(29,18,'USER_UPDATE','16','SUCCESS','2026-10-21 08:25:29'),(30,16,'LOGIN','verify_ad0b86f7s','SUCCESS','2026-10-21 08:25:29'),(31,18,'PASSWORD_RESET','16','SUCCESS','2026-10-21 08:25:29'),(32,NULL,'LOGIN','verify_ad0b86f7s','FAIL','2026-10-21 08:25:29'),(33,16,'LOGIN','verify_ad0b86f7s','SUCCESS','2026-10-21 08:25:30'),(34,16,'LOGIN','verify_ad0b86f7s','SUCCESS','2026-10-21 08:25:30'),(35,16,'PASSWORD_CHANGE','16','SUCCESS','2026-10-21 08:25:30'),(36,NULL,'LOGIN','verify_ad0b86f7locked','FAIL','2026-10-21 08:25:30'),(37,NULL,'LOGIN','verify_ad0b86f7locked','FAIL','2026-10-21 08:25:31'),(38,NULL,'LOGIN','verify_ad0b86f7locked','FAIL','2026-10-21 08:25:31'),(39,NULL,'LOGIN','verify_ad0b86f7locked','FAIL','2026-10-21 08:25:31'),(40,NULL,'LOGIN','verify_ad0b86f7locked','FAIL','2026-10-21 08:25:31'),(41,NULL,'LOGIN','verify_ad0b86f7rate','FAIL','2026-10-21 08:25:31'),(42,NULL,'LOGIN','verify_ad0b86f7rate','FAIL','2026-10-21 08:25:31'),(43,NULL,'LOGIN','verify_ad0b86f7rate','FAIL','2026-10-21 08:25:31'),(44,NULL,'LOGIN','verify_ad0b86f7rate','FAIL','2026-10-21 08:25:31'),(45,NULL,'LOGIN','verify_ad0b86f7rate','FAIL','2026-10-21 08:25:32'),(46,21,'LOGIN','upself_ui_verify_20261021','SUCCESS','2026-10-21 08:33:38'),(47,4,'LOGIN','admin01','SUCCESS','2026-10-21 09:10:47'),(48,22,'LOGIN','verify_4d4df0c0s','SUCCESS','2026-10-21 09:17:40'),(49,23,'LOGIN','verify_4d4df0c0m','SUCCESS','2026-10-21 09:17:41'),(50,24,'LOGIN','verify_4d4df0c0a','SUCCESS','2026-10-21 09:17:41'),(51,24,'USER_CREATE','25','SUCCESS','2026-10-21 09:17:41'),(52,24,'USER_UPDATE','22','SUCCESS','2026-10-21 09:17:41'),(53,NULL,'LOGIN','verify_4d4df0c0s','FAIL','2026-10-21 09:17:41'),(54,24,'USER_UPDATE','22','SUCCESS','2026-10-21 09:17:41'),(55,22,'LOGIN','verify_4d4df0c0s','SUCCESS','2026-10-21 09:17:42'),(56,24,'PASSWORD_RESET','22','SUCCESS','2026-10-21 09:17:42'),(57,NULL,'LOGIN','verify_4d4df0c0s','FAIL','2026-10-21 09:17:42'),(58,22,'LOGIN','verify_4d4df0c0s','SUCCESS','2026-10-21 09:17:42'),(59,22,'LOGIN','verify_4d4df0c0s','SUCCESS','2026-10-21 09:17:42'),(60,22,'PASSWORD_CHANGE','22','SUCCESS','2026-10-21 09:17:42'),(61,NULL,'LOGIN','verify_4d4df0c0locked','FAIL','2026-10-21 09:17:42'),(62,NULL,'LOGIN','verify_4d4df0c0locked','FAIL','2026-10-21 09:17:42'),(63,NULL,'LOGIN','verify_4d4df0c0locked','FAIL','2026-10-21 09:17:42'),(64,NULL,'LOGIN','verify_4d4df0c0locked','FAIL','2026-10-21 09:17:42'),(65,NULL,'LOGIN','verify_4d4df0c0locked','FAIL','2026-10-21 09:17:42'),(66,NULL,'LOGIN','verify_4d4df0c0rate','FAIL','2026-10-21 09:17:42'),(67,NULL,'LOGIN','verify_4d4df0c0rate','FAIL','2026-10-21 09:17:42'),(68,NULL,'LOGIN','verify_4d4df0c0rate','FAIL','2026-10-21 09:17:43'),(69,NULL,'LOGIN','verify_4d4df0c0rate','FAIL','2026-10-21 09:17:43'),(70,NULL,'LOGIN','verify_4d4df0c0rate','FAIL','2026-10-21 09:17:43'),(71,2,'LOGIN','student01','SUCCESS','2026-10-21 09:20:31'),(72,3,'LOGIN','mentor01','SUCCESS','2026-10-21 09:20:31'),(73,4,'LOGIN','admin01','SUCCESS','2026-10-21 09:20:31'),(74,4,'LOGIN','admin01','SUCCESS','2026-10-21 09:23:03'),(75,2,'LOGIN','student01','SUCCESS','2026-10-21 09:23:54'),(76,3,'LOGIN','mentor01','SUCCESS','2026-10-21 09:24:31'),(77,1,'LOGIN','maintenance','SUCCESS','2026-10-21 09:24:57'),(78,4,'LOGIN','admin01','SUCCESS','2026-10-21 09:25:30'),(79,27,'LOGIN','learning_check_f7c92cd0admin','SUCCESS','2026-10-21 09:50:36'),(80,28,'LOGIN','learning_check_f7c92cd0mentor','SUCCESS','2026-10-21 09:50:36'),(81,29,'LOGIN','learning_check_f7c92cd0othermentor','SUCCESS','2026-10-21 09:50:37'),(82,30,'LOGIN','learning_check_f7c92cd0student','SUCCESS','2026-10-21 09:50:37'),(83,31,'LOGIN','learning_check_f7c92cd0otherstudent','SUCCESS','2026-10-21 09:50:37'),(84,27,'COURSE_CREATE','1','SUCCESS','2026-10-21 09:50:38'),(85,27,'COURSE_CREATE','2','SUCCESS','2026-10-21 09:50:38'),(86,27,'COURSE_ASSIGN_MENTOR','1','SUCCESS','2026-10-21 09:50:38'),(87,27,'COURSE_ASSIGN_MENTOR','2','SUCCESS','2026-10-21 09:50:38'),(88,27,'COURSE_ENROLL','1','SUCCESS','2026-10-21 09:50:38'),(89,27,'COURSE_ENROLL','1','SUCCESS','2026-10-21 09:50:38'),(90,28,'CHAPTER_CREATE','1','SUCCESS','2026-10-21 09:50:38'),(91,28,'CHAPTER_CREATE','2','SUCCESS','2026-10-21 09:50:38'),(92,27,'COURSE_STATE','1','SUCCESS','2026-10-21 09:50:38'),(93,28,'RESOURCE_UPLOAD','1','SUCCESS','2026-10-21 09:50:39'),(94,28,'RESOURCE_UPLOAD','2','SUCCESS','2026-10-21 09:50:39'),(95,28,'RESOURCE_UPLOAD','3','SUCCESS','2026-10-21 09:50:39'),(96,28,'POST_CREATE','1','SUCCESS','2026-10-21 09:50:39'),(97,28,'POST_CREATE','2','SUCCESS','2026-10-21 09:50:39'),(98,27,'POST_OFFLINE','1','SUCCESS','2026-10-21 09:50:39'),(99,28,'POST_EDIT','1','SUCCESS','2026-10-21 09:50:39'),(100,27,'COURSE_ASSIGN_MENTOR','1','SUCCESS','2026-10-21 09:50:39'),(101,29,'CHAPTER_EDIT','1','SUCCESS','2026-10-21 09:50:40'),(102,27,'COURSE_UNENROLL','1','SUCCESS','2026-10-21 09:50:40'),(103,27,'COURSE_ENROLL','1','SUCCESS','2026-10-21 09:50:40'),(104,27,'COURSE_STATE','1','SUCCESS','2026-10-21 09:50:40'),(105,27,'COURSE_STATE','1','SUCCESS','2026-10-21 09:50:40'),(106,27,'COURSE_STATE','1','SUCCESS','2026-10-21 09:50:40'),(107,32,'LOGIN','verify_d61dbfd0s','SUCCESS','2026-10-21 09:53:20'),(108,33,'LOGIN','verify_d61dbfd0m','SUCCESS','2026-10-21 09:53:20'),(109,34,'LOGIN','verify_d61dbfd0a','SUCCESS','2026-10-21 09:53:20'),(110,34,'USER_CREATE','35','SUCCESS','2026-10-21 09:53:21'),(111,34,'USER_UPDATE','32','SUCCESS','2026-10-21 09:53:21'),(112,NULL,'LOGIN','verify_d61dbfd0s','FAIL','2026-10-21 09:53:21'),(113,34,'USER_UPDATE','32','SUCCESS','2026-10-21 09:53:21'),(114,32,'LOGIN','verify_d61dbfd0s','SUCCESS','2026-10-21 09:53:21'),(115,34,'PASSWORD_RESET','32','SUCCESS','2026-10-21 09:53:22'),(116,NULL,'LOGIN','verify_d61dbfd0s','FAIL','2026-10-21 09:53:22'),(117,32,'LOGIN','verify_d61dbfd0s','SUCCESS','2026-10-21 09:53:22'),(118,32,'LOGIN','verify_d61dbfd0s','SUCCESS','2026-10-21 09:53:22'),(119,32,'PASSWORD_CHANGE','32','SUCCESS','2026-10-21 09:53:22'),(120,NULL,'LOGIN','verify_d61dbfd0locked','FAIL','2026-10-21 09:53:22'),(121,NULL,'LOGIN','verify_d61dbfd0locked','FAIL','2026-10-21 09:53:22'),(122,NULL,'LOGIN','verify_d61dbfd0locked','FAIL','2026-10-21 09:53:23'),(123,NULL,'LOGIN','verify_d61dbfd0locked','FAIL','2026-10-21 09:53:23'),(124,NULL,'LOGIN','verify_d61dbfd0locked','FAIL','2026-10-21 09:53:23'),(125,NULL,'LOGIN','verify_d61dbfd0rate','FAIL','2026-10-21 09:53:23'),(126,NULL,'LOGIN','verify_d61dbfd0rate','FAIL','2026-10-21 09:53:23'),(127,NULL,'LOGIN','verify_d61dbfd0rate','FAIL','2026-10-21 09:53:23'),(128,NULL,'LOGIN','verify_d61dbfd0rate','FAIL','2026-10-21 09:53:23'),(129,NULL,'LOGIN','verify_d61dbfd0rate','FAIL','2026-10-21 09:53:23'),(130,2,'LOGIN','student01','SUCCESS','2026-10-21 10:02:02'),(131,3,'LOGIN','mentor01','SUCCESS','2026-10-21 10:02:03'),(132,1,'LOGIN','maintenance','SUCCESS','2026-10-21 10:06:49'),(133,1,'PASSWORD_RESET','4','SUCCESS','2026-10-21 10:06:49'),(134,4,'LOGIN','admin01','SUCCESS','2026-10-21 10:06:49'),(135,4,'COURSE_CREATE','3','SUCCESS','2026-10-21 10:06:49'),(136,4,'COURSE_EDIT','3','SUCCESS','2026-10-21 10:06:49'),(137,4,'COURSE_ASSIGN_MENTOR','3','SUCCESS','2026-10-21 10:06:49'),(138,4,'COURSE_ENROLL','3','SUCCESS','2026-10-21 10:06:49'),(139,4,'COURSE_STATE','3','SUCCESS','2026-10-21 10:06:49'),(140,4,'COURSE_UNENROLL','3','SUCCESS','2026-10-21 10:06:50'),(141,4,'COURSE_STATE','3','SUCCESS','2026-10-21 10:06:50'),(142,2,'LOGIN','student01','SUCCESS','2026-10-21 10:10:21'),(143,3,'LOGIN','mentor01','SUCCESS','2026-10-21 10:12:07'),(144,4,'LOGIN','admin01','SUCCESS','2026-10-22 03:50:38'),(145,3,'LOGIN','mentor01','SUCCESS','2026-10-22 03:50:39'),(146,2,'LOGIN','student01','SUCCESS','2026-10-22 03:50:39'),(147,4,'LOGIN','admin01','SUCCESS','2026-10-22 03:50:41'),(148,4,'COURSE_CREATE','1','SUCCESS','2026-10-22 03:50:43'),(149,4,'COURSE_EDIT','1','SUCCESS','2026-10-22 03:50:45'),(150,4,'COURSE_ASSIGN_MENTOR','1','SUCCESS','2026-10-22 03:50:46'),(151,4,'COURSE_ENROLL','1','SUCCESS','2026-10-22 03:50:47'),(152,4,'COURSE_STATE','1','SUCCESS','2026-10-22 03:50:47'),(153,3,'LOGIN','mentor01','SUCCESS','2026-10-22 03:50:50'),(154,3,'CHAPTER_CREATE','1','SUCCESS','2026-10-22 03:50:51'),(155,3,'CHAPTER_EDIT','1','SUCCESS','2026-10-22 03:50:51'),(156,3,'RESOURCE_UPLOAD','1','SUCCESS','2026-10-22 03:50:52'),(157,3,'RESOURCE_UPLOAD','2','SUCCESS','2026-10-22 03:50:53'),(158,3,'RESOURCE_UPLOAD','3','SUCCESS','2026-10-22 03:50:54'),(159,3,'RESOURCE_EDIT','1','SUCCESS','2026-10-22 03:50:55'),(160,3,'POST_CREATE','1','SUCCESS','2026-10-22 03:50:57'),(161,3,'POST_CREATE','2','SUCCESS','2026-10-22 03:50:58'),(162,2,'LOGIN','student01','SUCCESS','2026-10-22 03:51:00'),(163,4,'LOGIN','admin01','SUCCESS','2026-10-22 03:52:49'),(164,3,'LOGIN','mentor01','SUCCESS','2026-10-22 03:52:49'),(165,2,'LOGIN','student01','SUCCESS','2026-10-22 03:52:50'),(166,4,'LOGIN','admin01','SUCCESS','2026-10-22 03:52:51'),(167,4,'COURSE_CREATE','2','SUCCESS','2026-10-22 03:52:53'),(168,4,'COURSE_EDIT','2','SUCCESS','2026-10-22 03:52:55'),(169,4,'COURSE_ASSIGN_MENTOR','2','SUCCESS','2026-10-22 03:52:56'),(170,4,'COURSE_ENROLL','2','SUCCESS','2026-10-22 03:52:57'),(171,4,'COURSE_STATE','2','SUCCESS','2026-10-22 03:52:57'),(172,3,'LOGIN','mentor01','SUCCESS','2026-10-22 03:52:59'),(173,3,'CHAPTER_CREATE','2','SUCCESS','2026-10-22 03:53:01'),(174,3,'CHAPTER_EDIT','2','SUCCESS','2026-10-22 03:53:01'),(175,3,'RESOURCE_UPLOAD','4','SUCCESS','2026-10-22 03:53:01'),(176,3,'RESOURCE_UPLOAD','5','SUCCESS','2026-10-22 03:53:01'),(177,3,'RESOURCE_UPLOAD','6','SUCCESS','2026-10-22 03:53:02'),(178,4,'LOGIN','admin01','SUCCESS','2026-10-22 03:55:06'),(179,3,'LOGIN','mentor01','SUCCESS','2026-10-22 03:55:07'),(180,2,'LOGIN','student01','SUCCESS','2026-10-22 03:55:07'),(181,4,'LOGIN','admin01','SUCCESS','2026-10-22 03:55:08'),(182,4,'COURSE_CREATE','3','SUCCESS','2026-10-22 03:55:10'),(183,4,'COURSE_EDIT','3','SUCCESS','2026-10-22 03:55:11'),(184,4,'COURSE_ASSIGN_MENTOR','3','SUCCESS','2026-10-22 03:55:12'),(185,4,'COURSE_ENROLL','3','SUCCESS','2026-10-22 03:55:13'),(186,4,'COURSE_STATE','3','SUCCESS','2026-10-22 03:55:13'),(187,3,'LOGIN','mentor01','SUCCESS','2026-10-22 03:55:16'),(188,3,'CHAPTER_CREATE','3','SUCCESS','2026-10-22 03:55:16'),(189,3,'CHAPTER_EDIT','3','SUCCESS','2026-10-22 03:55:17'),(190,3,'RESOURCE_UPLOAD','7','SUCCESS','2026-10-22 03:55:18'),(191,3,'RESOURCE_UPLOAD','8','SUCCESS','2026-10-22 03:55:18'),(192,3,'RESOURCE_UPLOAD','9','SUCCESS','2026-10-22 03:55:18'),(193,3,'RESOURCE_EDIT','7','SUCCESS','2026-10-22 03:55:20'),(194,3,'POST_CREATE','3','SUCCESS','2026-10-22 03:55:22'),(195,3,'POST_CREATE','4','SUCCESS','2026-10-22 03:55:23'),(196,2,'LOGIN','student01','SUCCESS','2026-10-22 03:55:26'),(197,4,'COURSE_UNENROLL','3','SUCCESS','2026-10-22 03:55:34'),(198,4,'COURSE_ENROLL','3','SUCCESS','2026-10-22 03:55:35'),(199,4,'COURSE_STATE','3','SUCCESS','2026-10-22 03:55:36'),(200,4,'COURSE_STATE','3','SUCCESS','2026-10-22 03:55:36'),(201,4,'COURSE_STATE','3','SUCCESS','2026-10-22 03:55:37'),(202,4,'COURSE_STATE','3','SUCCESS','2026-10-22 03:55:38'),(203,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:00:24'),(204,4,'COURSE_CREATE','4','SUCCESS','2026-10-22 04:00:24'),(205,4,'COURSE_CREATE','5','SUCCESS','2026-10-22 04:00:24'),(206,4,'COURSE_CREATE','6','SUCCESS','2026-10-22 04:00:24'),(207,4,'COURSE_CREATE','7','SUCCESS','2026-10-22 04:00:24'),(208,4,'COURSE_CREATE','8','SUCCESS','2026-10-22 04:00:24'),(209,4,'COURSE_CREATE','9','SUCCESS','2026-10-22 04:00:24'),(210,4,'COURSE_CREATE','10','SUCCESS','2026-10-22 04:00:24'),(211,4,'COURSE_CREATE','11','SUCCESS','2026-10-22 04:00:24'),(212,4,'COURSE_CREATE','12','SUCCESS','2026-10-22 04:00:24'),(213,4,'COURSE_CREATE','13','SUCCESS','2026-10-22 04:00:24'),(214,4,'COURSE_CREATE','14','SUCCESS','2026-10-22 04:00:24'),(215,4,'COURSE_CREATE','15','SUCCESS','2026-10-22 04:00:24'),(216,4,'USER_CREATE','5','SUCCESS','2026-10-22 04:00:24'),(217,4,'USER_CREATE','6','SUCCESS','2026-10-22 04:00:24'),(218,4,'USER_CREATE','7','SUCCESS','2026-10-22 04:00:25'),(219,4,'USER_CREATE','8','SUCCESS','2026-10-22 04:00:25'),(220,4,'USER_CREATE','9','SUCCESS','2026-10-22 04:00:25'),(221,4,'USER_CREATE','10','SUCCESS','2026-10-22 04:00:25'),(222,4,'USER_CREATE','11','SUCCESS','2026-10-22 04:00:25'),(223,4,'USER_CREATE','12','SUCCESS','2026-10-22 04:00:25'),(224,4,'USER_CREATE','13','SUCCESS','2026-10-22 04:00:26'),(225,4,'USER_CREATE','14','SUCCESS','2026-10-22 04:00:26'),(226,4,'USER_CREATE','15','SUCCESS','2026-10-22 04:00:26'),(227,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:00:27'),(228,4,'COURSE_ASSIGN_MENTOR','4','SUCCESS','2026-10-22 04:00:30'),(229,4,'USER_UPDATE','5','SUCCESS','2026-10-22 04:00:33'),(230,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:00:35'),(231,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:04:57'),(232,4,'COURSE_CREATE','16','SUCCESS','2026-10-22 04:04:57'),(233,4,'COURSE_CREATE','17','SUCCESS','2026-10-22 04:04:57'),(234,4,'COURSE_CREATE','18','SUCCESS','2026-10-22 04:04:57'),(235,4,'COURSE_CREATE','19','SUCCESS','2026-10-22 04:04:57'),(236,4,'COURSE_CREATE','20','SUCCESS','2026-10-22 04:04:57'),(237,4,'COURSE_CREATE','21','SUCCESS','2026-10-22 04:04:57'),(238,4,'COURSE_CREATE','22','SUCCESS','2026-10-22 04:04:57'),(239,4,'COURSE_CREATE','23','SUCCESS','2026-10-22 04:04:57'),(240,4,'COURSE_CREATE','24','SUCCESS','2026-10-22 04:04:57'),(241,4,'COURSE_CREATE','25','SUCCESS','2026-10-22 04:04:57'),(242,4,'COURSE_CREATE','26','SUCCESS','2026-10-22 04:04:57'),(243,4,'COURSE_CREATE','27','SUCCESS','2026-10-22 04:04:57'),(244,4,'USER_CREATE','16','SUCCESS','2026-10-22 04:04:57'),(245,4,'USER_CREATE','17','SUCCESS','2026-10-22 04:04:57'),(246,4,'USER_CREATE','18','SUCCESS','2026-10-22 04:04:58'),(247,4,'USER_CREATE','19','SUCCESS','2026-10-22 04:04:58'),(248,4,'USER_CREATE','20','SUCCESS','2026-10-22 04:04:58'),(249,4,'USER_CREATE','21','SUCCESS','2026-10-22 04:04:58'),(250,4,'USER_CREATE','22','SUCCESS','2026-10-22 04:04:58'),(251,4,'USER_CREATE','23','SUCCESS','2026-10-22 04:04:58'),(252,4,'USER_CREATE','24','SUCCESS','2026-10-22 04:04:59'),(253,4,'USER_CREATE','25','SUCCESS','2026-10-22 04:04:59'),(254,4,'USER_CREATE','26','SUCCESS','2026-10-22 04:04:59'),(255,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:05:00'),(256,4,'COURSE_ASSIGN_MENTOR','16','SUCCESS','2026-10-22 04:05:02'),(257,4,'USER_UPDATE','16','SUCCESS','2026-10-22 04:05:04'),(258,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:05:05'),(259,3,'CHAPTER_CREATE','4','SUCCESS','2026-10-22 04:05:06'),(260,3,'CHAPTER_EDIT','4','SUCCESS','2026-10-22 04:05:07'),(261,3,'RESOURCE_UPLOAD','10','SUCCESS','2026-10-22 04:05:07'),(262,3,'RESOURCE_DELETE','10','SUCCESS','2026-10-22 04:05:09'),(263,3,'CHAPTER_DELETE','4','SUCCESS','2026-10-22 04:05:09'),(264,3,'POST_CREATE','5','SUCCESS','2026-10-22 04:05:11'),(265,3,'POST_EDIT','5','SUCCESS','2026-10-22 04:05:12'),(266,3,'POST_OFFLINE','5','SUCCESS','2026-10-22 04:05:12'),(267,3,'POST_DELETE','5','SUCCESS','2026-10-22 04:05:13'),(268,1,'LOGIN','maintenance','SUCCESS','2026-10-22 04:05:14'),(269,27,'LOGIN','learning_check_c8e8e846admin','SUCCESS','2026-10-22 04:09:22'),(270,28,'LOGIN','learning_check_c8e8e846mentor','SUCCESS','2026-10-22 04:09:23'),(271,29,'LOGIN','learning_check_c8e8e846othermentor','SUCCESS','2026-10-22 04:09:23'),(272,30,'LOGIN','learning_check_c8e8e846student','SUCCESS','2026-10-22 04:09:24'),(273,31,'LOGIN','learning_check_c8e8e846otherstudent','SUCCESS','2026-10-22 04:09:24'),(274,27,'COURSE_CREATE','28','SUCCESS','2026-10-22 04:09:25'),(275,27,'COURSE_CREATE','29','SUCCESS','2026-10-22 04:09:25'),(276,27,'COURSE_ASSIGN_MENTOR','28','SUCCESS','2026-10-22 04:09:25'),(277,27,'COURSE_ASSIGN_MENTOR','29','SUCCESS','2026-10-22 04:09:25'),(278,27,'COURSE_ENROLL','28','SUCCESS','2026-10-22 04:09:25'),(279,27,'COURSE_ENROLL','28','SUCCESS','2026-10-22 04:09:25'),(280,28,'CHAPTER_CREATE','5','SUCCESS','2026-10-22 04:09:26'),(281,28,'CHAPTER_CREATE','6','SUCCESS','2026-10-22 04:09:26'),(282,27,'COURSE_STATE','28','SUCCESS','2026-10-22 04:09:26'),(283,28,'RESOURCE_UPLOAD','11','SUCCESS','2026-10-22 04:09:26'),(284,28,'RESOURCE_UPLOAD','12','SUCCESS','2026-10-22 04:09:26'),(285,28,'RESOURCE_UPLOAD','13','SUCCESS','2026-10-22 04:09:26'),(286,28,'POST_CREATE','6','SUCCESS','2026-10-22 04:09:27'),(287,28,'POST_CREATE','7','SUCCESS','2026-10-22 04:09:27'),(288,27,'POST_OFFLINE','6','SUCCESS','2026-10-22 04:09:27'),(289,28,'POST_EDIT','6','SUCCESS','2026-10-22 04:09:27'),(290,27,'COURSE_ASSIGN_MENTOR','28','SUCCESS','2026-10-22 04:09:27'),(291,29,'CHAPTER_EDIT','5','SUCCESS','2026-10-22 04:09:27'),(292,27,'COURSE_UNENROLL','28','SUCCESS','2026-10-22 04:09:27'),(293,27,'COURSE_ENROLL','28','SUCCESS','2026-10-22 04:09:27'),(294,27,'COURSE_STATE','28','SUCCESS','2026-10-22 04:09:27'),(295,27,'COURSE_STATE','28','SUCCESS','2026-10-22 04:09:27'),(296,27,'COURSE_STATE','28','SUCCESS','2026-10-22 04:09:27'),(297,32,'LOGIN','verify_7be20357s','SUCCESS','2026-10-22 04:10:25'),(298,33,'LOGIN','verify_7be20357m','SUCCESS','2026-10-22 04:10:26'),(299,34,'LOGIN','verify_7be20357a','SUCCESS','2026-10-22 04:10:26'),(300,34,'USER_CREATE','35','SUCCESS','2026-10-22 04:10:27'),(301,34,'USER_UPDATE','32','SUCCESS','2026-10-22 04:10:27'),(302,NULL,'LOGIN','verify_7be20357s','FAIL','2026-10-22 04:10:27'),(303,34,'USER_UPDATE','32','SUCCESS','2026-10-22 04:10:27'),(304,32,'LOGIN','verify_7be20357s','SUCCESS','2026-10-22 04:10:28'),(305,34,'PASSWORD_RESET','32','SUCCESS','2026-10-22 04:10:28'),(306,NULL,'LOGIN','verify_7be20357s','FAIL','2026-10-22 04:10:28'),(307,32,'LOGIN','verify_7be20357s','SUCCESS','2026-10-22 04:10:28'),(308,32,'LOGIN','verify_7be20357s','SUCCESS','2026-10-22 04:10:28'),(309,32,'PASSWORD_CHANGE','32','SUCCESS','2026-10-22 04:10:29'),(310,NULL,'LOGIN','verify_7be20357locked','FAIL','2026-10-22 04:10:29'),(311,NULL,'LOGIN','verify_7be20357locked','FAIL','2026-10-22 04:10:29'),(312,NULL,'LOGIN','verify_7be20357locked','FAIL','2026-10-22 04:10:29'),(313,NULL,'LOGIN','verify_7be20357locked','FAIL','2026-10-22 04:10:29'),(314,NULL,'LOGIN','verify_7be20357locked','FAIL','2026-10-22 04:10:29'),(315,NULL,'LOGIN','verify_7be20357rate','FAIL','2026-10-22 04:10:30'),(316,NULL,'LOGIN','verify_7be20357rate','FAIL','2026-10-22 04:10:30'),(317,NULL,'LOGIN','verify_7be20357rate','FAIL','2026-10-22 04:10:30'),(318,NULL,'LOGIN','verify_7be20357rate','FAIL','2026-10-22 04:10:30'),(319,NULL,'LOGIN','verify_7be20357rate','FAIL','2026-10-22 04:10:30'),(320,2,'LOGIN','student01','SUCCESS','2026-10-22 04:14:53'),(321,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:15:16'),(322,4,'COURSE_CREATE','30','SUCCESS','2026-10-22 04:15:16'),(323,4,'COURSE_CREATE','31','SUCCESS','2026-10-22 04:15:16'),(324,4,'COURSE_CREATE','32','SUCCESS','2026-10-22 04:15:16'),(325,4,'COURSE_CREATE','33','SUCCESS','2026-10-22 04:15:16'),(326,4,'COURSE_CREATE','34','SUCCESS','2026-10-22 04:15:16'),(327,4,'COURSE_CREATE','35','SUCCESS','2026-10-22 04:15:16'),(328,4,'COURSE_CREATE','36','SUCCESS','2026-10-22 04:15:16'),(329,4,'COURSE_CREATE','37','SUCCESS','2026-10-22 04:15:16'),(330,4,'COURSE_CREATE','38','SUCCESS','2026-10-22 04:15:16'),(331,4,'COURSE_CREATE','39','SUCCESS','2026-10-22 04:15:16'),(332,4,'COURSE_CREATE','40','SUCCESS','2026-10-22 04:15:16'),(333,4,'COURSE_CREATE','41','SUCCESS','2026-10-22 04:15:16'),(334,4,'USER_CREATE','37','SUCCESS','2026-10-22 04:15:17'),(335,4,'USER_CREATE','38','SUCCESS','2026-10-22 04:15:17'),(336,4,'USER_CREATE','39','SUCCESS','2026-10-22 04:15:17'),(337,4,'USER_CREATE','40','SUCCESS','2026-10-22 04:15:17'),(338,4,'USER_CREATE','41','SUCCESS','2026-10-22 04:15:17'),(339,4,'USER_CREATE','42','SUCCESS','2026-10-22 04:15:18'),(340,4,'USER_CREATE','43','SUCCESS','2026-10-22 04:15:18'),(341,4,'USER_CREATE','44','SUCCESS','2026-10-22 04:15:18'),(342,4,'USER_CREATE','45','SUCCESS','2026-10-22 04:15:18'),(343,4,'USER_CREATE','46','SUCCESS','2026-10-22 04:15:18'),(344,4,'USER_CREATE','47','SUCCESS','2026-10-22 04:15:18'),(345,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:15:21'),(346,4,'COURSE_ASSIGN_MENTOR','30','SUCCESS','2026-10-22 04:15:24'),(347,4,'USER_UPDATE','37','SUCCESS','2026-10-22 04:15:27'),(348,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:15:28'),(349,3,'CHAPTER_CREATE','7','SUCCESS','2026-10-22 04:15:29'),(350,3,'CHAPTER_EDIT','7','SUCCESS','2026-10-22 04:15:31'),(351,3,'RESOURCE_UPLOAD','14','SUCCESS','2026-10-22 04:15:33'),(352,3,'RESOURCE_DELETE','14','SUCCESS','2026-10-22 04:15:34'),(353,3,'CHAPTER_DELETE','7','SUCCESS','2026-10-22 04:15:35'),(354,3,'POST_CREATE','8','SUCCESS','2026-10-22 04:15:37'),(355,3,'POST_EDIT','8','SUCCESS','2026-10-22 04:15:38'),(356,3,'POST_OFFLINE','8','SUCCESS','2026-10-22 04:15:38'),(357,3,'POST_DELETE','8','SUCCESS','2026-10-22 04:15:39'),(358,1,'LOGIN','maintenance','SUCCESS','2026-10-22 04:15:41'),(359,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:15:42'),(360,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:15:42'),(361,2,'LOGIN','student01','SUCCESS','2026-10-22 04:15:42'),(362,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:15:44'),(363,4,'COURSE_CREATE','42','SUCCESS','2026-10-22 04:15:45'),(364,4,'COURSE_EDIT','42','SUCCESS','2026-10-22 04:15:46'),(365,4,'COURSE_ASSIGN_MENTOR','42','SUCCESS','2026-10-22 04:15:47'),(366,4,'COURSE_ENROLL','42','SUCCESS','2026-10-22 04:15:48'),(367,4,'COURSE_STATE','42','SUCCESS','2026-10-22 04:15:48'),(368,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:15:51'),(369,3,'CHAPTER_CREATE','8','SUCCESS','2026-10-22 04:15:52'),(370,3,'CHAPTER_EDIT','8','SUCCESS','2026-10-22 04:15:53'),(371,3,'RESOURCE_UPLOAD','15','SUCCESS','2026-10-22 04:15:53'),(372,3,'RESOURCE_UPLOAD','16','SUCCESS','2026-10-22 04:15:53'),(373,3,'RESOURCE_UPLOAD','17','SUCCESS','2026-10-22 04:15:53'),(374,3,'RESOURCE_EDIT','15','SUCCESS','2026-10-22 04:15:55'),(375,3,'POST_CREATE','9','SUCCESS','2026-10-22 04:15:57'),(376,3,'POST_CREATE','10','SUCCESS','2026-10-22 04:15:58'),(377,2,'LOGIN','student01','SUCCESS','2026-10-22 04:16:00'),(378,4,'COURSE_UNENROLL','42','SUCCESS','2026-10-22 04:16:15'),(379,4,'COURSE_ENROLL','42','SUCCESS','2026-10-22 04:16:16'),(380,4,'COURSE_STATE','42','SUCCESS','2026-10-22 04:16:16'),(381,4,'COURSE_STATE','42','SUCCESS','2026-10-22 04:16:16'),(382,4,'COURSE_STATE','42','SUCCESS','2026-10-22 04:16:17'),(383,4,'COURSE_STATE','42','SUCCESS','2026-10-22 04:16:17'),(384,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:20:40'),(385,4,'COURSE_CREATE','43','SUCCESS','2026-10-22 04:20:40'),(386,4,'COURSE_CREATE','44','SUCCESS','2026-10-22 04:20:40'),(387,4,'COURSE_CREATE','45','SUCCESS','2026-10-22 04:20:40'),(388,4,'COURSE_CREATE','46','SUCCESS','2026-10-22 04:20:40'),(389,4,'COURSE_CREATE','47','SUCCESS','2026-10-22 04:20:40'),(390,4,'COURSE_CREATE','48','SUCCESS','2026-10-22 04:20:40'),(391,4,'COURSE_CREATE','49','SUCCESS','2026-10-22 04:20:40'),(392,4,'COURSE_CREATE','50','SUCCESS','2026-10-22 04:20:40'),(393,4,'COURSE_CREATE','51','SUCCESS','2026-10-22 04:20:40'),(394,4,'COURSE_CREATE','52','SUCCESS','2026-10-22 04:20:40'),(395,4,'COURSE_CREATE','53','SUCCESS','2026-10-22 04:20:40'),(396,4,'COURSE_CREATE','54','SUCCESS','2026-10-22 04:20:40'),(397,4,'USER_CREATE','48','SUCCESS','2026-10-22 04:20:41'),(398,4,'USER_CREATE','49','SUCCESS','2026-10-22 04:20:41'),(399,4,'USER_CREATE','50','SUCCESS','2026-10-22 04:20:41'),(400,4,'USER_CREATE','51','SUCCESS','2026-10-22 04:20:41'),(401,4,'USER_CREATE','52','SUCCESS','2026-10-22 04:20:41'),(402,4,'USER_CREATE','53','SUCCESS','2026-10-22 04:20:42'),(403,4,'USER_CREATE','54','SUCCESS','2026-10-22 04:20:42'),(404,4,'USER_CREATE','55','SUCCESS','2026-10-22 04:20:42'),(405,4,'USER_CREATE','56','SUCCESS','2026-10-22 04:20:42'),(406,4,'USER_CREATE','57','SUCCESS','2026-10-22 04:20:42'),(407,4,'USER_CREATE','58','SUCCESS','2026-10-22 04:20:42'),(408,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:20:44'),(409,4,'COURSE_ASSIGN_MENTOR','43','SUCCESS','2026-10-22 04:20:46'),(410,4,'USER_UPDATE','48','SUCCESS','2026-10-22 04:20:48'),(411,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:20:49'),(412,3,'CHAPTER_CREATE','9','SUCCESS','2026-10-22 04:20:49'),(413,3,'CHAPTER_EDIT','9','SUCCESS','2026-10-22 04:20:50'),(414,3,'RESOURCE_UPLOAD','18','SUCCESS','2026-10-22 04:20:50'),(415,3,'RESOURCE_DELETE','18','SUCCESS','2026-10-22 04:20:52'),(416,3,'CHAPTER_DELETE','9','SUCCESS','2026-10-22 04:20:53'),(417,3,'POST_CREATE','11','SUCCESS','2026-10-22 04:20:54'),(418,3,'POST_EDIT','11','SUCCESS','2026-10-22 04:20:55'),(419,3,'POST_OFFLINE','11','SUCCESS','2026-10-22 04:20:56'),(420,3,'POST_DELETE','11','SUCCESS','2026-10-22 04:20:57'),(421,1,'LOGIN','maintenance','SUCCESS','2026-10-22 04:20:58'),(422,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:20:59'),(423,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:20:59'),(424,2,'LOGIN','student01','SUCCESS','2026-10-22 04:20:59'),(425,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:21:01'),(426,4,'COURSE_CREATE','55','SUCCESS','2026-10-22 04:21:03'),(427,4,'COURSE_EDIT','55','SUCCESS','2026-10-22 04:21:04'),(428,4,'COURSE_ASSIGN_MENTOR','55','SUCCESS','2026-10-22 04:21:05'),(429,4,'COURSE_ENROLL','55','SUCCESS','2026-10-22 04:21:06'),(430,4,'COURSE_STATE','55','SUCCESS','2026-10-22 04:21:06'),(431,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:21:08'),(432,3,'CHAPTER_CREATE','10','SUCCESS','2026-10-22 04:21:09'),(433,3,'CHAPTER_EDIT','10','SUCCESS','2026-10-22 04:21:10'),(434,3,'RESOURCE_UPLOAD','19','SUCCESS','2026-10-22 04:21:10'),(435,3,'RESOURCE_UPLOAD','20','SUCCESS','2026-10-22 04:21:11'),(436,3,'RESOURCE_UPLOAD','21','SUCCESS','2026-10-22 04:21:11'),(437,3,'RESOURCE_EDIT','19','SUCCESS','2026-10-22 04:21:12'),(438,3,'POST_CREATE','12','SUCCESS','2026-10-22 04:21:14'),(439,3,'POST_CREATE','13','SUCCESS','2026-10-22 04:21:15'),(440,2,'LOGIN','student01','SUCCESS','2026-10-22 04:21:17'),(441,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:29:22'),(442,4,'COURSE_CREATE','56','SUCCESS','2026-10-22 04:29:22'),(443,4,'COURSE_CREATE','57','SUCCESS','2026-10-22 04:29:22'),(444,4,'COURSE_CREATE','58','SUCCESS','2026-10-22 04:29:22'),(445,4,'COURSE_CREATE','59','SUCCESS','2026-10-22 04:29:22'),(446,4,'COURSE_CREATE','60','SUCCESS','2026-10-22 04:29:22'),(447,4,'COURSE_CREATE','61','SUCCESS','2026-10-22 04:29:22'),(448,4,'COURSE_CREATE','62','SUCCESS','2026-10-22 04:29:22'),(449,4,'COURSE_CREATE','63','SUCCESS','2026-10-22 04:29:22'),(450,4,'COURSE_CREATE','64','SUCCESS','2026-10-22 04:29:22'),(451,4,'COURSE_CREATE','65','SUCCESS','2026-10-22 04:29:22'),(452,4,'COURSE_CREATE','66','SUCCESS','2026-10-22 04:29:22'),(453,4,'COURSE_CREATE','67','SUCCESS','2026-10-22 04:29:22'),(454,4,'USER_CREATE','59','SUCCESS','2026-10-22 04:29:22'),(455,4,'USER_CREATE','60','SUCCESS','2026-10-22 04:29:22'),(456,4,'USER_CREATE','61','SUCCESS','2026-10-22 04:29:22'),(457,4,'USER_CREATE','62','SUCCESS','2026-10-22 04:29:22'),(458,4,'USER_CREATE','63','SUCCESS','2026-10-22 04:29:22'),(459,4,'USER_CREATE','64','SUCCESS','2026-10-22 04:29:23'),(460,4,'USER_CREATE','65','SUCCESS','2026-10-22 04:29:23'),(461,4,'USER_CREATE','66','SUCCESS','2026-10-22 04:29:23'),(462,4,'USER_CREATE','67','SUCCESS','2026-10-22 04:29:23'),(463,4,'USER_CREATE','68','SUCCESS','2026-10-22 04:29:23'),(464,4,'USER_CREATE','69','SUCCESS','2026-10-22 04:29:23'),(465,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:29:24'),(466,4,'COURSE_ASSIGN_MENTOR','56','SUCCESS','2026-10-22 04:29:28'),(467,4,'USER_UPDATE','59','SUCCESS','2026-10-22 04:29:29'),(468,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:29:30'),(469,3,'CHAPTER_CREATE','11','SUCCESS','2026-10-22 04:29:31'),(470,3,'CHAPTER_EDIT','11','SUCCESS','2026-10-22 04:29:31'),(471,3,'RESOURCE_UPLOAD','22','SUCCESS','2026-10-22 04:29:32'),(472,3,'RESOURCE_DELETE','22','SUCCESS','2026-10-22 04:29:33'),(473,3,'CHAPTER_DELETE','11','SUCCESS','2026-10-22 04:29:34'),(474,3,'POST_CREATE','14','SUCCESS','2026-10-22 04:29:36'),(475,3,'POST_EDIT','14','SUCCESS','2026-10-22 04:29:37'),(476,3,'POST_OFFLINE','14','SUCCESS','2026-10-22 04:29:38'),(477,3,'POST_DELETE','14','SUCCESS','2026-10-22 04:29:39'),(478,1,'LOGIN','maintenance','SUCCESS','2026-10-22 04:29:39'),(479,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:29:40'),(480,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:29:40'),(481,2,'LOGIN','student01','SUCCESS','2026-10-22 04:29:40'),(482,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:29:42'),(483,4,'COURSE_CREATE','68','SUCCESS','2026-10-22 04:29:44'),(484,4,'COURSE_EDIT','68','SUCCESS','2026-10-22 04:29:45'),(485,4,'COURSE_ASSIGN_MENTOR','68','SUCCESS','2026-10-22 04:29:46'),(486,4,'COURSE_ENROLL','68','SUCCESS','2026-10-22 04:29:46'),(487,4,'COURSE_STATE','68','SUCCESS','2026-10-22 04:29:46'),(488,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:29:49'),(489,3,'CHAPTER_CREATE','12','SUCCESS','2026-10-22 04:29:50'),(490,3,'CHAPTER_EDIT','12','SUCCESS','2026-10-22 04:29:50'),(491,3,'RESOURCE_UPLOAD','23','SUCCESS','2026-10-22 04:29:50'),(492,3,'RESOURCE_UPLOAD','24','SUCCESS','2026-10-22 04:29:51'),(493,3,'RESOURCE_UPLOAD','25','SUCCESS','2026-10-22 04:29:52'),(494,3,'RESOURCE_EDIT','23','SUCCESS','2026-10-22 04:29:53'),(495,3,'POST_CREATE','15','SUCCESS','2026-10-22 04:29:56'),(496,3,'POST_CREATE','16','SUCCESS','2026-10-22 04:29:56'),(497,2,'LOGIN','student01','SUCCESS','2026-10-22 04:29:58'),(498,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:31:41'),(499,4,'COURSE_CREATE','69','SUCCESS','2026-10-22 04:31:41'),(500,4,'COURSE_CREATE','70','SUCCESS','2026-10-22 04:31:41'),(501,4,'COURSE_CREATE','71','SUCCESS','2026-10-22 04:31:41'),(502,4,'COURSE_CREATE','72','SUCCESS','2026-10-22 04:31:41'),(503,4,'COURSE_CREATE','73','SUCCESS','2026-10-22 04:31:41'),(504,4,'COURSE_CREATE','74','SUCCESS','2026-10-22 04:31:41'),(505,4,'COURSE_CREATE','75','SUCCESS','2026-10-22 04:31:41'),(506,4,'COURSE_CREATE','76','SUCCESS','2026-10-22 04:31:41'),(507,4,'COURSE_CREATE','77','SUCCESS','2026-10-22 04:31:41'),(508,4,'COURSE_CREATE','78','SUCCESS','2026-10-22 04:31:41'),(509,4,'COURSE_CREATE','79','SUCCESS','2026-10-22 04:31:41'),(510,4,'COURSE_CREATE','80','SUCCESS','2026-10-22 04:31:41'),(511,4,'USER_CREATE','70','SUCCESS','2026-10-22 04:31:42'),(512,4,'USER_CREATE','71','SUCCESS','2026-10-22 04:31:42'),(513,4,'USER_CREATE','72','SUCCESS','2026-10-22 04:31:42'),(514,4,'USER_CREATE','73','SUCCESS','2026-10-22 04:31:42'),(515,4,'USER_CREATE','74','SUCCESS','2026-10-22 04:31:42'),(516,4,'USER_CREATE','75','SUCCESS','2026-10-22 04:31:42'),(517,4,'USER_CREATE','76','SUCCESS','2026-10-22 04:31:42'),(518,4,'USER_CREATE','77','SUCCESS','2026-10-22 04:31:43'),(519,4,'USER_CREATE','78','SUCCESS','2026-10-22 04:31:43'),(520,4,'USER_CREATE','79','SUCCESS','2026-10-22 04:31:43'),(521,4,'USER_CREATE','80','SUCCESS','2026-10-22 04:31:43'),(522,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:31:45'),(523,4,'COURSE_ASSIGN_MENTOR','69','SUCCESS','2026-10-22 04:31:47'),(524,4,'USER_UPDATE','70','SUCCESS','2026-10-22 04:31:48'),(525,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:31:49'),(526,3,'CHAPTER_CREATE','13','SUCCESS','2026-10-22 04:31:50'),(527,3,'CHAPTER_EDIT','13','SUCCESS','2026-10-22 04:31:50'),(528,3,'RESOURCE_UPLOAD','26','SUCCESS','2026-10-22 04:31:52'),(529,3,'RESOURCE_DELETE','26','SUCCESS','2026-10-22 04:31:53'),(530,3,'CHAPTER_DELETE','13','SUCCESS','2026-10-22 04:31:54'),(531,3,'POST_CREATE','17','SUCCESS','2026-10-22 04:31:56'),(532,3,'POST_EDIT','17','SUCCESS','2026-10-22 04:31:57'),(533,3,'POST_OFFLINE','17','SUCCESS','2026-10-22 04:31:58'),(534,3,'POST_DELETE','17','SUCCESS','2026-10-22 04:31:59'),(535,1,'LOGIN','maintenance','SUCCESS','2026-10-22 04:32:00'),(536,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:32:00'),(537,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:32:00'),(538,2,'LOGIN','student01','SUCCESS','2026-10-22 04:32:01'),(539,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:32:02'),(540,4,'COURSE_CREATE','81','SUCCESS','2026-10-22 04:32:04'),(541,4,'COURSE_EDIT','81','SUCCESS','2026-10-22 04:32:04'),(542,4,'COURSE_ASSIGN_MENTOR','81','SUCCESS','2026-10-22 04:32:05'),(543,4,'COURSE_ENROLL','81','SUCCESS','2026-10-22 04:32:06'),(544,4,'COURSE_STATE','81','SUCCESS','2026-10-22 04:32:06'),(545,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:32:09'),(546,3,'CHAPTER_CREATE','14','SUCCESS','2026-10-22 04:32:09'),(547,3,'CHAPTER_EDIT','14','SUCCESS','2026-10-22 04:32:10'),(548,3,'RESOURCE_UPLOAD','27','SUCCESS','2026-10-22 04:32:10'),(549,3,'RESOURCE_UPLOAD','28','SUCCESS','2026-10-22 04:32:10'),(550,3,'RESOURCE_UPLOAD','29','SUCCESS','2026-10-22 04:32:11'),(551,3,'RESOURCE_EDIT','27','SUCCESS','2026-10-22 04:32:13'),(552,3,'POST_CREATE','18','SUCCESS','2026-10-22 04:32:16'),(553,3,'POST_CREATE','19','SUCCESS','2026-10-22 04:32:16'),(554,2,'LOGIN','student01','SUCCESS','2026-10-22 04:32:19'),(555,3,'CHAPTER_CREATE','15','SUCCESS','2026-10-22 04:32:22'),(556,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:35:05'),(557,4,'COURSE_CREATE','82','SUCCESS','2026-10-22 04:35:05'),(558,4,'COURSE_CREATE','83','SUCCESS','2026-10-22 04:35:05'),(559,4,'COURSE_CREATE','84','SUCCESS','2026-10-22 04:35:05'),(560,4,'COURSE_CREATE','85','SUCCESS','2026-10-22 04:35:05'),(561,4,'COURSE_CREATE','86','SUCCESS','2026-10-22 04:35:05'),(562,4,'COURSE_CREATE','87','SUCCESS','2026-10-22 04:35:05'),(563,4,'COURSE_CREATE','88','SUCCESS','2026-10-22 04:35:05'),(564,4,'COURSE_CREATE','89','SUCCESS','2026-10-22 04:35:05'),(565,4,'COURSE_CREATE','90','SUCCESS','2026-10-22 04:35:05'),(566,4,'COURSE_CREATE','91','SUCCESS','2026-10-22 04:35:05'),(567,4,'COURSE_CREATE','92','SUCCESS','2026-10-22 04:35:05'),(568,4,'COURSE_CREATE','93','SUCCESS','2026-10-22 04:35:05'),(569,4,'USER_CREATE','81','SUCCESS','2026-10-22 04:35:05'),(570,4,'USER_CREATE','82','SUCCESS','2026-10-22 04:35:05'),(571,4,'USER_CREATE','83','SUCCESS','2026-10-22 04:35:06'),(572,4,'USER_CREATE','84','SUCCESS','2026-10-22 04:35:06'),(573,4,'USER_CREATE','85','SUCCESS','2026-10-22 04:35:06'),(574,4,'USER_CREATE','86','SUCCESS','2026-10-22 04:35:06'),(575,4,'USER_CREATE','87','SUCCESS','2026-10-22 04:35:06'),(576,4,'USER_CREATE','88','SUCCESS','2026-10-22 04:35:06'),(577,4,'USER_CREATE','89','SUCCESS','2026-10-22 04:35:06'),(578,4,'USER_CREATE','90','SUCCESS','2026-10-22 04:35:07'),(579,4,'USER_CREATE','91','SUCCESS','2026-10-22 04:35:07'),(580,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:35:08'),(581,4,'COURSE_ASSIGN_MENTOR','82','SUCCESS','2026-10-22 04:35:12'),(582,4,'USER_UPDATE','81','SUCCESS','2026-10-22 04:35:13'),(583,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:35:14'),(584,3,'CHAPTER_CREATE','16','SUCCESS','2026-10-22 04:35:14'),(585,3,'CHAPTER_EDIT','16','SUCCESS','2026-10-22 04:35:15'),(586,3,'RESOURCE_UPLOAD','30','SUCCESS','2026-10-22 04:35:16'),(587,3,'RESOURCE_DELETE','30','SUCCESS','2026-10-22 04:35:17'),(588,3,'CHAPTER_DELETE','16','SUCCESS','2026-10-22 04:35:18'),(589,3,'POST_CREATE','20','SUCCESS','2026-10-22 04:35:19'),(590,3,'POST_EDIT','20','SUCCESS','2026-10-22 04:35:20'),(591,3,'POST_OFFLINE','20','SUCCESS','2026-10-22 04:35:21'),(592,3,'POST_DELETE','20','SUCCESS','2026-10-22 04:35:21'),(593,1,'LOGIN','maintenance','SUCCESS','2026-10-22 04:35:23'),(594,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:35:24'),(595,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:35:24'),(596,2,'LOGIN','student01','SUCCESS','2026-10-22 04:35:24'),(597,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:35:25'),(598,4,'COURSE_CREATE','94','SUCCESS','2026-10-22 04:35:27'),(599,4,'COURSE_EDIT','94','SUCCESS','2026-10-22 04:35:28'),(600,4,'COURSE_ASSIGN_MENTOR','94','SUCCESS','2026-10-22 04:35:29'),(601,4,'COURSE_ENROLL','94','SUCCESS','2026-10-22 04:35:30'),(602,4,'COURSE_STATE','94','SUCCESS','2026-10-22 04:35:30'),(603,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:35:32'),(604,3,'CHAPTER_CREATE','17','SUCCESS','2026-10-22 04:35:33'),(605,3,'CHAPTER_EDIT','17','SUCCESS','2026-10-22 04:35:33'),(606,3,'RESOURCE_UPLOAD','31','SUCCESS','2026-10-22 04:35:33'),(607,3,'RESOURCE_UPLOAD','32','SUCCESS','2026-10-22 04:35:34'),(608,3,'RESOURCE_UPLOAD','33','SUCCESS','2026-10-22 04:35:34'),(609,3,'RESOURCE_EDIT','31','SUCCESS','2026-10-22 04:35:35'),(610,3,'POST_CREATE','21','SUCCESS','2026-10-22 04:35:38'),(611,3,'POST_CREATE','22','SUCCESS','2026-10-22 04:35:39'),(612,2,'LOGIN','student01','SUCCESS','2026-10-22 04:35:42'),(613,3,'CHAPTER_CREATE','18','SUCCESS','2026-10-22 04:35:46'),(614,4,'COURSE_UNENROLL','94','SUCCESS','2026-10-22 04:35:55'),(615,4,'COURSE_ENROLL','94','SUCCESS','2026-10-22 04:35:55'),(616,4,'COURSE_STATE','94','SUCCESS','2026-10-22 04:35:56'),(617,4,'COURSE_STATE','94','SUCCESS','2026-10-22 04:35:56'),(618,4,'COURSE_STATE','94','SUCCESS','2026-10-22 04:35:57'),(619,4,'COURSE_STATE','94','SUCCESS','2026-10-22 04:35:58'),(620,2,'LOGIN','student01','SUCCESS','2026-10-22 04:47:56'),(621,1,'LOGIN','maintenance','SUCCESS','2026-10-22 04:49:43'),(622,1,'COURSE_ASSIGN_MENTOR','42','SUCCESS','2026-10-22 04:50:02'),(623,1,'COURSE_ASSIGN_MENTOR','42','SUCCESS','2026-10-22 04:50:08'),(624,1,'CHAPTER_CREATE','19','SUCCESS','2026-10-22 04:50:35'),(625,1,'USER_CREATE','92','SUCCESS','2026-10-22 04:51:40'),(626,NULL,'LOGIN','admin01','FAIL','2026-10-22 04:52:28'),(627,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:52:37'),(628,4,'COURSE_STATE','42','SUCCESS','2026-10-22 04:53:13'),(629,4,'COURSE_STATE','42','SUCCESS','2026-10-22 04:53:17'),(630,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:53:50'),(631,3,'LOGIN','mentor01','SUCCESS','2026-10-22 04:54:26'),(632,2,'LOGIN','student01','SUCCESS','2026-10-22 04:55:01'),(633,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:55:37'),(634,1,'LOGIN','maintenance','SUCCESS','2026-10-22 04:56:13'),(635,1,'COURSE_CREATE','95','SUCCESS','2026-10-22 04:56:21'),(636,1,'COURSE_ASSIGN_MENTOR','95','SUCCESS','2026-10-22 04:56:25'),(637,1,'COURSE_ASSIGN_MENTOR','95','SUCCESS','2026-10-22 04:56:28'),(638,1,'CHAPTER_CREATE','20','SUCCESS','2026-10-22 04:56:31'),(639,1,'CHAPTER_EDIT','20','SUCCESS','2026-10-22 04:56:44'),(640,1,'COURSE_ENROLL','95','SUCCESS','2026-10-22 04:57:07'),(641,1,'COURSE_STATE','95','SUCCESS','2026-10-22 04:57:23'),(642,4,'LOGIN','admin01','SUCCESS','2026-10-22 04:57:49'),(643,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:28:15'),(644,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:28:15'),(645,4,'USER_CREATE','93','SUCCESS','2026-10-22 07:28:15'),(646,93,'LOGIN','assignment_check_96414334mentor','SUCCESS','2026-10-22 07:28:15'),(647,4,'USER_CREATE','94','SUCCESS','2026-10-22 07:28:15'),(648,94,'LOGIN','assignment_check_96414334othermentor','SUCCESS','2026-10-22 07:28:16'),(649,4,'USER_CREATE','95','SUCCESS','2026-10-22 07:28:16'),(650,95,'LOGIN','assignment_check_96414334student','SUCCESS','2026-10-22 07:28:16'),(651,4,'USER_CREATE','96','SUCCESS','2026-10-22 07:28:16'),(652,96,'LOGIN','assignment_check_96414334otherstudent','SUCCESS','2026-10-22 07:28:16'),(653,4,'COURSE_CREATE','96','SUCCESS','2026-10-22 07:28:16'),(654,4,'COURSE_ASSIGN_MENTOR','96','SUCCESS','2026-10-22 07:28:16'),(655,4,'COURSE_ENROLL','96','SUCCESS','2026-10-22 07:28:16'),(656,4,'COURSE_ENROLL','96','SUCCESS','2026-10-22 07:28:16'),(657,4,'COURSE_STATE','96','SUCCESS','2026-10-22 07:28:16'),(658,93,'ASSIGNMENT_CREATE','1','SUCCESS','2026-10-22 07:28:16'),(659,93,'ASSIGNMENT_EDIT','1','SUCCESS','2026-10-22 07:28:17'),(660,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:28:45'),(661,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:28:46'),(662,4,'USER_CREATE','97','SUCCESS','2026-10-22 07:28:46'),(663,97,'LOGIN','assignment_check_5d5a713fmentor','SUCCESS','2026-10-22 07:28:46'),(664,4,'USER_CREATE','98','SUCCESS','2026-10-22 07:28:46'),(665,98,'LOGIN','assignment_check_5d5a713fothermentor','SUCCESS','2026-10-22 07:28:46'),(666,4,'USER_CREATE','99','SUCCESS','2026-10-22 07:28:46'),(667,99,'LOGIN','assignment_check_5d5a713fstudent','SUCCESS','2026-10-22 07:28:46'),(668,4,'USER_CREATE','100','SUCCESS','2026-10-22 07:28:47'),(669,100,'LOGIN','assignment_check_5d5a713fotherstudent','SUCCESS','2026-10-22 07:28:47'),(670,4,'COURSE_CREATE','97','SUCCESS','2026-10-22 07:28:47'),(671,4,'COURSE_ASSIGN_MENTOR','97','SUCCESS','2026-10-22 07:28:47'),(672,4,'COURSE_ENROLL','97','SUCCESS','2026-10-22 07:28:47'),(673,4,'COURSE_ENROLL','97','SUCCESS','2026-10-22 07:28:47'),(674,4,'COURSE_STATE','97','SUCCESS','2026-10-22 07:28:47'),(675,97,'ASSIGNMENT_CREATE','2','SUCCESS','2026-10-22 07:28:47'),(676,97,'ASSIGNMENT_EDIT','2','SUCCESS','2026-10-22 07:28:47'),(677,99,'ASSIGNMENT_SUBMIT','4','SUCCESS','2026-10-22 07:28:47'),(678,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:29:23'),(679,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:29:23'),(680,4,'USER_CREATE','101','SUCCESS','2026-10-22 07:29:23'),(681,101,'LOGIN','assignment_check_58521784mentor','SUCCESS','2026-10-22 07:29:23'),(682,4,'USER_CREATE','102','SUCCESS','2026-10-22 07:29:23'),(683,102,'LOGIN','assignment_check_58521784othermentor','SUCCESS','2026-10-22 07:29:23'),(684,4,'USER_CREATE','103','SUCCESS','2026-10-22 07:29:24'),(685,103,'LOGIN','assignment_check_58521784student','SUCCESS','2026-10-22 07:29:24'),(686,4,'USER_CREATE','104','SUCCESS','2026-10-22 07:29:24'),(687,104,'LOGIN','assignment_check_58521784otherstudent','SUCCESS','2026-10-22 07:29:24'),(688,4,'COURSE_CREATE','98','SUCCESS','2026-10-22 07:29:24'),(689,4,'COURSE_ASSIGN_MENTOR','98','SUCCESS','2026-10-22 07:29:24'),(690,4,'COURSE_ENROLL','98','SUCCESS','2026-10-22 07:29:24'),(691,4,'COURSE_ENROLL','98','SUCCESS','2026-10-22 07:29:24'),(692,4,'COURSE_STATE','98','SUCCESS','2026-10-22 07:29:24'),(693,101,'ASSIGNMENT_CREATE','3','SUCCESS','2026-10-22 07:29:24'),(694,101,'ASSIGNMENT_EDIT','3','SUCCESS','2026-10-22 07:29:24'),(695,103,'ASSIGNMENT_SUBMIT','6','SUCCESS','2026-10-22 07:29:25'),(696,101,'ASSIGNMENT_REVIEW','6','SUCCESS','2026-10-22 07:29:25'),(697,101,'ASSIGNMENT_EDIT','3','SUCCESS','2026-10-22 07:29:25'),(698,103,'ASSIGNMENT_SUBMIT','6','SUCCESS','2026-10-22 07:29:25'),(699,101,'ASSIGNMENT_REVIEW','6','SUCCESS','2026-10-22 07:29:25'),(700,4,'COURSE_UNENROLL','98','SUCCESS','2026-10-22 07:29:25'),(701,4,'COURSE_ASSIGN_MENTOR','98','SUCCESS','2026-10-22 07:29:25'),(702,4,'COURSE_STATE','98','SUCCESS','2026-10-22 07:29:26'),(703,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:34:35'),(704,4,'COURSE_CREATE','99','SUCCESS','2026-10-22 07:34:37'),(705,4,'COURSE_ASSIGN_MENTOR','99','SUCCESS','2026-10-22 07:34:38'),(706,4,'COURSE_ENROLL','99','SUCCESS','2026-10-22 07:34:39'),(707,4,'COURSE_STATE','99','SUCCESS','2026-10-22 07:34:39'),(708,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:34:41'),(709,3,'ASSIGNMENT_CREATE','4','SUCCESS','2026-10-22 07:34:42'),(710,2,'LOGIN','student01','SUCCESS','2026-10-22 07:34:45'),(711,3,'ASSIGNMENT_EDIT','4','SUCCESS','2026-10-22 07:34:48'),(712,2,'ASSIGNMENT_SUBMIT','7','SUCCESS','2026-10-22 07:34:51'),(713,3,'ASSIGNMENT_REVIEW','7','SUCCESS','2026-10-22 07:34:54'),(714,2,'ASSIGNMENT_SUBMIT','7','SUCCESS','2026-10-22 07:34:55'),(715,3,'ASSIGNMENT_REVIEW','7','SUCCESS','2026-10-22 07:34:56'),(716,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:35:36'),(717,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:35:36'),(718,4,'USER_CREATE','105','SUCCESS','2026-10-22 07:35:36'),(719,105,'LOGIN','assignment_check_c0d02770mentor','SUCCESS','2026-10-22 07:35:36'),(720,4,'USER_CREATE','106','SUCCESS','2026-10-22 07:35:37'),(721,106,'LOGIN','assignment_check_c0d02770othermentor','SUCCESS','2026-10-22 07:35:37'),(722,4,'USER_CREATE','107','SUCCESS','2026-10-22 07:35:37'),(723,107,'LOGIN','assignment_check_c0d02770student','SUCCESS','2026-10-22 07:35:37'),(724,4,'USER_CREATE','108','SUCCESS','2026-10-22 07:35:37'),(725,108,'LOGIN','assignment_check_c0d02770otherstudent','SUCCESS','2026-10-22 07:35:37'),(726,4,'COURSE_CREATE','100','SUCCESS','2026-10-22 07:35:37'),(727,4,'COURSE_ASSIGN_MENTOR','100','SUCCESS','2026-10-22 07:35:37'),(728,4,'COURSE_ENROLL','100','SUCCESS','2026-10-22 07:35:37'),(729,4,'COURSE_ENROLL','100','SUCCESS','2026-10-22 07:35:37'),(730,4,'COURSE_STATE','100','SUCCESS','2026-10-22 07:35:37'),(731,105,'ASSIGNMENT_CREATE','5','SUCCESS','2026-10-22 07:35:37'),(732,105,'ASSIGNMENT_EDIT','5','SUCCESS','2026-10-22 07:35:38'),(733,107,'ASSIGNMENT_SUBMIT','9','SUCCESS','2026-10-22 07:35:38'),(734,105,'ASSIGNMENT_REVIEW','9','SUCCESS','2026-10-22 07:35:38'),(735,105,'ASSIGNMENT_EDIT','5','SUCCESS','2026-10-22 07:35:38'),(736,107,'ASSIGNMENT_SUBMIT','9','SUCCESS','2026-10-22 07:35:38'),(737,105,'ASSIGNMENT_REVIEW','9','SUCCESS','2026-10-22 07:35:38'),(738,4,'COURSE_UNENROLL','100','SUCCESS','2026-10-22 07:35:38'),(739,4,'COURSE_ASSIGN_MENTOR','100','SUCCESS','2026-10-22 07:35:38'),(740,4,'COURSE_STATE','100','SUCCESS','2026-10-22 07:35:38'),(741,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:35:40'),(742,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:35:40'),(743,4,'USER_CREATE','109','SUCCESS','2026-10-22 07:35:40'),(744,109,'LOGIN','assignment_check_c2230201mentor','SUCCESS','2026-10-22 07:35:40'),(745,4,'USER_CREATE','110','SUCCESS','2026-10-22 07:35:40'),(746,110,'LOGIN','assignment_check_c2230201othermentor','SUCCESS','2026-10-22 07:35:40'),(747,4,'USER_CREATE','111','SUCCESS','2026-10-22 07:35:40'),(748,111,'LOGIN','assignment_check_c2230201student','SUCCESS','2026-10-22 07:35:40'),(749,4,'USER_CREATE','112','SUCCESS','2026-10-22 07:35:41'),(750,112,'LOGIN','assignment_check_c2230201otherstudent','SUCCESS','2026-10-22 07:35:41'),(751,4,'COURSE_CREATE','101','SUCCESS','2026-10-22 07:35:41'),(752,4,'COURSE_ASSIGN_MENTOR','101','SUCCESS','2026-10-22 07:35:41'),(753,4,'COURSE_ENROLL','101','SUCCESS','2026-10-22 07:35:41'),(754,4,'COURSE_ENROLL','101','SUCCESS','2026-10-22 07:35:41'),(755,4,'COURSE_STATE','101','SUCCESS','2026-10-22 07:35:41'),(756,109,'ASSIGNMENT_CREATE','6','SUCCESS','2026-10-22 07:35:41'),(757,109,'ASSIGNMENT_EDIT','6','SUCCESS','2026-10-22 07:35:41'),(758,111,'ASSIGNMENT_SUBMIT','11','SUCCESS','2026-10-22 07:35:41'),(759,109,'ASSIGNMENT_REVIEW','11','SUCCESS','2026-10-22 07:35:41'),(760,109,'ASSIGNMENT_EDIT','6','SUCCESS','2026-10-22 07:35:41'),(761,111,'ASSIGNMENT_SUBMIT','11','SUCCESS','2026-10-22 07:35:41'),(762,109,'ASSIGNMENT_REVIEW','11','SUCCESS','2026-10-22 07:35:41'),(763,4,'COURSE_UNENROLL','101','SUCCESS','2026-10-22 07:35:41'),(764,4,'COURSE_ASSIGN_MENTOR','101','SUCCESS','2026-10-22 07:35:41'),(765,4,'COURSE_STATE','101','SUCCESS','2026-10-22 07:35:42'),(766,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:35:43'),(767,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:35:43'),(768,4,'USER_CREATE','113','SUCCESS','2026-10-22 07:35:43'),(769,113,'LOGIN','assignment_check_25764591mentor','SUCCESS','2026-10-22 07:35:43'),(770,4,'USER_CREATE','114','SUCCESS','2026-10-22 07:35:43'),(771,114,'LOGIN','assignment_check_25764591othermentor','SUCCESS','2026-10-22 07:35:43'),(772,4,'USER_CREATE','115','SUCCESS','2026-10-22 07:35:43'),(773,115,'LOGIN','assignment_check_25764591student','SUCCESS','2026-10-22 07:35:44'),(774,4,'USER_CREATE','116','SUCCESS','2026-10-22 07:35:44'),(775,116,'LOGIN','assignment_check_25764591otherstudent','SUCCESS','2026-10-22 07:35:44'),(776,4,'COURSE_CREATE','102','SUCCESS','2026-10-22 07:35:44'),(777,4,'COURSE_ASSIGN_MENTOR','102','SUCCESS','2026-10-22 07:35:44'),(778,4,'COURSE_ENROLL','102','SUCCESS','2026-10-22 07:35:44'),(779,4,'COURSE_ENROLL','102','SUCCESS','2026-10-22 07:35:44'),(780,4,'COURSE_STATE','102','SUCCESS','2026-10-22 07:35:44'),(781,113,'ASSIGNMENT_CREATE','7','SUCCESS','2026-10-22 07:35:44'),(782,113,'ASSIGNMENT_EDIT','7','SUCCESS','2026-10-22 07:35:44'),(783,115,'ASSIGNMENT_SUBMIT','13','SUCCESS','2026-10-22 07:35:44'),(784,113,'ASSIGNMENT_REVIEW','13','SUCCESS','2026-10-22 07:35:44'),(785,113,'ASSIGNMENT_EDIT','7','SUCCESS','2026-10-22 07:35:44'),(786,115,'ASSIGNMENT_SUBMIT','13','SUCCESS','2026-10-22 07:35:44'),(787,113,'ASSIGNMENT_REVIEW','13','SUCCESS','2026-10-22 07:35:45'),(788,4,'COURSE_UNENROLL','102','SUCCESS','2026-10-22 07:35:45'),(789,4,'COURSE_ASSIGN_MENTOR','102','SUCCESS','2026-10-22 07:35:45'),(790,4,'COURSE_STATE','102','SUCCESS','2026-10-22 07:35:45'),(791,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:35:47'),(792,4,'COURSE_CREATE','103','SUCCESS','2026-10-22 07:35:47'),(793,4,'COURSE_CREATE','104','SUCCESS','2026-10-22 07:35:47'),(794,4,'COURSE_CREATE','105','SUCCESS','2026-10-22 07:35:47'),(795,4,'COURSE_CREATE','106','SUCCESS','2026-10-22 07:35:47'),(796,4,'COURSE_CREATE','107','SUCCESS','2026-10-22 07:35:47'),(797,4,'COURSE_CREATE','108','SUCCESS','2026-10-22 07:35:47'),(798,4,'COURSE_CREATE','109','SUCCESS','2026-10-22 07:35:47'),(799,4,'COURSE_CREATE','110','SUCCESS','2026-10-22 07:35:47'),(800,4,'COURSE_CREATE','111','SUCCESS','2026-10-22 07:35:47'),(801,4,'COURSE_CREATE','112','SUCCESS','2026-10-22 07:35:47'),(802,4,'COURSE_CREATE','113','SUCCESS','2026-10-22 07:35:47'),(803,4,'COURSE_CREATE','114','SUCCESS','2026-10-22 07:35:47'),(804,4,'USER_CREATE','117','SUCCESS','2026-10-22 07:35:47'),(805,4,'USER_CREATE','118','SUCCESS','2026-10-22 07:35:47'),(806,4,'USER_CREATE','119','SUCCESS','2026-10-22 07:35:47'),(807,4,'USER_CREATE','120','SUCCESS','2026-10-22 07:35:47'),(808,4,'USER_CREATE','121','SUCCESS','2026-10-22 07:35:47'),(809,4,'USER_CREATE','122','SUCCESS','2026-10-22 07:35:48'),(810,4,'USER_CREATE','123','SUCCESS','2026-10-22 07:35:48'),(811,4,'USER_CREATE','124','SUCCESS','2026-10-22 07:35:48'),(812,4,'USER_CREATE','125','SUCCESS','2026-10-22 07:35:48'),(813,4,'USER_CREATE','126','SUCCESS','2026-10-22 07:35:48'),(814,4,'USER_CREATE','127','SUCCESS','2026-10-22 07:35:48'),(815,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:35:50'),(816,4,'COURSE_ASSIGN_MENTOR','103','SUCCESS','2026-10-22 07:35:51'),(817,4,'USER_UPDATE','117','SUCCESS','2026-10-22 07:35:52'),(818,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:35:53'),(819,3,'CHAPTER_CREATE','21','SUCCESS','2026-10-22 07:35:53'),(820,3,'CHAPTER_EDIT','21','SUCCESS','2026-10-22 07:35:55'),(821,3,'RESOURCE_UPLOAD','34','SUCCESS','2026-10-22 07:35:56'),(822,3,'RESOURCE_DELETE','34','SUCCESS','2026-10-22 07:35:57'),(823,3,'CHAPTER_DELETE','21','SUCCESS','2026-10-22 07:35:58'),(824,3,'POST_CREATE','23','SUCCESS','2026-10-22 07:36:00'),(825,3,'POST_EDIT','23','SUCCESS','2026-10-22 07:36:03'),(826,3,'POST_OFFLINE','23','SUCCESS','2026-10-22 07:36:04'),(827,3,'POST_DELETE','23','SUCCESS','2026-10-22 07:36:06'),(828,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:36:10'),(829,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:36:12'),(830,4,'COURSE_CREATE','115','SUCCESS','2026-10-22 07:36:13'),(831,4,'COURSE_ASSIGN_MENTOR','115','SUCCESS','2026-10-22 07:36:14'),(832,4,'COURSE_ENROLL','115','SUCCESS','2026-10-22 07:36:14'),(833,4,'COURSE_STATE','115','SUCCESS','2026-10-22 07:36:14'),(834,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:36:16'),(835,3,'ASSIGNMENT_CREATE','8','SUCCESS','2026-10-22 07:36:18'),(836,2,'LOGIN','student01','SUCCESS','2026-10-22 07:36:20'),(837,3,'ASSIGNMENT_EDIT','8','SUCCESS','2026-10-22 07:36:21'),(838,2,'ASSIGNMENT_SUBMIT','14','SUCCESS','2026-10-22 07:36:26'),(839,3,'ASSIGNMENT_REVIEW','14','SUCCESS','2026-10-22 07:36:27'),(840,2,'ASSIGNMENT_SUBMIT','14','SUCCESS','2026-10-22 07:36:28'),(841,3,'ASSIGNMENT_REVIEW','14','SUCCESS','2026-10-22 07:36:29'),(842,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:36:45'),(843,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:36:46'),(844,2,'LOGIN','student01','SUCCESS','2026-10-22 07:36:46'),(845,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:36:48'),(846,4,'COURSE_CREATE','116','SUCCESS','2026-10-22 07:36:52'),(847,4,'COURSE_EDIT','116','SUCCESS','2026-10-22 07:36:53'),(848,4,'COURSE_ASSIGN_MENTOR','116','SUCCESS','2026-10-22 07:36:54'),(849,4,'COURSE_ENROLL','116','SUCCESS','2026-10-22 07:36:54'),(850,4,'COURSE_STATE','116','SUCCESS','2026-10-22 07:36:55'),(851,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:37:05'),(852,3,'CHAPTER_CREATE','22','SUCCESS','2026-10-22 07:37:07'),(853,3,'CHAPTER_EDIT','22','SUCCESS','2026-10-22 07:37:08'),(854,3,'RESOURCE_UPLOAD','35','SUCCESS','2026-10-22 07:37:09'),(855,3,'RESOURCE_UPLOAD','36','SUCCESS','2026-10-22 07:37:10'),(856,3,'RESOURCE_UPLOAD','37','SUCCESS','2026-10-22 07:37:10'),(857,3,'RESOURCE_EDIT','35','SUCCESS','2026-10-22 07:37:12'),(858,3,'POST_CREATE','24','SUCCESS','2026-10-22 07:37:13'),(859,3,'POST_CREATE','25','SUCCESS','2026-10-22 07:37:14'),(860,2,'LOGIN','student01','SUCCESS','2026-10-22 07:37:16'),(861,3,'CHAPTER_CREATE','23','SUCCESS','2026-10-22 07:37:22'),(862,4,'COURSE_UNENROLL','116','SUCCESS','2026-10-22 07:37:32'),(863,4,'COURSE_ENROLL','116','SUCCESS','2026-10-22 07:37:32'),(864,4,'COURSE_STATE','116','SUCCESS','2026-10-22 07:37:33'),(865,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:47:08'),(866,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:47:08'),(867,4,'USER_CREATE','128','SUCCESS','2026-10-22 07:47:08'),(868,128,'LOGIN','assignment_check_22ebfd74mentor','SUCCESS','2026-10-22 07:47:08'),(869,4,'USER_CREATE','129','SUCCESS','2026-10-22 07:47:09'),(870,129,'LOGIN','assignment_check_22ebfd74othermentor','SUCCESS','2026-10-22 07:47:09'),(871,4,'USER_CREATE','130','SUCCESS','2026-10-22 07:47:09'),(872,130,'LOGIN','assignment_check_22ebfd74student','SUCCESS','2026-10-22 07:47:09'),(873,4,'USER_CREATE','131','SUCCESS','2026-10-22 07:47:09'),(874,131,'LOGIN','assignment_check_22ebfd74otherstudent','SUCCESS','2026-10-22 07:47:09'),(875,4,'COURSE_CREATE','117','SUCCESS','2026-10-22 07:47:09'),(876,4,'COURSE_ASSIGN_MENTOR','117','SUCCESS','2026-10-22 07:47:09'),(877,4,'COURSE_ENROLL','117','SUCCESS','2026-10-22 07:47:10'),(878,4,'COURSE_ENROLL','117','SUCCESS','2026-10-22 07:47:10'),(879,4,'COURSE_STATE','117','SUCCESS','2026-10-22 07:47:10'),(880,128,'ASSIGNMENT_CREATE','9','SUCCESS','2026-10-22 07:47:10'),(881,128,'ASSIGNMENT_EDIT','9','SUCCESS','2026-10-22 07:47:10'),(882,130,'ASSIGNMENT_SUBMIT','16','SUCCESS','2026-10-22 07:47:10'),(883,128,'ASSIGNMENT_REVIEW','16','SUCCESS','2026-10-22 07:47:11'),(884,128,'ASSIGNMENT_EDIT','9','SUCCESS','2026-10-22 07:47:11'),(885,130,'ASSIGNMENT_SUBMIT','16','SUCCESS','2026-10-22 07:47:11'),(886,128,'ASSIGNMENT_REVIEW','16','SUCCESS','2026-10-22 07:47:11'),(887,4,'COURSE_UNENROLL','117','SUCCESS','2026-10-22 07:47:11'),(888,4,'COURSE_ASSIGN_MENTOR','117','SUCCESS','2026-10-22 07:47:11'),(889,4,'COURSE_STATE','117','SUCCESS','2026-10-22 07:47:11'),(890,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:47:14'),(891,4,'COURSE_CREATE','118','SUCCESS','2026-10-22 07:47:14'),(892,4,'COURSE_CREATE','119','SUCCESS','2026-10-22 07:47:14'),(893,4,'COURSE_CREATE','120','SUCCESS','2026-10-22 07:47:14'),(894,4,'COURSE_CREATE','121','SUCCESS','2026-10-22 07:47:14'),(895,4,'COURSE_CREATE','122','SUCCESS','2026-10-22 07:47:14'),(896,4,'COURSE_CREATE','123','SUCCESS','2026-10-22 07:47:14'),(897,4,'COURSE_CREATE','124','SUCCESS','2026-10-22 07:47:14'),(898,4,'COURSE_CREATE','125','SUCCESS','2026-10-22 07:47:14'),(899,4,'COURSE_CREATE','126','SUCCESS','2026-10-22 07:47:14'),(900,4,'COURSE_CREATE','127','SUCCESS','2026-10-22 07:47:14'),(901,4,'COURSE_CREATE','128','SUCCESS','2026-10-22 07:47:14'),(902,4,'COURSE_CREATE','129','SUCCESS','2026-10-22 07:47:14'),(903,4,'USER_CREATE','132','SUCCESS','2026-10-22 07:47:14'),(904,4,'USER_CREATE','133','SUCCESS','2026-10-22 07:47:15'),(905,4,'USER_CREATE','134','SUCCESS','2026-10-22 07:47:15'),(906,4,'USER_CREATE','135','SUCCESS','2026-10-22 07:47:15'),(907,4,'USER_CREATE','136','SUCCESS','2026-10-22 07:47:15'),(908,4,'USER_CREATE','137','SUCCESS','2026-10-22 07:47:15'),(909,4,'USER_CREATE','138','SUCCESS','2026-10-22 07:47:15'),(910,4,'USER_CREATE','139','SUCCESS','2026-10-22 07:47:15'),(911,4,'USER_CREATE','140','SUCCESS','2026-10-22 07:47:16'),(912,4,'USER_CREATE','141','SUCCESS','2026-10-22 07:47:16'),(913,4,'USER_CREATE','142','SUCCESS','2026-10-22 07:47:16'),(914,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:47:18'),(915,4,'COURSE_ASSIGN_MENTOR','118','SUCCESS','2026-10-22 07:47:23'),(916,4,'USER_UPDATE','132','SUCCESS','2026-10-22 07:47:26'),(917,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:47:26'),(918,3,'CHAPTER_CREATE','24','SUCCESS','2026-10-22 07:47:28'),(919,3,'CHAPTER_EDIT','24','SUCCESS','2026-10-22 07:47:31'),(920,3,'RESOURCE_UPLOAD','38','SUCCESS','2026-10-22 07:47:31'),(921,3,'RESOURCE_DELETE','38','SUCCESS','2026-10-22 07:47:33'),(922,3,'CHAPTER_DELETE','24','SUCCESS','2026-10-22 07:47:34'),(923,3,'POST_CREATE','26','SUCCESS','2026-10-22 07:47:35'),(924,3,'POST_EDIT','26','SUCCESS','2026-10-22 07:47:36'),(925,3,'POST_OFFLINE','26','SUCCESS','2026-10-22 07:47:38'),(926,3,'POST_DELETE','26','SUCCESS','2026-10-22 07:47:39'),(927,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:47:40'),(928,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:47:42'),(929,4,'COURSE_CREATE','130','SUCCESS','2026-10-22 07:47:43'),(930,4,'COURSE_ASSIGN_MENTOR','130','SUCCESS','2026-10-22 07:47:45'),(931,4,'COURSE_ENROLL','130','SUCCESS','2026-10-22 07:47:45'),(932,4,'COURSE_STATE','130','SUCCESS','2026-10-22 07:47:45'),(933,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:47:47'),(934,3,'ASSIGNMENT_CREATE','10','SUCCESS','2026-10-22 07:47:49'),(935,2,'LOGIN','student01','SUCCESS','2026-10-22 07:47:51'),(936,3,'ASSIGNMENT_EDIT','10','SUCCESS','2026-10-22 07:47:53'),(937,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:48:43'),(938,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:48:43'),(939,2,'LOGIN','student01','SUCCESS','2026-10-22 07:48:43'),(940,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:48:45'),(941,4,'COURSE_CREATE','131','SUCCESS','2026-10-22 07:48:46'),(942,4,'COURSE_EDIT','131','SUCCESS','2026-10-22 07:48:47'),(943,4,'COURSE_ASSIGN_MENTOR','131','SUCCESS','2026-10-22 07:48:48'),(944,4,'COURSE_ENROLL','131','SUCCESS','2026-10-22 07:48:48'),(945,4,'COURSE_STATE','131','SUCCESS','2026-10-22 07:48:49'),(946,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:48:51'),(947,3,'CHAPTER_CREATE','25','SUCCESS','2026-10-22 07:48:52'),(948,3,'CHAPTER_EDIT','25','SUCCESS','2026-10-22 07:48:52'),(949,3,'RESOURCE_UPLOAD','39','SUCCESS','2026-10-22 07:48:52'),(950,3,'RESOURCE_UPLOAD','40','SUCCESS','2026-10-22 07:48:53'),(951,3,'RESOURCE_UPLOAD','41','SUCCESS','2026-10-22 07:48:53'),(952,3,'RESOURCE_EDIT','39','SUCCESS','2026-10-22 07:48:55'),(953,3,'POST_CREATE','27','SUCCESS','2026-10-22 07:48:57'),(954,3,'POST_CREATE','28','SUCCESS','2026-10-22 07:48:57'),(955,2,'LOGIN','student01','SUCCESS','2026-10-22 07:48:59'),(956,3,'CHAPTER_CREATE','26','SUCCESS','2026-10-22 07:49:03'),(957,4,'COURSE_UNENROLL','131','SUCCESS','2026-10-22 07:49:09'),(958,4,'COURSE_ENROLL','131','SUCCESS','2026-10-22 07:49:10'),(959,4,'COURSE_STATE','131','SUCCESS','2026-10-22 07:49:10'),(960,4,'COURSE_STATE','131','SUCCESS','2026-10-22 07:49:10'),(961,4,'COURSE_STATE','131','SUCCESS','2026-10-22 07:49:11'),(962,4,'COURSE_STATE','131','SUCCESS','2026-10-22 07:49:11'),(963,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:51:59'),(964,4,'COURSE_CREATE','132','SUCCESS','2026-10-22 07:52:00'),(965,4,'COURSE_ASSIGN_MENTOR','132','SUCCESS','2026-10-22 07:52:01'),(966,4,'COURSE_ENROLL','132','SUCCESS','2026-10-22 07:52:02'),(967,4,'COURSE_STATE','132','SUCCESS','2026-10-22 07:52:02'),(968,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:52:04'),(969,3,'ASSIGNMENT_CREATE','11','SUCCESS','2026-10-22 07:52:06'),(970,2,'LOGIN','student01','SUCCESS','2026-10-22 07:52:08'),(971,3,'ASSIGNMENT_EDIT','11','SUCCESS','2026-10-22 07:52:09'),(972,2,'ASSIGNMENT_SUBMIT','18','SUCCESS','2026-10-22 07:52:44'),(973,3,'ASSIGNMENT_REVIEW','18','SUCCESS','2026-10-22 07:52:45'),(974,2,'ASSIGNMENT_SUBMIT','18','SUCCESS','2026-10-22 07:52:46'),(975,3,'ASSIGNMENT_REVIEW','18','SUCCESS','2026-10-22 07:52:46'),(976,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:52:48'),(977,3,'ASSIGNMENT_CREATE','12','SUCCESS','2026-10-22 07:52:48'),(978,3,'ASSIGNMENT_CREATE','13','SUCCESS','2026-10-22 07:52:48'),(979,3,'ASSIGNMENT_CREATE','14','SUCCESS','2026-10-22 07:52:48'),(980,3,'ASSIGNMENT_CREATE','15','SUCCESS','2026-10-22 07:52:48'),(981,3,'ASSIGNMENT_CREATE','16','SUCCESS','2026-10-22 07:52:48'),(982,3,'ASSIGNMENT_CREATE','17','SUCCESS','2026-10-22 07:52:48'),(983,3,'ASSIGNMENT_CREATE','18','SUCCESS','2026-10-22 07:52:48'),(984,3,'ASSIGNMENT_CREATE','19','SUCCESS','2026-10-22 07:52:48'),(985,3,'ASSIGNMENT_CREATE','20','SUCCESS','2026-10-22 07:52:48'),(986,3,'ASSIGNMENT_CREATE','21','SUCCESS','2026-10-22 07:52:48'),(987,3,'ASSIGNMENT_CREATE','22','SUCCESS','2026-10-22 07:52:48'),(988,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:55:56'),(989,4,'COURSE_CREATE','133','SUCCESS','2026-10-22 07:55:56'),(990,4,'COURSE_CREATE','134','SUCCESS','2026-10-22 07:55:56'),(991,4,'COURSE_CREATE','135','SUCCESS','2026-10-22 07:55:56'),(992,4,'COURSE_CREATE','136','SUCCESS','2026-10-22 07:55:56'),(993,4,'COURSE_CREATE','137','SUCCESS','2026-10-22 07:55:56'),(994,4,'COURSE_CREATE','138','SUCCESS','2026-10-22 07:55:56'),(995,4,'COURSE_CREATE','139','SUCCESS','2026-10-22 07:55:56'),(996,4,'COURSE_CREATE','140','SUCCESS','2026-10-22 07:55:56'),(997,4,'COURSE_CREATE','141','SUCCESS','2026-10-22 07:55:56'),(998,4,'COURSE_CREATE','142','SUCCESS','2026-10-22 07:55:56'),(999,4,'COURSE_CREATE','143','SUCCESS','2026-10-22 07:55:56'),(1000,4,'COURSE_CREATE','144','SUCCESS','2026-10-22 07:55:56'),(1001,4,'USER_CREATE','143','SUCCESS','2026-10-22 07:55:56'),(1002,4,'USER_CREATE','144','SUCCESS','2026-10-22 07:55:56'),(1003,4,'USER_CREATE','145','SUCCESS','2026-10-22 07:55:56'),(1004,4,'USER_CREATE','146','SUCCESS','2026-10-22 07:55:56'),(1005,4,'USER_CREATE','147','SUCCESS','2026-10-22 07:55:57'),(1006,4,'USER_CREATE','148','SUCCESS','2026-10-22 07:55:57'),(1007,4,'USER_CREATE','149','SUCCESS','2026-10-22 07:55:57'),(1008,4,'USER_CREATE','150','SUCCESS','2026-10-22 07:55:57'),(1009,4,'USER_CREATE','151','SUCCESS','2026-10-22 07:55:57'),(1010,4,'USER_CREATE','152','SUCCESS','2026-10-22 07:55:58'),(1011,4,'USER_CREATE','153','SUCCESS','2026-10-22 07:55:58'),(1012,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:56:01'),(1013,4,'COURSE_ASSIGN_MENTOR','133','SUCCESS','2026-10-22 07:56:04'),(1014,4,'USER_UPDATE','143','SUCCESS','2026-10-22 07:56:06'),(1015,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:56:07'),(1016,3,'CHAPTER_CREATE','27','SUCCESS','2026-10-22 07:56:08'),(1017,3,'CHAPTER_EDIT','27','SUCCESS','2026-10-22 07:56:10'),(1018,3,'RESOURCE_UPLOAD','42','SUCCESS','2026-10-22 07:56:12'),(1019,3,'RESOURCE_DELETE','42','SUCCESS','2026-10-22 07:56:13'),(1020,3,'CHAPTER_DELETE','27','SUCCESS','2026-10-22 07:56:15'),(1021,3,'POST_CREATE','29','SUCCESS','2026-10-22 07:56:17'),(1022,3,'POST_EDIT','29','SUCCESS','2026-10-22 07:56:18'),(1023,3,'POST_OFFLINE','29','SUCCESS','2026-10-22 07:56:20'),(1024,3,'POST_DELETE','29','SUCCESS','2026-10-22 07:56:22'),(1025,1,'LOGIN','maintenance','SUCCESS','2026-10-22 07:56:25'),(1026,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:56:28'),(1027,4,'COURSE_CREATE','145','SUCCESS','2026-10-22 07:56:29'),(1028,4,'COURSE_ASSIGN_MENTOR','145','SUCCESS','2026-10-22 07:56:30'),(1029,4,'COURSE_ENROLL','145','SUCCESS','2026-10-22 07:56:31'),(1030,4,'COURSE_STATE','145','SUCCESS','2026-10-22 07:56:31'),(1031,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:56:35'),(1032,3,'ASSIGNMENT_CREATE','23','SUCCESS','2026-10-22 07:56:39'),(1033,2,'LOGIN','student01','SUCCESS','2026-10-22 07:56:41'),(1034,3,'ASSIGNMENT_EDIT','23','SUCCESS','2026-10-22 07:56:43'),(1035,2,'ASSIGNMENT_SUBMIT','19','SUCCESS','2026-10-22 07:57:19'),(1036,3,'ASSIGNMENT_REVIEW','19','SUCCESS','2026-10-22 07:57:21'),(1037,2,'ASSIGNMENT_SUBMIT','19','SUCCESS','2026-10-22 07:57:21'),(1038,3,'ASSIGNMENT_REVIEW','19','SUCCESS','2026-10-22 07:57:22'),(1039,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:57:24'),(1040,3,'ASSIGNMENT_CREATE','24','SUCCESS','2026-10-22 07:57:24'),(1041,3,'ASSIGNMENT_CREATE','25','SUCCESS','2026-10-22 07:57:24'),(1042,3,'ASSIGNMENT_CREATE','26','SUCCESS','2026-10-22 07:57:24'),(1043,3,'ASSIGNMENT_CREATE','27','SUCCESS','2026-10-22 07:57:24'),(1044,3,'ASSIGNMENT_CREATE','28','SUCCESS','2026-10-22 07:57:24'),(1045,3,'ASSIGNMENT_CREATE','29','SUCCESS','2026-10-22 07:57:24'),(1046,3,'ASSIGNMENT_CREATE','30','SUCCESS','2026-10-22 07:57:24'),(1047,3,'ASSIGNMENT_CREATE','31','SUCCESS','2026-10-22 07:57:24'),(1048,3,'ASSIGNMENT_CREATE','32','SUCCESS','2026-10-22 07:57:24'),(1049,3,'ASSIGNMENT_CREATE','33','SUCCESS','2026-10-22 07:57:24'),(1050,3,'ASSIGNMENT_CREATE','34','SUCCESS','2026-10-22 07:57:24'),(1051,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:57:28'),(1052,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:57:28'),(1053,2,'LOGIN','student01','SUCCESS','2026-10-22 07:57:29'),(1054,4,'LOGIN','admin01','SUCCESS','2026-10-22 07:57:30'),(1055,4,'COURSE_CREATE','146','SUCCESS','2026-10-22 07:57:32'),(1056,4,'COURSE_EDIT','146','SUCCESS','2026-10-22 07:57:33'),(1057,4,'COURSE_ASSIGN_MENTOR','146','SUCCESS','2026-10-22 07:57:34'),(1058,4,'COURSE_ENROLL','146','SUCCESS','2026-10-22 07:57:34'),(1059,4,'COURSE_STATE','146','SUCCESS','2026-10-22 07:57:35'),(1060,3,'LOGIN','mentor01','SUCCESS','2026-10-22 07:57:38'),(1061,3,'CHAPTER_CREATE','28','SUCCESS','2026-10-22 07:57:38'),(1062,3,'CHAPTER_EDIT','28','SUCCESS','2026-10-22 07:57:40'),(1063,3,'RESOURCE_UPLOAD','43','SUCCESS','2026-10-22 07:57:40'),(1064,3,'RESOURCE_UPLOAD','44','SUCCESS','2026-10-22 07:57:41'),(1065,3,'RESOURCE_UPLOAD','45','SUCCESS','2026-10-22 07:57:41'),(1066,3,'RESOURCE_EDIT','43','SUCCESS','2026-10-22 07:57:43'),(1067,3,'POST_CREATE','30','SUCCESS','2026-10-22 07:57:45'),(1068,3,'POST_CREATE','31','SUCCESS','2026-10-22 07:57:46'),(1069,2,'LOGIN','student01','SUCCESS','2026-10-22 07:57:48'),(1070,3,'CHAPTER_CREATE','29','SUCCESS','2026-10-22 07:57:53'),(1071,4,'COURSE_UNENROLL','146','SUCCESS','2026-10-22 07:58:02'),(1072,4,'COURSE_ENROLL','146','SUCCESS','2026-10-22 07:58:03'),(1073,4,'COURSE_STATE','146','SUCCESS','2026-10-22 07:58:04'),(1074,4,'COURSE_STATE','146','SUCCESS','2026-10-22 07:58:04'),(1075,4,'COURSE_STATE','146','SUCCESS','2026-10-22 07:58:05'),(1076,4,'COURSE_STATE','146','SUCCESS','2026-10-22 07:58:05'),(1077,2,'LOGIN','student01','SUCCESS','2026-10-22 08:03:15'),(1078,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:03:58'),(1079,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:04:41'),(1080,2,'LOGIN','student01','SUCCESS','2026-10-22 08:05:05'),(1081,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:05:16'),(1082,3,'ASSIGNMENT_CREATE','35','SUCCESS','2026-10-22 08:06:08'),(1083,2,'LOGIN','student01','SUCCESS','2026-10-22 08:06:48'),(1084,2,'ASSIGNMENT_SUBMIT','20','SUCCESS','2026-10-22 08:07:11'),(1085,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:07:22'),(1086,3,'ASSIGNMENT_REVIEW','20','SUCCESS','2026-10-22 08:08:06'),(1087,1,'LOGIN','maintenance','SUCCESS','2026-10-22 08:14:44'),(1088,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:14:48'),(1089,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:15:09'),(1090,92,'LOGIN','student02','SUCCESS','2026-10-22 08:15:18'),(1091,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:37:53'),(1092,1,'LOGIN','maintenance','SUCCESS','2026-10-22 08:37:53'),(1093,4,'USER_CREATE','154','SUCCESS','2026-10-22 08:37:53'),(1094,154,'LOGIN','mood_check_c125eca5mentor','SUCCESS','2026-10-22 08:37:54'),(1095,4,'USER_CREATE','155','SUCCESS','2026-10-22 08:37:54'),(1096,155,'LOGIN','mood_check_c125eca5othermentor','SUCCESS','2026-10-22 08:37:54'),(1097,4,'USER_CREATE','156','SUCCESS','2026-10-22 08:37:54'),(1098,156,'LOGIN','mood_check_c125eca5student','SUCCESS','2026-10-22 08:37:54'),(1099,4,'USER_CREATE','157','SUCCESS','2026-10-22 08:37:54'),(1100,157,'LOGIN','mood_check_c125eca5otherstudent','SUCCESS','2026-10-22 08:37:55'),(1101,156,'MOOD_NOTICE_ACCEPT','2026-10-22-v1','SUCCESS','2026-10-22 08:37:55'),(1102,156,'MOOD_RECORD','1','SUCCESS','2026-10-22 08:37:56'),(1103,4,'COURSE_CREATE','147','SUCCESS','2026-10-22 08:37:56'),(1104,4,'COURSE_ASSIGN_MENTOR','147','SUCCESS','2026-10-22 08:37:56'),(1105,4,'COURSE_ENROLL','147','SUCCESS','2026-10-22 08:37:56'),(1106,4,'COURSE_ENROLL','147','SUCCESS','2026-10-22 08:37:56'),(1107,4,'COURSE_STATE','147','SUCCESS','2026-10-22 08:37:56'),(1108,4,'COURSE_CREATE','148','SUCCESS','2026-10-22 08:37:56'),(1109,4,'COURSE_ASSIGN_MENTOR','148','SUCCESS','2026-10-22 08:37:56'),(1110,4,'COURSE_ENROLL','148','SUCCESS','2026-10-22 08:37:56'),(1111,4,'COURSE_ENROLL','148','SUCCESS','2026-10-22 08:37:56'),(1112,4,'COURSE_STATE','148','SUCCESS','2026-10-22 08:37:56'),(1113,157,'MOOD_NOTICE_ACCEPT','2026-10-22-v1','SUCCESS','2026-10-22 08:37:57'),(1114,156,'MOOD_NOTICE_ACCEPT','2026-10-22-v1','SUCCESS','2026-10-22 08:37:58'),(1115,4,'COURSE_UNENROLL','147','SUCCESS','2026-10-22 08:37:58'),(1116,4,'COURSE_UNENROLL','148','SUCCESS','2026-10-22 08:37:58'),(1117,4,'COURSE_ENROLL','147','SUCCESS','2026-10-22 08:37:59'),(1118,4,'COURSE_ASSIGN_MENTOR','147','SUCCESS','2026-10-22 08:37:59'),(1119,4,'COURSE_STATE','147','SUCCESS','2026-10-22 08:37:59'),(1120,4,'COURSE_STATE','147','SUCCESS','2026-10-22 08:37:59'),(1121,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:42:06'),(1122,4,'USER_CREATE','158','SUCCESS','2026-10-22 08:42:06'),(1123,4,'USER_CREATE','159','SUCCESS','2026-10-22 08:42:06'),(1124,4,'USER_CREATE','160','SUCCESS','2026-10-22 08:42:07'),(1125,4,'USER_CREATE','161','SUCCESS','2026-10-22 08:42:07'),(1126,4,'USER_CREATE','162','SUCCESS','2026-10-22 08:42:07'),(1127,4,'USER_CREATE','163','SUCCESS','2026-10-22 08:42:07'),(1128,4,'USER_CREATE','164','SUCCESS','2026-10-22 08:42:07'),(1129,4,'USER_CREATE','165','SUCCESS','2026-10-22 08:42:07'),(1130,4,'USER_CREATE','166','SUCCESS','2026-10-22 08:42:07'),(1131,4,'USER_CREATE','167','SUCCESS','2026-10-22 08:42:08'),(1132,4,'USER_CREATE','168','SUCCESS','2026-10-22 08:42:08'),(1133,4,'USER_CREATE','169','SUCCESS','2026-10-22 08:42:08'),(1134,4,'USER_CREATE','170','SUCCESS','2026-10-22 08:42:08'),(1135,4,'USER_CREATE','171','SUCCESS','2026-10-22 08:42:08'),(1136,4,'COURSE_CREATE','149','SUCCESS','2026-10-22 08:42:08'),(1137,4,'COURSE_ASSIGN_MENTOR','149','SUCCESS','2026-10-22 08:42:08'),(1138,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1139,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1140,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1141,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1142,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1143,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1144,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1145,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1146,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1147,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1148,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1149,4,'COURSE_ENROLL','149','SUCCESS','2026-10-22 08:42:08'),(1150,4,'COURSE_STATE','149','SUCCESS','2026-10-22 08:42:08'),(1151,160,'LOGIN','mood_ui_mvjalnom_student0','SUCCESS','2026-10-22 08:42:09'),(1152,158,'LOGIN','mood_ui_mvjalnom_mentor','SUCCESS','2026-10-22 08:42:09'),(1153,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:42:11'),(1154,160,'LOGIN','mood_ui_mvjalnom_student0','SUCCESS','2026-10-22 08:42:13'),(1155,160,'MOOD_NOTICE_ACCEPT','2026-10-22-v1','SUCCESS','2026-10-22 08:42:16'),(1156,160,'MOOD_RECORD','14','SUCCESS','2026-10-22 08:42:19'),(1157,158,'LOGIN','mood_ui_mvjalnom_mentor','SUCCESS','2026-10-22 08:42:24'),(1158,4,'COURSE_ASSIGN_MENTOR','149','SUCCESS','2026-10-22 08:42:27'),(1159,159,'LOGIN','mood_ui_mvjalnom_othermentor','SUCCESS','2026-10-22 08:42:29'),(1160,4,'COURSE_UNENROLL','149','SUCCESS','2026-10-22 08:42:29'),(1161,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:47:36'),(1162,4,'USER_CREATE','172','SUCCESS','2026-10-22 08:47:37'),(1163,4,'USER_CREATE','173','SUCCESS','2026-10-22 08:47:37'),(1164,4,'USER_CREATE','174','SUCCESS','2026-10-22 08:47:37'),(1165,4,'USER_CREATE','175','SUCCESS','2026-10-22 08:47:37'),(1166,4,'USER_CREATE','176','SUCCESS','2026-10-22 08:47:37'),(1167,4,'USER_CREATE','177','SUCCESS','2026-10-22 08:47:37'),(1168,4,'USER_CREATE','178','SUCCESS','2026-10-22 08:47:37'),(1169,4,'USER_CREATE','179','SUCCESS','2026-10-22 08:47:37'),(1170,4,'USER_CREATE','180','SUCCESS','2026-10-22 08:47:38'),(1171,4,'USER_CREATE','181','SUCCESS','2026-10-22 08:47:38'),(1172,4,'USER_CREATE','182','SUCCESS','2026-10-22 08:47:38'),(1173,4,'USER_CREATE','183','SUCCESS','2026-10-22 08:47:38'),(1174,4,'USER_CREATE','184','SUCCESS','2026-10-22 08:47:38'),(1175,4,'USER_CREATE','185','SUCCESS','2026-10-22 08:47:38'),(1176,4,'COURSE_CREATE','150','SUCCESS','2026-10-22 08:47:38'),(1177,4,'COURSE_ASSIGN_MENTOR','150','SUCCESS','2026-10-22 08:47:38'),(1178,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1179,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1180,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1181,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1182,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1183,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1184,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1185,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1186,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:38'),(1187,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:39'),(1188,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:39'),(1189,4,'COURSE_ENROLL','150','SUCCESS','2026-10-22 08:47:39'),(1190,4,'COURSE_STATE','150','SUCCESS','2026-10-22 08:47:39'),(1191,174,'LOGIN','mood_ui_mvjasql4_student0','SUCCESS','2026-10-22 08:47:39'),(1192,172,'LOGIN','mood_ui_mvjasql4_mentor','SUCCESS','2026-10-22 08:47:39'),(1193,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:47:40'),(1194,174,'LOGIN','mood_ui_mvjasql4_student0','SUCCESS','2026-10-22 08:47:43'),(1195,174,'MOOD_NOTICE_ACCEPT','2026-10-22-v1','SUCCESS','2026-10-22 08:47:44'),(1196,174,'MOOD_RECORD','27','SUCCESS','2026-10-22 08:47:47'),(1197,172,'LOGIN','mood_ui_mvjasql4_mentor','SUCCESS','2026-10-22 08:47:53'),(1198,4,'COURSE_ASSIGN_MENTOR','150','SUCCESS','2026-10-22 08:47:57'),(1199,173,'LOGIN','mood_ui_mvjasql4_othermentor','SUCCESS','2026-10-22 08:47:58'),(1200,4,'COURSE_UNENROLL','150','SUCCESS','2026-10-22 08:47:59'),(1201,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:50:21'),(1202,4,'COURSE_CREATE','151','SUCCESS','2026-10-22 08:50:21'),(1203,4,'COURSE_CREATE','152','SUCCESS','2026-10-22 08:50:21'),(1204,4,'COURSE_CREATE','153','SUCCESS','2026-10-22 08:50:21'),(1205,4,'COURSE_CREATE','154','SUCCESS','2026-10-22 08:50:21'),(1206,4,'COURSE_CREATE','155','SUCCESS','2026-10-22 08:50:21'),(1207,4,'COURSE_CREATE','156','SUCCESS','2026-10-22 08:50:21'),(1208,4,'COURSE_CREATE','157','SUCCESS','2026-10-22 08:50:21'),(1209,4,'COURSE_CREATE','158','SUCCESS','2026-10-22 08:50:21'),(1210,4,'COURSE_CREATE','159','SUCCESS','2026-10-22 08:50:21'),(1211,4,'COURSE_CREATE','160','SUCCESS','2026-10-22 08:50:21'),(1212,4,'COURSE_CREATE','161','SUCCESS','2026-10-22 08:50:21'),(1213,4,'COURSE_CREATE','162','SUCCESS','2026-10-22 08:50:21'),(1214,4,'USER_CREATE','186','SUCCESS','2026-10-22 08:50:21'),(1215,4,'USER_CREATE','187','SUCCESS','2026-10-22 08:50:21'),(1216,4,'USER_CREATE','188','SUCCESS','2026-10-22 08:50:21'),(1217,4,'USER_CREATE','189','SUCCESS','2026-10-22 08:50:22'),(1218,4,'USER_CREATE','190','SUCCESS','2026-10-22 08:50:22'),(1219,4,'USER_CREATE','191','SUCCESS','2026-10-22 08:50:22'),(1220,4,'USER_CREATE','192','SUCCESS','2026-10-22 08:50:22'),(1221,4,'USER_CREATE','193','SUCCESS','2026-10-22 08:50:22'),(1222,4,'USER_CREATE','194','SUCCESS','2026-10-22 08:50:22'),(1223,4,'USER_CREATE','195','SUCCESS','2026-10-22 08:50:22'),(1224,4,'USER_CREATE','196','SUCCESS','2026-10-22 08:50:23'),(1225,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:50:24'),(1226,4,'COURSE_ASSIGN_MENTOR','151','SUCCESS','2026-10-22 08:50:26'),(1227,4,'USER_UPDATE','186','SUCCESS','2026-10-22 08:50:28'),(1228,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:50:29'),(1229,3,'CHAPTER_CREATE','30','SUCCESS','2026-10-22 08:50:30'),(1230,3,'CHAPTER_EDIT','30','SUCCESS','2026-10-22 08:50:32'),(1231,3,'RESOURCE_UPLOAD','46','SUCCESS','2026-10-22 08:50:32'),(1232,3,'RESOURCE_DELETE','46','SUCCESS','2026-10-22 08:50:34'),(1233,3,'CHAPTER_DELETE','30','SUCCESS','2026-10-22 08:50:35'),(1234,3,'POST_CREATE','32','SUCCESS','2026-10-22 08:50:36'),(1235,3,'POST_EDIT','32','SUCCESS','2026-10-22 08:50:38'),(1236,3,'POST_OFFLINE','32','SUCCESS','2026-10-22 08:50:38'),(1237,3,'POST_DELETE','32','SUCCESS','2026-10-22 08:50:39'),(1238,1,'LOGIN','maintenance','SUCCESS','2026-10-22 08:50:41'),(1239,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:50:45'),(1240,4,'COURSE_CREATE','163','SUCCESS','2026-10-22 08:50:46'),(1241,4,'COURSE_ASSIGN_MENTOR','163','SUCCESS','2026-10-22 08:50:47'),(1242,4,'COURSE_ENROLL','163','SUCCESS','2026-10-22 08:50:48'),(1243,4,'COURSE_STATE','163','SUCCESS','2026-10-22 08:50:48'),(1244,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:50:49'),(1245,3,'ASSIGNMENT_CREATE','36','SUCCESS','2026-10-22 08:50:51'),(1246,2,'LOGIN','student01','SUCCESS','2026-10-22 08:50:53'),(1247,3,'ASSIGNMENT_EDIT','36','SUCCESS','2026-10-22 08:50:55'),(1248,2,'ASSIGNMENT_SUBMIT','21','SUCCESS','2026-10-22 08:51:29'),(1249,3,'ASSIGNMENT_REVIEW','21','SUCCESS','2026-10-22 08:51:31'),(1250,2,'ASSIGNMENT_SUBMIT','21','SUCCESS','2026-10-22 08:51:31'),(1251,3,'ASSIGNMENT_REVIEW','21','SUCCESS','2026-10-22 08:51:32'),(1252,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:51:34'),(1253,3,'ASSIGNMENT_CREATE','37','SUCCESS','2026-10-22 08:51:34'),(1254,3,'ASSIGNMENT_CREATE','38','SUCCESS','2026-10-22 08:51:34'),(1255,3,'ASSIGNMENT_CREATE','39','SUCCESS','2026-10-22 08:51:34'),(1256,3,'ASSIGNMENT_CREATE','40','SUCCESS','2026-10-22 08:51:34'),(1257,3,'ASSIGNMENT_CREATE','41','SUCCESS','2026-10-22 08:51:34'),(1258,3,'ASSIGNMENT_CREATE','42','SUCCESS','2026-10-22 08:51:34'),(1259,3,'ASSIGNMENT_CREATE','43','SUCCESS','2026-10-22 08:51:34'),(1260,3,'ASSIGNMENT_CREATE','44','SUCCESS','2026-10-22 08:51:34'),(1261,3,'ASSIGNMENT_CREATE','45','SUCCESS','2026-10-22 08:51:34'),(1262,3,'ASSIGNMENT_CREATE','46','SUCCESS','2026-10-22 08:51:34'),(1263,3,'ASSIGNMENT_CREATE','47','SUCCESS','2026-10-22 08:51:34'),(1264,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:51:37'),(1265,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:51:37'),(1266,2,'LOGIN','student01','SUCCESS','2026-10-22 08:51:37'),(1267,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:51:39'),(1268,4,'COURSE_CREATE','164','SUCCESS','2026-10-22 08:51:40'),(1269,4,'COURSE_EDIT','164','SUCCESS','2026-10-22 08:51:41'),(1270,4,'COURSE_ASSIGN_MENTOR','164','SUCCESS','2026-10-22 08:51:42'),(1271,4,'COURSE_ENROLL','164','SUCCESS','2026-10-22 08:51:43'),(1272,4,'COURSE_STATE','164','SUCCESS','2026-10-22 08:51:43'),(1273,3,'LOGIN','mentor01','SUCCESS','2026-10-22 08:51:45'),(1274,3,'CHAPTER_CREATE','31','SUCCESS','2026-10-22 08:51:46'),(1275,3,'CHAPTER_EDIT','31','SUCCESS','2026-10-22 08:51:46'),(1276,3,'RESOURCE_UPLOAD','47','SUCCESS','2026-10-22 08:51:46'),(1277,3,'RESOURCE_UPLOAD','48','SUCCESS','2026-10-22 08:51:46'),(1278,3,'RESOURCE_UPLOAD','49','SUCCESS','2026-10-22 08:51:47'),(1279,3,'RESOURCE_EDIT','47','SUCCESS','2026-10-22 08:51:48'),(1280,3,'POST_CREATE','33','SUCCESS','2026-10-22 08:51:50'),(1281,3,'POST_CREATE','34','SUCCESS','2026-10-22 08:51:51'),(1282,2,'LOGIN','student01','SUCCESS','2026-10-22 08:51:52'),(1283,3,'CHAPTER_CREATE','32','SUCCESS','2026-10-22 08:51:56'),(1284,4,'COURSE_UNENROLL','164','SUCCESS','2026-10-22 08:52:02'),(1285,4,'COURSE_ENROLL','164','SUCCESS','2026-10-22 08:52:03'),(1286,4,'COURSE_STATE','164','SUCCESS','2026-10-22 08:52:03'),(1287,4,'COURSE_STATE','164','SUCCESS','2026-10-22 08:52:03'),(1288,4,'COURSE_STATE','164','SUCCESS','2026-10-22 08:52:04'),(1289,4,'COURSE_STATE','164','SUCCESS','2026-10-22 08:52:04'),(1290,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:52:07'),(1291,4,'USER_CREATE','197','SUCCESS','2026-10-22 08:52:07'),(1292,4,'USER_CREATE','198','SUCCESS','2026-10-22 08:52:07'),(1293,4,'USER_CREATE','199','SUCCESS','2026-10-22 08:52:07'),(1294,4,'USER_CREATE','200','SUCCESS','2026-10-22 08:52:07'),(1295,4,'USER_CREATE','201','SUCCESS','2026-10-22 08:52:08'),(1296,4,'USER_CREATE','202','SUCCESS','2026-10-22 08:52:08'),(1297,4,'USER_CREATE','203','SUCCESS','2026-10-22 08:52:08'),(1298,4,'USER_CREATE','204','SUCCESS','2026-10-22 08:52:08'),(1299,4,'USER_CREATE','205','SUCCESS','2026-10-22 08:52:08'),(1300,4,'USER_CREATE','206','SUCCESS','2026-10-22 08:52:08'),(1301,4,'USER_CREATE','207','SUCCESS','2026-10-22 08:52:08'),(1302,4,'USER_CREATE','208','SUCCESS','2026-10-22 08:52:08'),(1303,4,'USER_CREATE','209','SUCCESS','2026-10-22 08:52:09'),(1304,4,'USER_CREATE','210','SUCCESS','2026-10-22 08:52:09'),(1305,4,'COURSE_CREATE','165','SUCCESS','2026-10-22 08:52:09'),(1306,4,'COURSE_ASSIGN_MENTOR','165','SUCCESS','2026-10-22 08:52:09'),(1307,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1308,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1309,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1310,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1311,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1312,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1313,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1314,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1315,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1316,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1317,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1318,4,'COURSE_ENROLL','165','SUCCESS','2026-10-22 08:52:09'),(1319,4,'COURSE_STATE','165','SUCCESS','2026-10-22 08:52:09'),(1320,199,'LOGIN','mood_ui_mvjayj5n_student0','SUCCESS','2026-10-22 08:52:09'),(1321,197,'LOGIN','mood_ui_mvjayj5n_mentor','SUCCESS','2026-10-22 08:52:09'),(1322,4,'LOGIN','admin01','SUCCESS','2026-10-22 08:52:11'),(1323,199,'LOGIN','mood_ui_mvjayj5n_student0','SUCCESS','2026-10-22 08:52:12'),(1324,199,'MOOD_NOTICE_ACCEPT','2026-10-22-v1','SUCCESS','2026-10-22 08:52:13'),(1325,199,'MOOD_RECORD','40','SUCCESS','2026-10-22 08:52:15'),(1326,197,'LOGIN','mood_ui_mvjayj5n_mentor','SUCCESS','2026-10-22 08:52:21'),(1327,4,'COURSE_ASSIGN_MENTOR','165','SUCCESS','2026-10-22 08:52:24'),(1328,198,'LOGIN','mood_ui_mvjayj5n_othermentor','SUCCESS','2026-10-22 08:52:26'),(1329,4,'COURSE_UNENROLL','165','SUCCESS','2026-10-22 08:52:26'),(1330,2,'LOGIN','student01','SUCCESS','2026-10-22 09:08:53'),(1331,2,'MOOD_NOTICE_ACCEPT','2026-10-22-v1','SUCCESS','2026-10-22 09:09:06'),(1332,2,'MOOD_RECORD','53','SUCCESS','2026-10-22 09:09:19'),(1333,3,'LOGIN','mentor01','SUCCESS','2026-10-22 09:10:24');
/*!40000 ALTER TABLE `sys_audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint(20) NOT NULL,
  `parent_id` bigint(20) NOT NULL DEFAULT '0',
  `menu_name` varchar(60) NOT NULL,
  `path` varchar(100) NOT NULL DEFAULT '',
  `menu_type` char(1) NOT NULL,
  `perms` varchar(100) DEFAULT NULL,
  `order_num` int(11) NOT NULL DEFAULT '0',
  `status` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (100,0,'My account','profile','C','',1,'0'),(200,0,'Account management','users','C','system:user:list',2,'0'),(201,200,'Create account','','F','system:user:add',1,'0'),(202,200,'Edit account and role','','F','system:user:edit',2,'0'),(203,200,'Reset password','','F','system:user:resetPwd',3,'0'),(204,200,'Enable or disable account','','F','system:user:changeStatus',4,'0'),(205,200,'Reset authenticator','','F','system:user:resetTotp',5,'0'),(300,0,'Courses','courses','C','learning:course:query',2,'0'),(301,300,'Create course','','F','learning:course:add',3,'0'),(302,300,'Edit course','','F','learning:course:edit',4,'0'),(303,300,'Assign course members','','F','learning:course:assign',5,'0'),(304,300,'Publish course','','F','learning:course:publish',6,'0'),(305,300,'Course members','','F','learning:course:members',7,'0'),(306,300,'View chapters','','F','learning:chapter:query',8,'0'),(307,300,'Maintain chapters','','F','learning:chapter:edit',9,'0'),(308,300,'View resources','','F','learning:resource:query',10,'0'),(309,300,'Maintain resources','','F','learning:resource:upload',11,'0'),(310,300,'Course posts','','F','learning:post:query',12,'0'),(311,300,'Create post','','F','learning:post:add',13,'0'),(312,300,'Edit own post','','F','learning:post:edit',14,'0'),(313,300,'Unpublish post','','F','learning:post:offline',15,'0'),(400,0,'Assignments','assignments','C','assignment:read',3,'0'),(401,400,'Maintain assignments','','F','assignment:edit',1,'0'),(402,400,'Submit own work','','F','assignment:submit',2,'0'),(403,400,'Review submissions','','F','assignment:review',3,'0'),(500,0,'My mood','mood','C','mood:own:read',4,'0'),(501,500,'Record weekly mood','','F','mood:own:record',1,'0'),(510,0,'Student mood','student-mood','C','mood:students:read',4,'0');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `role_id` bigint(20) NOT NULL,
  `role_name` varchar(60) NOT NULL,
  `role_key` varchar(60) NOT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'Maintenance administrator','admin','0'),(10,'Student','student','0'),(20,'Mentor','mentor','0'),(30,'Platform administrator','platform_admin','0');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint(20) NOT NULL,
  `menu_id` bigint(20) NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`),
  KEY `menu_id` (`menu_id`),
  CONSTRAINT `sys_role_menu_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`),
  CONSTRAINT `sys_role_menu_ibfk_2` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (10,100),(20,100),(30,100),(30,200),(30,201),(30,202),(30,203),(30,204),(10,300),(20,300),(30,300),(30,301),(30,302),(30,303),(30,304),(20,305),(30,305),(10,306),(20,306),(30,306),(20,307),(30,307),(10,308),(20,308),(30,308),(20,309),(30,309),(10,310),(20,310),(30,310),(20,311),(20,312),(20,313),(30,313),(10,400),(20,400),(20,401),(10,402),(20,403),(10,500),(10,501),(20,510);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_name` varchar(100) NOT NULL,
  `nick_name` varchar(60) NOT NULL,
  `password` varchar(100) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  `auth_version` bigint(20) NOT NULL DEFAULT '0',
  `totp_secret` varchar(255) DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_name` (`user_name`),
  KEY `fk_user_role` (`role_id`),
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'maintenance','maintenance','$2a$10$p/c0b8aUWV1mR68KknGuguKmmJXV5LFVidH5mt8yURtQWkDIZV/xm',1,'0',0,NULL,'2026-10-21 07:54:32'),(2,'student01','student01','$2a$10$GhK3cXZqpYcVbLaWLU0QX.Wc3hCF2TY60I2Lb4ErGCQKf5aWHA1oy',10,'0',0,NULL,'2026-10-21 07:54:32'),(3,'mentor01','mentor01','$2a$10$ndetcPLnJs2xBBKGQEy4JeG9.lLO264EfNnzZG2J6CZqP8BpBZhTy',20,'0',0,NULL,'2026-10-21 07:54:33'),(4,'admin01','admin01','$2a$10$oes01aHF4le1/wtrmt0cZuqSt1.3gZdi3Yarx7OnIxq9ZgiYN3ffm',30,'0',1,'A0xkIWniE5/NuVyf72W1no52McbEzQNVAz+V7VH5e+kfvlCkGIpgprOLEDP614i0kyWOI1+BwAs9ZxHC','2026-10-21 07:54:33'),(92,'student02','Mike','$2a$10$ejMXVPpFX.khmlmikFnVm.16AFrw1Jnht9y9vS3WuBKZsIWgFYeoi',10,'0',0,NULL,'2026-10-22 04:51:40');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'up_self_dev'
--

--
-- Dumping routines for database 'up_self_dev'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-22 17:16:41
