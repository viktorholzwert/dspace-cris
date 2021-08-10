--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- ADD table subscription_parameter
-----------------------------------------------------------------------------------


CREATE SEQUENCE subscription_parameter_seq;
CREATE TABLE  subscription_parameter
(
  subscription_parameter_id  INTEGER NOT NULL,
  name   VARCHAR(255),
  value  VARCHAR(255),
  subscription_id     INTEGER  NOT NULL,
  CONSTRAINT subscription_parameter_pkey PRIMARY KEY (subscription_parameter_id),
  CONSTRAINT subscription_parameter_subscription_fkey  FOREIGN KEY  (subscription_id)
  REFERENCES subscription (subscription_id)
);
-- --

--ALTER TABLE subscription DROP CONSTRAINT subscription_collection_id_fkey
------ --
--ALTER TABLE subscription ALTER COLUMN collection_id RENAME TO dspace_object_id;
---- --
ALTER TABLE subscription ADD COL
---- --
--ALTER TABLE subscription ADD CONSTRAINT subscription_dspaceobject_fkey FOREIGN KEY (dspace_object_id) REFERENCES dspaceobject (uuid);
--


