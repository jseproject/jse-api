# JavaSound Enhancement Project API

JSE API provides a collection of extended API for the JavaSound subsystem of the Java Platform.

## Compatibility
JDK 8+

## API
All the API are in 2 classes: [Audios](/src/main/java/javasound/sampled/Audios.java) and [Midis](/src/main/java/javasound/midi/Midis.java).
They're effectively replacements for `javax.sound.sampled.AudioSystem` and `javax.sound.midi.MidiSystem`, with more useful methods implemented by the backing SPI providers.

## SPI
### Sampled
- [AudioResourceReader](/src/main/java/javasound/sampled/spi/AudioResourceReader.java) is an interface to load sampled audio from resources (with a `java.lang.ClassLoader` and a pathname string).
- [AudioCompressionWriter](/src/main/java/javasound/sampled/spi/AudioCompressionWriter.java) is an interface to write audio files with specific `java.util.Map<String, Object> properties`.
- [FormatEncodingProvider](/src/main/java/javasound/sampled/spi/FormatEncodingProvider.java) is an interface to provide a "unified" or "standard" way to get or check which `javax.sound.sampled.AudioFormat.Encoding` and `javax.sound.sampled.AudioFileFormat.Type` are supported.

### Midi
- [MidiResourceReader](/src/main/java/javasound/midi/spi/MidiResourceReader.java) is an interface to load midi audio from resources (with a `java.lang.ClassLoader` and a pathname string).
- [SoundbankResourceReader](/src/main/java/javasound/midi/spi/SoundbankResourceReader.java) is an interface to load midi soundbank from resources (with a `java.lang.ClassLoader` and a pathname string).

## Installing
### Maven
```xml
<dependencies>
    <dependency>
        <groupId>io.github.jseproject</groupId>
        <artifactId>jse-api</artifactId>
        <version>1.1.0</version>
    </dependency>
</dependencies>
```
### Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.jseproject:jse-api:1.1.0'
}
```

## License
[BSD 3-Clause](/LICENSE)
