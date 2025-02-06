package io.github.jseproject;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Extended {@link AudioFileFormat} which holds {@code long} byteLength and {@code long} frameLength.
 * It also exposes the byteLength parameter in the protected constructor of its super class.
 *
 * @see #getByteLengthLong()
 * @see #getFrameLengthLong()
 * @see AudioFileFormat
 */
public class ExtendedAudioFileFormat extends AudioFileFormat {

    private final long byteLength;
    private final long frameLength;

    private final HashMap<String, Object> properties;

    /**
     * Constructs an audio file format object. This protected constructor is
     * intended for use by providers of file-reading services when returning
     * information about an audio file or about supported audio file formats.
     *
     * @param  type the type of the audio file
     * @param  byteLength the length of the file in bytes, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     */
    public ExtendedAudioFileFormat(Type type, AudioFormat format, long frameLength, long byteLength) {
        super(type, round(byteLength), format, round(frameLength));
        this.properties = null;
        this.byteLength = byteLength;
        this.frameLength = frameLength;
    }

    /**
     * Constructs an audio file format object. This protected constructor is
     * intended for use by providers of file-reading services when returning
     * information about an audio file or about supported audio file formats.
     *
     * @param  type the type of the audio file
     * @param  byteLength the length of the file in bytes, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @param  properties a {@code Map<String, Object>} object with properties
     */
    public ExtendedAudioFileFormat(Type type, AudioFormat format, long frameLength, long byteLength, Map<String, Object> properties) {
        super(type, round(byteLength), format, round(frameLength));
        this.properties = new HashMap<>(properties);
        this.byteLength = byteLength;
        this.frameLength = frameLength;
    }

    /**
     * Constructs an audio file format object. This public constructor may be
     * used by applications to describe the properties of a requested audio
     * file.
     *
     * @param  type the type of the audio file
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     */
    public ExtendedAudioFileFormat(Type type, AudioFormat format, long frameLength) {
        super(type, format, round(frameLength));
        this.properties = null;
        this.byteLength = AudioSystem.NOT_SPECIFIED;
        this.frameLength = frameLength;
    }

    /**
     * Construct an audio file format object with a set of defined properties.
     * This public constructor may be used by applications to describe the
     * properties of a requested audio file. The properties map will be copied
     * to prevent any changes to it.
     *
     * @param  type the type of the audio file
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @param  properties a {@code Map<String, Object>} object with properties
     */
    public ExtendedAudioFileFormat(Type type, AudioFormat format, long frameLength, Map<String, Object> properties) {
        super(type, format, round(frameLength));
        this.properties = new HashMap<>(properties);
        this.byteLength = AudioSystem.NOT_SPECIFIED;
        this.frameLength = frameLength;
    }

    private static int round(long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) return AudioSystem.NOT_SPECIFIED;
        return (int) value;
    }

    /**
     * Obtains the size in bytes of the entire audio file (not just its audio
     * data).
     *
     * @return the audio file length in bytes
     * @see AudioSystem#NOT_SPECIFIED
     */
    public long getByteLengthLong() {
        return byteLength;
    }

    /**
     * Obtains the length of the audio data contained in the file, expressed in
     * sample frames.
     *
     * @return the number of sample frames of audio data in the file
     * @see AudioSystem#NOT_SPECIFIED
     */
    public long getFrameLengthLong() {
        return frameLength;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> properties() {
        Map<String, Object> result;
        if (properties == null) result = new HashMap<>(0);
        else result = (Map<String, Object>) properties.clone();
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Object getProperty(String key) {
        if (properties == null) return null;
        return properties.get(key);
    }

}
