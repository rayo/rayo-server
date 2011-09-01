package com.rayo.server.jmx;

import java.util.List;

import com.rayo.core.cdr.Cdr;

public interface CdrsMXBean {

	public List<Cdr> getActiveCDRs();
}
