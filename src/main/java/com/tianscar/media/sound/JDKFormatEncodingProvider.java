package com.tianscar.media.sound;

import javasound.enhancement.sampled.spi.AudioCompressionWriter;
import javasound.enhancement.sampled.spi.FormatEncodingProvider;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.AudioFileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * The default implementation of {@link FormatEncodingProvider}
 * which obtains the file encodings and types for which file reading and writing support
 * are provided by JDK.
 *
 * @author Naoko Mitsurugi
 */
public class JDKFormatEncodingProvider extends FormatEncodingProvider {

    private static final AudioFormat.Encoding[] ENCODINGS = new AudioFormat.Encoding[] {
            AudioFormat.Encoding.ALAW, AudioFormat.Encoding.ULAW,
            AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED,
            AudioFormat.Encoding.PCM_FLOAT
    };
    private static final AudioFormat.Encoding[] EMPTY_ENCODING_ARRAY = new AudioFormat.Encoding[0];

    @Override
    public AudioFormat.Encoding[] getReaderEncodings() {
        return ENCODINGS.clone();
    }

    @Override
    public boolean isReaderSupportedEncoding(AudioFormat.Encoding encoding) {
        for (AudioFormat.Encoding e : ENCODINGS) {
            if (e.equals(encoding)) return true;
        }
        return false;
    }

    @Override
    public AudioFormat.Encoding[] getWriterEncodings() {
        return ENCODINGS.clone();
    }

    @Override
    public boolean isWriterSupportedEncoding(AudioFormat.Encoding encoding) {
        for (AudioFormat.Encoding e : ENCODINGS) {
            if (e.equals(encoding)) return true;
        }
        return false;
    }

    @Override
    public AudioFormat.Encoding[] getWriterEncodings(AudioFileFormat.Type fileType) {
        if (isWriterSupportedFileType(fileType)) return ENCODINGS.clone();
        else return EMPTY_ENCODING_ARRAY;
    }

    @Override
    public AudioFormat.Encoding[] getWriterEncodings(AudioInputStream stream) {
        if (getWriterFileTypes(stream).length > 0) return ENCODINGS.clone();
        else return EMPTY_ENCODING_ARRAY;
    }

    @Override
    public boolean isWriterSupportedEncoding(AudioFormat.Encoding encoding, AudioInputStream stream) {
        if (getWriterFileTypes(stream).length > 0) return isWriterSupportedEncoding(encoding);
        else return false;
    }

    @Override
    public AudioFormat.Encoding getEncodingByFormatName(String formatName) {
        for (AudioFormat.Encoding encoding : ENCODINGS) {
            if (encoding.toString().equalsIgnoreCase(formatName)) return encoding;
        }
        return null;
    }

    private static final AudioFileFormat.Type[] READER_TYPES = new AudioFileFormat.Type[] {
            AudioFileFormat.Type.WAVE, AudioFileFormat.Type.AU, AudioFileFormat.Type.SND,
            AudioFileFormat.Type.AIFF, AudioFileFormat.Type.AIFC
    };
    private static final AudioFileFormat.Type[] WRITER_TYPES = new AudioFileFormat.Type[] {
            AudioFileFormat.Type.WAVE, AudioFileFormat.Type.AU, AudioFileFormat.Type.SND,
            AudioFileFormat.Type.AIFF
    };
    private static final AudioFileFormat.Type[] EMPTY_TYPE_ARRAY = new AudioFileFormat.Type[0];

    @Override
    public AudioFileFormat.Type[] getReaderFileTypes() {
        return READER_TYPES.clone();
    }

    @Override
    public boolean isReaderSupportedFileType(AudioFileFormat.Type fileType) {
        for (AudioFileFormat.Type type : READER_TYPES) {
            if (type.equals(fileType)) return true;
        }
        return false;
    }

    @Override
    public AudioFileFormat.Type[] getWriterFileTypes() {
        return WRITER_TYPES.clone();
    }

    @Override
    public boolean isWriterSupportedFileType(AudioFileFormat.Type fileType) {
        for (AudioFileFormat.Type type : WRITER_TYPES) {
            if (type.equals(fileType)) return true;
        }
        return false;
    }

    @Override
    public AudioFileFormat.Type[] getWriterFileTypes(AudioInputStream stream) {
        Set<AudioFileFormat.Type> types = new HashSet<>();
        for (AudioFileFormat.Type type : getAudioFileTypes(stream)) {
            if (isWriterSupportedFileType(type)) types.add(type);
        }
        return types.toArray(EMPTY_TYPE_ARRAY);
    }

    private static AudioFileFormat.Type[] getAudioFileTypes(AudioInputStream stream) {
        Objects.requireNonNull(stream);
        Set<AudioFileFormat.Type> result = new HashSet<>();
        for (AudioFileWriter writer : getAudioFileWriters()) {
            Collections.addAll(result, writer.getAudioFileTypes(stream));
        }
        for (AudioCompressionWriter writer : getAudioCompressionWriters()) {
            Collections.addAll(result, writer.getAudioFileTypes(stream));
        }
        return result.toArray(EMPTY_TYPE_ARRAY);
    }

    @Override
    public boolean isWriterSupportedFileType(AudioFileFormat.Type fileType, AudioInputStream stream) {
        return isWriterSupportedFileType(fileType) && isFileTypeSupported(fileType, stream);
    }

    private static boolean isFileTypeSupported(AudioFileFormat.Type fileType, AudioInputStream stream) {
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(stream);
        for (AudioFileWriter writer : getAudioFileWriters()) {
            if (writer.isFileTypeSupported(fileType, stream)) {
                return true;
            }
        }
        for (AudioCompressionWriter writer : getAudioCompressionWriters()) {
            if (writer.isFileTypeSupported(fileType, stream)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AudioFileFormat.Type getFileTypeByFormatName(String formatName) {
        for (AudioFileFormat.Type type : READER_TYPES) {
            if (type.toString().equalsIgnoreCase(formatName)) return type;
        }
        return null;
    }

    @Override
    public AudioFileFormat.Type getFileTypeBySuffix(String suffix) {
        for (AudioFileFormat.Type type : READER_TYPES) {
            if (type.getExtension().equalsIgnoreCase(suffix)) return type;
        }
        return null;
    }

    private static List<AudioFileWriter> getAudioFileWriters() {
        List<AudioFileWriter> writers = new ArrayList<>();
        for (AudioFileWriter writer : ServiceLoader.load(AudioFileWriter.class)) {
            if (writer.getClass().getName().startsWith("com.sun.media.sound")) writers.add(writer);
        }
        return writers;
    }

    private static List<AudioCompressionWriter> getAudioCompressionWriters() {
        List<AudioCompressionWriter> writers = new ArrayList<>();
        for (AudioCompressionWriter writer : ServiceLoader.load(AudioCompressionWriter.class)) {
            if (writer.getClass() == JDKAudioCompressionWriter.class) writers.add(writer);
        }
        return writers;
    }

}
