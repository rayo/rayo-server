package com.rayo.core;

import java.net.InetAddress;

import com.voxeo.guido.Guido;
import com.voxeo.guido.GuidoException;
import com.voxeo.logging.Loggerf;
import com.voxeo.utils.IdGenerator;
import com.voxeo.utils.Networks;

public class GuidoIdGenerator implements IdGenerator<String> {

    private static Loggerf log = Loggerf.getLogger(GuidoIdGenerator.class);

    private InetAddress resolvedAddress;

    public GuidoIdGenerator() {
        this(null);
    }

    public GuidoIdGenerator(String networkAddress) {

        if (networkAddress == null) {
            resolvedAddress = Networks.extractDefaultLocalInetAddress();
        }
        else {
            resolvedAddress = Networks.findInetAddressByIp(networkAddress);
        }

        log.info("Created GuidoIdGenerator [nic=%s]", resolvedAddress);

    }

    @Override
    public String makeId() {
        try {
            Guido guido = new Guido(resolvedAddress, null);
            return guido.toString();
        }
        catch (GuidoException e) {
            throw new IllegalStateException(e);
        }
    }

}
