/*******************************************************************************
 * Copyright 2013 Università degli Studi di Firenze
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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.User;
import it.drwolf.ridire.session.CrawlerManager;
import it.drwolf.ridire.util.exceptions.HeritrixException;

import java.util.Date;

import javax.ejb.Local;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.annotations.async.Expiration;
import org.jboss.seam.annotations.async.FinalExpiration;
import org.jboss.seam.annotations.async.IntervalCron;
import org.jboss.seam.async.QuartzTriggerHandle;

@Name("scheduledJobExecutor")
@Scope(ScopeType.APPLICATION)
@Local
public class ScheduledJobExecutor {

	@In(create = true)
	private CrawlerManager crawlerManager;

	@Asynchronous
	@Transactional
	public QuartzTriggerHandle schedulePeriodicJob(
			@Expiration Date expirationDate, @IntervalCron String cronData,
			@FinalExpiration Date endDate, Job job, User user)
			throws HeritrixException {
		QuartzTriggerHandle handle = new QuartzTriggerHandle(
				"RIDIRE periodic job scheduler");
		this.crawlerManager.reRunJob(job, user);
		return handle;
	}

	@Asynchronous
	@Transactional
	public QuartzTriggerHandle scheduleRunOnceJob(
			@Expiration Date expirationDate, Job job, User user)
			throws HeritrixException {
		QuartzTriggerHandle handle = new QuartzTriggerHandle(
				"RIDIRE runonce job scheduler");
		this.crawlerManager.reRunJob(job, user);
		return handle;
	}

}
