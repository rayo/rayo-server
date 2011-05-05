package com.tropo.server;

import java.net.URI;
import java.util.List;

import com.tropo.core.verb.PromptItem;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

public class MohoUtil {

    public static AudibleResource resolveAudio(final PromptItem item) {
        return new AudibleResource() {

            public URI toURI() {
                return item.toUri();
            }
        };
    }

    public static AudibleResource[] resolveAudio(List<PromptItem> items) {
        AudibleResource[] result = new AudibleResource[items.size()];
        for (int i = 0; i < items.size(); i++) {
            result[i] = resolveAudio(items.get(i));
        }
        return result;
    }

    public static OutputCommand output(List<PromptItem> items) {
        return new OutputCommand(resolveAudio(items));
    }

}
