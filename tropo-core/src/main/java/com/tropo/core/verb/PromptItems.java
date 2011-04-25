package com.tropo.core.verb;

import java.util.Collection;
import java.util.LinkedList;

public class PromptItems extends LinkedList<PromptItem> {

    public PromptItems() {
        super();
    }

    public PromptItems(Collection<? extends PromptItem> c) {
        super(c);
    }

}
