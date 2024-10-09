package javasound.enhancement.sampled;

import com.tianscar.media.sound.ExtendedAudioFileFormat;
import javasound.enhancement.sampled.spi.AudioCompressionWriter;
import javasound.enhancement.sampled.spi.FormatEncodingProvider;
import javasound.enhancement.sampled.spi.AudioResourceReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioPermission;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.MixerProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * The {@code Audios} class acts as the entry point to the sampled-audio
 * system resources. This class lets you query and access the mixers that are
 * installed on the system. {@code Audios} includes a number of methods for
 * converting audio data between different formats, and for translating between
 * audio files and streams. It also provides a method for obtaining a
 * {@link Line} directly from the {@code Audios} without dealing explicitly
 * with mixers.
 * <p>
 * Properties can be used to specify the default mixer for specific line types.
 * Both system properties and a properties file are considered. The
 * "sound.properties" properties file is read from an implementation-specific
 * location (typically it is the {@code conf} directory in the Java installation
 * directory). The optional "javax.sound.config.file" system property can be
 * used to specify the properties file that will be read as the initial
 * configuration. If a property exists both as a system property and in the
 * properties file, the system property takes precedence. If none is specified,
 * a suitable default is chosen among the available devices. The syntax of the
 * properties file is specified in
 * {@link Properties#load(InputStream) Properties.load}. The following table
 * lists the available property keys and which methods consider them:
 *
 * <table class="striped">
 * <caption>Audio System Property Keys</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Property Key
 *     <th scope="col">Interface
 *     <th scope="col">Affected Method(s)
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">{@code javax.sound.sampled.Clip}
 *     <td>{@link Clip}
 *     <td>{@link #getLine}, {@link #getClip}
 *   <tr>
 *     <th scope="row">{@code javax.sound.sampled.Port}
 *     <td>{@link Port}
 *     <td>{@link #getLine}
 *   <tr>
 *     <th scope="row">{@code javax.sound.sampled.SourceDataLine}
 *     <td>{@link SourceDataLine}
 *     <td>{@link #getLine}, {@link #getSourceDataLine}
 *   <tr>
 *     <th scope="row">{@code javax.sound.sampled.TargetDataLine}
 *     <td>{@link TargetDataLine}
 *     <td>{@link #getLine}, {@link #getTargetDataLine}
 * </tbody>
 * </table>
 *
 * The property value consists of the provider class name and the mixer name,
 * separated by the hash mark ("#"). The provider class name is the
 * fully-qualified name of a concrete {@link MixerProvider mixer provider}
 * class. The mixer name is matched against the {@code String} returned by the
 * {@code getName} method of {@code Mixer.Info}. Either the class name, or the
 * mixer name may be omitted. If only the class name is specified, the trailing
 * hash mark is optional.
 * <p>
 * If the provider class is specified, and it can be successfully retrieved from
 * the installed providers, the list of {@code Mixer.Info} objects is retrieved
 * from the provider. Otherwise, or when these mixers do not provide a
 * subsequent match, the list is retrieved from {@link #getMixerInfo} to contain
 * all available {@code Mixer.Info} objects.
 * <p>
 * If a mixer name is specified, the resulting list of {@code Mixer.Info}
 * objects is searched: the first one with a matching name, and whose
 * {@code Mixer} provides the respective line interface, will be returned. If no
 * matching {@code Mixer.Info} object is found, or the mixer name is not
 * specified, the first mixer from the resulting list, which provides the
 * respective line interface, will be returned.
 * <p>
 * For example, the property {@code javax.sound.sampled.Clip} with a value
 * {@code "com.sun.media.sound.MixerProvider#SunClip"} will have the following
 * consequences when {@code getLine} is called requesting a {@code Clip}
 * instance: if the class {@code com.sun.media.sound.MixerProvider} exists in
 * the list of installed mixer providers, the first {@code Clip} from the first
 * mixer with name {@code "SunClip"} will be returned. If it cannot be found,
 * the first {@code Clip} from the first mixer of the specified provider will be
 * returned, regardless of name. If there is none, the first {@code Clip} from
 * the first {@code Mixer} with name {@code "SunClip"} in the list of all mixers
 * (as returned by {@code getMixerInfo}) will be returned, or, if not found, the
 * first {@code Clip} of the first {@code Mixer} that can be found in the list
 * of all mixers is returned. If that fails, too, an
 * {@code IllegalArgumentException} is thrown.
 *
 * @author Naoko Mitsurugi
 *
 * @see AudioFormat
 * @see AudioFileFormat
 * @see AudioInputStream
 * @see Mixer
 * @see Line
 * @see Line.Info
 */
public final class Audios {

    public static final int NOT_SPECIFIED = AudioSystem.NOT_SPECIFIED;
    public static final int MONO = 1;
    public static final int STEREO = 2;

    private Audios() {
        throw new AssertionError("No javasound.enhancement.sampled.Audios instances for you!");
    }

    /**
     * Constructs an {@code AudioFormat} with the given parameters. The encoding
     * specifies the convention used to represent the data. The other parameters
     * are further explained in the {@link AudioFormat class description}.
     *
     * @param  encoding the audio encoding technique
     * @param  sampleRate the number of samples per second
     * @param  sampleSizeInBits the number of bits in each sample
     * @param  channels the number of channels (1 for mono, 2 for stereo, and so
     *         on)
     * @param  frameSize the number of bytes in each frame
     * @param  frameRate the number of frames per second
     * @param  bigEndian indicates whether the data for a single sample is
     *         stored in big-endian byte order ({@code false} means
     *         little-endian)
     * @param  properties a {@code Map<String, Object>} object containing format
     *         properties
     */
    public static AudioFormat newAudioFormat(AudioFormat.Encoding encoding,
                                             float sampleRate, int sampleSizeInBits,
                                             int channels, int frameSize, float frameRate, boolean bigEndian,
                                             Map<String, Object> properties) {
        if (properties == null) return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        else return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian, properties);
    }

    /**
     * Constructs an {@code AudioFormat} with a linear PCM encoding and the
     * given parameters. The frame size is set to the number of bytes required
     * to contain one sample from each channel, and the frame rate is set to the
     * sample rate.
     *
     * @param  sampleRate the number of samples per second
     * @param  sampleSizeInBits the number of bits in each sample
     * @param  channels the number of channels (1 for mono, 2 for stereo, and so
     *         on)
     * @param  signed indicates whether the data is signed or unsigned
     * @param  bigEndian indicates whether the data for a single sample is
     *         stored in big-endian byte order ({@code false} means
     *         little-endian)
     * @param  properties a {@code Map<String, Object>} object containing format
     *         properties
     */
    public static AudioFormat newAudioFormat(float sampleRate, int sampleSizeInBits,
                                             int channels, boolean signed, boolean bigEndian,
                                             Map<String, Object> properties) {
        if (properties == null) return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        else return new AudioFormat(
                signed ? AudioFormat.Encoding.PCM_SIGNED : AudioFormat.Encoding.PCM_UNSIGNED,
                sampleRate, sampleSizeInBits, channels,
                (channels == AudioSystem.NOT_SPECIFIED || sampleSizeInBits == AudioSystem.NOT_SPECIFIED) ?
                        AudioSystem.NOT_SPECIFIED : (sampleSizeInBits + 7) / 8 * channels,
                sampleRate, bigEndian,
                properties
        );
    }

    /**
     * Constructs an {@code AudioFormat} with an {@code AudioFormat}.
     *
     * @param format the format to copy of
     */
    public static AudioFormat newAudioFormat(AudioFormat format) {
        return new AudioFormat(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(),
                format.getChannels(), format.getFrameSize(), format.getFrameRate(), format.isBigEndian(), format.properties());
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
    public static AudioFileFormat newAudioFileFormat(AudioFileFormat.Type type, AudioFormat format, int frameLength, int byteLength, Map<String, Object> properties) {
        if (properties == null) return new ExtendedAudioFileFormat(type, format, byteLength, frameLength);
        else return new ExtendedAudioFileFormat(type, format, byteLength, frameLength, properties);
    }

    /**
     * Constructs an {@code AudioFileFormat} with an {@code AudioFileFormat}.
     *
     * @param format the format to copy of
     */
    public static AudioFileFormat newAudioFileFormat(AudioFileFormat format) {
        return new ExtendedAudioFileFormat(format.getType(), format.getFormat(), format.getFrameLength(), format.getByteLength(), format.properties());
    }

    /**
     * Obtains an array of mixer info objects that represents the set of audio
     * mixers that are currently installed on the system.
     *
     * @return an array of info objects for the currently installed mixers. If
     *         no mixers are available on the system, an array of length 0 is
     *         returned.
     * @see #getMixer
     */
    public static Mixer.Info[] getMixerInfo() {
        return AudioSystem.getMixerInfo();
    }

    /**
     * Obtains the requested audio mixer.
     *
     * @param  info a {@code Mixer.Info} object representing the desired mixer,
     *         or {@code null} for the system default mixer
     * @return the requested mixer
     * @throws SecurityException if the requested mixer is unavailable because
     *         of security restrictions
     * @throws IllegalArgumentException if the info object does not represent a
     *         mixer installed on the system
     * @see #getMixerInfo
     */
    public static Mixer getMixer(Mixer.Info info) {
        return AudioSystem.getMixer(info);
    }

    /**
     * Obtains information about all source lines of a particular type that are
     * supported by the installed mixers.
     *
     * @param  info a {@code Line.Info} object that specifies the kind of lines
     *         about which information is requested
     * @return an array of {@code Line.Info} objects describing source lines
     *         matching the type requested. If no matching source lines are
     *         supported, an array of length 0 is returned.
     * @see Mixer#getSourceLineInfo(Line.Info)
     */
    public static Line.Info[] getSourceLineInfo(Line.Info info) {
        return AudioSystem.getSourceLineInfo(info);
    }

    /**
     * Obtains information about all target lines of a particular type that are
     * supported by the installed mixers.
     *
     * @param  info a {@code Line.Info} object that specifies the kind of lines
     *         about which information is requested
     * @return an array of {@code Line.Info} objects describing target lines
     *         matching the type requested. If no matching target lines are
     *         supported, an array of length 0 is returned.
     * @see Mixer#getTargetLineInfo(Line.Info)
     */
    public static Line.Info[] getTargetLineInfo(Line.Info info) {
        return AudioSystem.getTargetLineInfo(info);
    }

    /**
     * Indicates whether the system supports any lines that match the specified
     * {@code Line.Info} object. A line is supported if any installed mixer
     * supports it.
     *
     * @param  info a {@code Line.Info} object describing the line for which
     *         support is queried
     * @return {@code true} if at least one matching line is supported,
     *         otherwise {@code false}
     * @see Mixer#isLineSupported(Line.Info)
     */
    public static boolean isLineSupported(Line.Info info) {
        return AudioSystem.isLineSupported(info);
    }

    /**
     * Obtains a line that matches the description in the specified
     * {@code Line.Info} object.
     * <p>
     * If a {@code DataLine} is requested, and {@code info} is an instance of
     * {@code DataLine.Info} specifying at least one fully qualified audio
     * format, the last one will be used as the default format of the returned
     * {@code DataLine}.
     * <p>
     * If system properties
     * {@code javax.sound.sampled.Clip},
     * {@code javax.sound.sampled.Port},
     * {@code javax.sound.sampled.SourceDataLine} and
     * {@code javax.sound.sampled.TargetDataLine} are defined or they are
     * defined in the file "sound.properties", they are used to retrieve default
     * lines. For details, refer to the {@link AudioSystem class description}.
     *
     * If the respective property is not set, or the mixer requested in the
     * property is not installed or does not provide the requested line, all
     * installed mixers are queried for the requested line type. A Line will be
     * returned from the first mixer providing the requested line type.
     *
     * @param  info a {@code Line.Info} object describing the desired kind of
     *         line
     * @return a line of the requested kind
     * @throws LineUnavailableException if a matching line is not available due
     *         to resource restrictions
     * @throws SecurityException if a matching line is not available due to
     *         security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one line matching the specified {@code Line.Info} object through
     *         any installed mixer
     */
    public static Line getLine(Line.Info info)
            throws LineUnavailableException, SecurityException, IllegalArgumentException {
        return AudioSystem.getLine(info);
    }

    /**
     * Obtains a clip that can be used for playing back an audio file or an
     * audio stream. The returned clip will be provided by the default system
     * mixer, or, if not possible, by any other mixer installed in the system
     * that supports a {@code Clip} object.
     * <p>
     * The returned clip must be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioInputStream)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * If the system property {@code javax.sound.sampled.Clip} is defined or it
     * is defined in the file "sound.properties", it is used to retrieve the
     * default clip. For details, refer to the
     * {@link AudioSystem class description}.
     *
     * @return the desired clip object
     * @throws LineUnavailableException if a clip object is not available due to
     *         resource restrictions
     * @throws SecurityException if a clip object is not available due to
     *         security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one clip instance through any installed mixer
     * @see #getClip(Mixer.Info)
     */
    public static Clip getClip()
            throws LineUnavailableException, SecurityException, IllegalArgumentException {
        return AudioSystem.getClip();
    }

    /**
     * Obtains a clip from the specified mixer that can be used for playing back
     * an audio file or an audio stream.
     * <p>
     * The returned clip must be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioInputStream)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     *
     * @param  mixerInfo a {@code Mixer.Info} object representing the desired
     *         mixer, or {@code null} for the system default mixer
     * @return a clip object from the specified mixer
     * @throws LineUnavailableException if a clip is not available from this
     *         mixer due to resource restrictions
     * @throws SecurityException if a clip is not available from this mixer due
     *         to security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one clip through the specified mixer
     * @see #getClip()
     */
    public static Clip getClip(Mixer.Info mixerInfo)
            throws LineUnavailableException, SecurityException, IllegalArgumentException {
        return AudioSystem.getClip(mixerInfo);
    }

    /**
     * Obtains a source data line that can be used for playing back audio data
     * in the format specified by the {@code AudioFormat} object. The returned
     * line will be provided by the default system mixer, or, if not possible,
     * by any other mixer installed in the system that supports a matching
     * {@code SourceDataLine} object.
     * <p>
     * The returned line should be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioFormat, int)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * The returned {@code SourceDataLine}'s default audio format will be
     * initialized with {@code format}.
     * <p>
     * If the system property {@code javax.sound.sampled.SourceDataLine} is
     * defined or it is defined in the file "sound.properties", it is used to
     * retrieve the default source data line. For details, refer to the
     * {@link AudioSystem class description}.
     *
     * @param  format an {@code AudioFormat} object specifying the supported
     *         audio format of the returned line, or {@code null} for any audio
     *         format
     * @return the desired {@code SourceDataLine} object
     * @throws LineUnavailableException if a matching source data line is not
     *         available due to resource restrictions
     * @throws SecurityException if a matching source data line is not available
     *         due to security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one source data line supporting the specified audio format
     *         through any installed mixer
     * @see #getSourceDataLine(AudioFormat, Mixer.Info)
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format)
            throws LineUnavailableException, SecurityException, IllegalArgumentException {
        return AudioSystem.getSourceDataLine(format);
    }

    /**
     * Obtains a source data line that can be used for playing back audio data
     * in the format specified by the {@code AudioFormat} object, provided by
     * the mixer specified by the {@code Mixer.Info} object.
     * <p>
     * The returned line should be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioFormat, int)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * The returned {@code SourceDataLine}'s default audio format will be
     * initialized with {@code format}.
     *
     * @param  format an {@code AudioFormat} object specifying the supported
     *         audio format of the returned line, or {@code null} for any audio
     *         format
     * @param  mixerinfo a {@code Mixer.Info} object representing the desired
     *         mixer, or {@code null} for the system default mixer
     * @return the desired {@code SourceDataLine} object
     * @throws LineUnavailableException if a matching source data line is not
     *         available from the specified mixer due to resource restrictions
     * @throws SecurityException if a matching source data line is not available
     *         from the specified mixer due to security restrictions
     * @throws IllegalArgumentException if the specified mixer does not support
     *         at least one source data line supporting the specified audio
     *         format
     * @see #getSourceDataLine(AudioFormat)
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format, Mixer.Info mixerinfo)
            throws LineUnavailableException, SecurityException, IllegalArgumentException {
        return AudioSystem.getSourceDataLine(format, mixerinfo);
    }

    /**
     * Obtains a target data line that can be used for recording audio data in
     * the format specified by the {@code AudioFormat} object. The returned line
     * will be provided by the default system mixer, or, if not possible, by any
     * other mixer installed in the system that supports a matching
     * {@code TargetDataLine} object.
     * <p>
     * The returned line should be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioFormat, int)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * The returned {@code TargetDataLine}'s default audio format will be
     * initialized with {@code format}.
     * <p>
     * If the system property {@code javax.sound.sampled.TargetDataLine} is
     * defined or it is defined in the file "sound.properties", it is used to
     * retrieve the default target data line. For details, refer to the
     * {@link AudioSystem class description}.
     *
     * @param  format an {@code AudioFormat} object specifying the supported
     *         audio format of the returned line, or {@code null} for any audio
     *         format
     * @return the desired {@code TargetDataLine} object
     * @throws LineUnavailableException if a matching target data line is not
     *         available due to resource restrictions
     * @throws SecurityException if a matching target data line is not available
     *         due to security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one target data line supporting the specified audio format
     *         through any installed mixer
     * @see #getTargetDataLine(AudioFormat, Mixer.Info)
     * @see AudioPermission
     */
    public static TargetDataLine getTargetDataLine(AudioFormat format)
            throws LineUnavailableException, SecurityException, IllegalArgumentException {
        return AudioSystem.getTargetDataLine(format);
    }

    /**
     * Obtains a target data line that can be used for recording audio data in
     * the format specified by the {@code AudioFormat} object, provided by the
     * mixer specified by the {@code Mixer.Info} object.
     * <p>
     * The returned line should be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioFormat, int)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * The returned {@code TargetDataLine}'s default audio format will be
     * initialized with {@code format}.
     *
     * @param  format an {@code AudioFormat} object specifying the supported
     *         audio format of the returned line, or {@code null} for any audio
     *         format
     * @param  mixerinfo a {@code Mixer.Info} object representing the desired
     *         mixer, or {@code null} for the system default mixer
     * @return the desired {@code TargetDataLine} object
     * @throws LineUnavailableException if a matching target data line is not
     *         available from the specified mixer due to resource restrictions
     * @throws SecurityException if a matching target data line is not available
     *         from the specified mixer due to security restrictions
     * @throws IllegalArgumentException if the specified mixer does not support
     *         at least one target data line supporting the specified audio
     *         format
     * @see #getTargetDataLine(AudioFormat)
     * @see AudioPermission
     */
    public static TargetDataLine getTargetDataLine(AudioFormat format, Mixer.Info mixerinfo)
            throws LineUnavailableException, SecurityException, IllegalArgumentException {
        return AudioSystem.getTargetDataLine(format, mixerinfo);
    }

    /**
     * Obtains the encodings that the system can obtain from an audio input
     * stream with the specified encoding using the set of installed format
     * converters.
     *
     * @param  sourceEncoding the encoding for which conversion support is
     *         queried
     * @return array of encodings. If {@code sourceEncoding} is not supported,
     *         an array of length 0 is returned. Otherwise, the array will have
     *         a length of at least 1, representing {@code sourceEncoding}
     *         (no conversion).
     * @throws NullPointerException if {@code sourceEncoding} is {@code null}
     */
    public static AudioFormat.Encoding[] getTargetEncodings(AudioFormat.Encoding sourceEncoding) {
        return AudioSystem.getTargetEncodings(sourceEncoding);
    }

    /**
     * Obtains the encodings that the system can obtain from an audio input
     * stream with the specified format using the set of installed format
     * converters.
     *
     * @param  sourceFormat the audio format for which conversion is queried
     * @return array of encodings. If {@code sourceFormat}is not supported, an
     *         array of length 0 is returned. Otherwise, the array will have a
     *         length of at least 1, representing the encoding of
     *         {@code sourceFormat} (no conversion).
     * @throws NullPointerException if {@code sourceFormat} is {@code null}
     */
    public static AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        return AudioSystem.getTargetEncodings(sourceFormat);
    }

    /**
     * Indicates whether an audio input stream of the specified encoding can be
     * obtained from an audio input stream that has the specified format.
     *
     * @param  targetEncoding the desired encoding after conversion
     * @param  sourceFormat the audio format before conversion
     * @return {@code true} if the conversion is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceFormat} are {@code null}
     */
    public static boolean isConversionSupported(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        return AudioSystem.isConversionSupported(targetEncoding, sourceFormat);
    }

    /**
     * Obtains an audio input stream of the indicated encoding, by converting
     * the provided audio input stream.
     *
     * @param  targetEncoding the desired encoding after conversion
     * @param  sourceStream the stream to be converted
     * @return an audio input stream of the indicated encoding
     * @throws IllegalArgumentException if the conversion is not supported
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceStream} are {@code null}
     * @see #getTargetEncodings(AudioFormat.Encoding)
     * @see #getTargetEncodings(AudioFormat)
     * @see #isConversionSupported(AudioFormat.Encoding, AudioFormat)
     * @see #getAudioInputStream(AudioFormat, AudioInputStream)
     */
    public static AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream)
            throws IllegalArgumentException {
        return AudioSystem.getAudioInputStream(targetEncoding, sourceStream);
    }

    /**
     * Obtains the formats that have a particular encoding and that the system
     * can obtain from a stream of the specified format using the set of
     * installed format converters.
     *
     * @param  targetEncoding the desired encoding after conversion
     * @param  sourceFormat the audio format before conversion
     * @return array of formats. If no formats of the specified encoding are
     *         supported, an array of length 0 is returned.
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceFormat} are {@code null}
     */
    public static AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        return AudioSystem.getTargetFormats(targetEncoding, sourceFormat);
    }

    /**
     * Indicates whether an audio input stream of a specified format can be
     * obtained from an audio input stream of another specified format.
     *
     * @param  targetFormat the desired audio format after conversion
     * @param  sourceFormat the audio format before conversion
     * @return {@code true} if the conversion is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code targetFormat} or
     *         {@code sourceFormat} are {@code null}
     */
    public static boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        return AudioSystem.isConversionSupported(targetFormat, sourceFormat);
    }

    /**
     * Obtains an audio input stream of the indicated format, by converting the
     * provided audio input stream.
     *
     * @param  targetFormat the desired audio format after conversion
     * @param  sourceStream the stream to be converted
     * @return an audio input stream of the indicated format
     * @throws IllegalArgumentException if the conversion is not supported
     * @throws NullPointerException if {@code targetFormat} or
     *         {@code sourceStream} are {@code null}
     * @see #getTargetEncodings(AudioFormat)
     * @see #getTargetFormats(AudioFormat.Encoding, AudioFormat)
     * @see #isConversionSupported(AudioFormat, AudioFormat)
     * @see #getAudioInputStream(AudioFormat.Encoding, AudioInputStream)
     */
    public static AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
    }

    /**
     * Obtains the audio file format of the provided input stream. The stream
     * must point to valid audio file data. The implementation of this method
     * may require multiple parsers to examine the stream to determine whether
     * they support it. These parsers must be able to mark the stream, read
     * enough data to determine whether they support the stream, and reset the
     * stream's read pointer to its original position. If the input stream does
     * not support these operations, this method may fail with an
     * {@code IOException}.
     *
     * @param  stream the input stream from which file format information should
     *         be extracted
     * @return an {@code AudioFileFormat} object describing the stream's audio
     *         file format
     * @throws UnsupportedAudioFileException if the stream does not point to
     *         valid audio file data recognized by the system
     * @throws IOException if an input/output exception occurs
     * @throws NullPointerException if {@code stream} is {@code null}
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public static AudioFileFormat getAudioFileFormat(InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioFileFormat(stream);
    }

    /**
     * Obtains the audio file format of the specified {@code URL}. The
     * {@code URL} must point to valid audio file data.
     *
     * @param  url the {@code URL} from which file format information should be
     *         extracted
     * @return an {@code AudioFileFormat} object describing the audio file
     *         format
     * @throws UnsupportedAudioFileException if the {@code URL} does not point
     *         to valid audio file data recognized by the system
     * @throws IOException if an input/output exception occurs
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public static AudioFileFormat getAudioFileFormat(URL url)
            throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioFileFormat(url);
    }

    /**
     * Obtains the audio file format of the specified {@code File}. The
     * {@code File} must point to valid audio file data.
     *
     * @param  file the {@code File} from which file format information should
     *         be extracted
     * @return an {@code AudioFileFormat} object describing the audio file
     *         format
     * @throws UnsupportedAudioFileException if the {@code File} does not point
     *         to valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static AudioFileFormat getAudioFileFormat(File file)
            throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioFileFormat(file);
    }

    /**
     * Obtains the audio file format of the specified resource.  The resource must
     * point to valid audio file data.
     * @param resourceLoader the {@code ClassLoader} to load resource
     * @param name the resource name from which file format information should be
     * extracted
     * @return an <code>AudioFileFormat</code> object describing the audio file format
     * @throws UnsupportedAudioFileException if the resource does not point to valid audio
     * file data recognized by the system
     * @throws IOException if an input/output exception occurs
     */
    public static AudioFileFormat getAudioFileFormat(ClassLoader resourceLoader, String name)
            throws UnsupportedAudioFileException, IOException {
        AudioFileFormat format = null;
        for (AudioResourceReader reader : getAudioResourceReaders()) {
            try {
                format = reader.getAudioFileFormat(resourceLoader, name); // throws IOException
                break;
            }
            catch (UnsupportedAudioFileException ignored) {
            }
        }
        if (format == null) throw new UnsupportedAudioFileException("file is not a supported file type");
        else return format;
    }

    /**
     * Obtains an audio input stream from the provided input stream. The stream
     * must point to valid audio file data. The implementation of this method
     * may require multiple parsers to examine the stream to determine whether
     * they support it. These parsers must be able to mark the stream, read
     * enough data to determine whether they support the stream, and reset the
     * stream's read pointer to its original position. If the input stream does
     * not support these operation, this method may fail with an
     * {@code IOException}.
     *
     * @param  stream the input stream from which the {@code AudioInputStream}
     *         should be constructed
     * @return an {@code AudioInputStream} object based on the audio file data
     *         contained in the input stream
     * @throws UnsupportedAudioFileException if the stream does not point to
     *         valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code stream} is {@code null}
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public static AudioInputStream getAudioInputStream(InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(stream);
    }

    /**
     * Obtains an audio input stream from the {@code URL} provided. The
     * {@code URL} must point to valid audio file data.
     *
     * @param  url the {@code URL} for which the {@code AudioInputStream} should
     *         be constructed
     * @return an {@code AudioInputStream} object based on the audio file data
     *         pointed to by the {@code URL}
     * @throws UnsupportedAudioFileException if the {@code URL} does not point
     *         to valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public static AudioInputStream getAudioInputStream(URL url)
            throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(url);
    }

    /**
     * Obtains an audio input stream from the provided {@code File}. The
     * {@code File} must point to valid audio file data.
     *
     * @param  file the {@code File} for which the {@code AudioInputStream}
     *         should be constructed
     * @return an {@code AudioInputStream} object based on the audio file data
     *         pointed to by the {@code File}
     * @throws UnsupportedAudioFileException if the {@code File} does not point
     *         to valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static AudioInputStream getAudioInputStream(File file)
            throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(file);
    }

    /**
     * Obtains an audio input stream from the resource provided.  The resource must
     * point to valid audio file data.
     * @param resourceLoader the {@code ClassLoader} to load resource
     * @param name the resource name for which the <code>AudioInputStream</code> should be
     * constructed
     * @return an <code>AudioInputStream</code> object based on the audio file data pointed
     * to by the resource
     * @throws UnsupportedAudioFileException if the resource does not point to valid audio
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs
     */
    public static AudioInputStream getAudioInputStream(ClassLoader resourceLoader, String name)
            throws UnsupportedAudioFileException, IOException {
        AudioInputStream audioStream = null;
        for (AudioResourceReader reader : getAudioResourceReaders()) {
            try {
                audioStream = reader.getAudioInputStream(resourceLoader, name); // throws IOException
                break;
            }
            catch (UnsupportedAudioFileException ignored) {
            }
        }
        if (audioStream == null) throw new UnsupportedAudioFileException("could not get audio input stream from input resource");
        else return audioStream;
    }

    private static final AudioFormat.Encoding[] EMPTY_ENCODING_ARRAY = new AudioFormat.Encoding[0];

    /**
     * Obtains the audio format encodings for which file reading support is provided by the
     * system.
     *
     * @return array of unique audio format encodings. If no format encodings are supported, an
     *         array of length 0 is returned.
     */
    public static AudioFormat.Encoding[] getReaderEncodings() {
        Set<AudioFormat.Encoding> encodings = new HashSet<>();
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            encodings.addAll(Arrays.asList(provider.getReaderEncodings()));
        }
        return encodings.toArray(EMPTY_ENCODING_ARRAY);
    }

    /**
     * Indicates whether file reading support for the specified file format encoding is
     * provided by the system.
     *
     * @param  encoding the encoding for which read capabilities are queried
     * @return {@code true} if the encoding is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code encoding} is {@code null}
     */
    public static boolean isReaderSupportedEncoding(AudioFormat.Encoding encoding) {
        boolean supported = false;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            supported = provider.isReaderSupportedEncoding(encoding);
            if (supported) break;
        }
        return supported;
    }

    /**
     * Obtains the audio format encodings for which file writing support is provided by the
     * system.
     *
     * @return array of unique audio format encodings. If no format encodings are supported, an
     *         array of length 0 is returned.
     */
    public static AudioFormat.Encoding[] getWriterEncodings() {
        Set<AudioFormat.Encoding> encodings = new HashSet<>();
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            encodings.addAll(Arrays.asList(provider.getWriterEncodings()));
        }
        return encodings.toArray(EMPTY_ENCODING_ARRAY);
    }

    /**
     * Indicates whether file writing support for the specified file format encoding is
     * provided by the system.
     *
     * @param  encoding the encoding for which write capabilities are queried
     * @return {@code true} if the encoding is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code encoding} is {@code null}
     */
    public static boolean isWriterSupportedEncoding(AudioFormat.Encoding encoding) {
        boolean supported = false;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            supported = provider.isWriterSupportedEncoding(encoding);
            if (supported) break;
        }
        return supported;
    }

    /**
     * Obtains the encodings that the system can write from the audio format
     * type specified.
     *
     * @param  fileType the audio file type for which audio format encoding support
     *         is queried
     * @return array of encodings. If no encodings are supported, an array of
     *         length 0 is returned.
     * @throws NullPointerException if {@code stream} is {@code null}
     */
    public static AudioFormat.Encoding[] getWriterEncodings(AudioFileFormat.Type fileType) {
        Set<AudioFormat.Encoding> encodings = new HashSet<>();
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            encodings.addAll(Arrays.asList(provider.getWriterEncodings(fileType)));
        }
        return encodings.toArray(EMPTY_ENCODING_ARRAY);
    }

    /**
     * Obtains the encodings that the system can write from the audio input
     * stream specified.
     *
     * @param  stream the audio input stream for which audio format encoding support
     *         is queried
     * @return array of encodings. If no encodings are supported, an array of
     *         length 0 is returned.
     * @throws NullPointerException if {@code stream} is {@code null}
     */
    public static AudioFormat.Encoding[] getWriterEncodings(AudioInputStream stream) {
        Set<AudioFormat.Encoding> encodings = new HashSet<>();
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            encodings.addAll(Arrays.asList(provider.getWriterEncodings(stream)));
        }
        return encodings.toArray(EMPTY_ENCODING_ARRAY);
    }

    /**
     * Indicates whether an audio file of the specified encoding can be written
     * from the indicated audio input stream.
     *
     * @param  encoding the encoding for which write capabilities are queried
     * @param  stream the stream for which file-writing support is queried
     * @return {@code true} if the encoding is supported for this audio input
     *         stream, otherwise {@code false}
     * @throws NullPointerException if {@code encoding} or {@code stream} are
     *         {@code null}
     */
    public static boolean isWriterSupportedEncoding(AudioFormat.Encoding encoding, AudioInputStream stream) {
        boolean supported = false;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            supported = provider.isWriterSupportedEncoding(encoding, stream);
            if (supported) break;
        }
        return supported;
    }

    /**
     * Obtains the audio format encoding by format name.
     *
     * @param formatName the informal name of a format (e.g. "PCM_SIGNED" or "ALAW").
     *
     * @return the audio format encoding
     *
     * @see #getReaderEncodings()
     * @see #getWriterEncodings()
     */
    public static AudioFormat.Encoding getEncodingByFormatName(String formatName) {
        AudioFormat.Encoding encoding = null;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            encoding = provider.getEncodingByFormatName(formatName);
            if (encoding != null) break;
        }
        return encoding;
    }

    private static final AudioFileFormat.Type[] EMPTY_TYPE_ARRAY = new AudioFileFormat.Type[0];

    /**
     * Obtains the file types for which file reading support is provided by the
     * system.
     *
     * @return array of unique file types. If no file types are supported, an
     *         array of length 0 is returned.
     */
    public static AudioFileFormat.Type[] getReaderFileTypes() {
        Set<AudioFileFormat.Type> types = new HashSet<>();
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            Collections.addAll(types, provider.getReaderFileTypes());
        }
        return types.toArray(EMPTY_TYPE_ARRAY);
    }

    /**
     * Indicates whether file reading support for the specified file type is
     * provided by the system.
     *
     * @param  fileType the file type for which read capabilities are queried
     * @return {@code true} if the file type is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code fileType} is {@code null}
     */
    public static boolean isReaderSupportedFileType(AudioFileFormat.Type fileType) {
        boolean supported = false;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            supported = provider.isReaderSupportedFileType(fileType);
            if (supported) break;
        }
        return supported;
    }

    /**
     * Obtains the file types for which file writing support is provided by the
     * system.
     *
     * @return array of unique file types. If no file types are supported, an
     *         array of length 0 is returned.
     */
    public static AudioFileFormat.Type[] getWriterFileTypes() {
        Set<AudioFileFormat.Type> types = new HashSet<>();
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            types.addAll(Arrays.asList(provider.getWriterFileTypes()));
        }
        return types.toArray(EMPTY_TYPE_ARRAY);
    }

    /**
     * Indicates whether file writing support for the specified file type is
     * provided by the system.
     *
     * @param  fileType the file type for which write capabilities are queried
     * @return {@code true} if the file type is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code fileType} is {@code null}
     */
    public static boolean isWriterSupportedFileType(AudioFileFormat.Type fileType) {
        boolean supported = false;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            supported = provider.isWriterSupportedFileType(fileType);
            if (supported) break;
        }
        return supported;
    }

    /**
     * Obtains the file types that the system can write from the audio input
     * stream specified.
     *
     * @param  stream the audio input stream for which audio file type support
     *         is queried
     * @return array of file types. If no file types are supported, an array of
     *         length 0 is returned.
     * @throws NullPointerException if {@code stream} is {@code null}
     */
    public static AudioFileFormat.Type[] getWriterFileTypes(AudioInputStream stream) {
        Set<AudioFileFormat.Type> types = new HashSet<>();
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            types.addAll(Arrays.asList(provider.getWriterFileTypes(stream)));
        }
        return types.toArray(EMPTY_TYPE_ARRAY);
    }

    /**
     * Indicates whether an audio file of the specified file type can be written
     * from the indicated audio input stream.
     *
     * @param  fileType the file type for which write capabilities are queried
     * @param  stream the stream for which file-writing support is queried
     * @return {@code true} if the file type is supported for this audio input
     *         stream, otherwise {@code false}
     * @throws NullPointerException if {@code fileType} or {@code stream} are
     *         {@code null}
     */
    public static boolean isWriterSupportedFileType(AudioFileFormat.Type fileType, AudioInputStream stream) {
        boolean supported = false;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            supported = provider.isWriterSupportedFileType(fileType, stream);
            if (supported) break;
        }
        return supported;
    }

    /**
     * Obtains the audio file format type by format name.
     *
     * @param formatName the informal name of a format (e.g. "WAVE" or "AIFF-C").
     *
     * @return the audio file format type
     *
     * @see #getReaderFileTypes()
     * @see #getWriterFileTypes()
     */
    public static AudioFileFormat.Type getFileTypeByFormatName(String formatName) {
        AudioFileFormat.Type type = null;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            type = provider.getFileTypeByFormatName(formatName);
            if (type != null) break;
        }
        return type;
    }

    /**
     * Obtains the audio file format type by suffix.
     *
     * @param suffix the extension of a format (e.g. "wav" or "aifc").
     *
     * @return the audio file format type
     *
     * @see #getReaderFileTypes()
     * @see #getWriterFileTypes()
     */
    public static AudioFileFormat.Type getFileTypeBySuffix(String suffix) {
        AudioFileFormat.Type type = null;
        for (FormatEncodingProvider provider : getFormatEncodingProviders()) {
            type = provider.getFileTypeByFormatName(suffix);
            if (type != null) break;
        }
        return type;
    }

    /**
     * Writes a stream of bytes representing an audio file of the specified file
     * type to the output stream provided. Some file types require that the
     * length be written into the file header; such files cannot be written from
     * start to finish unless the length is known in advance. An attempt to
     * write a file of such a type will fail with an {@code IOException} if the
     * length in the audio file type is {@code AudioSystem.NOT_SPECIFIED}.
     *
     * @param  stream the audio input stream containing audio data to be written
     *         to the file
     * @param  fileType the kind of audio file to write
     * @param  out the stream to which the file data should be written
     * @return the number of bytes written to the output stream
     * @throws IOException if an input/output exception occurs
     * @throws IllegalArgumentException if the file type is not supported by the
     *         system
     * @throws NullPointerException if {@code stream} or {@code fileType} or
     *         {@code out} are {@code null}
     * @see #isWriterSupportedFileType
     * @see #getWriterFileTypes
     */
    public static int write(AudioInputStream stream, AudioFileFormat.Type fileType, OutputStream out)
            throws IOException, IllegalArgumentException {
        return AudioSystem.write(stream, fileType, out);
    }

    /**
     * Writes a stream of bytes representing an audio file of the specified file
     * type to the external file provided.
     *
     * @param  stream the audio input stream containing audio data to be written
     *         to the file
     * @param  fileType the kind of audio file to write
     * @param  out the external file to which the file data should be written
     * @return the number of bytes written to the file
     * @throws IOException if an I/O exception occurs
     * @throws IllegalArgumentException if the file type is not supported by the
     *         system
     * @throws NullPointerException if {@code stream} or {@code fileType} or
     *         {@code out} are {@code null}
     * @see #isWriterSupportedFileType
     * @see #getWriterFileTypes
     */
    public static int write(AudioInputStream stream, AudioFileFormat.Type fileType, File out)
            throws IOException, IllegalArgumentException {
        return AudioSystem.write(stream, fileType, out);
    }

    /**
     * Writes a stream of bytes representing an audio file of the specified file
     * type to the external file provided.
     *
     * @param  stream the audio input stream containing audio data to be written
     *         to the file
     * @param  fileType the kind of audio file to write
     * @param  quality quality of output data, 0 for lowest, 1 for highest, range [0, 1]
     * @param  out the external file to which the file data should be written
     * @return the number of bytes written to the file
     * @throws IOException if an I/O exception occurs
     * @throws IllegalArgumentException if the file type is not supported by the
     *         system
     * @throws NullPointerException if {@code stream} or {@code fileType} or
     *         {@code out} are {@code null}
     * @see #isWriterSupportedFileType
     * @see #getWriterFileTypes
     */
    public static int write(AudioInputStream stream, AudioFileFormat.Type fileType, float quality, OutputStream out)
            throws IOException, IllegalArgumentException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(out);

        quality = Math.max(0, Math.min(quality, 1));

        for (AudioCompressionWriter writer : getAudioCompressionWriters()) {
            try {
                return writer.write(stream, fileType, quality, out);
            }
            catch (IllegalArgumentException ignored) {
                // thrown if this provider cannot write the stream, try next
            }
        }

        throw new IllegalArgumentException("could not write audio file: file type not supported: " + fileType);
    }

    /**
     * Writes a stream of bytes representing an audio file of the specified file
     * type to the external file provided.
     *
     * @param  stream the audio input stream containing audio data to be written
     *         to the file
     * @param  fileType the kind of audio file to write
     * @param  quality quality of output data, 0 for lowest, 1 for highest, range [0, 1]
     * @param  out the external file to which the file data should be written
     * @return the number of bytes written to the file
     * @throws IOException if an I/O exception occurs
     * @throws IllegalArgumentException if the file type is not supported by the
     *         system
     * @throws NullPointerException if {@code stream} or {@code fileType} or
     *         {@code out} are {@code null}
     * @see #isWriterSupportedFileType
     * @see #getWriterFileTypes
     */
    public static int write(AudioInputStream stream, AudioFileFormat.Type fileType, float quality, File out)
            throws IOException, IllegalArgumentException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(out);

        quality = Math.max(0, Math.min(quality, 1));

        for (AudioCompressionWriter writer : getAudioCompressionWriters()) {
            try {
                return writer.write(stream, fileType, quality, out);
            }
            catch (IllegalArgumentException ignored) {
                // thrown if this provider cannot write the stream, try next
            }
        }

        throw new IllegalArgumentException("could not write audio file: file type not supported: " + fileType);
    }
    
    private static List<AudioResourceReader> getAudioResourceReaders() {
        return getProviders(AudioResourceReader.class);
    }

    private static List<AudioCompressionWriter> getAudioCompressionWriters() {
        return getProviders(AudioCompressionWriter.class);
    }

    private static List<FormatEncodingProvider> getFormatEncodingProviders() {
        return getProviders(FormatEncodingProvider.class);
    }

    private static <T> List<T> getProviders(Class<T> providerClass) {
        List <T> providers = new ArrayList<>();
        for (T t : ServiceLoader.load(providerClass)) {
            if (providerClass.isInstance(t)) providers.add(t);
        }
        return providers;
    }

}
