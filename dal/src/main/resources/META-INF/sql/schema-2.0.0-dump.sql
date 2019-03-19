create sequence bdprole_seq start 1 increment 1;
create sequence bdprules_seq start 1 increment 1;
create sequence bdpuser_seq start 1 increment 1;
create sequence edge_seq start 1 increment 1;
create sequence measurement_seq start 1 increment 1;
create sequence measurementhistory_seq start 1 increment 1;
create sequence measurementstring_seq start 1 increment 1;
create sequence measurementstringhistory_seq start 1 increment 1;
create sequence metadata_seq start 1 increment 1;
create sequence provenance_seq start 1 increment 1;
create sequence station_seq start 1 increment 1;
create sequence type_seq start 1 increment 1;
create table bdppermissions (uuid int8 not null, period int4, role_id int8, station_id int8, type_id int8, primary key (uuid));
create table bdprole (id int8 default nextval('bdprole_seq') not null, description varchar(255), name varchar(255) not null, parent_id int8, primary key (id));
create table bdprules (id int8 default nextval('bdprules_seq') not null, period int4, role_id int8, station_id int8, type_id int8, primary key (id));
create table bdpuser (id int8 default nextval('bdpuser_seq') not null, email varchar(255) not null, enabled boolean default true not null, password varchar(255) not null, token_expired boolean default false not null, primary key (id));
create table bdpusers_bdproles (user_id int8 not null, role_id int8 not null);
create table edge (id int8 default nextval('edge_seq') not null, directed boolean default true not null, linegeometry GEOMETRY, destination_id int8 not null, edge_data_id int8, origin_id int8 not null, primary key (id));
create table measurement (id int8 default nextval('measurement_seq') not null, created_on timestamp not null, period int4 not null, timestamp timestamp not null, double_value float8 not null, provenance_id int8, station_id int8 not null, type_id int8 not null, primary key (id));
create table measurementhistory (id int8 default nextval('measurementhistory_seq') not null, created_on timestamp not null, period int4 not null, timestamp timestamp not null, double_value float8 not null, provenance_id int8, station_id int8 not null, type_id int8 not null, primary key (id));
create table measurementstring (id int8 default nextval('measurementstring_seq') not null, created_on timestamp not null, period int4 not null, timestamp timestamp not null, string_value varchar(255) not null, provenance_id int8, station_id int8 not null, type_id int8 not null, primary key (id));
create table measurementstringhistory (id int8 default nextval('measurementstringhistory_seq') not null, created_on timestamp not null, period int4 not null, timestamp timestamp not null, string_value varchar(255) not null, provenance_id int8, station_id int8 not null, type_id int8 not null, primary key (id));
create table metadata (id int8 default nextval('metadata_seq') not null, created_on timestamp, json jsonb, station_id int8, primary key (id));
create table provenance (id int8 default nextval('provenance_seq') not null, data_collector varchar(255) not null, data_collector_version varchar(255), lineage varchar(255) not null, primary key (id));
create table station (id int8 default nextval('station_seq') not null, active boolean, available boolean, name varchar(255) not null, origin varchar(255), pointprojection GEOMETRY, stationcode varchar(255) not null, stationtype varchar(255) not null, meta_data_id int8, parent_id int8, primary key (id));
create table type (id int8 default nextval('type_seq') not null, cname varchar(255) not null, created_on timestamp, cunit varchar(255), description varchar(255), rtype varchar(255), primary key (id));
create index idx_bdppermissions_station_id_type_id_period on bdppermissions (station_id, type_id, period);
alter table bdprole add constraint uc_bdprole_name unique (name);
alter table bdpuser add constraint uc_bdpuser_email unique (email);
create index idx_measurement_timestamp on measurement (timestamp desc);
create index idx_measurementhistory_created_on on measurementhistory (created_on desc);
alter table measurement add constraint uc_measurement_station_id_type_id_period unique (station_id, type_id, period);
create index idx_measurementhistory_station_id_type_id_timestamp_period on measurementhistory (station_id, type_id, timestamp desc, period);
alter table measurementhistory add constraint uc_measurementhistory_station_i__timestamp_period_double_value_ unique (station_id, type_id, timestamp, period, double_value);
create index idx_measurementstring_timestamp on measurementstring (timestamp desc);
alter table measurementstring add constraint uc_measurementstring_station_id_type_id_period unique (station_id, type_id, period);
create index idx_measurementstringhistory_created_on on measurementstringhistory (created_on desc);
create index idx_measurementstringhistory_st_on_id_type_id_timestamp_period_ on measurementstringhistory (station_id, type_id, timestamp desc, period);
alter table measurementstringhistory add constraint uc_measurementstringhistory_sta__timestamp_period_string_value_ unique (station_id, type_id, timestamp, period, string_value);
alter table provenance add constraint uc_provenance_lineage_data_collector_data_collector_version unique (lineage, data_collector, data_collector_version);
alter table station add constraint uc_station_stationcode_stationtype unique (stationcode, stationtype);
alter table type add constraint uc_type_cname unique (cname);
alter table bdppermissions add constraint fk_bdppermissions_role_id_bdprole_pk foreign key (role_id) references bdprole;
alter table bdppermissions add constraint fk_bdppermissions_station_id_station_pk foreign key (station_id) references station;
alter table bdppermissions add constraint fk_bdppermissions_type_id_type_pk foreign key (type_id) references type;
alter table bdprole add constraint fk_bdprole_parent_id_bdprole_pk foreign key (parent_id) references bdprole;
alter table bdprules add constraint fk_bdprules_role_id_bdprole_pk foreign key (role_id) references bdprole;
alter table bdprules add constraint fk_bdprules_station_id_station_pk foreign key (station_id) references station;
alter table bdprules add constraint fk_bdprules_type_id_type_pk foreign key (type_id) references type;
alter table bdpusers_bdproles add constraint fk_bdpusers_bdproles_role_id_bdprole_pk foreign key (role_id) references bdprole;
alter table bdpusers_bdproles add constraint fk_bdpusers_bdproles_user_id_bdpuser_pk foreign key (user_id) references bdpuser;
alter table edge add constraint fk_edge_destination_id_station_pk foreign key (destination_id) references station;
alter table edge add constraint fk_edge_edge_data_id_station_pk foreign key (edge_data_id) references station;
alter table edge add constraint fk_edge_origin_id_station_pk foreign key (origin_id) references station;
alter table measurement add constraint fk_measurement_provenance_id_provenance_pk foreign key (provenance_id) references provenance;
alter table measurement add constraint fk_measurement_station_id_station_pk foreign key (station_id) references station;
alter table measurement add constraint fk_measurement_type_id_type_pk foreign key (type_id) references type;
alter table measurementhistory add constraint fk_measurementhistory_provenance_id_provenance_pk foreign key (provenance_id) references provenance;
alter table measurementhistory add constraint fk_measurementhistory_station_id_station_pk foreign key (station_id) references station;
alter table measurementhistory add constraint fk_measurementhistory_type_id_type_pk foreign key (type_id) references type;
alter table measurementstring add constraint fk_measurementstring_provenance_id_provenance_pk foreign key (provenance_id) references provenance;
alter table measurementstring add constraint fk_measurementstring_station_id_station_pk foreign key (station_id) references station;
alter table measurementstring add constraint fk_measurementstring_type_id_type_pk foreign key (type_id) references type;
alter table measurementstringhistory add constraint fk_measurementstringhistory_provenance_id_provenance_pk foreign key (provenance_id) references provenance;
alter table measurementstringhistory add constraint fk_measurementstringhistory_station_id_station_pk foreign key (station_id) references station;
alter table measurementstringhistory add constraint fk_measurementstringhistory_type_id_type_pk foreign key (type_id) references type;
alter table metadata add constraint fk_metadata_station_id_station_pk foreign key (station_id) references station;
alter table station add constraint fk_station_meta_data_id_metadata_pk foreign key (meta_data_id) references metadata;
alter table station add constraint fk_station_parent_id_station_pk foreign key (parent_id) references station;
