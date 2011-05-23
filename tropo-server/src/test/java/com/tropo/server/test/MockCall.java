package com.tropo.server.test;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableStream;
import javax.media.mscontrol.join.JoinableStream.StreamType;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.ExceptionHandler;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Participant;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.AutowiredEventTarget;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.EarlyMediaEvent;
import com.voxeo.moho.event.EventDispatcher;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.ForwardableEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.SignalEvent;
import com.voxeo.moho.util.Utils;
import com.voxeo.moho.utils.Event;
import com.voxeo.moho.utils.EventListener;

public class MockCall extends Call {

    private static final Logger log = Logger.getLogger(MockCall.class);

    private URI to;
    private URI from;
    private String id;
    private State state = State.ACCEPTED;
    private Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private ApplicationContext applicationContext;

    protected EventDispatcher _dispatcher = new EventDispatcher(Executors.newFixedThreadPool(10));
    protected Map<String, String> _states = new ConcurrentHashMap<String, String>();
    protected ConcurrentHashMap<Observer, AutowiredEventListener> _observers = new ConcurrentHashMap<Observer, AutowiredEventListener>();

    // attribute store
    private Map<String, Object> _attributes = new ConcurrentHashMap<String, Object>();
    private boolean supervised;
    private MediaService mediaService;

    public MockCall() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public JoinableStream[] getJoinableStreams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Endpoint getAddress() {
        return new SimpleEndpoint(to);
    }

    @Override
    public Joint join(Participant other, JoinType type, Direction direction) {
        return new SimpleJoint(new JoinCompleteEvent(this, this, Cause.JOINED));
    }

    @Override
    public void unjoin(Participant other) {}

    @Override
    public Participant[] getParticipants() {
        return new Participant[] {};
    }

    @Override
    public Participant[] getParticipants(Direction direction) {
        return new Participant[] {};
    }

    @Override
    public void disconnect() {
        dispatch(new CallCompleteEvent(this, com.voxeo.moho.event.CallCompleteEvent.Cause.NEAR_END_DISCONNECT));
    }

    @Override
    public MediaObject getMediaObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getApplicationState() {
        return _states.get(AutowiredEventTarget.DEFAULT_FSM);
    }

    @Override
    public String getApplicationState(String FSM) {
        return _states.get(FSM);
    }

    @Override
    public void setApplicationState(String state) {
        _states.put(AutowiredEventTarget.DEFAULT_FSM, state);
    }

    @Override
    public void setApplicationState(String FSM, String state) {
        _states.put(FSM, state);
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void addListener(final EventListener<?> listener) {
        if (listener != null) {
            _dispatcher.addListener(Event.class, listener);
        }
    }

    public void addListeners(final EventListener<?>... listeners) {
        if (listeners != null) {
            for (final EventListener<?> listener : listeners) {
                this.addListener(listener);
            }
        }
    }

    public <E extends Event<?>, T extends EventListener<E>> void addListener(final Class<E> type, final T listener) {
        if (listener != null) {
            _dispatcher.addListener(type, listener);
        }
    }

    @Override
    public <E extends Event<?>, T extends EventListener<E>> void addListeners(final Class<E> type, final T... listeners) {
        if (listeners != null) {
            for (final T listener : listeners) {
                this.addListener(type, listener);
            }
        }
    }

    public void addObserver(final Observer observer) {
        if (observer != null) {
            if (observer instanceof EventListener) {
                @SuppressWarnings("rawtypes")
                EventListener l = (EventListener) observer;
                @SuppressWarnings("rawtypes")
                Class claz = Utils.getGenericType(observer);
                if (claz == null) {
                    claz = Event.class;
                }
                _dispatcher.addListener(claz, l);
            } else {
                final AutowiredEventListener autowire = new AutowiredEventListener(observer);
                if (_observers.putIfAbsent(observer, autowire) == null) {
                    _dispatcher.addListener(Event.class, autowire);
                }
            }

        }
    }

    @Override
    public void addObservers(final Observer... observers) {
        if (observers != null) {
            for (final Observer o : observers) {
                addObserver(o);
            }
        }
    }

    @Override
    public void removeListener(final EventListener<?> listener) {
        _dispatcher.removeListener(listener);
    }

    @Override
    public void removeObserver(final Observer listener) {
        final AutowiredEventListener autowiredEventListener = _observers.remove(listener);
        if (autowiredEventListener != null) {
            _dispatcher.removeListener(autowiredEventListener);
        }
    }

    @Override
    public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event, final Runnable afterExec) {
        return _dispatcher.fire(event, true, afterExec);
    }

    public <S extends EventSource, T extends Event<S>> Future<T> internalDispatch(final T event) {
        return _dispatcher.fire(event, true, null);
    }

    @Override
    public <S extends EventSource, T extends Event<S>> Future<T> dispatch(final T event) {
        Future<T> retval = null;
        if (!(event instanceof SignalEvent)) {
            retval = this.internalDispatch(event);
        } else {
            final Runnable acceptor = new Runnable() {

                @Override
                public void run() {
                    if (!((SignalEvent) event).isProcessed()) {
                        try {
                            if (event instanceof EarlyMediaEvent) {
                                ((EarlyMediaEvent) event).reject(null);
                            } else {
                                ((SignalEvent) event).accept();
                            }
                        } catch (final SignalException e) {
                            log.warn("", e);
                        }
                    }
                }
            };
            if (isSupervised() || event instanceof ForwardableEvent) {
                retval = this.dispatch(event, acceptor);
            } else {
                acceptor.run();
            }
            // retval = super.dispatch(event, acceptor);
        }
        return retval;
    }

    @Override
    public void addExceptionHandler(final ExceptionHandler... handlers) {
        _dispatcher.addExceptionHandler(handlers);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Object getAttribute(final String name) {
        if (name == null) {
            return null;
        }
        return _attributes.get(name);
    }

    @Override
    public Map<String, Object> getAttributeMap() {
        return new HashMap<String, Object>(_attributes);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        if (value == null) {
            _attributes.remove(name);
        } else {
            _attributes.put(name, value);
        }
    }

    @Override
    public void reject(Reason reason, Map<String, String> headers) throws SignalException {
        switch (reason) {
        case BUSY:
            internalDispatch(new CallCompleteEvent(this, CallCompleteEvent.Cause.FORBIDDEN));
            break;
        case DECLINE:
            internalDispatch(new CallCompleteEvent(this, CallCompleteEvent.Cause.DECLINE));
            break;
        case ERROR:
            internalDispatch(new CallCompleteEvent(this, CallCompleteEvent.Cause.ERROR));
            break;
        case FORBIDEN:
            internalDispatch(new CallCompleteEvent(this, CallCompleteEvent.Cause.FORBIDDEN));
            break;
        }
    }

    @Override
    public void redirect(Endpoint other, Map<String, String> headers) throws SignalException, IllegalArgumentException {

    }

    @Override
    public void accept(Map<String, String> headers) throws SignalException, IllegalStateException {}

    @Override
    public Joint join() {
        return new SimpleJoint(new JoinCompleteEvent(this, this, Cause.JOINED));
    }

    @Override
    public Joint join(Direction direction) {
        return new SimpleJoint(new JoinCompleteEvent(this, this, Cause.JOINED));
    }

    @Override
    public Joint join(CallableEndpoint other, JoinType type, Direction direction) {
        return new SimpleJoint(new JoinCompleteEvent(this, this, Cause.JOINED));
    }

    @Override
    public Joint join(CallableEndpoint other, JoinType type, Direction direction, Map<String, String> headers) {
        return new SimpleJoint(new JoinCompleteEvent(this, this, Cause.JOINED));
    }

    @Override
    public boolean isSupervised() {
        return supervised;
    }

    @Override
    public void setSupervised(boolean supervised) {
        this.supervised = supervised;
    }

    @Override
    public MediaService getMediaService(boolean reinvite) {
        return mediaService;
    }

    @Override
    public MediaService getMediaService() {
        return mediaService;
    }

    @Override
    public State getCallState() {
        return state;
    }

    @Override
    public Call[] getPeers() {
        return new Call[] {};
    }

    @Override
    public void mute() {}

    @Override
    public void unmute() {}

    @Override
    public void hold() {}

    @Override
    public void unhold() {}

    @Override
    public void disconnect(Map<String, String> headers) {
        dispatch(new CallCompleteEvent(this, com.voxeo.moho.event.CallCompleteEvent.Cause.NEAR_END_DISCONNECT));
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name) != null ? headers.get(name).get(0) : null;
    }

    @Override
    public ListIterator<String> getHeaders(String name) {
        return headers.get(name) != null ? headers.get(name).listIterator() : null;
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return headers.keySet().iterator();
    }

    @Override
    public Endpoint getInvitor() {
        return new SimpleEndpoint(from);
    }

    @Override
    public CallableEndpoint getInvitee() {
        return new SimpleEndpoint(to);
    }

    @Override
    public Call acceptCall(Map<String, String> headers, Observer... observer) throws SignalException, IllegalStateException {
        return this;
    }

    @Override
    public Call acceptCallWithEarlyMedia(Map<String, String> headers, Observer... observers) throws SignalException, MediaException, IllegalStateException {
        return this;
    }

    @Override
    public Call answer(Map<String, String> headers, Observer... observer) throws SignalException, IllegalStateException {
        return this;
    }

    public void setTo(URI to) {
        this.to = to;
    }

    public URI getTo() {
        return to;
    }

    public void setMediaService(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Override
    public JoinableStream getJoinableStream(StreamType value) {
        throw new UnsupportedOperationException();
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        if (headers != null) {
            this.headers = headers;
        }
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    public URI getFrom() {
        return from;
    }

}
