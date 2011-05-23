package com.tropo.server;

import java.net.URI;

import com.tropo.core.verb.SsmlItem;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

public class MohoUtil {

    public static AudibleResource resolveAudio(final SsmlItem item) {
        return new AudibleResource() {
            public URI toURI() {
                return item.toUri();
            }
        };
    }

    public static OutputCommand output(SsmlItem items) {
        return new OutputCommand(resolveAudio(items));
    }

}
