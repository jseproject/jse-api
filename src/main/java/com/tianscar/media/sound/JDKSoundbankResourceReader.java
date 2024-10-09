package com.tianscar.media.sound;

import javasound.enhancement.midi.spi.SoundbankResourceReader;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The default implementation of {@link SoundbankResourceReader}
 * which re-directs to {@link javax.sound.midi.spi.SoundbankReader}s bundled in JDK.
 *
 * @author Naoko Mitsurugi
 */
public class JDKSoundbankResourceReader extends SoundbankResourceReader {

    @Override
    public Soundbank getSoundbank(ClassLoader resourceLoader, String name) throws InvalidMidiDataException, IOException {
        try (InputStream stream = resourceLoader.getResourceAsStream(name)) {
            if (stream == null) throw new IOException("could not load resource \"" + name + "\" with ClassLoader \"" + resourceLoader + "\"");
            else {
                InputStream input = stream.markSupported() ? stream : new BufferedInputStream(stream);
                for (SoundbankReader reader : getSoundbankReaders()) {
                    try {
                        return reader.getSoundbank(input);
                    } catch (InvalidMidiDataException ignored) {
                    }
                }
                throw new InvalidMidiDataException("Stream of unsupported format");
            }
        }
    }

    private static List<SoundbankReader> getSoundbankReaders() {
        List<SoundbankReader> readers = new ArrayList<>();
        for (SoundbankReader reader : ServiceLoader.load(SoundbankReader.class)) {
            if (reader.getClass().getName().startsWith("com.sun.media.sound")) readers.add(reader);
        }
        return readers;
    }

}
