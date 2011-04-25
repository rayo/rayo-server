package com.tropo.core.verb;

import java.util.Collection;
import java.util.LinkedList;

public class ChoicesList extends LinkedList<Choices> {

    public ChoicesList() {
        super();
    }

    public ChoicesList(Collection<? extends Choices> c) {
        super(c);
    }

}
