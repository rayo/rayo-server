package com.rayo.server.ameche;

import java.util.List;

import org.dom4j.Element;

import com.voxeo.utils.Lists;

public class FixedAppInstanceResolver implements AppInstanceResolver {

    private AppInstance appInstance;

    @Override
    public List<AppInstance> lookup(Element offer) {
        return Lists.make(appInstance);
    }

    public AppInstance getAppInstance() {
        return appInstance;
    }

    public void setAppInstance(AppInstance appInstance) {
        this.appInstance = appInstance;
    }

}
