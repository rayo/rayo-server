package com.rayo.server.test;

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
import javax.media.mscontrol.mediagroup.MediaGroup;

import org.apache.log4j.Logger;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Participant;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.event.AcceptableEvent;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.AutowiredEventTarget;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.CallEvent;
import com.voxeo.moho.event.EarlyMediaEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventDispatcher;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.MohoCallCompleteEvent;
import com.voxeo.moho.event.MohoEarlyMediaEvent;
import com.voxeo.moho.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.RequestEvent;
import com.voxeo.moho.event.ResponseEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.moho.util.Utils;
import com.voxeo.moho.utils.EventListener;

public class MockCall implements IncomingCall {

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
    private MediaService<Call> mediaService;

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
        return new SimpleJoint(new MohoJoinCompleteEvent(this, this, Cause.JOINED, true));
    }

    @Override
    public Unjoint unjoin(Participant other) {
        return null;
    }

    @Override
    public Participant[] getParticipants() {
        return new Participant[] {};
    }

    @Override
    public boolean isHold() {
    	return false;
    }
    
    @Override
    public boolean isMute() {
    	return false;
    }
    
    @Override
    public Participant[] getParticipants(Direction direction) {
        return new Participant[] {};
    }

    @Override
    public void disconnect() {
        dispatch(new MohoCallCompleteEvent(this, com.voxeo.moho.event.CallCompleteEvent.Cause.NEAR_END_DISCONNECT));
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
      if (!(event instanceof CallEvent) && !(event instanceof RequestEvent) && !(event instanceof ResponseEvent)) {
        retval = this.internalDispatch(event);
      }
      else {
        final Runnable acceptor = new Runnable() {
          @Override
          public void run() {
            if (event instanceof EarlyMediaEvent) {
              if (!((MohoEarlyMediaEvent) event).isProcessed()) {
                try {
                  ((EarlyMediaEvent) event).reject(null);
                }
                catch (final SignalException e) {
                  log.warn(e);
                }
              }
            }

            else if (event instanceof AcceptableEvent) {
              if (!((AcceptableEvent) event).isAccepted() && !((AcceptableEvent) event).isRejected()) {
                try {
                  ((AcceptableEvent) event).accept();
                }
                catch (final SignalException e) {
                    log.warn(e);
                }
              }
            }

          }
        };
        retval = this.dispatch(event, acceptor);
      }
      return retval;
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
            internalDispatch(new MohoCallCompleteEvent(this, CallCompleteEvent.Cause.FORBIDDEN));
            break;
        case DECLINE:
            internalDispatch(new MohoCallCompleteEvent(this, CallCompleteEvent.Cause.DECLINE));
            break;
        case ERROR:
            internalDispatch(new MohoCallCompleteEvent(this, CallCompleteEvent.Cause.ERROR));
            break;
        case FORBIDEN:
            internalDispatch(new MohoCallCompleteEvent(this, CallCompleteEvent.Cause.FORBIDDEN));
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
        return new SimpleJoint(new MohoJoinCompleteEvent(this, this, Cause.JOINED, true));
    }

    @Override
    public Joint join(Direction direction) {
        return new SimpleJoint(new MohoJoinCompleteEvent(this, this, Cause.JOINED, true));
    }

    @Override
    public Joint join(CallableEndpoint other, JoinType type, Direction direction) {
        return new SimpleJoint(new MohoJoinCompleteEvent(this, this, Cause.JOINED, true));
    }

    @Override
    public Joint join(CallableEndpoint other, JoinType type, Direction direction, Map<String, String> headers) {
        return new SimpleJoint(new MohoJoinCompleteEvent(this, this, Cause.JOINED, true));
    }

    protected MediaService<Call> getMediaService(boolean reinvite) {
        return mediaService;
    }

    public MediaService<Call> getMediaService() {
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
    public void hangup(Map<String, String> headers) {
        dispatch(new MohoCallCompleteEvent(this, com.voxeo.moho.event.CallCompleteEvent.Cause.NEAR_END_DISCONNECT));
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
    public void acceptWithEarlyMedia(Map<String, String> headers) throws SignalException, MediaException, IllegalStateException {
    }

    @Override
    public void answer(Map<String, String> headers) throws SignalException {
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

    @Override
    public void hangup() {}

    @Override
    public void addObserver(Observer... observers) {
        for(Observer observer : observers) {
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
    }

    @Override
    public Output<Call> output(String text) throws MediaException {
        return mediaService.output(text);
    }

    @Override
    public Output<Call> output(URI media) throws MediaException {
        return mediaService.output(media);
    }

    @Override
    public Output<Call> output(OutputCommand output) throws MediaException {
        return mediaService.output(output);
    }

    @Override
    public Prompt<Call> prompt(String text, String grammar, int repeat) throws MediaException {
        return mediaService.prompt(text, grammar, repeat);
    }

    @Override
    public Prompt<Call> prompt(URI media, String grammar, int repeat) throws MediaException {
        return mediaService.prompt(media, grammar, repeat);
    }

    @Override
    public Prompt<Call> prompt(OutputCommand output, InputCommand input, int repeat) throws MediaException {
        return mediaService.prompt(output, input, repeat);
    }

    @Override
    public Input<Call> input(String grammar) throws MediaException {
        return mediaService.input(grammar);
    }

    @Override
    public Input<Call> input(InputCommand input) throws MediaException {
        return mediaService.input(input);
    }

    @Override
    public Recording<Call> record(URI recording) throws MediaException {
        return mediaService.record(recording);
    }

    @Override
    public Recording<Call> record(RecordCommand command) throws MediaException {
        return mediaService.record(command);
    }

    @Override
    public MediaGroup getMediaGroup() {
        return null;
    }

    @Override
    public Call getSource() {
        return null;
    }

    @Override
    public boolean isAccepted() {
        return false;
    }

    @Override
    public boolean isRejected() {
        return false;
    }

    @Override
    public void accept() throws SignalException {}

    @Override
    public void reject(Reason reason) throws SignalException {}

    @Override
    public boolean isRedirected() {
        return false;
    }

    @Override
    public void redirect(Endpoint other) throws SignalException {}

    @Override
    public boolean isAcceptedWithEarlyMedia() {
        return false;
    }

    @Override
    public void acceptWithEarlyMedia() throws SignalException, MediaException {}

    @Override
    public void acceptWithEarlyMedia(Observer... observer) throws SignalException, MediaException {}

    @Override
    public void accept(Observer... observer) throws SignalException {}

    @Override
    public void answer() throws SignalException, MediaException {}

    @Override
    public void answer(Observer... observer) throws SignalException, MediaException {}

    @Override
    public void proxyTo(boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException {}

    @Override
    public boolean isProxied() {
    	return false;
    }
    
    @Override
    public void proxyTo(boolean recordRoute, boolean parallel,
    		Map<String, String> headers, Endpoint... destinations) {
    }
}
