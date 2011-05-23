package com.tropo.server.verb;

import java.net.URI;

import com.tropo.core.verb.Ssml;
import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbEvent;
import com.voxeo.moho.Call;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

public abstract class AbstractLocalVerbHandler<T extends Verb> implements VerbHandler<T> {

    protected T model;
    protected Call call;
    protected MediaService media;
    private EventDispatcher eventDispatcher;

    private volatile boolean complete = false;

    @Override
    public void onCommand(VerbCommand command) {}
    
    @Override
    public Verb getModel() {
        return model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setModel(Verb model) {
        this.model = (T)model;
    }

    @Override
    public Call getCall() {
        return call;
    }

    @Override
    public void setCall(Call call) {
        this.call = call;
        this.media = call.getMediaService();
    }

    protected AudibleResource resolveAudio(final Ssml item) {
        return new AudibleResource() {
            public URI toURI() {
                return item.toUri();
            }
        };
    }

    protected OutputCommand output(Ssml items) {
        return new OutputCommand(resolveAudio(items));
    }

    protected void complete(VerbCompleteEvent event) {
        if (!complete) {
            complete = true;
            eventDispatcher.fire(event);
        }
    }
    
    protected void fire(VerbEvent event) {
        eventDispatcher.fire(event);
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public void setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }
    
    @Override
    public boolean isComplete() {
        return complete;
    }

}