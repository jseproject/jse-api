package javasound.enhancement.sampled.spi;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * Obtains the file encodings and types for which file reading and writing support
 * are provided by the system.
 *
 * @author Naoko Mitsurugi
 */
public abstract class FormatEncodingProvider {

    /**
     * Obtains the audio format encodings for which file reading support is provided by the
     * system.
     *
     * @return array of unique audio format encodings. If no format encodings are supported, an
     *         array of length 0 is returned.
     */
    public abstract AudioFormat.Encoding[] getReaderEncodings();
    /**
     * Indicates whether file reading support for the specified file format encoding is
     * provided by the system.
     *
     * @param  encoding the encoding for which read capabilities are queried
     * @return {@code true} if the encoding is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code encoding} is {@code null}
     */
    public abstract boolean isReaderSupportedEncoding(AudioFormat.Encoding encoding);
    /**
     * Obtains the audio format encodings for which file writing support is provided by the
     * system.
     *
     * @return array of unique audio format encodings. If no format encodings are supported, an
     *         array of length 0 is returned.
     */
    public abstract AudioFormat.Encoding[] getWriterEncodings();
    /**
     * Indicates whether file writing support for the specified file format encoding is
     * provided by the system.
     *
     * @param  encoding the encoding for which write capabilities are queried
     * @return {@code true} if the encoding is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code encoding} is {@code null}
     */
    public abstract boolean isWriterSupportedEncoding(AudioFormat.Encoding encoding);
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
    public abstract AudioFormat.Encoding[] getWriterEncodings(AudioFileFormat.Type fileType);
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
    public abstract AudioFormat.Encoding[] getWriterEncodings(AudioInputStream stream);
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
    public abstract boolean isWriterSupportedEncoding(AudioFormat.Encoding encoding, AudioInputStream stream);
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
    public abstract AudioFormat.Encoding getEncodingByFormatName(String formatName);
    /**
     * Obtains the file types for which file reading support is provided by the
     * system.
     *
     * @return array of unique file types. If no file types are supported, an
     *         array of length 0 is returned.
     */
    public abstract AudioFileFormat.Type[] getReaderFileTypes();
    /**
     * Indicates whether file reading support for the specified file type is
     * provided by the system.
     *
     * @param  fileType the file type for which read capabilities are queried
     * @return {@code true} if the file type is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code fileType} is {@code null}
     */
    public abstract boolean isReaderSupportedFileType(AudioFileFormat.Type fileType);
    /**
     * Obtains the file types for which file writing support is provided by the
     * system.
     *
     * @return array of unique file types. If no file types are supported, an
     *         array of length 0 is returned.
     */
    public abstract AudioFileFormat.Type[] getWriterFileTypes();
    /**
     * Indicates whether file writing support for the specified file type is
     * provided by the system.
     *
     * @param  fileType the file type for which write capabilities are queried
     * @return {@code true} if the file type is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code fileType} is {@code null}
     */
    public abstract boolean isWriterSupportedFileType(AudioFileFormat.Type fileType);
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
    public abstract AudioFileFormat.Type[] getWriterFileTypes(AudioInputStream stream);
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
    public abstract boolean isWriterSupportedFileType(AudioFileFormat.Type fileType, AudioInputStream stream);
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
    public abstract AudioFileFormat.Type getFileTypeByFormatName(String formatName);
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
    public abstract AudioFileFormat.Type getFileTypeBySuffix(String suffix);

}
