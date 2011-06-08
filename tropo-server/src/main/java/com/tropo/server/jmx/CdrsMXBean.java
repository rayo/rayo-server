package com.tropo.server.jmx;

import java.util.List;

import com.tropo.core.cdr.Cdr;

public interface CdrsMXBean {

	public List<Cdr> getActiveCDRs();
}
