package com.tianscar.media.sound;

import javasound.enhancement.sampled.spi.AudioResourceReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The default implementation of {@link AudioResourceReader}
 * which re-directs to {@link javax.sound.sampled.spi.AudioFileReader}s bundled in JDK.
 *
 * @author Naoko Mitsurugi
 */
public class JDKAudioResourceReader extends AudioResourceReader {

    @Override
    public AudioFileFormat getAudioFileFormat(ClassLoader resourceLoader, String name) throws UnsupportedAudioFileException, IOException {
        try (InputStream stream = resourceLoader.getResourceAsStream(name)) {
            if (stream == null) throw new IOException("could not load resource \"" + name + "\" with ClassLoader \"" + resourceLoader + "\"");
            else {
                InputStream input = stream.markSupported() ? stream : new BufferedInputStream(stream);
                for (AudioFileReader reader : getAudioFileReaders()) {
                    try {
                        return reader.getAudioFileFormat(input);
                    } catch (UnsupportedAudioFileException ignored) {
                    }
                }
                throw new UnsupportedAudioFileException("Stream of unsupported format");
            }
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(ClassLoader resourceLoader, String name) throws UnsupportedAudioFileException, IOException {
        InputStream stream = resourceLoader.getResourceAsStream(name);
        if (stream == null) throw new IOException("could not load resource \"" + name + "\" with ClassLoader \"" + resourceLoader + "\"");
        else {
            InputStream input = stream.markSupported() ? stream : new BufferedInputStream(stream);
            for (AudioFileReader reader : getAudioFileReaders()) {
                try {
                    return reader.getAudioInputStream(input);
                } catch (UnsupportedAudioFileException ignored) {
                }
            }
            throw new UnsupportedAudioFileException("Stream of unsupported format");
        }
    }

    private static List<AudioFileReader> getAudioFileReaders() {
        List<AudioFileReader> readers = new ArrayList<>();
        for (AudioFileReader reader : ServiceLoader.load(AudioFileReader.class)) {
            if (reader.getClass().getName().startsWith("com.sun.media.sound")) readers.add(reader);
        }
        return readers;
    }

}
