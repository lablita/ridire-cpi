/*******************************************************************************
 * Copyright 2013 University of Florence
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.drwolf.ridire.entity;

import static javax.persistence.GenerationType.AUTO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;

@Entity
public class ScheduledJobHandle {
	private String jobFullName;
	private byte[] serializedHandle;
	private Integer id;
	private Job job;

	@Id
	@GeneratedValue(strategy = AUTO)
	public Integer getId() {
		return this.id;
	}

	@OneToOne
	public Job getJob() {
		return this.job;
	}

	public String getJobFullName() {
		return this.jobFullName;
	}

	@Lob
	@Column(columnDefinition = "BLOB")
	public byte[] getSerializedHandle() {
		return this.serializedHandle;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public void setJobFullName(String jobFullName) {
		this.jobFullName = jobFullName;
	}

	public void setSerializedHandle(byte[] serializedHandle) {
		this.serializedHandle = serializedHandle;
	}
}
