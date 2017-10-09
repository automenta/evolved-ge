/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.ege.distributed.master;

import it.units.malelab.ege.distributed.Job;
import it.units.malelab.ege.distributed.worker.WorkerMessage;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author eric
 */
public class ClientInfo {
  
  private final Set<Job> jobs;
  private WorkerMessage lastMessage;
  private Date lastContactDate;
  
  public ClientInfo() {
    this.jobs = Collections.synchronizedSet(new HashSet<Job>());
  }

  public WorkerMessage getLastMessage() {
    return lastMessage;
  }

  public Set<Job> getJobs() {
    return jobs;
  }

  public Date getLastContactDate() {
    return lastContactDate;
  }

  public void setLastMessage(WorkerMessage lastMessage) {
    this.lastMessage = lastMessage;
  }

  public void setLastContactDate(Date lastContactDate) {
    this.lastContactDate = lastContactDate;
  }
    
}
