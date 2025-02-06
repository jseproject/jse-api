package io.github.jseproject;

import javasound.sampled.spi.AudioCompressionWriter;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.AudioFileWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * The default implementation of {@link AudioCompressionWriter}
 * which re-directs to {@link javax.sound.sampled.spi.AudioFileWriter}s bundled in JDK.
 *
 * @author Naoko Mitsurugi
 */
public class JDKAudioCompressionWriter extends AudioCompressionWriter {

    private static final AudioFileFormat.Type[] TYPES = new AudioFileFormat.Type[] {
            AudioFileFormat.Type.WAVE, AudioFileFormat.Type.AU, AudioFileFormat.Type.SND,
            AudioFileFormat.Type.AIFF
    };
    private static final AudioFileFormat.Type[] EMPTY_TYPE_ARRAY = new AudioFileFormat.Type[0];

    @Override
    public AudioFileFormat.Type[] getAudioFileTypes() {
        return TYPES.clone();
    }

    @Override
    public AudioFileFormat.Type[] getAudioFileTypes(AudioInputStream stream) {
        Set<AudioFileFormat.Type> types = new HashSet<>();
        for (AudioFileWriter writer : getAudioFileWriters()) {
            Collections.addAll(types, writer.getAudioFileTypes(stream));
        }
        return types.toArray(EMPTY_TYPE_ARRAY);
    }

    @Override
    public int write(AudioInputStream stream, AudioFileFormat.Type fileType, Map<String, Object> properties, OutputStream out) throws IOException, IllegalArgumentException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(out);
        for (AudioFileWriter writer : getAudioFileWriters()) {
            try {
                return writer.write(stream, fileType, out);
            } catch (IllegalArgumentException ignored) {
                // thrown if this provider cannot write the stream, try next
            }
        }
        throw new IllegalArgumentException("could not write audio file: file type not supported: " + fileType);
    }

    @Override
    public int write(AudioInputStream stream, AudioFileFormat.Type fileType, Map<String, Object> properties, File out) throws IOException, IllegalArgumentException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(out);
        for (AudioFileWriter writer : getAudioFileWriters()) {
            try {
                return writer.write(stream, fileType, out);
            } catch (IllegalArgumentException ignored) {
                // thrown if this provider cannot write the stream, try next
            }
        }
        throw new IllegalArgumentException("could not write audio file: file type not supported: " + fileType);
    }

    private static List<AudioFileWriter> getAudioFileWriters() {
        List<AudioFileWriter> writers = new ArrayList<>();
        for (AudioFileWriter writer : ServiceLoader.load(AudioFileWriter.class)) {
            if (writer.getClass().getName().startsWith("com.sun.media.sound")) writers.add(writer);
        }
        return writers;
    }

}
