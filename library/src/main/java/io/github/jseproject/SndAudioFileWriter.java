package io.github.jseproject;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.AudioFileWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The missing {@code .snd} {@link AudioFileWriter} which re-directs to {@code .au} {@link AudioFileWriter} bundled in JDK.
 *
 * @author Naoko Mitsurugi
 */
public class SndAudioFileWriter extends AudioFileWriter {

    private static final AudioFileFormat.Type[] EMPTY_TYPE_ARRAY = new AudioFileFormat.Type[0];
    private static final AudioFileFormat.Type[] TYPES = new AudioFileFormat.Type[] { AudioFileFormat.Type.SND };

    @Override
    public AudioFileFormat.Type[] getAudioFileTypes() {
        return TYPES.clone();
    }

    @Override
    public AudioFileFormat.Type[] getAudioFileTypes(AudioInputStream stream) {
        AudioFormat.Encoding encoding = stream.getFormat().getEncoding();
        if (AudioFormat.Encoding.ALAW.equals(encoding) || AudioFormat.Encoding.ULAW.equals(encoding)
                || AudioFormat.Encoding.PCM_SIGNED.equals(encoding) || AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)
                || AudioFormat.Encoding.PCM_FLOAT.equals(encoding)) return TYPES.clone();
        else return EMPTY_TYPE_ARRAY;
    }

    @Override
    public int write(AudioInputStream stream, AudioFileFormat.Type fileType, OutputStream out) throws IOException {
        if (!isFileTypeSupported(fileType, stream)) throw new IllegalArgumentException("File type " + fileType + " not supported.");
        else return AudioSystem.write(stream, AudioFileFormat.Type.AU, out);
    }

    @Override
    public int write(AudioInputStream stream, AudioFileFormat.Type fileType, File out) throws IOException {
        if (!isFileTypeSupported(fileType, stream)) throw new IllegalArgumentException("File type " + fileType + " not supported.");
        else return AudioSystem.write(stream, AudioFileFormat.Type.AU, out);
    }

}
