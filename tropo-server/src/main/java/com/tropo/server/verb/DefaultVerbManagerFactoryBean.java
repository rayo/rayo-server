package com.tropo.server.verb;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class DefaultVerbManagerFactoryBean implements FactoryBean<VerbManager>, InitializingBean  {

    private VerbManager verbManager;
    private List<VerbFactory> verbFactories;

    @Override
    public void afterPropertiesSet() throws Exception {
        if(verbManager == null) {
            verbManager = new DefaultVerbManager();
        }
        for (VerbFactory factory : verbFactories) {
            verbManager.registerVerbFactory(factory);
        }
    }
    
    @Override
    public VerbManager getObject() throws Exception {
        return verbManager;
    }

    @Override
    public Class<?> getObjectType() {
        return VerbManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setVerbFactoryList(List<VerbFactory> verbFactories) {
        this.verbFactories = verbFactories;
    }

    public List<VerbFactory> getVerbFactories() {
        return verbFactories;
    }

    public VerbManager getVerbManager() {
        return verbManager;
    }

    public void setVerbManager(VerbManager verbManager) {
        this.verbManager = verbManager;
    }

}
