/**
 * BDP data - Data Access Layer for the Big Data Platform
 * Copyright © 2018 IDM Südtirol - Alto Adige (info@idm-suedtirol.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program (see LICENSES/GPL-3.0.txt). If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package it.bz.idm.bdp.dal;


import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ColumnDefault;

import it.bz.idm.bdp.dal.authentication.BDPRole;
import it.bz.idm.bdp.dto.RecordDto;

@Table(name = "measurementhistory")
@Entity
public class MeasurementHistory extends MHistory {
	@Transient
	private static final long serialVersionUID = 2900270107783989197L;

    @Id
	@GeneratedValue(generator = "measurementhistory_gen", strategy = GenerationType.SEQUENCE)
	@SequenceGenerator(name = "measurementhistory_gen", sequenceName = "measurementhistory_seq", schema = "intime", allocationSize = 1)
	@ColumnDefault(value = "nextval('intime.measurementhistory_seq')")
	private Long id;

	private Double value;

	public MeasurementHistory() {
	}
	public MeasurementHistory(Station station, DataType type, Double value, Date timestamp, Integer period, Date created_on) {
		super(station,type,timestamp,period);
		setValue(value);
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	@Override
	public List<RecordDto> findRecords(EntityManager em, String stationtype, String identifier, String cname, Long seconds, Integer period, BDPRole role) {
		return MHistory.findRecordsImpl(em, stationtype, identifier, cname, seconds, period, role, this);
	}

	@Override
	public List<RecordDto> findRecords(EntityManager em, String stationtype, String identifier, String cname, Date start, Date end, Integer period, BDPRole role) {
		return MHistory.findRecordsImpl(em, stationtype, identifier, cname, start, end, period, role, this);
	}

	@Override
	public MHistory findRecord(EntityManager em, Station station, DataType type, String value, Date timestamp, Integer period, BDPRole role) {
		return MHistory.findRecordImpl(em, station, type, value, timestamp, period, role, MeasurementHistory.class);
	}

}
