package com.tropo.server.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class UnclosablePrintStream extends PrintStream {

    public UnclosablePrintStream(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(file, csn);
    }

    public UnclosablePrintStream(File file) throws FileNotFoundException {
        super(file);
    }

    public UnclosablePrintStream(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException {
        super(out, autoFlush, encoding);
    }

    public UnclosablePrintStream(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public UnclosablePrintStream(OutputStream out) {
        super(out);
    }

    public UnclosablePrintStream(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(fileName, csn);
    }

    public UnclosablePrintStream(String fileName) throws FileNotFoundException {
        super(fileName);
    }
    
    @Override
    public void close() {
        // Do Nothing
    }

}
