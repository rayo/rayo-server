package com.tropo.core;

import com.voxeo.utils.Identifiable;

public interface Registry<T extends Identifiable<I>, I> {

    public void register(T identifiable);

    public T unregister(I id);

    public T lookup(I id);

    public boolean contains(I id);

    public int getCount();

}
