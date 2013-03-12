package com.rayo.core;

import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;

/**
 * Used to specify the directionality of an intercepted network call.
 * 
 * This is not the same as {@link IncomingCall} vs {@link OutgoingCall} from a platform
 * perspective but instead is used to differentiate between a call made <b>to</b> a mobile
 * subscriber vs a call originated <b>by</b> the same mobile subscriber.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5502">RFC5502</a>  
 * 
 * 
 * 
 * @author jdecastro
 *
 */
public enum CallDirection {
    TERM, ORIG
}