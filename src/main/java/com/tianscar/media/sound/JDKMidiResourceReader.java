package com.tianscar.media.sound;

import javasound.enhancement.midi.spi.MidiResourceReader;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.Sequence;
import javax.sound.midi.spi.MidiFileReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The default implementation of {@link MidiResourceReader}
 * which re-directs to {@link javax.sound.midi.spi.MidiFileReader}s bundled in JDK.
 *
 * @author Naoko Mitsurugi
 */
public class JDKMidiResourceReader extends MidiResourceReader {

    @Override
    public MidiFileFormat getMidiFileFormat(ClassLoader resourceLoader, String name) throws InvalidMidiDataException, IOException {
        try (InputStream stream = resourceLoader.getResourceAsStream(name)) {
            if (stream == null) throw new IOException("could not load resource \"" + name + "\" with ClassLoader \"" + resourceLoader + "\"");
            else {
                InputStream input = stream.markSupported() ? stream : new BufferedInputStream(stream);
                for (MidiFileReader reader : getMidiFileReaders()) {
                    try {
                        return reader.getMidiFileFormat(input);
                    } catch (InvalidMidiDataException ignored) {
                    }
                }
                throw new InvalidMidiDataException("Stream of unsupported format");
            }
        }
    }

    @Override
    public Sequence getSequence(ClassLoader resourceLoader, String name) throws InvalidMidiDataException, IOException {
        InputStream stream = resourceLoader.getResourceAsStream(name);
        if (stream == null) throw new IOException("could not load resource \"" + name + "\" with ClassLoader \"" + resourceLoader + "\"");
        else {
            InputStream input = stream.markSupported() ? stream : new BufferedInputStream(stream);
            for (MidiFileReader reader : getMidiFileReaders()) {
                try {
                    return reader.getSequence(input);
                } catch (InvalidMidiDataException ignored) {
                }
            }
            throw new InvalidMidiDataException("Stream of unsupported format");
        }
    }

    private static List<MidiFileReader> getMidiFileReaders() {
        List<MidiFileReader> readers = new ArrayList<>();
        for (MidiFileReader reader : ServiceLoader.load(MidiFileReader.class)) {
            if (reader.getClass().getName().startsWith("com.sun.media.sound")) readers.add(reader);
        }
        return readers;
    }

}
