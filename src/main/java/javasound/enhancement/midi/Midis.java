package javasound.enhancement.midi;

import javasound.enhancement.midi.spi.MidiResourceReader;
import javasound.enhancement.midi.spi.SoundbankResourceReader;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.spi.MidiDeviceProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * The {@code Midis} class provides access to the installed MIDI system
 * resources, including devices such as synthesizers, sequencers, and MIDI input
 * and output ports. A typical simple MIDI application might begin by invoking
 * one or more {@code Midis} methods to learn what devices are installed
 * and to obtain the ones needed in that application.
 * <p>
 * The class also has methods for reading files, streams, and URLs that contain
 * standard MIDI file data or soundbanks. You can query the {@code Midis}
 * for the format of a specified MIDI file.
 * <p>
 * You cannot instantiate a {@code Midis}; all the methods are static.
 * <p>
 * Properties can be used to specify default MIDI devices. Both system
 * properties and a properties file are considered. The "sound.properties"
 * properties file is read from an implementation-specific location (typically
 * it is the {@code conf} directory in the Java installation directory).
 * The optional "javax.sound.config.file" system property can be used to specify
 * the properties file that will be read as the initial configuration. If a
 * property exists both as a system property and in the properties file, the
 * system property takes precedence. If none is specified, a suitable default is
 * chosen among the available devices. The syntax of the properties file is
 * specified in {@link Properties#load(InputStream) Properties.load}. The
 * following table lists the available property keys and which methods consider
 * them:
 *
 * <table class="striped">
 * <caption>MIDI System Property Keys</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Property Key
 *     <th scope="col">Interface
 *     <th scope="col">Affected Method
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">{@code javax.sound.midi.Receiver}
 *     <td>{@link Receiver}
 *     <td>{@link #getReceiver}
 *   <tr>
 *     <th scope="row">{@code javax.sound.midi.Sequencer}
 *     <td>{@link Sequencer}
 *     <td>{@link #getSequencer}
 *   <tr>
 *     <th scope="row">{@code javax.sound.midi.Synthesizer}
 *     <td>{@link Synthesizer}
 *     <td>{@link #getSynthesizer}
 *   <tr>
 *     <th scope="row">{@code javax.sound.midi.Transmitter}
 *     <td>{@link Transmitter}
 *     <td>{@link #getTransmitter}
 * </tbody>
 * </table>
 *
 * The property value consists of the provider class name and the device name,
 * separated by the hash mark ("#"). The provider class name is the
 * fully-qualified name of a concrete
 * {@link MidiDeviceProvider MIDI device provider} class. The device name is
 * matched against the {@code String} returned by the {@code getName} method of
 * {@code MidiDevice.Info}. Either the class name, or the device name may be
 * omitted. If only the class name is specified, the trailing hash mark is
 * optional.
 * <p>
 * If the provider class is specified, and it can be successfully retrieved from
 * the installed providers, the list of {@code MidiDevice.Info} objects is
 * retrieved from the provider. Otherwise, or when these devices do not provide
 * a subsequent match, the list is retrieved from {@link #getMidiDeviceInfo} to
 * contain all available {@code MidiDevice.Info} objects.
 * <p>
 * If a device name is specified, the resulting list of {@code MidiDevice.Info}
 * objects is searched: the first one with a matching name, and whose
 * {@code MidiDevice} implements the respective interface, will be returned. If
 * no matching {@code MidiDevice.Info} object is found, or the device name is
 * not specified, the first suitable device from the resulting list will be
 * returned. For Sequencer and Synthesizer, a device is suitable if it
 * implements the respective interface; whereas for Receiver and Transmitter, a
 * device is suitable if it implements neither Sequencer nor Synthesizer and
 * provides at least one Receiver or Transmitter, respectively.
 * <p>
 * For example, the property {@code javax.sound.midi.Receiver} with a value
 * {@code "com.sun.media.sound.MidiProvider#SunMIDI1"} will have the following
 * consequences when {@code getReceiver} is called: if the class
 * {@code com.sun.media.sound.MidiProvider} exists in the list of installed MIDI
 * device providers, the first {@code Receiver} device with name
 * {@code "SunMIDI1"} will be returned. If it cannot be found, the first
 * {@code Receiver} from that provider will be returned, regardless of name. If
 * there is none, the first {@code Receiver} with name {@code "SunMIDI1"} in the
 * list of all devices (as returned by {@code getMidiDeviceInfo}) will be
 * returned, or, if not found, the first {@code Receiver} that can be found in
 * the list of all devices is returned. If that fails, too, a
 * {@code MidiUnavailableException} is thrown.
 *
 * @author Naoko Mitsurugi
 */
public final class Midis {

    private Midis() {
        throw new AssertionError("No javasound.enhancement.midi.Midis instances for you!");
    }

    /**
     * Obtains an array of information objects representing the set of all MIDI
     * devices available on the system. A returned information object can then
     * be used to obtain the corresponding device object, by invoking
     * {@link #getMidiDevice(MidiDevice.Info) getMidiDevice}.
     *
     * @return an array of {@code MidiDevice.Info} objects, one for each
     *         installed MIDI device. If no such devices are installed, an array
     *         of length 0 is returned.
     */
    public static MidiDevice.Info[] getMidiDeviceInfo() {
        return MidiSystem.getMidiDeviceInfo();
    }

    /**
     * Obtains the requested MIDI device.
     *
     * @param  info a device information object representing the desired device
     * @return the requested device
     * @throws MidiUnavailableException if the requested device is not available
     *         due to resource restrictions
     * @throws IllegalArgumentException if the info object does not represent a
     *         MIDI device installed on the system
     * @throws NullPointerException if {@code info} is {@code null}
     * @see #getMidiDeviceInfo
     */
    public static MidiDevice getMidiDevice(MidiDevice.Info info)
            throws MidiUnavailableException, IllegalArgumentException {
        return MidiSystem.getMidiDevice(info);
    }

    /**
     * Obtains a MIDI receiver from an external MIDI port or other default
     * device. The returned receiver always implements the
     * {@code MidiDeviceReceiver} interface.
     * <p>
     * If the system property {@code javax.sound.midi.Receiver} is defined or it
     * is defined in the file "sound.properties", it is used to identify the
     * device that provides the default receiver. For details, refer to the
     * {@link MidiSystem class description}.
     * <p>
     * If a suitable MIDI port is not available, the Receiver is retrieved from
     * an installed synthesizer.
     * <p>
     * If a native receiver provided by the default device does not implement
     * the {@code MidiDeviceReceiver} interface, it will be wrapped in a wrapper
     * class that implements the {@code MidiDeviceReceiver} interface. The
     * corresponding {@code Receiver} method calls will be forwarded to the
     * native receiver.
     * <p>
     * If this method returns successfully, the {@link MidiDevice MidiDevice}
     * the {@code Receiver} belongs to is opened implicitly, if it is not
     * already open. It is possible to close an implicitly opened device by
     * calling {@link Receiver#close close} on the returned {@code Receiver}.
     * All open {@code Receiver} instances have to be closed in order to release
     * system resources hold by the {@code MidiDevice}. For a detailed
     * description of open/close behaviour see the class description of
     * {@link MidiDevice MidiDevice}.
     *
     * @return the default MIDI receiver
     * @throws MidiUnavailableException if the default receiver is not available
     *         due to resource restrictions, or no device providing receivers is
     *         installed in the system
     */
    public static Receiver getReceiver() throws MidiUnavailableException {
        return MidiSystem.getReceiver();
    }

    /**
     * Obtains a MIDI transmitter from an external MIDI port or other default
     * source. The returned transmitter always implements the
     * {@code MidiDeviceTransmitter} interface.
     * <p>
     * If the system property {@code javax.sound.midi.Transmitter} is defined or
     * it is defined in the file "sound.properties", it is used to identify the
     * device that provides the default transmitter. For details, refer to the
     * {@link MidiSystem class description}.
     * <p>
     * If a native transmitter provided by the default device does not implement
     * the {@code MidiDeviceTransmitter} interface, it will be wrapped in a
     * wrapper class that implements the {@code MidiDeviceTransmitter}
     * interface. The corresponding {@code Transmitter} method calls will be
     * forwarded to the native transmitter.
     * <p>
     * If this method returns successfully, the {@link MidiDevice MidiDevice}
     * the {@code Transmitter} belongs to is opened implicitly, if it is not
     * already open. It is possible to close an implicitly opened device by
     * calling {@link Transmitter#close close} on the returned
     * {@code Transmitter}. All open {@code Transmitter} instances have to be
     * closed in order to release system resources hold by the
     * {@code MidiDevice}. For a detailed description of open/close behaviour
     * see the class description of {@link MidiDevice MidiDevice}.
     *
     * @return the default MIDI transmitter
     * @throws MidiUnavailableException if the default transmitter is not
     *         available due to resource restrictions, or no device providing
     *         transmitters is installed in the system
     */
    public static Transmitter getTransmitter() throws MidiUnavailableException {
        return MidiSystem.getTransmitter();
    }

    /**
     * Obtains the default synthesizer.
     * <p>
     * If the system property {@code javax.sound.midi.Synthesizer} is defined or
     * it is defined in the file "sound.properties", it is used to identify the
     * default synthesizer. For details, refer to the
     * {@link MidiSystem class description}.
     *
     * @return the default synthesizer
     * @throws MidiUnavailableException if the synthesizer is not available due
     *         to resource restrictions, or no synthesizer is installed in the
     *         system
     */
    public static Synthesizer getSynthesizer() throws MidiUnavailableException {
        return MidiSystem.getSynthesizer();
    }

    /**
     * Obtains the default {@code Sequencer}, connected to a default device. The
     * returned {@code Sequencer} instance is connected to the default
     * {@code Synthesizer}, as returned by {@link #getSynthesizer}. If there is
     * no {@code Synthesizer} available, or the default {@code Synthesizer}
     * cannot be opened, the {@code sequencer} is connected to the default
     * {@code Receiver}, as returned by {@link #getReceiver}. The connection is
     * made by retrieving a {@code Transmitter} instance from the
     * {@code Sequencer} and setting its {@code Receiver}. Closing and
     * re-opening the sequencer will restore the connection to the default
     * device.
     * <p>
     * This method is equivalent to calling {@code getSequencer(true)}.
     * <p>
     * If the system property {@code javax.sound.midi.Sequencer} is defined or
     * it is defined in the file "sound.properties", it is used to identify the
     * default sequencer. For details, refer to the
     * {@link MidiSystem class description}.
     *
     * @return the default sequencer, connected to a default Receiver
     * @throws MidiUnavailableException if the sequencer is not available due to
     *         resource restrictions, or there is no {@code Receiver} available
     *         by any installed {@code MidiDevice}, or no sequencer is installed
     *         in the system
     * @see #getSequencer(boolean)
     * @see #getSynthesizer
     * @see #getReceiver
     */
    public static Sequencer getSequencer() throws MidiUnavailableException {
        return MidiSystem.getSequencer();
    }

    /**
     * Obtains the default {@code Sequencer}, optionally connected to a default
     * device.
     * <p>
     * If {@code connected} is true, the returned {@code Sequencer} instance is
     * connected to the default {@code Synthesizer}, as returned by
     * {@link #getSynthesizer}. If there is no {@code Synthesizer} available, or
     * the default {@code Synthesizer} cannot be opened, the {@code sequencer}
     * is connected to the default {@code Receiver}, as returned by
     * {@link #getReceiver}. The connection is made by retrieving a
     * {@code Transmitter} instance from the {@code Sequencer} and setting its
     * {@code Receiver}. Closing and re-opening the sequencer will restore the
     * connection to the default device.
     * <p>
     * If {@code connected} is false, the returned {@code Sequencer} instance is
     * not connected, it has no open {@code Transmitters}. In order to play the
     * sequencer on a MIDI device, or a {@code Synthesizer}, it is necessary to
     * get a {@code Transmitter} and set its {@code Receiver}.
     * <p>
     * If the system property {@code javax.sound.midi.Sequencer} is defined or
     * it is defined in the file "sound.properties", it is used to identify the
     * default sequencer. For details, refer to the
     * {@link MidiSystem class description}.
     *
     * @param  connected whether or not the returned {@code Sequencer} is
     *         connected to the default {@code Synthesizer}
     * @return the default sequencer
     * @throws MidiUnavailableException if the sequencer is not available due to
     *         resource restrictions, or no sequencer is installed in the
     *         system, or if {@code connected} is true, and there is no
     *         {@code Receiver} available by any installed {@code MidiDevice}
     * @see #getSynthesizer
     * @see #getReceiver
     */
    public static Sequencer getSequencer(boolean connected) throws MidiUnavailableException {
        return MidiSystem.getSequencer(connected);
    }

    /**
     * Constructs a MIDI sound bank by reading it from the specified stream. The
     * stream must point to a valid MIDI soundbank file. In general, MIDI
     * soundbank providers may need to read some data from the stream before
     * determining whether they support it. These parsers must be able to mark
     * the stream, read enough data to determine whether they support the
     * stream, and, if not, reset the stream's read pointer to its original
     * position. If the input stream does not support this, this method may fail
     * with an {@code IOException}.
     *
     * @param  stream the source of the sound bank data
     * @return the sound bank
     * @throws InvalidMidiDataException if the stream does not point to valid
     *         MIDI soundbank data recognized by the system
     * @throws IOException if an I/O error occurred when loading the soundbank
     * @throws NullPointerException if {@code stream} is {@code null}
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public static Soundbank getSoundbank(InputStream stream) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSoundbank(stream);
    }

    /**
     * Constructs a {@code Soundbank} by reading it from the specified URL. The
     * URL must point to a valid MIDI soundbank file.
     *
     * @param  url the source of the sound bank data
     * @return the sound bank
     * @throws InvalidMidiDataException if the URL does not point to valid MIDI
     *         soundbank data recognized by the system
     * @throws IOException if an I/O error occurred when loading the soundbank
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public static Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSoundbank(url);
    }

    /**
     * Constructs a {@code Soundbank} by reading it from the specified
     * {@code File}. The {@code File} must point to a valid MIDI soundbank file.
     *
     * @param  file the source of the sound bank data
     * @return the sound bank
     * @throws InvalidMidiDataException if the {@code File} does not point to
     *         valid MIDI soundbank data recognized by the system
     * @throws IOException if an I/O error occurred when loading the soundbank
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSoundbank(file);
    }

    /**
     * Obtains the MIDI file format of the data in the specified input stream.
     * The stream must point to valid MIDI file data for a file type recognized
     * by the system.
     * <p>
     * This method and/or the code it invokes may need to read some data from
     * the stream to determine whether its data format is supported. The
     * implementation may therefore need to mark the stream, read enough data to
     * determine whether it is in a supported format, and reset the stream's
     * read pointer to its original position. If the input stream does not
     * permit this set of operations, this method may fail with an
     * {@code IOException}.
     * <p>
     * This operation can only succeed for files of a type which can be parsed
     * by an installed file reader. It may fail with an
     * {@code InvalidMidiDataException} even for valid files if no compatible
     * file reader is installed. It will also fail with an
     * {@code InvalidMidiDataException} if a compatible file reader is
     * installed, but encounters errors while determining the file format.
     *
     * @param  stream the input stream from which file format information should
     *         be extracted
     * @return an {@code MidiFileFormat} object describing the MIDI file format
     * @throws InvalidMidiDataException if the stream does not point to valid
     *         MIDI file data recognized by the system
     * @throws IOException if an I/O exception occurs while accessing the stream
     * @throws NullPointerException if {@code stream} is {@code null}
     * @see #getMidiFileFormat(URL)
     * @see #getMidiFileFormat(File)
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public static MidiFileFormat getMidiFileFormat(InputStream stream) throws InvalidMidiDataException, IOException {
        return MidiSystem.getMidiFileFormat(stream);
    }

    /**
     * Obtains the MIDI file format of the data in the specified URL. The URL
     * must point to valid MIDI file data for a file type recognized by the
     * system.
     * <p>
     * This operation can only succeed for files of a type which can be parsed
     * by an installed file reader. It may fail with an
     * {@code InvalidMidiDataException} even for valid files if no compatible
     * file reader is installed. It will also fail with an
     * {@code InvalidMidiDataException} if a compatible file reader is
     * installed, but encounters errors while determining the file format.
     *
     * @param  url the URL from which file format information should be
     *         extracted
     * @return a {@code MidiFileFormat} object describing the MIDI file format
     * @throws InvalidMidiDataException if the URL does not point to valid MIDI
     *         file data recognized by the system
     * @throws IOException if an I/O exception occurs while accessing the URL
     * @throws NullPointerException if {@code url} is {@code null}
     * @see #getMidiFileFormat(InputStream)
     * @see #getMidiFileFormat(File)
     */
    public static MidiFileFormat getMidiFileFormat(URL url) throws InvalidMidiDataException, IOException {
        return MidiSystem.getMidiFileFormat(url);
    }

    /**
     * Obtains the MIDI file format of the specified {@code File}. The
     * {@code File} must point to valid MIDI file data for a file type
     * recognized by the system.
     * <p>
     * This operation can only succeed for files of a type which can be parsed
     * by an installed file reader. It may fail with an
     * {@code InvalidMidiDataException} even for valid files if no compatible
     * file reader is installed. It will also fail with an
     * {@code InvalidMidiDataException} if a compatible file reader is
     * installed, but encounters errors while determining the file format.
     *
     * @param  file the {@code File} from which file format information should
     *         be extracted
     * @return a {@code MidiFileFormat} object describing the MIDI file format
     * @throws InvalidMidiDataException if the {@code File} does not point to
     *         valid MIDI file data recognized by the system
     * @throws IOException if an I/O exception occurs while accessing the file
     * @throws NullPointerException if {@code file} is {@code null}
     * @see #getMidiFileFormat(InputStream)
     * @see #getMidiFileFormat(URL)
     */
    public static MidiFileFormat getMidiFileFormat(File file) throws InvalidMidiDataException, IOException {
        return MidiSystem.getMidiFileFormat(file);
    }

    /**
     * Obtains a MIDI sequence from the specified input stream. The stream must
     * point to valid MIDI file data for a file type recognized by the system.
     * <p>
     * This method and/or the code it invokes may need to read some data from
     * the stream to determine whether its data format is supported. The
     * implementation may therefore need to mark the stream, read enough data to
     * determine whether it is in a supported format, and reset the stream's
     * read pointer to its original position. If the input stream does not
     * permit this set of operations, this method may fail with an
     * {@code IOException}.
     * <p>
     * This operation can only succeed for files of a type which can be parsed
     * by an installed file reader. It may fail with an
     * {@code InvalidMidiDataException} even for valid files if no compatible
     * file reader is installed. It will also fail with an
     * {@code InvalidMidiDataException} if a compatible file reader is
     * installed, but encounters errors while constructing the {@code Sequence}
     * object from the file data.
     *
     * @param  stream the input stream from which the {@code Sequence} should be
     *         constructed
     * @return a {@code Sequence} object based on the MIDI file data contained
     *         in the input stream
     * @throws InvalidMidiDataException if the stream does not point to valid
     *         MIDI file data recognized by the system
     * @throws IOException if an I/O exception occurs while accessing the stream
     * @throws NullPointerException if {@code stream} is {@code null}
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public static Sequence getSequence(InputStream stream) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSequence(stream);
    }

    /**
     * Obtains a MIDI sequence from the specified URL. The URL must point to
     * valid MIDI file data for a file type recognized by the system.
     * <p>
     * This operation can only succeed for files of a type which can be parsed
     * by an installed file reader. It may fail with an
     * {@code InvalidMidiDataException} even for valid files if no compatible
     * file reader is installed. It will also fail with an
     * {@code InvalidMidiDataException} if a compatible file reader is
     * installed, but encounters errors while constructing the {@code Sequence}
     * object from the file data.
     *
     * @param  url the URL from which the {@code Sequence} should be constructed
     * @return a {@code Sequence} object based on the MIDI file data pointed to
     *         by the URL
     * @throws InvalidMidiDataException if the URL does not point to valid MIDI
     *         file data recognized by the system
     * @throws IOException if an I/O exception occurs while accessing the URL
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public static Sequence getSequence(URL url) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSequence(url);
    }

    /**
     * Obtains a MIDI sequence from the specified {@code File}. The {@code File}
     * must point to valid MIDI file data for a file type recognized by the
     * system.
     * <p>
     * This operation can only succeed for files of a type which can be parsed
     * by an installed file reader. It may fail with an
     * {@code InvalidMidiDataException} even for valid files if no compatible
     * file reader is installed. It will also fail with an
     * {@code InvalidMidiDataException} if a compatible file reader is
     * installed, but encounters errors while constructing the {@code Sequence}
     * object from the file data.
     *
     * @param  file the {@code File} from which the {@code Sequence} should be
     *         constructed
     * @return a {@code Sequence} object based on the MIDI file data pointed to
     *         by the File
     * @throws InvalidMidiDataException if the File does not point to valid MIDI
     *         file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static Sequence getSequence(File file) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSequence(file);
    }

    /**
     * Obtains the set of MIDI file types for which file writing support is
     * provided by the system.
     *
     * @return array of unique file types. If no file types are supported, an
     *         array of length 0 is returned.
     */
    public static int[] getWriterFileTypes() {
        return MidiSystem.getMidiFileTypes();
    }

    /**
     * Indicates whether file writing support for the specified MIDI file type
     * is provided by the system.
     *
     * @param  fileType the file type for which write capabilities are queried
     * @return {@code true} if the file type is supported, otherwise
     *         {@code false}
     */
    public static boolean isWriterSupportedFileType(int fileType) {
        return MidiSystem.isFileTypeSupported(fileType);
    }

    /**
     * Obtains the set of MIDI file types that the system can write from the
     * sequence specified.
     *
     * @param  sequence the sequence for which MIDI file type support is queried
     * @return the set of unique supported file types. If no file types are
     *         supported, returns an array of length 0.
     * @throws NullPointerException if {@code sequence} is {@code null}
     */
    public static int[] getWriterFileTypes(Sequence sequence) {
        return MidiSystem.getMidiFileTypes(sequence);
    }

    /**
     * Indicates whether a MIDI file of the file type specified can be written
     * from the sequence indicated.
     *
     * @param  fileType the file type for which write capabilities are queried
     * @param  sequence the sequence for which file writing support is queried
     * @return {@code true} if the file type is supported for this sequence,
     *         otherwise {@code false}
     * @throws NullPointerException if {@code sequence} is {@code null}
     */
    public static boolean isWriterSupportedFileType(int fileType, Sequence sequence) {
        return MidiSystem.isFileTypeSupported(fileType, sequence);
    }

    /**
     * Writes a stream of bytes representing a file of the MIDI file type
     * indicated to the output stream provided.
     *
     * @param  in sequence containing MIDI data to be written to the file
     * @param  fileType the file type of the file to be written to the output
     *         stream
     * @param  out stream to which the file data should be written
     * @return the number of bytes written to the output stream
     * @throws IOException if an I/O exception occurs
     * @throws IllegalArgumentException if the file format is not supported by
     *         the system
     * @throws NullPointerException if {@code in} or {@code out} are
     *         {@code null}
     * @see #isWriterSupportedFileType(int, Sequence)
     * @see #getWriterFileTypes(Sequence)
     */
    public static int write(Sequence in, int fileType, OutputStream out) throws IOException, IllegalArgumentException {
        return MidiSystem.write(in, fileType, out);
    }

    /**
     * Writes a stream of bytes representing a file of the MIDI file type
     * indicated to the external file provided.
     *
     * @param  in sequence containing MIDI data to be written to the file
     * @param  fileType the file type of the file to be written to the output stream
     * @param  out external file to which the file data should be written
     * @return the number of bytes written to the file
     * @throws IOException if an I/O exception occurs
     * @throws IllegalArgumentException if the file type is not supported by the
     *         system
     * @throws NullPointerException if {@code in} or {@code out} are
     *         {@code null}
     * @see #isWriterSupportedFileType(int, Sequence)
     * @see #getWriterFileTypes(Sequence)
     */
    public static int write(Sequence in, int fileType, File out) throws IOException, IllegalArgumentException {
        return MidiSystem.write(in, fileType, out);
    }

    /**
     * Obtains the MIDI file format of the data in the specified resource. The resource
     * must point to valid MIDI file data for a file type recognized
     * by the system.
     * <p>
     * This operation can only succeed for files of a type which can be parsed
     * by an installed file reader.  It may fail with an InvalidMidiDataException
     * even for valid files if no compatible file reader is installed.  It
     * will also fail with an InvalidMidiDataException if a compatible file reader
     * is installed, but encounters errors while determining the file format.
     *
     * @param  resourceLoader the {@code ClassLoader} to load resource
     * @param  name the resource name from which file format information should be
     * extracted
     * @return a <code>MidiFileFormat</code> object describing the MIDI file
     * format
     * @throws InvalidMidiDataException if the resource does not point to valid MIDI
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs while accessing the resource
     */
    public static MidiFileFormat getMidiFileFormat(ClassLoader resourceLoader, String name) throws InvalidMidiDataException, IOException {
        List<MidiResourceReader> providers = getMidiResourceReaders();
        MidiFileFormat format = null;
        for (MidiResourceReader reader : providers) {
            try {
                format = reader.getMidiFileFormat(resourceLoader, name); // throws IOException
                break;
            }
            catch (InvalidMidiDataException ignored) {
            }
        }
        if (format == null) throw new InvalidMidiDataException("resource is not a supported file type");
        else return format;
    }

    /**
     * Obtains a MIDI sequence from the specified resource. The resource must
     * point to valid MIDI file data for a file type recognized
     * by the system.
     * <p>
     * This operation can only succeed for files of a type which can be parsed
     * by an installed file reader.  It may fail with an InvalidMidiDataException
     * even for valid files if no compatible file reader is installed.  It
     * will also fail with an InvalidMidiDataException if a compatible file reader
     * is installed, but encounters errors while constructing the <code>Sequence</code>
     * object from the file data.
     *
     * @param  resourceLoader the {@code ClassLoader} to load resource
     * @param  name the resource name from which the <code>Sequence</code> should be
     * constructed
     * @return a <code>Sequence</code> object based on the MIDI file data
     * pointed to by the resource
     * @throws InvalidMidiDataException if the resource does not point to valid MIDI
     * file data recognized by the system
     * @throws IOException if an I/O exception occurs while accessing the resource
     */
    public static Sequence getSequence(ClassLoader resourceLoader, String name) throws InvalidMidiDataException, IOException {
        List<MidiResourceReader> providers = getMidiResourceReaders();
        Sequence sequence = null;
        for (MidiResourceReader reader : providers) {
            try {
                sequence = reader.getSequence(resourceLoader, name); // throws IOException
                break;
            } catch (InvalidMidiDataException ignored) {
            }
        }
        if (sequence == null) throw new InvalidMidiDataException("could not get sequence from resource");
        else return sequence;
    }

    /**
     * Constructs a <code>Soundbank</code> by reading it from the specified resource.
     * The resource must point to a valid MIDI soundbank file.
     *
     * @param  resourceLoader the {@code ClassLoader} to load resource
     * @param  name the source of the sound bank data
     * @return the sound bank
     * @throws InvalidMidiDataException if the resource does not point to valid MIDI
     * soundbank data recognized by the system
     * @throws IOException if an I/O error occurred when loading the soundbank
     */
    public static Soundbank getSoundbank(ClassLoader resourceLoader, String name) throws InvalidMidiDataException, IOException {
        SoundbankResourceReader sp;
        Soundbank s;
        List<SoundbankResourceReader> providers = getSoundbankResourceReaders();
        for (SoundbankResourceReader provider : providers) {
            sp = provider;
            s = sp.getSoundbank(resourceLoader, name);
            if (s != null) return s;
        }
        throw new InvalidMidiDataException("cannot get soundbank from resource");

    }

    private static List<SoundbankResourceReader> getSoundbankResourceReaders() {
        return getProviders(SoundbankResourceReader.class);
    }

    private static List<MidiResourceReader> getMidiResourceReaders() {
        return getProviders(MidiResourceReader.class);
    }

    private static <T> List<T> getProviders(Class<T> providerClass) {
        List<T> providers = new ArrayList<>();
        for (T t : ServiceLoader.load(providerClass)) {
            if (providerClass.isInstance(t)) providers.add(t);
        }
        return providers;
    }

}
