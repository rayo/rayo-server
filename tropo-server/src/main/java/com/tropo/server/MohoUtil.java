package com.tropo.server;

import java.net.URI;

import com.tropo.core.verb.Ssml;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

public class MohoUtil {

    public static AudibleResource resolveAudio(final Ssml item) {
        return new AudibleResource() {
            public URI toURI() {
                return item.toUri();
            }
        };
    }

    public static OutputCommand output(Ssml items) {
        return new OutputCommand(resolveAudio(items));
    }

}
